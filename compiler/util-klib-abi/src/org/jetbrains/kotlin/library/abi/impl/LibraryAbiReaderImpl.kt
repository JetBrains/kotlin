/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind.CLASS_SYMBOL
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignature.*
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.compact
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedMapNotNull
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDefinitelyNotNullType as ProtoIrDefinitelyNotNullType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleTypeNullability as ProtoSimpleTypeNullability
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleTypeLegacy as ProtoSimpleTypeLegacy
import org.jetbrains.kotlin.backend.common.serialization.IrFlags as ProtoFlags

internal class LibraryAbiReaderImpl(libraryFile: File) {
    private val library = resolveSingleFileKlib(KFile(libraryFile.absolutePath))

    fun readAbi(): LibraryAbi {
        val supportedSignatureVersions = readSupportedSignatureVersions()

        return LibraryAbi(
            manifest = readManifest(),
            supportedSignatureVersions = supportedSignatureVersions,
            topLevelDeclarations = LibraryDeserializer(library, supportedSignatureVersions).deserialize()
        )
    }

    private fun readManifest(): LibraryManifest {
        val versions = library.versions
        return LibraryManifest(
            uniqueName = library.uniqueName,
            platform = library.builtInsPlatform,
            nativeTargets = library.nativeTargets.sorted(),
            compilerVersion = versions.compilerVersion,
            abiVersion = versions.abiVersion?.toString(),
            libraryVersion = versions.libraryVersion,
            irProviderName = library.irProviderName
        )
    }

    private fun readSupportedSignatureVersions(): Set<AbiSignatureVersion> {
        fun resolveSignatureVersion(signatureVersion: String): AbiSignatureVersion =
            AbiSignatureVersion.entries.firstOrNull { it.alias == signatureVersion }
                ?: error("Unsupported signature version: $signatureVersion")

        val signatureVersions = library.signatureVersions
        return if (signatureVersions.isNotEmpty())
            signatureVersions.mapTo(hashSetOf()) { signatureVersion -> resolveSignatureVersion(signatureVersion.lowercase()) }
        else
            setOf(AbiSignatureVersion.V1) // The default one.
    }
}

private class LibraryDeserializer(private val library: KotlinLibrary, supportedSignatureVersions: Set<AbiSignatureVersion>) {
    private val interner = IrInterningService()
    private val needV1Signatures = AbiSignatureVersion.V1 in supportedSignatureVersions
    private val needV2Signatures = AbiSignatureVersion.V2 in supportedSignatureVersions

    inner class FileDeserializer(fileIndex: Int) {
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val topLevelDeclarationIds: List<Int>
        private val signatureDeserializer: IdSignatureDeserializer
        private val typeDeserializer: TypeDeserializer

        init {
            val proto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, IrLibraryFileFromBytes.extensionRegistryLite)
            topLevelDeclarationIds = proto.declarationIdList

            val fileSignature = FileSignature(
                id = Any(), // Just an unique object.
                fqName = FqName(fileReader.deserializeFqName(proto.fqNameList)),
                fileName = if (proto.hasFileEntry() && proto.fileEntry.hasName()) proto.fileEntry.name else "<unknown>"
            )
            signatureDeserializer = IdSignatureDeserializer(fileReader, fileSignature, interner)
            typeDeserializer = TypeDeserializer(fileReader, signatureDeserializer)
        }

        fun deserializeTo(output: MutableList<AbiDeclaration>) {
            topLevelDeclarationIds.mapNotNullTo(output) { topLevelDeclarationId ->
                deserializeDeclaration(fileReader.declaration(topLevelDeclarationId), containingClassModality = null)
            }
        }

        private fun deserializeDeclaration(proto: ProtoDeclaration, containingClassModality: AbiModality?): AbiDeclaration? =
            when (proto.declaratorCase) {
                ProtoDeclaration.DeclaratorCase.IR_CLASS -> deserializeClass(proto.irClass)
                ProtoDeclaration.DeclaratorCase.IR_CONSTRUCTOR -> deserializeFunction(
                    proto.irConstructor.base,
                    containingClassModality,
                    isConstructor = true
                )
                ProtoDeclaration.DeclaratorCase.IR_FUNCTION -> deserializeFunction(proto.irFunction.base, containingClassModality)
                ProtoDeclaration.DeclaratorCase.IR_PROPERTY -> deserializeProperty(proto.irProperty, containingClassModality)
                ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY -> deserializeEnumEntry(proto.irEnumEntry)
                else -> null
            }

        private fun deserializeClass(proto: ProtoClass): AbiClass? {
            if (!getVisibilityStatus(proto.base).isPubliclyVisible)
                return null

            val flags = ClassFlags.decode(proto.base.flags)
            val modality = deserializeModality(flags.modality, containingClassModality = null)

            val memberDeclarations = proto.declarationList.memoryOptimizedMapNotNull { declaration ->
                deserializeDeclaration(declaration, containingClassModality = modality)
            }

            return AbiClassImpl(
                name = deserializeName(proto.name),
                signatures = deserializeSignatures(proto.base),
                modality = modality,
                kind = when (val kind = flags.kind) {
                    ClassKind.CLASS -> AbiClassKind.CLASS
                    ClassKind.INTERFACE -> AbiClassKind.INTERFACE
                    ClassKind.OBJECT -> AbiClassKind.OBJECT
                    ClassKind.ENUM_CLASS -> AbiClassKind.ENUM_CLASS
                    ClassKind.ANNOTATION_CLASS -> AbiClassKind.ANNOTATION_CLASS
                    ClassKind.ENUM_ENTRY -> error("Unexpected class kind: $kind")
                },
                isInner = flags.isInner,
                isValue = flags.isValue,
                isFunction = flags.isFun,
                superTypes = deserializeSuperTypes(proto),
                declarations = memberDeclarations
            )
        }

        private fun deserializeSuperTypes(proto: ProtoClass): List<AbiType> {
            val superTypeIds: List<Int> = proto.superTypeList
            if (superTypeIds.isEmpty())
                return emptyList()

            return proto.superTypeList.memoryOptimizedMapNotNull { typeId ->
                typeDeserializer.deserializePubliclyVisibleType(typeId)?.takeUnless { it.isKotlinAny() }
            }
        }

        private fun deserializeEnumEntry(proto: ProtoEnumEntry): AbiEnumEntry {
            return AbiEnumEntryImpl(
                name = deserializeName(proto.name),
                signatures = deserializeSignatures(proto.base)
            )
        }

        /**
         * @property parentVisibilityStatus - visibility status of the immediate containing declaration. At the moment, needs to
         *   be taken into account only for getter/setter with own 'internal' visibility to determine if it is publicly visible.
         * @property containingClassModality - used to compute the effective modality of the member function declaration.
         */
        private fun deserializeFunction(
            proto: ProtoFunctionBase,
            containingClassModality: AbiModality?,
            parentVisibilityStatus: VisibilityStatus? = null,
            isConstructor: Boolean = false,
        ): AbiFunction? {
            if (!getVisibilityStatus(proto.base, parentVisibilityStatus).isPubliclyVisible)
                return null

            val flags = FunctionFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            return AbiFunctionImpl(
                name = deserializeName(BinaryNameAndType.decode(proto.nameType).nameIndex),
                signatures = deserializeSignatures(proto.base),
                modality = deserializeModality(flags.modality, containingClassModality),
                isConstructor = isConstructor,
                isInline = flags.isInline,
                isSuspend = flags.isSuspend,
                valueParameters = deserializeValueParameters(proto)
            )
        }

        /**
         * @property containingClassModality - used to compute the effective modality of the member function declaration.
         */
        private fun deserializeProperty(
            proto: ProtoProperty,
            containingClassModality: AbiModality?
        ): AbiProperty? {
            val visibilityStatus = getVisibilityStatus(proto.base)
            if (!visibilityStatus.isPubliclyVisible)
                return null

            val flags = PropertyFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            return AbiPropertyImpl(
                name = deserializeName(proto.name),
                signatures = deserializeSignatures(proto.base),
                modality = deserializeModality(flags.modality, containingClassModality),
                kind = when {
                    flags.isConst -> AbiPropertyKind.CONST_VAL
                    flags.isVar -> AbiPropertyKind.VAR
                    else -> AbiPropertyKind.VAL
                },
                getter = if (proto.hasGetter())
                    deserializeFunction(proto.getter.base, containingClassModality, parentVisibilityStatus = visibilityStatus)
                else null,
                setter = if (proto.hasSetter())
                    deserializeFunction(proto.setter.base, containingClassModality, parentVisibilityStatus = visibilityStatus)
                else null
            )
        }

        private fun deserializeName(nameId: Int): String {
            return interner.string(fileReader.string(nameId))
        }

        private fun deserializeSignatures(proto: ProtoDeclarationBase): AbiSignatures {
            val signature = deserializeIdSignature(proto.symbol)

            return AbiSignaturesImpl(
                signatureV1 = if (needV1Signatures) signature.render(IdSignatureRenderer.LEGACY) else null,
                signatureV2 = if (needV2Signatures) signature.render(IdSignatureRenderer.DEFAULT) else null
            )
        }

        private fun getVisibilityStatus(proto: ProtoDeclarationBase, parentVisibilityStatus: VisibilityStatus? = null): VisibilityStatus =
            when (ProtoFlags.VISIBILITY.get(proto.flags.toInt())) {
                ProtoBuf.Visibility.PUBLIC, ProtoBuf.Visibility.PROTECTED -> VisibilityStatus.PUBLIC
                ProtoBuf.Visibility.INTERNAL -> when {
                    parentVisibilityStatus == VisibilityStatus.INTERNAL_PUBLISHED_API -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    proto.annotationList.any { deserializeIdSignature(it.symbol).isPublishedApi() } -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    else -> VisibilityStatus.NON_PUBLIC
                }
                else -> VisibilityStatus.NON_PUBLIC
            }

        private fun deserializeModality(modality: Modality, containingClassModality: AbiModality?): AbiModality = when (modality) {
            Modality.FINAL -> AbiModality.FINAL
            Modality.OPEN -> if (containingClassModality == AbiModality.FINAL) AbiModality.FINAL else AbiModality.OPEN
            Modality.ABSTRACT -> AbiModality.ABSTRACT
            Modality.SEALED -> AbiModality.SEALED
        }

        private fun deserializeIdSignature(symbolId: Long): IdSignature {
            val signatureId = BinarySymbolData.decode(symbolId).signatureId
            return signatureDeserializer.deserializeIdSignature(signatureId)
        }

        private fun deserializeValueParameters(proto: ProtoFunctionBase): List<AbiValueParameter> {
            val allValueParameters: List<ProtoValueParameter> = proto.valueParameterList
            if (allValueParameters.isEmpty())
                return emptyList()

            val contextReceiverParametersCount = if (proto.hasContextReceiverParametersCount()) proto.contextReceiverParametersCount else 0

            val valueParameterRange = contextReceiverParametersCount until allValueParameters.size
            if (valueParameterRange.isEmpty())
                return emptyList()

            return valueParameterRange.toList().memoryOptimizedMap { index ->
                val valueParameter = allValueParameters[index]
                val flags = ValueParameterFlags.decode(valueParameter.base.flags)

                AbiValueParameterImpl(
                    hasDefaultArg = valueParameter.hasDefaultValue(),
                    isNoinline = flags.isNoInline,
                    isCrossinline = flags.isCrossInline
                )
            }
        }
    }

    private class TypeDeserializer(
        private val libraryFile: IrLibraryFile,
        private val signatureDeserializer: IdSignatureDeserializer
    ) {
        private val cache = HashMap</* type id */ Int, AbiType?>()

        fun deserializePubliclyVisibleType(typeId: Int): AbiType? {
            return cache.computeIfAbsent(typeId) {
                val proto = libraryFile.type(typeId)
                when (val kindCase = proto.kindCase) {
                    ProtoType.KindCase.DNN -> deserializeDefinitelyNotNullType(proto.dnn)
                    ProtoType.KindCase.SIMPLE -> deserializeSimpleType(proto.simple)
                    ProtoType.KindCase.LEGACYSIMPLE -> deserializeSimpleType(proto.legacySimple)
                    ProtoType.KindCase.DYNAMIC -> DynamicTypeImpl
                    ProtoType.KindCase.ERROR -> ErrorTypeImpl
                    ProtoType.KindCase.KIND_NOT_SET -> error("Unexpected IR type: $kindCase")
                }
            }
        }

        private fun deserializeDefinitelyNotNullType(proto: ProtoIrDefinitelyNotNullType): AbiType? {
            assert(proto.typesCount == 1) { "Only DefinitelyNotNull type is now supported" }

            val underlyingType = deserializePubliclyVisibleType(proto.getTypes(0))
            return if (underlyingType is AbiType.Simple && underlyingType.nullability != AbiTypeNullability.DEFINITELY_NOT_NULL)
                SimpleTypeImpl(underlyingType.classifier, underlyingType.arguments, AbiTypeNullability.DEFINITELY_NOT_NULL)
            else
                underlyingType
        }

        private fun deserializeSimpleType(proto: ProtoSimpleType): AbiType.Simple? = deserializeSimpleType(
            symbolId = proto.classifier,
            typeArgumentIds = proto.argumentList,
            nullability = if (proto.hasNullability()) {
                when (proto.nullability!!) {
                    ProtoSimpleTypeNullability.MARKED_NULLABLE -> AbiTypeNullability.MARKED_NULLABLE
                    ProtoSimpleTypeNullability.NOT_SPECIFIED -> AbiTypeNullability.NOT_SPECIFIED
                    ProtoSimpleTypeNullability.DEFINITELY_NOT_NULL -> AbiTypeNullability.DEFINITELY_NOT_NULL
                }
            } else AbiTypeNullability.NOT_SPECIFIED
        )

        private fun deserializeSimpleType(proto: ProtoSimpleTypeLegacy): AbiType.Simple? = deserializeSimpleType(
            symbolId = proto.classifier,
            typeArgumentIds = proto.argumentList,
            nullability = if (proto.hasHasQuestionMark() && proto.hasQuestionMark)
                AbiTypeNullability.MARKED_NULLABLE
            else
                AbiTypeNullability.NOT_SPECIFIED
        )

        private fun deserializeSimpleType(
            symbolId: Long,
            typeArgumentIds: List<Long>,
            nullability: AbiTypeNullability
        ): AbiType.Simple? {
            val symbolData = BinarySymbolData.decode(symbolId)
            val signature = signatureDeserializer.deserializeIdSignature(symbolData.signatureId)
            val symbolKind = symbolData.kind

            return when {
                symbolKind == CLASS_SYMBOL && signature is CommonSignature -> {
                    // Publicly visible class or interface.
                    SimpleTypeImpl(
                        classifier = ClassImpl(
                            className = with(signature) { "$packageFqName/$declarationFqName" }
                        ),
                        arguments = deserializeTypeArguments(typeArgumentIds),
                        nullability = nullability
                    )
                }

                symbolKind == CLASS_SYMBOL && signature is CompositeSignature && signature.container is FileSignature -> {
                    // Non-publicly visible classifier. Practically, this can be a private top-level interface that some
                    // public class inherits.
                    null
                }

                symbolKind == TYPE_PARAMETER_SYMBOL && signature is CompositeSignature -> {
                    // A type-parameter.
                    SimpleTypeImpl(
                        classifier = TypeParameterImpl(
                            declaringClassName = with(signature.container as CommonSignature) { "$packageFqName/$declarationFqName" },
                            index = (signature.inner as LocalSignature).index()
                        ),
                        arguments = emptyList(),
                        nullability = nullability
                    )
                }

                else -> error("Unexpected combination of symbol kind ($symbolKind) and a signature: ${signature::class.java}, ${signature.render()}")
            }
        }

        private fun deserializeTypeArguments(typeArgumentIds: List<Long>): List<AbiTypeArgument> {
            if (typeArgumentIds.isEmpty())
                return emptyList()

            return typeArgumentIds.memoryOptimizedMap { typeArgumentId ->
                val typeProjection = BinaryTypeProjection.decode(typeArgumentId)
                if (typeProjection.isStarProjection)
                    StarProjectionImpl
                else
                    RegularProjectionImpl(
                        type = deserializePubliclyVisibleType(typeProjection.typeIndex)
                            ?: /* Normally, this should not happen. */ ErrorTypeImpl,
                        projectionKind = when (typeProjection.variance) {
                            Variance.INVARIANT -> AbiVariance.INVARIANT
                            Variance.IN_VARIANCE -> AbiVariance.IN_VARIANCE
                            Variance.OUT_VARIANCE -> AbiVariance.OUT_VARIANCE
                        }
                    )
            }
        }
    }

    fun deserialize(): AbiTopLevelDeclarations {
        val topLevels = ArrayList<AbiDeclaration>()

        for (fileIndex in 0 until library.fileCount()) {
            FileDeserializer(fileIndex).deserializeTo(topLevels)
        }

        return AbiTopLevelDeclarationsImpl(topLevels.compact())
    }

    private enum class VisibilityStatus(val isPubliclyVisible: Boolean) {
        PUBLIC(true), INTERNAL_PUBLISHED_API(true), NON_PUBLIC(false)
    }

    companion object {
        private const val PUBLISHED_API_PACKAGE_NAME = "kotlin"
        private const val PUBLISHED_API_DECLARATION_NAME = "PublishedApi.<init>"

        private fun IdSignature.isPublishedApi(): Boolean =
            this is CommonSignature
                    && packageFqName == PUBLISHED_API_PACKAGE_NAME
                    && declarationFqName == PUBLISHED_API_DECLARATION_NAME

        private fun AbiType.isKotlinAny(): Boolean =
            this is AbiType.Simple && (classifier as? AbiClassifier.Class)?.className == "kotlin/Any"
    }
}
