/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrStarProjectionImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiTypeNullability.*
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.types.Variance
import java.util.*
import kotlin.metadata.ExperimentalAnnotationsInMetadata
import kotlin.metadata.KmAnnotation

private const val PUBLISHED_API_CLASS_NAME = "kotlin/PublishedApi"

@OptIn(UnsafeDuringIrConstructionAPI::class)
@ExperimentalLibraryAbiReader
internal class MetadataLibraryAbiReaderImpl(
    private val library: KotlinLibrary,
    filters: List<AbiReadingFilter>,
) {
    private val compositeFilter: AbiReadingFilter.Composite? = if (filters.isNotEmpty()) AbiReadingFilter.Composite(filters) else null

    fun readAbi(): LibraryAbi {
        val supportedSignatureVersions = readSupportedSignatureVersions()
        val moduleMetadata = loadModuleMetadata()

        // Convert metadata to IR stubs and create signature computer
        val converter = MetadataToIrStubConverter(moduleMetadata)
        converter.convert()
        val signatureComputer = CInteropIdSignatureComputer()

        return LibraryAbi(
            manifest = readManifest(),
            uniqueName = library.uniqueName,
            signatureVersions = supportedSignatureVersions,
            topLevelDeclarations = walkIrStubs(moduleMetadata, converter, signatureComputer, supportedSignatureVersions)
        )
    }

    private fun loadModuleMetadata(): KlibModuleMetadata {
        val metadata = library.metadata
        return KlibModuleMetadata.readLenient(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData get() = metadata.moduleHeaderData
            override val metadataVersion = KlibMetadataVersion(
                library.metadataVersion?.toArray() ?: error("No metadata version specified in ${library.location}")
            )

            override fun packageMetadataParts(fqName: String) = metadata.getPackageFragmentNames(fqName)
            override fun packageMetadata(fqName: String, partName: String) = metadata.getPackageFragment(fqName, partName)
        })
    }

    private fun walkIrStubs(
        module: KlibModuleMetadata,
        converter: MetadataToIrStubConverter,
        signatureComputer: CInteropIdSignatureComputer,
        supportedSignatureVersions: Set<AbiSignatureVersion>,
    ): AbiTopLevelDeclarations {
        val needV1Signatures = AbiSignatureVersions.Supported.V1 in supportedSignatureVersions
        val needV2Signatures = AbiSignatureVersions.Supported.V2 in supportedSignatureVersions

        val walker = IrStubAbiWalker(converter, signatureComputer, needV1Signatures, needV2Signatures)
        return walker.walk(module)
    }

    /**
     * Walks the enriched IR stubs produced by [MetadataToIrStubConverter] and produces
     * ABI declarations. This replaces the former [Km*]-walking approach with an IR-tree walk.
     */
    private inner class IrStubAbiWalker(
        private val converter: MetadataToIrStubConverter,
        private val signatureComputer: CInteropIdSignatureComputer,
        private val needV1Signatures: Boolean,
        private val needV2Signatures: Boolean,
    ) {
        fun walk(module: KlibModuleMetadata): AbiTopLevelDeclarations {
            val topLevels = ArrayList<AbiDeclaration>()

            for (fragment in module.fragments) {
                val packageFqnStr = fragment.fqName?.replace('/', '.') ?: ""
                val packageName = AbiCompoundName(packageFqnStr)

                if (compositeFilter?.isPackageExcluded(packageName) == true)
                    continue

                // Process top-level classes — only top-level (no parent dot in name)
                for (kmClass in fragment.classes) {
                    if (kmClass.name.contains('.')) continue // nested; handled by parent
                    val irClass = converter.classStubs[kmClass.name] ?: continue
                    walkClass(irClass, containingClassModality = null, parentTypeParameterResolver = null)
                        ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                        ?.let { topLevels.add(it) }
                }

                // Process top-level functions and properties from the package fragment
                fragment.pkg?.let { _ ->
                    val packageFragment = converter.getPackageFragment(packageFqnStr)
                    for (decl in packageFragment?.declarations.orEmpty()) {
                        when (decl) {
                            is IrSimpleFunction -> walkFunction(
                                decl,
                                containingClassModality = null,
                                parentPropertyVisibilityStatus = null,
                                parentTypeParameterResolver = null,
                            )
                            is IrProperty -> walkProperty(
                                decl,
                                containingClassModality = null,
                                parentTypeParameterResolver = null,
                            )
                            else -> null
                        }?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                            ?.let { topLevels.add(it) }
                    }
                }
            }

            return AbiTopLevelDeclarationsImpl(topLevels)
        }

        // ---- Class walking ----

        private fun walkClass(
            irClass: IrClass,
            containingClassModality: AbiModality?,
            parentTypeParameterResolver: IrTypeParameterResolver?,
        ): AbiClass? {
            if (irClass.kind == ClassKind.ENUM_ENTRY) return null

            val annotations = deserializeAnnotations(converter.annotationsMap[irClass])
            val visibility = irClass.visibility

            if (!visibility.toVisibilityStatus(containingClassModality, hasPublishedApiAnnotation = hasPublishedApi(irClass)).isPubliclyVisible)
                return null

            val modality = irClass.modality.toAbiModality(containingClassModality = null)
            val qualifiedName = classNameToQualifiedName(findClassName(irClass) ?: return null)
            val isInner = irClass.isInner

            val thisClassTypeParameterResolver = IrTypeParameterResolver(
                parent = if (isInner) parentTypeParameterResolver else null,
                levelAdjustment = if (!isInner && parentTypeParameterResolver != null) parentTypeParameterResolver.level + 1 else 0,
                typeParameters = irClass.typeParameters,
            )

            val memberDeclarations = ArrayList<AbiDeclaration>()

            // Walk declarations in the order: constructors, enum entries, functions, properties, nested classes
            val constructors = ArrayList<IrConstructor>()
            val enumEntries = ArrayList<IrEnumEntry>()
            val functions = ArrayList<IrSimpleFunction>()
            val properties = ArrayList<IrProperty>()
            val nestedClasses = ArrayList<IrClass>()

            for (decl in irClass.declarations) {
                when (decl) {
                    is IrConstructor -> constructors.add(decl)
                    is IrEnumEntry -> enumEntries.add(decl)
                    is IrSimpleFunction -> {
                        // Skip property accessors, they are handled through their property
                        if (decl.correspondingPropertySymbol == null) functions.add(decl)
                    }
                    is IrProperty -> properties.add(decl)
                    is IrClass -> nestedClasses.add(decl)
                    else -> {}
                }
            }

            for (ctor in constructors) {
                walkConstructor(ctor, modality, thisClassTypeParameterResolver)
                    ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                    ?.let { memberDeclarations.add(it) }
            }

            for (entry in enumEntries) {
                val abiEntry = walkEnumEntry(entry, qualifiedName)
                if (compositeFilter?.isDeclarationExcluded(abiEntry) != true) {
                    memberDeclarations.add(abiEntry)
                }
            }

            for (func in functions) {
                walkFunction(func, containingClassModality = modality, parentPropertyVisibilityStatus = null, parentTypeParameterResolver = thisClassTypeParameterResolver)
                    ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                    ?.let { memberDeclarations.add(it) }
            }

            for (prop in properties) {
                walkProperty(prop, containingClassModality = modality, parentTypeParameterResolver = thisClassTypeParameterResolver)
                    ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                    ?.let { memberDeclarations.add(it) }
            }

            for (nested in nestedClasses) {
                walkClass(nested, containingClassModality = modality, parentTypeParameterResolver = thisClassTypeParameterResolver)
                    ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                    ?.let { memberDeclarations.add(it) }
            }

            val classSignatures = computeSignatures(signatureComputer.computeSignature(irClass))

            return AbiClassImpl(
                qualifiedName = qualifiedName,
                signatures = classSignatures,
                annotations = annotations,
                modality = modality,
                kind = when (irClass.kind) {
                    ClassKind.CLASS -> AbiClassKind.CLASS
                    ClassKind.INTERFACE -> AbiClassKind.INTERFACE
                    ClassKind.OBJECT -> AbiClassKind.OBJECT
                    ClassKind.ENUM_CLASS -> AbiClassKind.ENUM_CLASS
                    ClassKind.ANNOTATION_CLASS -> AbiClassKind.ANNOTATION_CLASS
                    ClassKind.ENUM_ENTRY -> error("Unexpected class kind: ENUM_ENTRY")
                },
                isInner = isInner,
                isValue = irClass.isValue,
                isFunction = irClass.isFun,
                superTypes = irClass.superTypes.mapNotNull { superType ->
                    val abiType = convertIrType(superType, thisClassTypeParameterResolver)
                    if (isKotlinAnyType(abiType)) null else abiType
                },
                declarations = memberDeclarations,
                typeParameters = convertTypeParameters(irClass.typeParameters, thisClassTypeParameterResolver),
            )
        }

        // ---- Function walking ----

        private fun walkFunction(
            irFunction: IrSimpleFunction,
            containingClassModality: AbiModality?,
            parentPropertyVisibilityStatus: VisibilityStatus?,
            parentTypeParameterResolver: IrTypeParameterResolver?,
        ): AbiFunction? {
            val annotations = deserializeAnnotations(converter.annotationsMap[irFunction])

            if (!irFunction.visibility.toVisibilityStatus(
                    containingClassModality,
                    parentPropertyVisibilityStatus,
                    hasPublishedApi(irFunction),
                ).isPubliclyVisible
            ) {
                return null
            }

            // Always skip fake overrides for cinterop libraries.
            if (irFunction.origin == IrDeclarationOrigin.FAKE_OVERRIDE)
                return null

            val qualifiedName = computeQualifiedNameForDeclaration(irFunction)

            val isAccessor = irFunction.correspondingPropertySymbol != null
            val funcTypeParameterResolver = if (isAccessor) {
                IrTypeParameterResolver(parentTypeParameterResolver, levelAdjustment = 1, typeParameters = irFunction.typeParameters)
            } else {
                IrTypeParameterResolver(parentTypeParameterResolver, typeParameters = irFunction.typeParameters)
            }

            val allValueParameters = ArrayList<AbiValueParameter>()

            for (param in irFunction.parameters) {
                when (param.kind) {
                    IrParameterKind.DispatchReceiver -> {} // skip
                    IrParameterKind.Context -> {
                        allValueParameters.add(
                            AbiValueParameterImpl(
                                kind = AbiValueParameterKind.CONTEXT,
                                type = convertIrType(param.type, funcTypeParameterResolver),
                                isVararg = false,
                                hasDefaultArg = false,
                                isNoinline = false,
                                isCrossinline = false,
                            )
                        )
                    }
                    IrParameterKind.ExtensionReceiver -> {
                        allValueParameters.add(
                            AbiValueParameterImpl(
                                kind = AbiValueParameterKind.EXTENSION_RECEIVER,
                                type = convertIrType(param.type, funcTypeParameterResolver),
                                isVararg = false,
                                hasDefaultArg = false,
                                isNoinline = false,
                                isCrossinline = false,
                            )
                        )
                    }
                    IrParameterKind.Regular -> {
                        allValueParameters.add(
                            AbiValueParameterImpl(
                                kind = AbiValueParameterKind.REGULAR,
                                type = convertIrType(param.type, funcTypeParameterResolver),
                                isVararg = param.varargElementType != null,
                                hasDefaultArg = param.defaultValue != null,
                                isNoinline = param.isNoinline,
                                isCrossinline = param.isCrossinline,
                            )
                        )
                    }
                }
            }

            val returnType = run {
                val abiType = convertIrType(irFunction.returnType, funcTypeParameterResolver)
                if (isKotlinUnitType(abiType)) null else abiType
            }

            val modality = irFunction.modality.toAbiModality(containingClassModality)
            val funcSignatures = computeSignatures(signatureComputer.computeSignature(irFunction))

            return AbiFunctionImpl(
                qualifiedName = qualifiedName,
                signatures = funcSignatures,
                annotations = annotations,
                modality = modality,
                isInline = irFunction.isInline,
                isSuspend = irFunction.isSuspend,
                typeParameters = convertTypeParameters(irFunction.typeParameters, funcTypeParameterResolver),
                valueParameters = allValueParameters,
                returnType = returnType,
            )
        }

        // ---- Constructor walking ----

        private fun walkConstructor(
            irConstructor: IrConstructor,
            containingClassModality: AbiModality,
            classTypeParameterResolver: IrTypeParameterResolver,
        ): AbiFunction? {
            val annotations = deserializeAnnotations(converter.annotationsMap[irConstructor])

            if (!irConstructor.visibility.toVisibilityStatus(
                    containingClassModality,
                    hasPublishedApiAnnotation = hasPublishedApi(irConstructor),
                ).isPubliclyVisible
            ) {
                return null
            }

            // Exclude constructors of sealed classes from ABI dump.
            if (containingClassModality == AbiModality.SEALED)
                return null

            val qualifiedName = computeQualifiedNameForDeclaration(irConstructor)

            val allValueParameters = ArrayList<AbiValueParameter>()
            for (param in irConstructor.parameters) {
                if (param.kind != IrParameterKind.Regular) continue
                allValueParameters.add(
                    AbiValueParameterImpl(
                        kind = AbiValueParameterKind.REGULAR,
                        type = convertIrType(param.type, classTypeParameterResolver),
                        isVararg = param.varargElementType != null,
                        hasDefaultArg = param.defaultValue != null,
                        isNoinline = param.isNoinline,
                        isCrossinline = param.isCrossinline,
                    )
                )
            }

            val ctorSignatures = computeSignatures(signatureComputer.computeSignature(irConstructor))

            return AbiConstructorImpl(
                qualifiedName = qualifiedName,
                signatures = ctorSignatures,
                annotations = annotations,
                isInline = false,
                valueParameters = allValueParameters,
            )
        }

        // ---- Enum entry walking ----

        private fun walkEnumEntry(
            irEnumEntry: IrEnumEntry,
            containingClassName: AbiQualifiedName,
        ): AbiEnumEntry {
            val annotations = deserializeAnnotations(converter.annotationsMap[irEnumEntry])
            val qualifiedName = qualifiedNameFromParent(containingClassName, irEnumEntry.name.asString())
            val signature = signatureComputer.computeSignature(irEnumEntry)

            return AbiEnumEntryImpl(
                qualifiedName = qualifiedName,
                signatures = computeSignatures(signature),
                annotations = annotations,
            )
        }

        // ---- Property walking ----

        private fun walkProperty(
            irProperty: IrProperty,
            containingClassModality: AbiModality?,
            parentTypeParameterResolver: IrTypeParameterResolver?,
        ): AbiProperty? {
            val annotations = deserializeAnnotations(converter.annotationsMap[irProperty])

            val visibilityStatus = irProperty.visibility.toVisibilityStatus(
                containingClassModality,
                hasPublishedApiAnnotation = hasPublishedApi(irProperty),
            )
            if (!visibilityStatus.isPubliclyVisible)
                return null

            // Always skip fake overrides for cinterop libraries.
            if (irProperty.origin == IrDeclarationOrigin.FAKE_OVERRIDE)
                return null

            val qualifiedName = computeQualifiedNameForDeclaration(irProperty)
            val propSignatures = computeSignatures(signatureComputer.computeSignature(irProperty))
            val modality = irProperty.modality.toAbiModality(containingClassModality)

            fun walkAccessor(accessor: IrSimpleFunction?): AbiFunction? {
                if (accessor == null) return null
                val accessorVisibility = accessor.visibility.toVisibilityStatus(
                    containingClassModality,
                    parentPropertyVisibilityStatus = visibilityStatus,
                    hasPublishedApiAnnotation = hasPublishedApi(accessor),
                )
                if (!accessorVisibility.isPubliclyVisible) return null
                return walkFunction(accessor, containingClassModality, visibilityStatus, parentTypeParameterResolver)
                    ?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
            }

            val getter = walkAccessor(irProperty.getter)
            val setter = walkAccessor(irProperty.setter)

            return AbiPropertyImpl(
                qualifiedName = qualifiedName,
                signatures = propSignatures,
                annotations = annotations,
                modality = modality,
                kind = when {
                    irProperty.isConst -> AbiPropertyKind.CONST_VAL
                    irProperty.isVar -> AbiPropertyKind.VAR
                    else -> AbiPropertyKind.VAL
                },
                getter = getter,
                setter = setter,
                backingField = null, // CInterop libraries typically don't have backing fields
            )
        }

        // ---- Type conversion (IrType → AbiType) ----

        private fun convertIrType(irType: IrType, resolver: IrTypeParameterResolver): AbiType {
            if (irType !is IrSimpleType) return ErrorTypeImpl

            val nullability = when (irType.nullability) {
                SimpleTypeNullability.MARKED_NULLABLE -> MARKED_NULLABLE
                SimpleTypeNullability.NOT_SPECIFIED -> NOT_SPECIFIED
                SimpleTypeNullability.DEFINITELY_NOT_NULL -> DEFINITELY_NOT_NULL
            }

            val classifier = irType.classifier
            return when (classifier) {
                is IrClassSymbol -> {
                    val className = classNameToQualifiedName(findClassName(classifier.owner) ?: return ErrorTypeImpl)
                    SimpleTypeImpl(
                        classifierReference = ClassReferenceImpl(className),
                        arguments = irType.arguments.map { convertIrTypeArgument(it, resolver) },
                        nullability = nullability,
                    )
                }
                is IrTypeParameterSymbol -> {
                    val tag = resolver.resolveTag(classifier)
                    SimpleTypeImpl(
                        classifierReference = TypeParameterReferenceImpl(tag),
                        arguments = emptyList(),
                        nullability = nullability,
                    )
                }
                else -> ErrorTypeImpl
            }
        }

        private fun convertIrTypeArgument(arg: IrTypeArgument, resolver: IrTypeParameterResolver): AbiTypeArgument {
            if (arg is IrStarProjectionImpl) return StarProjectionImpl
            if (arg is IrTypeProjection) {
                val abiType = convertIrType(arg.type, resolver)
                return when (arg.variance) {
                    Variance.INVARIANT -> TypeProjectionImpl(abiType, AbiVariance.INVARIANT)
                    Variance.IN_VARIANCE -> TypeProjectionImpl(abiType, AbiVariance.IN)
                    Variance.OUT_VARIANCE -> TypeProjectionImpl(abiType, AbiVariance.OUT)
                }
            }
            return StarProjectionImpl
        }

        // ---- Type parameters ----

        private fun convertTypeParameters(
            typeParameters: List<IrTypeParameter>,
            resolver: IrTypeParameterResolver,
        ): List<AbiTypeParameter> = typeParameters.mapIndexed { index, irTp ->
            AbiTypeParameterImpl(
                tag = resolver.computeTag(index),
                variance = irTp.variance.toAbiVariance(),
                isReified = irTp.isReified,
                upperBounds = irTp.superTypes.mapNotNull { bound ->
                    val abiType = convertIrType(bound, resolver)
                    if (isNullableAnyType(abiType)) null else abiType
                },
            )
        }

        // ---- Annotations ----

        @OptIn(ExperimentalAnnotationsInMetadata::class)
        private fun deserializeAnnotations(annotations: List<KmAnnotation>?): AbiAnnotationListImpl {
            if (annotations.isNullOrEmpty()) return AbiAnnotationListImpl.EMPTY
            return AbiAnnotationListImpl(annotations.map { annotation ->
                AbiAnnotationImpl(classNameToQualifiedName(annotation.className))
            })
        }

        @OptIn(ExperimentalAnnotationsInMetadata::class)
        private fun hasPublishedApi(irDeclaration: IrDeclaration): Boolean {
            val annotations = converter.annotationsMap[irDeclaration] ?: return false
            return annotations.any { it.className == PUBLISHED_API_CLASS_NAME }
        }

        // ---- Helpers ----

        private fun computeQualifiedNameForDeclaration(irDeclaration: IrDeclaration): AbiQualifiedName {
            val parent = (irDeclaration as IrDeclarationWithName).parent
            val simpleName = irDeclaration.name.asString()
            return when (parent) {
                is IrClass -> {
                    val parentName = classNameToQualifiedName(findClassName(parent) ?: error("Cannot find class name for ${parent.name}"))
                    qualifiedNameFromParent(parentName, simpleName)
                }
                is IrPackageFragment -> {
                    val packageName = AbiCompoundName(parent.packageFqName.asString())
                    qualifiedNameFromPackage(packageName, simpleName)
                }
                else -> error("Unexpected parent: $parent")
            }
        }

        private fun computeSignatures(idSignature: IdSignature?): AbiSignatures = AbiSignaturesImpl(
            signatureV1 = if (needV1Signatures) idSignature?.render(IdSignatureRenderer.LEGACY) else null,
            signatureV2 = if (needV2Signatures) idSignature?.render(IdSignatureRenderer.DEFAULT) else null,
        )
    }

    /**
     * Resolves type parameter tags by [IrTypeParameterSymbol] identity,
     * instead of integer Km* IDs.
     */
    private class IrTypeParameterResolver(
        val parent: IrTypeParameterResolver?,
        levelAdjustment: Int = 0,
        typeParameters: List<IrTypeParameter> = emptyList(),
    ) {
        val level: Int = (parent?.let { it.level + 1 } ?: 0) + levelAdjustment

        private val symbolToIndex = IdentityHashMap<IrTypeParameterSymbol, Int>()

        init {
            for ((index, tp) in typeParameters.withIndex()) {
                symbolToIndex[tp.symbol] = index
            }
        }

        fun computeTag(index: Int): String {
            val tagPrefix = computeTypeParameterTagPrefix(index)
            return if (level > 0) "$tagPrefix$level" else tagPrefix
        }

        fun resolveTag(symbol: IrTypeParameterSymbol): String {
            val localIndex = symbolToIndex[symbol]
            return if (localIndex != null)
                computeTag(localIndex)
            else
                parent?.resolveTag(symbol)
                    ?: error("Cannot resolve type parameter ${symbol.owner.name}")
        }
    }

    private fun readManifest(): LibraryManifest = library.readAbiManifest()

    private fun readSupportedSignatureVersions(): Set<AbiSignatureVersion> {
        // CInterop libraries may not have irSignatureVersions in their manifest.
        // For cinterop libraries, we always support both V1 and V2 since they can be computed from metadata.
        val versionsFromManifest = library.versions.irSignatureVersions.mapTo(hashSetOf()) {
            AbiSignatureVersions.resolveByVersionNumber(it.number)
        }

        return if (versionsFromManifest.isEmpty()) {
            setOf(AbiSignatureVersions.Supported.V1, AbiSignatureVersions.Supported.V2)
        } else {
            versionsFromManifest
        }
    }

    companion object {
        private fun isKotlinAnyType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_ANY_QUALIFIED_NAME, DEFINITELY_NOT_NULL)

        private fun isKotlinUnitType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_UNIT_QUALIFIED_NAME, DEFINITELY_NOT_NULL)

        private fun isNullableAnyType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_ANY_QUALIFIED_NAME, MARKED_NULLABLE)
    }
}
