/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
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
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.*
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
import org.jetbrains.kotlin.backend.common.serialization.proto.IrConstructorCall as ProtoConstructorCall
import org.jetbrains.kotlin.backend.common.serialization.proto.IrType as ProtoType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDefinitelyNotNullType as ProtoIrDefinitelyNotNullType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleType as ProtoSimpleType
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleTypeNullability as ProtoSimpleTypeNullability
import org.jetbrains.kotlin.backend.common.serialization.proto.IrSimpleTypeLegacy as ProtoSimpleTypeLegacy
import org.jetbrains.kotlin.backend.common.serialization.IrFlags as ProtoFlags

@ExperimentalLibraryAbiReader
internal class LibraryAbiReaderImpl(libraryFile: File, filters: List<AbiReadingFilter>) {
    private val library = resolveSingleFileKlib(
        KFile(libraryFile.absolutePath),
        strategy = ToolingSingleFileKlibResolveStrategy
    )

    private val compositeFilter: AbiReadingFilter.Composite? = if (filters.isNotEmpty()) AbiReadingFilter.Composite(filters) else null

    fun readAbi(): LibraryAbi {
        val supportedSignatureVersions = readSupportedSignatureVersions()

        return LibraryAbi(
            manifest = readManifest(),
            uniqueName = library.uniqueName,
            signatureVersions = supportedSignatureVersions,
            topLevelDeclarations = LibraryDeserializer(library, supportedSignatureVersions, compositeFilter).deserialize()
        )
    }

    private fun readManifest(): LibraryManifest {
        val versions = library.versions
        return LibraryManifest(
            platform = library.builtInsPlatform,
            nativeTargets = library.nativeTargets.sorted(),
            compilerVersion = versions.compilerVersion,
            abiVersion = versions.abiVersion?.toString(),
            libraryVersion = versions.libraryVersion,
            irProviderName = library.irProviderName
        )
    }

    private fun readSupportedSignatureVersions(): Set<AbiSignatureVersion> {
        return library.versions.irSignatureVersions.mapTo(hashSetOf()) { AbiSignatureVersions.resolveByVersionNumber(it.number) }
    }
}

@ExperimentalLibraryAbiReader
private class LibraryDeserializer(
    private val library: KotlinLibrary,
    supportedSignatureVersions: Set<AbiSignatureVersion>,
    private val compositeFilter: AbiReadingFilter.Composite?
) {
    private val interner = IrInterningService()

    private val annotationsInterner = object {
        private val uniqueAnnotationClassNames = ObjectOpenHashSet<AbiQualifiedName>()
        fun intern(annotationClassName: AbiQualifiedName): AbiQualifiedName = uniqueAnnotationClassNames.addOrGet(annotationClassName)
    }

    private val needV1Signatures = AbiSignatureVersions.Supported.V1 in supportedSignatureVersions
    private val needV2Signatures = AbiSignatureVersions.Supported.V2 in supportedSignatureVersions

    private fun <T : AbiDeclaration> T?.discardIfExcluded(): T? =
        if (this != null && compositeFilter?.isDeclarationExcluded(this) == true) null else this

    private inner class FileDeserializer(fileIndex: Int) {
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val packageName: AbiCompoundName
        private val topLevelDeclarationIds: List<Int>
        private val signatureDeserializer: IdSignatureDeserializer
        private val typeDeserializer: TypeDeserializer

        init {
            val proto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, IrLibraryFileFromBytes.extensionRegistryLite)
            topLevelDeclarationIds = proto.declarationIdList

            val packageFQN = fileReader.deserializeFqName(proto.fqNameList)
            packageName = AbiCompoundName(packageFQN)

            val fileSignature = FileSignature(
                id = Any(), // Just an unique object.
                fqName = FqName(packageFQN),
                fileName = if (proto.hasFileEntry() && proto.fileEntry.hasName()) proto.fileEntry.name else "<unknown>"
            )
            signatureDeserializer = IdSignatureDeserializer(fileReader, fileSignature, interner)
            typeDeserializer = TypeDeserializer(fileReader, signatureDeserializer)
        }

        fun deserializeTo(output: MutableList<AbiDeclaration>) {
            if (compositeFilter?.isPackageExcluded(packageName) == true)
                return

            topLevelDeclarationIds.mapNotNullTo(output) { topLevelDeclarationId ->
                deserializeDeclaration(
                    proto = fileReader.declaration(topLevelDeclarationId),
                    containingEntity = ContainingEntity.Package(packageName)
                )
            }
        }

        private fun deserializeDeclaration(proto: ProtoDeclaration, containingEntity: ContainingEntity): AbiDeclaration? =
            when (proto.declaratorCase) {
                ProtoDeclaration.DeclaratorCase.IR_CLASS -> deserializeClass(proto.irClass, containingEntity)
                ProtoDeclaration.DeclaratorCase.IR_CONSTRUCTOR -> deserializeFunction(
                    proto.irConstructor.base,
                    containingEntity,
                    isConstructor = true
                )
                ProtoDeclaration.DeclaratorCase.IR_FUNCTION -> deserializeFunction(proto.irFunction.base, containingEntity)
                ProtoDeclaration.DeclaratorCase.IR_PROPERTY -> deserializeProperty(proto.irProperty, containingEntity)
                ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY -> deserializeEnumEntry(proto.irEnumEntry, containingEntity)
                else -> null
            }.discardIfExcluded()

        private fun deserializeClass(proto: ProtoClass, containingEntity: ContainingEntity): AbiClass? {
            val annotations = deserializeAnnotations(proto.base)
            val containingClassModality = (containingEntity as? ContainingEntity.Class)?.modality
            if (!computeVisibilityStatus(proto.base, annotations, containingClassModality).isPubliclyVisible)
                return null

            val flags = ClassFlags.decode(proto.base.flags)
            val modality = deserializeModality(
                flags.modality,
                containingClassModality = /* Open nested classes in final class remain open. */ null
            )

            val qualifiedName = deserializeQualifiedName(proto.name, containingEntity)
            val thisClassEntity = ContainingEntity.Class(qualifiedName, modality)

            val memberDeclarations = proto.declarationList.memoryOptimizedMapNotNull { declaration ->
                deserializeDeclaration(declaration, containingEntity = thisClassEntity)
            }

            return AbiClassImpl(
                qualifiedName = qualifiedName,
                signatures = deserializeSignatures(proto.base),
                annotations = annotations,
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
                typeDeserializer.deserializeType(typeId).takeUnless { type ->
                    isKotlinBuiltInType(type, KOTLIN_ANY_QUALIFIED_NAME) || !typeDeserializer.isPubliclyVisible(type)
                }
            }
        }

        private fun deserializeEnumEntry(proto: ProtoEnumEntry, containingEntity: ContainingEntity): AbiEnumEntry {
            return AbiEnumEntryImpl(
                qualifiedName = deserializeQualifiedName(proto.name, containingEntity),
                signatures = deserializeSignatures(proto.base),
                annotations = deserializeAnnotations(proto.base)
            )
        }

        private fun deserializeFunction(
            proto: ProtoFunctionBase,
            containingEntity: ContainingEntity,
            isConstructor: Boolean = false,
        ): AbiFunction? {
            val annotations = deserializeAnnotations(proto.base)

            val containingClassModality = when (containingEntity) {
                is ContainingEntity.Class -> containingEntity.modality
                is ContainingEntity.Property -> if (isConstructor) null else containingEntity.containingClassModality
                is ContainingEntity.Package -> null
            }

            val parentVisibilityStatus = (containingEntity as? ContainingEntity.Property)?.propertyVisibilityStatus
            if (!computeVisibilityStatus(proto.base, annotations, containingClassModality, parentVisibilityStatus).isPubliclyVisible)
                return null

            val flags = FunctionFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            val extensionReceiver = if (proto.hasExtensionReceiver()) deserializeValueParameter(proto.extensionReceiver) else null
            val contextReceiversCount = if (proto.hasContextReceiverParametersCount()) proto.contextReceiverParametersCount else 0

            val allValueParameters = ArrayList<AbiValueParameter>()
            allValueParameters.addIfNotNull(extensionReceiver)
            proto.valueParameterList.mapTo(allValueParameters, ::deserializeValueParameter)

            val nameAndType = BinaryNameAndType.decode(proto.nameType)
            val functionName = deserializeQualifiedName(
                nameId = nameAndType.nameIndex,
                containingEntity = containingEntity
            )

            val nonTrivialReturnType = if (isConstructor) {
                // Don't show the return type for constructors.
                null
            } else {
                // Show only a non-trivial return type for the others.
                typeDeserializer.deserializeType(nameAndType.typeIndex).takeUnless { isKotlinBuiltInType(it, KOTLIN_UNIT_QUALIFIED_NAME) }
            }

            return AbiFunctionImpl(
                qualifiedName = functionName,
                signatures = deserializeSignatures(proto.base),
                annotations = annotations,
                modality = deserializeModality(flags.modality, containingClassModality),
                isConstructor = isConstructor,
                isInline = flags.isInline,
                isSuspend = flags.isSuspend,
                hasExtensionReceiver = extensionReceiver != null,
                contextReceiverParametersCount = contextReceiversCount,
                valueParameters = allValueParameters.compact(),
                returnType = nonTrivialReturnType
            )
        }

        private fun deserializeProperty(proto: ProtoProperty, containingEntity: ContainingEntity): AbiProperty? {
            val annotations = deserializeAnnotations(proto.base)
            val containingClassModality = (containingEntity as? ContainingEntity.Class)?.modality

            val visibilityStatus = computeVisibilityStatus(proto.base, annotations, containingClassModality)
            if (!visibilityStatus.isPubliclyVisible)
                return null

            val flags = PropertyFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            val qualifiedName = deserializeQualifiedName(proto.name, containingEntity)
            val thisPropertyEntity = ContainingEntity.Property(qualifiedName, containingClassModality, visibilityStatus)

            return AbiPropertyImpl(
                qualifiedName = qualifiedName,
                signatures = deserializeSignatures(proto.base),
                annotations = annotations,
                modality = deserializeModality(flags.modality, containingClassModality),
                kind = when {
                    flags.isConst -> AbiPropertyKind.CONST_VAL
                    flags.isVar -> AbiPropertyKind.VAR
                    else -> AbiPropertyKind.VAL
                },
                getter = if (proto.hasGetter()) deserializeFunction(proto.getter.base, thisPropertyEntity).discardIfExcluded() else null,
                setter = if (proto.hasSetter()) deserializeFunction(proto.setter.base, thisPropertyEntity).discardIfExcluded() else null
            )
        }

        private fun deserializeQualifiedName(nameId: Int, containingEntity: ContainingEntity): AbiQualifiedName {
            return containingEntity.computeNestedName(fileReader.string(nameId))
        }

        private fun deserializeSignatures(proto: ProtoDeclarationBase): AbiSignatures {
            val signature = deserializeIdSignature(proto.symbol)

            return AbiSignaturesImpl(
                signatureV1 = if (needV1Signatures) signature.render(IdSignatureRenderer.LEGACY) else null,
                signatureV2 = if (needV2Signatures) signature.render(IdSignatureRenderer.DEFAULT) else null
            )
        }

        private fun deserializeAnnotations(proto: ProtoDeclarationBase): Set<AbiQualifiedName> {
            fun deserialize(annotation: ProtoConstructorCall): AbiQualifiedName {
                val signature = deserializeIdSignature(annotation.symbol)
                val annotationClassName = when {
                    signature is CommonSignature -> signature
                    signature is CompositeSignature && signature.container is FileSignature -> signature.inner as CommonSignature
                    else -> error("Unexpected annotation signature encountered: ${signature::class.java}, ${signature.render()}")
                }.extractQualifiedName { rawRelativeName -> rawRelativeName.removeSuffix(INIT_SUFFIX) }

                // Avoid duplicated instances of popular signature names:
                return annotationsInterner.intern(annotationClassName)
            }

            return when (proto.annotationCount) {
                0 -> return emptySet()
                1 -> return setOf(deserialize(proto.annotationList[0]))
                else -> proto.annotationList.mapTo(SmartSet.create(), ::deserialize)
            }
        }

        private fun computeVisibilityStatus(
            proto: ProtoDeclarationBase,
            annotations: Set<AbiQualifiedName>,
            containingClassModality: AbiModality?,
            parentVisibilityStatus: VisibilityStatus? = null
        ): VisibilityStatus = when (ProtoFlags.VISIBILITY.get(proto.flags.toInt())) {
            ProtoBuf.Visibility.PUBLIC -> VisibilityStatus.PUBLIC

            ProtoBuf.Visibility.PROTECTED -> {
                if (containingClassModality == AbiModality.FINAL)
                    VisibilityStatus.NON_PUBLIC
                else
                    VisibilityStatus.PUBLIC
            }

            ProtoBuf.Visibility.INTERNAL -> when {
                parentVisibilityStatus == VisibilityStatus.INTERNAL_PUBLISHED_API -> VisibilityStatus.INTERNAL_PUBLISHED_API
                PUBLISHED_API_CONSTRUCTOR_QUALIFIED_NAME in annotations -> VisibilityStatus.INTERNAL_PUBLISHED_API
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

        private fun deserializeValueParameter(proto: ProtoValueParameter): AbiValueParameter {
            val flags = ValueParameterFlags.decode(proto.base.flags)

            return AbiValueParameterImpl(
                type = typeDeserializer.deserializeType(BinaryNameAndType.decode(proto.nameType).typeIndex),
                isVararg = proto.hasVarargElementType(),
                hasDefaultArg = proto.hasDefaultValue(),
                isNoinline = flags.isNoInline,
                isCrossinline = flags.isCrossInline
            )
        }
    }

    private sealed interface ContainingEntity {
        fun computeNestedName(simpleName: String): AbiQualifiedName

        class Package(val packageName: AbiCompoundName) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedName(packageName, simpleName)
        }

        class Class(val className: AbiQualifiedName, val modality: AbiModality) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedName(className, simpleName)
        }

        class Property(
            val propertyName: AbiQualifiedName,
            val containingClassModality: AbiModality?,
            val propertyVisibilityStatus: VisibilityStatus
        ) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedName(propertyName, simpleName)
        }

        companion object {
            private fun qualifiedName(packageName: AbiCompoundName, topLevelSimpleName: String) =
                AbiQualifiedName(packageName, AbiCompoundName(topLevelSimpleName))

            private fun qualifiedName(parentName: AbiQualifiedName, memberSimpleName: String) =
                AbiQualifiedName(
                    parentName.packageName,
                    AbiCompoundName("${parentName.relativeName}${AbiCompoundName.SEPARATOR}$memberSimpleName")
                )
        }
    }

    private class TypeDeserializer(
        private val libraryFile: IrLibraryFile,
        private val signatureDeserializer: IdSignatureDeserializer
    ) {
        private val cache = HashMap</* type id */ Int, AbiType>()
        private val nonPublicTopLevelClassNames = HashSet<AbiQualifiedName>()

        fun deserializeType(typeId: Int): AbiType {
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

        fun isPubliclyVisible(type: AbiType): Boolean =
            ((type as? AbiType.Simple)?.classifier as? AbiClassifier.Class)?.className !in nonPublicTopLevelClassNames

        private fun deserializeDefinitelyNotNullType(proto: ProtoIrDefinitelyNotNullType): AbiType {
            assert(proto.typesCount == 1) { "Only DefinitelyNotNull type is now supported" }

            val underlyingType = deserializeType(proto.getTypes(0))
            return if (underlyingType is AbiType.Simple && underlyingType.nullability != AbiTypeNullability.DEFINITELY_NOT_NULL)
                SimpleTypeImpl(underlyingType.classifier, underlyingType.arguments, AbiTypeNullability.DEFINITELY_NOT_NULL)
            else
                underlyingType
        }

        private fun deserializeSimpleType(proto: ProtoSimpleType): AbiType.Simple = deserializeSimpleType(
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

        private fun deserializeSimpleType(proto: ProtoSimpleTypeLegacy): AbiType.Simple = deserializeSimpleType(
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
        ): AbiType.Simple {
            val symbolData = BinarySymbolData.decode(symbolId)
            val signature = signatureDeserializer.deserializeIdSignature(symbolData.signatureId)
            val symbolKind = symbolData.kind

            return when {
                symbolKind == CLASS_SYMBOL && signature is CommonSignature -> {
                    // Publicly visible class or interface.
                    SimpleTypeImpl(
                        classifier = ClassImpl(
                            className = signature.extractQualifiedName()
                        ),
                        arguments = deserializeTypeArguments(typeArgumentIds),
                        nullability = nullability
                    )
                }

                symbolKind == CLASS_SYMBOL && signature is CompositeSignature && signature.container is FileSignature -> {
                    // Non-publicly visible classifier. Practically, this can only be a private top-level interface
                    // that some publicly visible class inherits. Need to memoize it to avoid displaying it later among
                    // supertypes of the inherited class.
                    val className = (signature.inner as CommonSignature).extractQualifiedName()
                    nonPublicTopLevelClassNames += className

                    SimpleTypeImpl(
                        classifier = ClassImpl(className),
                        arguments = deserializeTypeArguments(typeArgumentIds),
                        nullability = nullability
                    )
                }

                symbolKind == TYPE_PARAMETER_SYMBOL && signature is CompositeSignature -> {
                    // A type-parameter.
                    SimpleTypeImpl(
                        classifier = TypeParameterImpl(
                            declaringClassName = (signature.container as CommonSignature).extractQualifiedName(),
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
                        type = deserializeType(typeProjection.typeIndex),
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
        private val INIT_SUFFIX = "." + SpecialNames.INIT.asString()

        private val KOTLIN_COMPOUND_NAME = AbiCompoundName("kotlin")
        private val PUBLISHED_API_CONSTRUCTOR_QUALIFIED_NAME = AbiQualifiedName(KOTLIN_COMPOUND_NAME, AbiCompoundName("PublishedApi"))
        private val KOTLIN_ANY_QUALIFIED_NAME = AbiQualifiedName(KOTLIN_COMPOUND_NAME, AbiCompoundName("Any"))
        private val KOTLIN_UNIT_QUALIFIED_NAME = AbiQualifiedName(KOTLIN_COMPOUND_NAME, AbiCompoundName("Unit"))

        private fun isKotlinBuiltInType(type: AbiType, className: AbiQualifiedName): Boolean {
            if (type !is AbiType.Simple || type.nullability != AbiTypeNullability.DEFINITELY_NOT_NULL) return false
            return (type.classifier as? AbiClassifier.Class)?.className == className
        }

        private inline fun CommonSignature.extractQualifiedName(transformRelativeName: (String) -> String = { it }): AbiQualifiedName =
            AbiQualifiedName(AbiCompoundName(packageFqName), AbiCompoundName(transformRelativeName(declarationFqName)))
    }
}
