/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins.jvm

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.DROP_LIST_METHOD_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.HIDDEN_CONSTRUCTOR_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.HIDDEN_METHOD_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.MUTABLE_METHOD_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.VISIBLE_CONSTRUCTOR_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.VISIBLE_METHOD_SIGNATURES
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.isArrayOrPrimitiveArray
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsSignatures.isSerializableInJava
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.createDeprecatedAnnotation
import org.jetbrains.kotlin.descriptors.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.descriptors.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.descriptors.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.SignatureBuildingComponents
import org.jetbrains.kotlin.load.kotlin.computeJvmDescriptor
import org.jetbrains.kotlin.load.kotlin.signature
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.LazyWrappedType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

open class JvmBuiltInsSettings(
    private val moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager,
    deferredOwnerModuleDescriptor: () -> ModuleDescriptor,
    isAdditionalBuiltInsFeatureSupported: () -> Boolean
) : AdditionalClassPartsProvider, PlatformDependentDeclarationFilter {
    private val j2kClassMapper = JavaToKotlinClassMapper

    private val ownerModuleDescriptor: ModuleDescriptor by lazy(deferredOwnerModuleDescriptor)
    private val isAdditionalBuiltInsFeatureSupported: Boolean by lazy(isAdditionalBuiltInsFeatureSupported)

    private val mockSerializableType = storageManager.createMockJavaIoSerializableType()
    private val cloneableType by storageManager.createLazyValue {
        ownerModuleDescriptor.findNonGenericClassAcrossDependencies(
            JvmBuiltInClassDescriptorFactory.CLONEABLE_CLASS_ID,
            NotFoundClasses(storageManager, ownerModuleDescriptor)
        ).defaultType
    }

    private val javaAnalogueClassesWithCustomSupertypeCache = storageManager.createCacheWithNotNullValues<FqName, ClassDescriptor>()

    // Most this properties are lazy because they depends on KotlinBuiltIns initialization that depends on JvmBuiltInsSettings object
    private val notConsideredDeprecation by storageManager.createLazyValue {
        val annotation = moduleDescriptor.builtIns.createDeprecatedAnnotation(
            "This member is not fully supported by Kotlin compiler, so it may be absent or have different signature in next major version"
        )
        Annotations.create(listOf(annotation))
    }

    private fun StorageManager.createMockJavaIoSerializableType(): KotlinType {
        val mockJavaIoPackageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName("java.io")) {
            override fun getMemberScope() = MemberScope.Empty
        }

        //NOTE: can't reference anyType right away, because this is sometimes called when JvmBuiltIns are initializing
        val superTypes = listOf(LazyWrappedType(this) { moduleDescriptor.builtIns.anyType })

        val mockSerializableClass = ClassDescriptorImpl(
            mockJavaIoPackageFragment, Name.identifier("Serializable"), Modality.ABSTRACT, ClassKind.INTERFACE, superTypes,
            SourceElement.NO_SOURCE, false, this
        )

        mockSerializableClass.initialize(MemberScope.Empty, emptySet(), null)
        return mockSerializableClass.defaultType
    }

    override fun getSupertypes(classDescriptor: ClassDescriptor): Collection<KotlinType> {
        val fqName = classDescriptor.fqNameUnsafe
        return when {
            isArrayOrPrimitiveArray(fqName) -> listOf(cloneableType, mockSerializableType)
            isSerializableInJava(fqName) -> listOf(mockSerializableType)
            else -> listOf()
        }
    }

    override fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor> {
        if (name == CloneableClassScope.CLONE_NAME && classDescriptor is DeserializedClassDescriptor &&
            KotlinBuiltIns.isArrayOrPrimitiveArray(classDescriptor)
        ) {
            // Do not create clone for arrays deserialized from metadata in the old (1.0) runtime, because clone is declared there anyway
            if (classDescriptor.classProto.functionList.any { functionProto ->
                    classDescriptor.c.nameResolver.getName(functionProto.name) == CloneableClassScope.CLONE_NAME
                }) {
                return emptyList()
            }
            return listOf(
                createCloneForArray(
                    classDescriptor, cloneableType.memberScope.getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS).single()
                )
            )
        }

        if (!isAdditionalBuiltInsFeatureSupported) return emptyList()

        return getAdditionalFunctions(classDescriptor) {
            it.getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS)
        }.mapNotNull { additionalMember ->
            val substitutedWithKotlinTypeParameters =
                additionalMember.substitute(
                    createMappedTypeParametersSubstitution(
                        additionalMember.containingDeclaration as ClassDescriptor, classDescriptor
                    ).buildSubstitutor()
                ) as SimpleFunctionDescriptor

            substitutedWithKotlinTypeParameters.newCopyBuilder().apply {
                setOwner(classDescriptor)
                setDispatchReceiverParameter(classDescriptor.thisAsReceiverParameter)
                setPreserveSourceElement()

                val memberStatus = additionalMember.getJdkMethodStatus()
                when (memberStatus) {
                    JDKMemberStatus.HIDDEN -> {
                        // hidden methods in final class can't be overridden or called with 'super'
                        if (classDescriptor.isFinalClass) return@mapNotNull null
                        setHiddenForResolutionEverywhereBesideSupercalls()
                    }

                    JDKMemberStatus.NOT_CONSIDERED -> {
                        setAdditionalAnnotations(notConsideredDeprecation)
                    }

                    JDKMemberStatus.DROP -> return@mapNotNull null

                    JDKMemberStatus.VISIBLE -> Unit // Do nothing
                }

            }.build()!!
        }
    }

    override fun getFunctionsNames(classDescriptor: ClassDescriptor): Set<Name> {
        if (!isAdditionalBuiltInsFeatureSupported) return emptySet()
        // NB: It's just an approximation that could be calculated relatively fast
        // More precise computation would look like `getAdditionalFunctions` (and the measurements show that it would be rather slow)
        return classDescriptor.getJavaAnalogue()?.unsubstitutedMemberScope?.getFunctionNames() ?: emptySet()
    }

    private fun getAdditionalFunctions(
        classDescriptor: ClassDescriptor,
        functionsByScope: (MemberScope) -> Collection<SimpleFunctionDescriptor>
    ): Collection<SimpleFunctionDescriptor> {
        val javaAnalogueDescriptor = classDescriptor.getJavaAnalogue() ?: return emptyList()

        val kotlinClassDescriptors = j2kClassMapper.mapPlatformClass(javaAnalogueDescriptor.fqNameSafe, FallbackBuiltIns.Instance)
        val kotlinMutableClassIfContainer = kotlinClassDescriptors.lastOrNull() ?: return emptyList()
        val kotlinVersions = SmartSet.create(kotlinClassDescriptors.map { it.fqNameSafe })

        val isMutable = j2kClassMapper.isMutable(classDescriptor)

        val fakeJavaClassDescriptor = javaAnalogueClassesWithCustomSupertypeCache.computeIfAbsent(javaAnalogueDescriptor.fqNameSafe) {
            javaAnalogueDescriptor.copy(
                javaResolverCache = JavaResolverCache.EMPTY,
                additionalSupertypeClassDescriptor = kotlinMutableClassIfContainer
            )
        }

        val scope = fakeJavaClassDescriptor.unsubstitutedMemberScope

        return functionsByScope(scope)
            .filter { analogueMember ->
                if (analogueMember.kind != CallableMemberDescriptor.Kind.DECLARATION) return@filter false
                if (!analogueMember.visibility.isPublicAPI) return@filter false
                if (KotlinBuiltIns.isDeprecated(analogueMember)) return@filter false

                if (analogueMember.overriddenDescriptors.any {
                        it.containingDeclaration.fqNameSafe in kotlinVersions
                    }) return@filter false

                !analogueMember.isMutabilityViolation(isMutable)
            }
    }

    private fun createCloneForArray(
        arrayClassDescriptor: DeserializedClassDescriptor,
        cloneFromCloneable: SimpleFunctionDescriptor
    ): SimpleFunctionDescriptor = cloneFromCloneable.newCopyBuilder().apply {
        setOwner(arrayClassDescriptor)
        setVisibility(DescriptorVisibilities.PUBLIC)
        setReturnType(arrayClassDescriptor.defaultType)
        setDispatchReceiverParameter(arrayClassDescriptor.thisAsReceiverParameter)
    }.build()!!

    private fun SimpleFunctionDescriptor.isMutabilityViolation(isMutable: Boolean): Boolean {
        val owner = containingDeclaration as ClassDescriptor
        val jvmDescriptor = computeJvmDescriptor()

        if ((SignatureBuildingComponents.signature(owner, jvmDescriptor) in MUTABLE_METHOD_SIGNATURES) xor isMutable) return true

        return DFS.ifAny<CallableMemberDescriptor>(
            listOf(this),
            { it.original.overriddenDescriptors }
        ) { overridden ->
            overridden.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                    j2kClassMapper.isMutable(overridden.containingDeclaration as ClassDescriptor)
        }
    }

    private fun FunctionDescriptor.getJdkMethodStatus(): JDKMemberStatus {
        val owner = containingDeclaration as ClassDescriptor
        val jvmDescriptor = computeJvmDescriptor()
        var result: JDKMemberStatus? = null
        return DFS.dfs<ClassDescriptor, JDKMemberStatus>(
            listOf(owner),
            {
                // Search through mapped supertypes to determine that Set.toArray should be invisible, while we have only
                // Collection.toArray there explicitly
                // Note, that we can't find j.u.Collection.toArray within overriddenDescriptors of j.u.Set.toArray
                it.typeConstructor.supertypes.mapNotNull {
                    (it.constructor.declarationDescriptor?.original as? ClassDescriptor)?.getJavaAnalogue()
                }
            },
            object : DFS.AbstractNodeHandler<ClassDescriptor, JDKMemberStatus>() {
                override fun beforeChildren(javaClassDescriptor: ClassDescriptor): Boolean {
                    val signature = SignatureBuildingComponents.signature(javaClassDescriptor, jvmDescriptor)
                    when (signature) {
                        in HIDDEN_METHOD_SIGNATURES -> result = JDKMemberStatus.HIDDEN
                        in VISIBLE_METHOD_SIGNATURES -> result = JDKMemberStatus.VISIBLE
                        in DROP_LIST_METHOD_SIGNATURES -> result = JDKMemberStatus.DROP
                    }

                    return result == null
                }

                override fun result() = result ?: JDKMemberStatus.NOT_CONSIDERED
            })
    }

    private enum class JDKMemberStatus {
        HIDDEN, VISIBLE, NOT_CONSIDERED, DROP
    }

    private fun ClassDescriptor.getJavaAnalogue(): LazyJavaClassDescriptor? {
        // Prevents recursive dependency: memberScope(Any) -> memberScope(Object) -> memberScope(Any)
        // No additional members should be added to Any
        if (KotlinBuiltIns.isAny(this)) return null

        // Optimization: only classes under kotlin.* can have Java analogues
        if (!KotlinBuiltIns.isUnderKotlinPackage(this)) return null

        val fqName = fqNameUnsafe
        if (!fqName.isSafe) return null
        val javaAnalogueFqName = JavaToKotlinClassMap.mapKotlinToJava(fqName)?.asSingleFqName() ?: return null

        return ownerModuleDescriptor.resolveClassByFqName(javaAnalogueFqName, NoLookupLocation.FROM_BUILTINS) as? LazyJavaClassDescriptor
    }

    override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> {
        if (classDescriptor.kind != ClassKind.CLASS || !isAdditionalBuiltInsFeatureSupported) return emptyList()

        val javaAnalogueDescriptor = classDescriptor.getJavaAnalogue() ?: return emptyList()

        val defaultKotlinVersion =
            j2kClassMapper.mapJavaToKotlin(javaAnalogueDescriptor.fqNameSafe, FallbackBuiltIns.Instance) ?: return emptyList()

        val substitutor = createMappedTypeParametersSubstitution(defaultKotlinVersion, javaAnalogueDescriptor).buildSubstitutor()

        fun ConstructorDescriptor.isEffectivelyTheSameAs(javaConstructor: ConstructorDescriptor) =
            OverridingUtil.getBothWaysOverridability(this, javaConstructor.substitute(substitutor)) ==
                    OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE

        return javaAnalogueDescriptor.constructors.filter { javaConstructor ->
            javaConstructor.visibility.isPublicAPI &&
                    defaultKotlinVersion.constructors.none { it.isEffectivelyTheSameAs(javaConstructor) } &&
                    !javaConstructor.isTrivialCopyConstructorFor(classDescriptor) &&
                    !KotlinBuiltIns.isDeprecated(javaConstructor) &&
                    SignatureBuildingComponents.signature(
                        javaAnalogueDescriptor,
                        javaConstructor.computeJvmDescriptor()
                    ) !in HIDDEN_CONSTRUCTOR_SIGNATURES
        }.map { javaConstructor ->
            javaConstructor.newCopyBuilder().apply {
                setOwner(classDescriptor)
                setReturnType(classDescriptor.defaultType)
                setPreserveSourceElement()
                setSubstitution(substitutor.substitution)
                if (SignatureBuildingComponents.signature(
                        javaAnalogueDescriptor, javaConstructor.computeJvmDescriptor()
                    ) !in VISIBLE_CONSTRUCTOR_SIGNATURES
                ) {
                    setAdditionalAnnotations(notConsideredDeprecation)
                }

            }.build() as ClassConstructorDescriptor
        }
    }

    override fun isFunctionAvailable(classDescriptor: ClassDescriptor, functionDescriptor: SimpleFunctionDescriptor): Boolean {
        val javaAnalogueClassDescriptor = classDescriptor.getJavaAnalogue() ?: return true

        if (!functionDescriptor.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)) return true
        if (!isAdditionalBuiltInsFeatureSupported) return false

        val jvmDescriptor = functionDescriptor.computeJvmDescriptor()
        return javaAnalogueClassDescriptor
            .unsubstitutedMemberScope
            .getContributedFunctions(functionDescriptor.name, NoLookupLocation.FROM_BUILTINS)
            .any { it.computeJvmDescriptor() == jvmDescriptor }
    }

    private fun ConstructorDescriptor.isTrivialCopyConstructorFor(classDescriptor: ClassDescriptor): Boolean =
        valueParameters.size == 1 &&
                valueParameters.single().type.constructor.declarationDescriptor?.fqNameUnsafe == classDescriptor.fqNameUnsafe
}

private class FallbackBuiltIns private constructor() : KotlinBuiltIns(LockBasedStorageManager("FallbackBuiltIns")) {
    init {
        createBuiltInsModule(true)
    }

    companion object {
        @JvmStatic
        val Instance: KotlinBuiltIns =
            FallbackBuiltIns()
    }

    override fun getPlatformDependentDeclarationFilter() = PlatformDependentDeclarationFilter.All
}
