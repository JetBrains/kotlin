/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.library.abi.AbiTypeNullability.*
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.reportLoadingProblemsIfAny
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.isPublishedApi
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.isValueClass
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isDefinitelyNotNullType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.utils.KotlinNativePaths

@ExperimentalLibraryAbiReader
internal class MetadataLibraryAbiReaderImpl(
    private val library: KotlinLibrary,
    filters: List<AbiReadingFilter>,
) {
    private val compositeFilter: AbiReadingFilter.Composite? = if (filters.isNotEmpty()) AbiReadingFilter.Composite(filters) else null

    private val signaturer = KonanIdSignaturer(KonanManglerDesc)

    fun readAbi(): LibraryAbi {
        val supportedSignatureVersions = readSupportedSignatureVersions()
        val module = loadModuleDescriptor()

        return LibraryAbi(
            manifest = readManifest(),
            uniqueName = library.uniqueName,
            signatureVersions = supportedSignatureVersions,
            topLevelDeclarations = deserializeFromDescriptors(module, supportedSignatureVersions)
        )
    }

    private fun loadModuleDescriptor(): ModuleDescriptorImpl {
        val storageManager = LockBasedStorageManager("MetadataLibraryAbiReader")
        val languageVersionSettings = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)

        val module = KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(
            library,
            languageVersionSettings,
            storageManager,
        )

        val defaultModules = mutableListOf<ModuleDescriptorImpl>()
        if (!module.isNativeStdlib()) {
            val stdlibPath = KotlinNativePaths.homePath.resolve("klib/common/stdlib").absolutePath
            val stdlibLoadResult = KlibLoader { libraryPaths(stdlibPath) }.load()
            if (!stdlibLoadResult.reportLoadingProblemsIfAny { _, _ -> }) {
                val stdlib = stdlibLoadResult.librariesStdlibFirst.single()
                defaultModules += KlibFactories.DefaultDeserializedDescriptorFactory.createDescriptor(
                    stdlib,
                    languageVersionSettings,
                    storageManager,
                    module.builtIns,
                )
            }
        }

        (defaultModules + module).let { allModules ->
            allModules.forEach { it.setDependencies(allModules) }
        }

        return module
    }

    private fun deserializeFromDescriptors(
        module: ModuleDescriptorImpl,
        supportedSignatureVersions: Set<AbiSignatureVersion>,
    ): AbiTopLevelDeclarations {
        val needV1Signatures = AbiSignatureVersions.Supported.V1 in supportedSignatureVersions
        val needV2Signatures = AbiSignatureVersions.Supported.V2 in supportedSignatureVersions

        val walker = DescriptorAbiWalker(needV1Signatures, needV2Signatures)
        return walker.walk(module)
    }

    private inner class DescriptorAbiWalker(
        private val needV1Signatures: Boolean,
        private val needV2Signatures: Boolean,
    ) {
        fun walk(module: ModuleDescriptorImpl): AbiTopLevelDeclarations {
            val topLevels = ArrayList<AbiDeclaration>()
            val packageFragments = collectPackageFragments(module)

            for (fragment in packageFragments) {
                val packageName = AbiCompoundName(fragment.fqName.asString())
                if (compositeFilter?.isPackageExcluded(packageName) == true)
                    continue

                val containingEntity = ContainingEntity.Package(packageName)
                for (descriptor in fragment.getMemberScope().getContributedDescriptors()) {
                    deserializeDeclaration(descriptor, containingEntity, parentTypeParameterResolver = null)
                        ?.let { topLevels.add(it) }
                }
            }

            return AbiTopLevelDeclarationsImpl(topLevels)
        }

        private fun collectPackageFragments(module: ModuleDescriptorImpl): List<PackageFragmentDescriptor> {
            val result = mutableListOf<PackageFragmentDescriptor>()
            val packagesFqNames = getPackagesFqNames(module)
            for (fqName in packagesFqNames) {
                val fragments = module.getPackage(fqName).fragments.filter { it.module == module }
                result.addAll(fragments)
            }
            return result
        }

        private fun getPackagesFqNames(module: ModuleDescriptor): Set<FqName> {
            val result = mutableSetOf<FqName>()
            val packageFragmentProvider =
                (module as? ModuleDescriptorImpl)?.packageFragmentProviderForModuleContentWithoutDependencies

            fun getSubPackages(fqName: FqName) {
                if (!result.add(fqName)) return
                val subPackages = packageFragmentProvider?.getSubPackagesOf(fqName) { true }
                    ?: module.getSubPackagesOf(fqName) { true }
                subPackages.forEach { getSubPackages(it) }
            }

            getSubPackages(FqName.ROOT)
            return result
        }

        private fun deserializeDeclaration(
            descriptor: DeclarationDescriptor,
            containingEntity: ContainingEntity,
            parentTypeParameterResolver: TypeParameterResolver?,
        ): AbiDeclaration? {
            val result = when (descriptor) {
                is ClassDescriptor -> deserializeClass(descriptor, containingEntity, parentTypeParameterResolver)
                is FunctionDescriptor -> {
                    if (descriptor is ConstructorDescriptor)
                        deserializeConstructor(descriptor, containingEntity, parentTypeParameterResolver)
                    else
                        deserializeFunction(descriptor, containingEntity, parentTypeParameterResolver)
                }
                is PropertyDescriptor -> deserializeProperty(descriptor, containingEntity, parentTypeParameterResolver)
                is TypeAliasDescriptor -> null // Skip type aliases, same as IR reader.
                else -> null
            }

            return result?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
        }

        private fun deserializeClass(
            descriptor: ClassDescriptor,
            containingEntity: ContainingEntity,
            parentTypeParameterResolver: TypeParameterResolver?,
        ): AbiClass? {
            if (descriptor.kind == ClassKind.ENUM_ENTRY) {
                // Enum entries are handled separately — they are discovered via getContributedDescriptors
                // but should be represented as AbiEnumEntry.
                return null
            }

            val annotations = deserializeAnnotations(descriptor)
            val containingClassModality = (containingEntity as? ContainingEntity.Class)?.modality

            if (!computeVisibilityStatus(descriptor, containingClassModality).isPubliclyVisible)
                return null

            val modality = descriptor.modality.toAbiModality(
                containingClassModality = null // Open nested classes in a final class remain open.
            )

            val qualifiedName = computeQualifiedName(descriptor.name.asString(), containingEntity)

            val thisClassEntity = ContainingEntity.Class(qualifiedName, modality)

            val isInner = descriptor.isInner

            val thisClassTypeParameterResolver = TypeParameterResolver(
                declarationName = qualifiedName,
                parent = if (isInner) parentTypeParameterResolver else null,
                levelAdjustment = if (!isInner && parentTypeParameterResolver != null) parentTypeParameterResolver.level + 1 else 0
            )

            val memberDeclarations = ArrayList<AbiDeclaration>()

            // Process constructors.
            for (constructor in descriptor.constructors) {
                deserializeDeclaration(constructor, thisClassEntity, thisClassTypeParameterResolver)?.let {
                    memberDeclarations.add(it)
                }
            }

            // Process member scope.
            for (member in descriptor.unsubstitutedMemberScope.getContributedDescriptors()) {
                if (member is ClassDescriptor && member.kind == ClassKind.ENUM_ENTRY) {
                    deserializeEnumEntry(member, thisClassEntity)?.let { memberDeclarations.add(it) }
                } else {
                    deserializeDeclaration(member, thisClassEntity, thisClassTypeParameterResolver)?.let {
                        memberDeclarations.add(it)
                    }
                }
            }

            return AbiClassImpl(
                qualifiedName = qualifiedName,
                signatures = computeSignatures(descriptor),
                annotations = annotations,
                modality = modality,
                kind = when (descriptor.kind) {
                    ClassKind.CLASS -> AbiClassKind.CLASS
                    ClassKind.INTERFACE -> AbiClassKind.INTERFACE
                    ClassKind.OBJECT -> AbiClassKind.OBJECT
                    ClassKind.ENUM_CLASS -> AbiClassKind.ENUM_CLASS
                    ClassKind.ANNOTATION_CLASS -> AbiClassKind.ANNOTATION_CLASS
                    ClassKind.ENUM_ENTRY -> error("Unexpected class kind: ENUM_ENTRY")
                },
                isInner = isInner,
                isValue = descriptor.isValueClass(),
                isFunction = descriptor.isFun,
                superTypes = descriptor.typeConstructor.supertypes.mapNotNull { type ->
                    val abiType = convertType(type, thisClassTypeParameterResolver)
                    // Filter out kotlin.Any and non-publicly-visible supertypes.
                    if (isKotlinAnyType(abiType)) null else abiType
                },
                declarations = memberDeclarations,
                typeParameters = deserializeTypeParameters(descriptor.declaredTypeParameters, thisClassTypeParameterResolver)
            )
        }

        private fun deserializeEnumEntry(
            descriptor: ClassDescriptor,
            containingEntity: ContainingEntity,
        ): AbiEnumEntry? {
            val annotations = deserializeAnnotations(descriptor)
            val qualifiedName = computeQualifiedName(descriptor.name.asString(), containingEntity)

            val signature = signaturer.composeEnumEntrySignature(descriptor) ?: return null

            return AbiEnumEntryImpl(
                qualifiedName = qualifiedName,
                signatures = signature.toAbiSignatures(),
                annotations = annotations
            ).takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
        }

        private fun deserializeFunction(
            descriptor: FunctionDescriptor,
            containingEntity: ContainingEntity,
            parentTypeParameterResolver: TypeParameterResolver?,
        ): AbiFunction? {
            val annotations = deserializeAnnotations(descriptor)

            val containingProperty: ContainingEntity.Property?
            val effectiveContainingClass: ContainingEntity.Class?

            when (containingEntity) {
                is ContainingEntity.Class -> {
                    containingProperty = null
                    effectiveContainingClass = containingEntity
                }
                is ContainingEntity.Property -> {
                    containingProperty = containingEntity
                    effectiveContainingClass = containingEntity.containingClass
                }
                else -> {
                    containingProperty = null
                    effectiveContainingClass = null
                }
            }

            val parentPropertyVisibilityStatus = containingProperty?.propertyVisibilityStatus
            if (!computeVisibilityStatus(
                    descriptor,
                    effectiveContainingClass?.modality,
                    parentPropertyVisibilityStatus
                ).isPubliclyVisible
            ) {
                return null
            }

            // Always skip fake overrides for cinterop libraries.
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                return null

            val functionName = computeQualifiedName(descriptor.name.asString(), containingEntity)

            val thisFunctionTypeParameterResolver = when {
                containingProperty != null -> {
                    TypeParameterResolver(containingProperty.propertyName, parentTypeParameterResolver, levelAdjustment = 1)
                }
                else -> TypeParameterResolver(functionName, parentTypeParameterResolver)
            }

            val allValueParameters = ArrayList<AbiValueParameter>()

            // Context receivers.
            descriptor.contextReceiverParameters.mapTo(allValueParameters) { contextParam ->
                AbiValueParameterImpl(
                    kind = AbiValueParameterKind.CONTEXT,
                    type = convertType(contextParam.type, thisFunctionTypeParameterResolver),
                    isVararg = false,
                    hasDefaultArg = false,
                    isNoinline = false,
                    isCrossinline = false
                )
            }

            // Extension receiver.
            descriptor.extensionReceiverParameter?.let { extensionReceiver ->
                allValueParameters.add(
                    AbiValueParameterImpl(
                        kind = AbiValueParameterKind.EXTENSION_RECEIVER,
                        type = convertType(extensionReceiver.type, thisFunctionTypeParameterResolver),
                        isVararg = false,
                        hasDefaultArg = false,
                        isNoinline = false,
                        isCrossinline = false
                    )
                )
            }

            // Regular value parameters.
            descriptor.valueParameters.mapTo(allValueParameters) { param ->
                AbiValueParameterImpl(
                    kind = AbiValueParameterKind.REGULAR,
                    type = convertType(param.type, thisFunctionTypeParameterResolver),
                    isVararg = param.varargElementType != null,
                    hasDefaultArg = param.declaresDefaultValue(),
                    isNoinline = param.isNoinline,
                    isCrossinline = param.isCrossinline
                )
            }

            val returnType = descriptor.returnType?.let { rt ->
                val abiType = convertType(rt, thisFunctionTypeParameterResolver)
                // Don't show trivial return type (kotlin.Unit).
                if (isKotlinUnitType(abiType)) null else abiType
            }

            val modality = descriptor.modality.toAbiModality(effectiveContainingClass?.modality)

            return AbiFunctionImpl(
                qualifiedName = functionName,
                signatures = computeSignatures(descriptor),
                annotations = annotations,
                modality = modality,
                isInline = descriptor.isInline,
                isSuspend = descriptor.isSuspend,
                typeParameters = deserializeTypeParameters(descriptor.typeParameters, thisFunctionTypeParameterResolver),
                valueParameters = allValueParameters,
                returnType = returnType
            )
        }

        private fun deserializeConstructor(
            descriptor: ConstructorDescriptor,
            containingEntity: ContainingEntity,
            parentTypeParameterResolver: TypeParameterResolver?,
        ): AbiFunction? {
            val annotations = deserializeAnnotations(descriptor)
            val containingClass = containingEntity as? ContainingEntity.Class

            if (!computeVisibilityStatus(descriptor, containingClass?.modality).isPubliclyVisible)
                return null

            // Exclude constructors of sealed classes from ABI dump.
            if (containingClass?.modality == AbiModality.SEALED)
                return null

            val constructorName = computeQualifiedName(descriptor.name.asString(), containingEntity)

            val allValueParameters = ArrayList<AbiValueParameter>()

            // Context receivers.
            descriptor.contextReceiverParameters.mapTo(allValueParameters) { contextParam ->
                AbiValueParameterImpl(
                    kind = AbiValueParameterKind.CONTEXT,
                    type = convertType(contextParam.type, parentTypeParameterResolver ?: TypeParameterResolver(constructorName, null)),
                    isVararg = false,
                    hasDefaultArg = false,
                    isNoinline = false,
                    isCrossinline = false
                )
            }

            // Regular value parameters.
            descriptor.valueParameters.mapTo(allValueParameters) { param ->
                AbiValueParameterImpl(
                    kind = AbiValueParameterKind.REGULAR,
                    type = convertType(param.type, parentTypeParameterResolver ?: TypeParameterResolver(constructorName, null)),
                    isVararg = param.varargElementType != null,
                    hasDefaultArg = param.declaresDefaultValue(),
                    isNoinline = param.isNoinline,
                    isCrossinline = param.isCrossinline
                )
            }

            return AbiConstructorImpl(
                qualifiedName = constructorName,
                signatures = computeSignatures(descriptor),
                annotations = annotations,
                isInline = descriptor.isInline,
                valueParameters = allValueParameters
            )
        }

        private fun deserializeProperty(
            descriptor: PropertyDescriptor,
            containingEntity: ContainingEntity,
            parentTypeParameterResolver: TypeParameterResolver?,
        ): AbiProperty? {
            val annotations = deserializeAnnotations(descriptor)
            val containingClass = containingEntity as? ContainingEntity.Class

            val visibilityStatus = computeVisibilityStatus(descriptor, containingClass?.modality)
            if (!visibilityStatus.isPubliclyVisible)
                return null

            // Always skip fake overrides for cinterop libraries.
            if (descriptor.kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
                return null

            val qualifiedName = computeQualifiedName(descriptor.name.asString(), containingEntity)
            val thisPropertyEntity = ContainingEntity.Property(qualifiedName, containingClass, visibilityStatus)

            return AbiPropertyImpl(
                qualifiedName = qualifiedName,
                signatures = computeSignatures(descriptor),
                annotations = annotations,
                modality = descriptor.modality.toAbiModality(containingClass?.modality),
                kind = when {
                    descriptor.isConst -> AbiPropertyKind.CONST_VAL
                    descriptor.isVar -> AbiPropertyKind.VAR
                    else -> AbiPropertyKind.VAL
                },
                getter = descriptor.getter?.let { getter ->
                    deserializeFunction(
                        getter,
                        containingEntity = thisPropertyEntity,
                        parentTypeParameterResolver = parentTypeParameterResolver,
                    )?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                },
                setter = descriptor.setter?.let { setter ->
                    deserializeFunction(
                        setter,
                        containingEntity = thisPropertyEntity,
                        parentTypeParameterResolver = parentTypeParameterResolver,
                    )?.takeUnless { compositeFilter?.isDeclarationExcluded(it) == true }
                },
                backingField = descriptor.backingField?.let { field ->
                    AbiFieldImpl(deserializeAnnotations(field))
                }
            )
        }

        private fun deserializeAnnotations(descriptor: DeclarationDescriptor): AbiAnnotationListImpl =
            deserializeAnnotationsFromAnnotated(descriptor)

        private fun deserializeAnnotations(descriptor: FieldDescriptor): AbiAnnotationListImpl =
            deserializeAnnotationsFromAnnotated(descriptor)

        private fun deserializeAnnotationsFromAnnotated(annotated: org.jetbrains.kotlin.descriptors.annotations.Annotated): AbiAnnotationListImpl {
            val annotations = annotated.annotations
            if (annotations.isEmpty()) return AbiAnnotationListImpl.EMPTY

            return AbiAnnotationListImpl(annotations.mapNotNull { annotation ->
                val fqName = annotation.fqName ?: return@mapNotNull null
                val packageName = fqName.parent().asString()
                val relativeName = fqName.shortName().asString()
                AbiAnnotationImpl(AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(relativeName)))
            })
        }

        private fun deserializeTypeParameters(
            typeParameters: List<TypeParameterDescriptor>,
            typeParameterResolver: TypeParameterResolver,
        ): List<AbiTypeParameter> = typeParameters.mapIndexed { index, tp ->
            AbiTypeParameterImpl(
                tag = typeParameterResolver.computeTypeParameterTag(index),
                variance = tp.variance.toAbiVariance(),
                isReified = tp.isReified,
                upperBounds = tp.upperBounds.mapNotNull { bound ->
                    val abiType = convertType(bound, typeParameterResolver)
                    // Filter out trivial upper bound (nullable Any).
                    if (isNullableAnyType(abiType)) null else abiType
                }
            )
        }

        private fun convertType(type: KotlinType, typeParameterResolver: TypeParameterResolver): AbiType {
            if (type.isError) return ErrorTypeImpl

            val classifier = type.constructor.declarationDescriptor

            return when (classifier) {
                is ClassDescriptor -> {
                    val className = qualifiedNameOf(classifier)
                    SimpleTypeImpl(
                        classifierReference = ClassReferenceImpl(className),
                        arguments = type.arguments.map { arg ->
                            if (arg.isStarProjection)
                                StarProjectionImpl
                            else
                                TypeProjectionImpl(
                                    type = convertType(arg.type, typeParameterResolver),
                                    variance = arg.projectionKind.toAbiVariance()
                                )
                        },
                        nullability = when {
                            type.isDefinitelyNotNullType -> DEFINITELY_NOT_NULL
                            type.isMarkedNullable -> MARKED_NULLABLE
                            else -> NOT_SPECIFIED
                        }
                    )
                }
                is TypeParameterDescriptor -> {
                    val tag = typeParameterResolver.resolveTypeParameterTag(classifier)
                    SimpleTypeImpl(
                        classifierReference = TypeParameterReferenceImpl(tag),
                        arguments = emptyList(),
                        nullability = when {
                            type.isDefinitelyNotNullType -> DEFINITELY_NOT_NULL
                            type.isMarkedNullable -> MARKED_NULLABLE
                            else -> NOT_SPECIFIED
                        }
                    )
                }
                else -> ErrorTypeImpl
            }
        }

        private fun qualifiedNameOf(classDescriptor: ClassDescriptor): AbiQualifiedName {
            val fqName = classDescriptor.fqNameSafe
            val packageFqName = findPackageFqName(classDescriptor)
            val relativeName = fqName.asString().removePrefix(packageFqName.asString()).removePrefix(".")
            return AbiQualifiedName(AbiCompoundName(packageFqName.asString()), AbiCompoundName(relativeName))
        }

        private fun findPackageFqName(descriptor: DeclarationDescriptor): FqName {
            var current: DeclarationDescriptor? = descriptor
            while (current != null) {
                if (current is PackageFragmentDescriptor) return current.fqName
                current = current.containingDeclaration
            }
            return FqName.ROOT
        }

        private fun computeQualifiedName(simpleName: String, containingEntity: ContainingEntity): AbiQualifiedName {
            return containingEntity.computeNestedName(simpleName)
        }

        private fun computeSignatures(descriptor: DeclarationDescriptor): AbiSignatures {
            val idSignature = if (descriptor is ClassDescriptor && descriptor.kind == ClassKind.ENUM_ENTRY) {
                signaturer.composeEnumEntrySignature(descriptor)
            } else {
                signaturer.composeSignature(descriptor)
            }

            return AbiSignaturesImpl(
                signatureV1 = if (needV1Signatures) idSignature?.render(IdSignatureRenderer.LEGACY) else null,
                signatureV2 = if (needV2Signatures) idSignature?.render(IdSignatureRenderer.DEFAULT) else null,
            )
        }

        private fun IdSignature.toAbiSignatures(): AbiSignatures = AbiSignaturesImpl(
            signatureV1 = if (needV1Signatures) render(IdSignatureRenderer.LEGACY) else null,
            signatureV2 = if (needV2Signatures) render(IdSignatureRenderer.DEFAULT) else null,
        )

        private fun computeVisibilityStatus(
            descriptor: DeclarationDescriptor,
            containingClassModality: AbiModality?,
            parentPropertyVisibilityStatus: VisibilityStatus? = null,
        ): VisibilityStatus {
            val visibility = (descriptor as? DeclarationDescriptorWithVisibility)?.visibility
                ?: return VisibilityStatus.PUBLIC

            return when (visibility) {
                DescriptorVisibilities.PUBLIC -> VisibilityStatus.PUBLIC
                DescriptorVisibilities.PROTECTED -> {
                    if (containingClassModality == AbiModality.FINAL)
                        VisibilityStatus.NON_PUBLIC
                    else
                        VisibilityStatus.PUBLIC
                }
                DescriptorVisibilities.INTERNAL -> when {
                    parentPropertyVisibilityStatus == VisibilityStatus.INTERNAL_PUBLISHED_API -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    descriptor.isPublishedApi() -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    else -> VisibilityStatus.NON_PUBLIC
                }
                else -> VisibilityStatus.NON_PUBLIC
            }
        }
    }

    private fun readManifest(): LibraryManifest = library.readAbiManifest()

    private fun readSupportedSignatureVersions(): Set<AbiSignatureVersion> {
        // CInterop libraries may not have irSignatureVersions in their manifest.
        // For cinterop libraries, we always support both V1 and V2 since they can be computed from descriptors.
        val versionsFromManifest = library.versions.irSignatureVersions.mapTo(hashSetOf()) {
            AbiSignatureVersions.resolveByVersionNumber(it.number)
        }

        return if (versionsFromManifest.isEmpty()) {
            setOf(AbiSignatureVersions.Supported.V1, AbiSignatureVersions.Supported.V2)
        } else {
            versionsFromManifest
        }
    }

    private sealed interface ContainingEntity {
        fun computeNestedName(simpleName: String): AbiQualifiedName

        class Package(val packageName: AbiCompoundName) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedNameFromPackage(packageName, simpleName)
        }

        class Class(
            val className: AbiQualifiedName,
            val modality: AbiModality,
        ) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedNameFromParent(className, simpleName)
        }

        class Property(
            val propertyName: AbiQualifiedName,
            val containingClass: Class?,
            val propertyVisibilityStatus: VisibilityStatus,
        ) : ContainingEntity {
            override fun computeNestedName(simpleName: String) = qualifiedNameFromParent(propertyName, simpleName)
        }
    }

    private class TypeParameterResolver(
        private val declarationName: AbiQualifiedName,
        val parent: TypeParameterResolver?,
        levelAdjustment: Int = 0,
    ) {
        val level: Int = (parent?.let { it.level + 1 } ?: 0) + levelAdjustment

        fun computeTypeParameterTag(index: Int): String {
            val tagPrefix = computeTypeParameterTagPrefix(index)
            return if (level > 0) "$tagPrefix$level" else tagPrefix
        }

        fun resolveTypeParameterTag(typeParameterDescriptor: TypeParameterDescriptor): String {
            val ownerDescriptor = typeParameterDescriptor.containingDeclaration
            val ownerName = computeOwnerQualifiedName(ownerDescriptor)
            val index = typeParameterDescriptor.index

            return if (ownerName == declarationName)
                computeTypeParameterTag(index)
            else
                parent?.resolveTypeParameterTag(typeParameterDescriptor)
                    ?: error("Type parameter with index $index can not be resolved for $ownerName")
        }

        private fun computeOwnerQualifiedName(descriptor: DeclarationDescriptor): AbiQualifiedName {
            val fqName = (descriptor as? DeclarationDescriptorNonRoot)?.fqNameSafe ?: return AbiQualifiedName(AbiCompoundName(""), AbiCompoundName(descriptor.name.asString()))
            var current: DeclarationDescriptor? = descriptor
            while (current != null) {
                if (current is PackageFragmentDescriptor) {
                    val packageFqName = current.fqName.asString()
                    val relativeName = fqName.asString().removePrefix(packageFqName).removePrefix(".")
                    return AbiQualifiedName(AbiCompoundName(packageFqName), AbiCompoundName(relativeName))
                }
                current = current.containingDeclaration
            }
            return AbiQualifiedName(AbiCompoundName(""), AbiCompoundName(fqName.asString()))
        }
    }

    companion object {
        private val KlibFactories = KlibMetadataFactories(::KonanBuiltIns, DynamicTypeDeserializer)

        private fun isKotlinAnyType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_ANY_QUALIFIED_NAME, DEFINITELY_NOT_NULL)

        private fun isKotlinUnitType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_UNIT_QUALIFIED_NAME, DEFINITELY_NOT_NULL)

        private fun isNullableAnyType(type: AbiType): Boolean =
            isKotlinBuiltInType(type, KOTLIN_ANY_QUALIFIED_NAME, MARKED_NULLABLE)
    }
}
