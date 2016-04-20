/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.components.JavaResolverCache
import org.jetbrains.kotlin.load.java.lazy.descriptors.LazyJavaClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.platform.createMappedTypeParametersSubstitution
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.AdditionalClassPartsProvider
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.types.DelegatingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.Serializable
import java.util.*

open class BuiltInClassesAreSerializableOnJvm(
        private val moduleDescriptor: ModuleDescriptor,
        deferredOwnerModuleDescriptor: () -> ModuleDescriptor
) : AdditionalClassPartsProvider {
    private val j2kClassMap = JavaToKotlinClassMap.INSTANCE

    private val ownerModuleDescriptor: ModuleDescriptor by lazy(deferredOwnerModuleDescriptor)

    private val mockSerializableType = createMockJavaIoSerializableType()

    private fun createMockJavaIoSerializableType(): KotlinType {
        val mockJavaIoPackageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName("java.io")) {
            override fun getMemberScope() = MemberScope.Empty
        }

        //NOTE: can't reference anyType right away, because this is sometimes called when JvmBuiltIns are initializing
        val superTypes = listOf(object : DelegatingType() {
            override fun getDelegate(): KotlinType {
                return moduleDescriptor.builtIns.anyType
            }
        })

        val mockSerializableClass = ClassDescriptorImpl(
                mockJavaIoPackageFragment, Name.identifier("Serializable"), Modality.ABSTRACT, ClassKind.INTERFACE, superTypes, SourceElement.NO_SOURCE
        )

        mockSerializableClass.initialize(MemberScope.Empty, emptySet(), null)
        return mockSerializableClass.defaultType
    }

    override fun getSupertypes(classDescriptor: DeserializedClassDescriptor): Collection<KotlinType> {
        if (isSerializableInJava(classDescriptor.fqNameSafe)) {
            return listOf(mockSerializableType)
        }
        else return listOf()
    }

    override fun getFunctions(name: Name, classDescriptor: DeserializedClassDescriptor): Collection<SimpleFunctionDescriptor> =
            getAdditionalFunctions(classDescriptor) {
                it.getContributedFunctions(name, NoLookupLocation.FROM_BUILTINS)
            }
            .map {
                additionalMember ->
                additionalMember.newCopyBuilder().apply {
                    setOwner(classDescriptor)
                    setDispatchReceiverParameter(classDescriptor.thisAsReceiverParameter)
                    setPreserveSourceElement()
                    setSubstitution(createMappedTypeParametersSubstitution(
                            additionalMember.containingDeclaration as ClassDescriptor, classDescriptor))
                }.build()!!
            }

    override fun getFunctionsNames(classDescriptor: DeserializedClassDescriptor): Collection<Name> =
            getAdditionalFunctions(classDescriptor) {
                it.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS).filterIsInstance<SimpleFunctionDescriptor>()
            }.map(SimpleFunctionDescriptor::getName)

    private fun getAdditionalFunctions(
            classDescriptor: DeserializedClassDescriptor,
            functionsByScope: (MemberScope) -> Collection<SimpleFunctionDescriptor>
    ): Collection<SimpleFunctionDescriptor> {
        val javaAnalogueDescriptor = classDescriptor.getJavaAnalogue() ?: return emptyList()

        val kotlinClassDescriptors = j2kClassMap.mapPlatformClass(javaAnalogueDescriptor.fqNameSafe, DefaultBuiltIns.Instance)
        val kotlinMutableClassIfContainer = kotlinClassDescriptors.lastOrNull() ?: return emptyList()
        val kotlinVersions = SmartSet.create(kotlinClassDescriptors.map { it.fqNameSafe })

        val isMutable = j2kClassMap.isMutable(classDescriptor)

        val fakeJavaClassDescriptor =
                javaAnalogueDescriptor.copy(
                        javaResolverCache = JavaResolverCache.EMPTY,
                        additionalSupertypeClassDescriptor = kotlinMutableClassIfContainer)

        val scope = fakeJavaClassDescriptor.unsubstitutedMemberScope

        return functionsByScope(scope)
                .filter { analogueMember ->
                    if (analogueMember.kind != CallableMemberDescriptor.Kind.DECLARATION) return@filter false
                    if (!analogueMember.visibility.isPublicAPI) return@filter false
                    if (KotlinBuiltIns.isDeprecated(analogueMember)) return@filter false

                    if (analogueMember.overriddenDescriptors.any {
                        it.containingDeclaration.fqNameSafe in kotlinVersions
                    }) return@filter false

                    !analogueMember.isInBlackOrMutabilityViolation(isMutable)
                }
    }

    private fun SimpleFunctionDescriptor.isInBlackOrMutabilityViolation(isMutable: Boolean): Boolean {
        val jvmDescriptor = computeJvmDescriptor()
        val owner = containingDeclaration as ClassDescriptor
        if (DFS.ifAny(
                listOf(owner),
                {
                    // Search through mapped supertypes to determine that Set.toArray is in blacklist, while we have only
                    // Collection.toArray there explicitly
                    // Note, that we can't find j.u.Collection.toArray within overriddenDescriptors of j.u.Set.toArray
                    it.typeConstructor.supertypes.mapNotNull {
                        (it.constructor.declarationDescriptor?.original as? ClassDescriptor)?.getJavaAnalogue()
                    }
                }
        ) {
            javaClassDescriptor ->
            signature(javaClassDescriptor, jvmDescriptor) in BLACK_LIST_METHOD_SIGNATURES
        }) return true

        if ((signature(owner, jvmDescriptor) in MUTABLE_METHOD_SIGNATURES) xor isMutable) return true

        return DFS.ifAny<CallableMemberDescriptor>(
                listOf(this),
                { it.original.overriddenDescriptors }
        ) {
            overridden ->
            overridden.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                j2kClassMap.isMutable(overridden.containingDeclaration as ClassDescriptor)
        }
    }

    private fun signature(javaClassDescriptor: ClassDescriptor, jvmDescriptor: String) = javaClassDescriptor.internalName + "." + jvmDescriptor

    private fun ClassDescriptor.getJavaAnalogue(): LazyJavaClassDescriptor? {
        // Prevents recursive dependency: memberScope(Any) -> memberScope(Object) -> memberScope(Any)
        // No additional members should be added to Any
        if (isAny) return null

        val fqName = fqNameUnsafe.check { it.isSafe }?.toSafe() ?: return null
        val javaAnalogueFqName = j2kClassMap.mapKotlinToJava(fqName.toUnsafe())?.asSingleFqName() ?: return null

        return ownerModuleDescriptor.resolveClassByFqName(javaAnalogueFqName, NoLookupLocation.FROM_BUILTINS) as? LazyJavaClassDescriptor
    }

    override fun getConstructors(classDescriptor: DeserializedClassDescriptor): Collection<ConstructorDescriptor> {
        if (classDescriptor.kind != ClassKind.CLASS) return emptyList()

        val javaAnalogueDescriptor = classDescriptor.getJavaAnalogue() ?: return emptyList()

        val defaultKotlinVersion =
                j2kClassMap.mapJavaToKotlin(javaAnalogueDescriptor.fqNameSafe, DefaultBuiltIns.Instance) ?: return emptyList()

        val substitutor = createMappedTypeParametersSubstitution(defaultKotlinVersion, javaAnalogueDescriptor).buildSubstitutor()

        fun ConstructorDescriptor.isEffectivelyTheSameAs(javaConstructor: ConstructorDescriptor) =
                OverridingUtil.getBothWaysOverridability(this, javaConstructor.substitute(substitutor)) ==
                    OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE

        return javaAnalogueDescriptor.constructors.filter {
            javaConstructor ->
            javaConstructor.visibility.isPublicAPI &&
                defaultKotlinVersion.constructors.none { it.isEffectivelyTheSameAs(javaConstructor) } &&
                !javaConstructor.isTrivialCopyConstructorFor(classDescriptor) &&
                !KotlinBuiltIns.isDeprecated(javaConstructor)
        }.map {
            javaConstructor ->
            javaConstructor.newCopyBuilder().apply {
                setOwner(classDescriptor)
                setReturnType(classDescriptor.defaultType)
                setPreserveSourceElement()
                setSubstitution(substitutor.substitution)
            }.build() as ConstructorDescriptor
        }
    }

    private fun ConstructorDescriptor.isTrivialCopyConstructorFor(classDescriptor: DeserializedClassDescriptor): Boolean
        = valueParameters.size == 1 &&
            valueParameters.single().type.constructor.declarationDescriptor?.fqNameUnsafe == classDescriptor.fqNameUnsafe

    companion object {
        fun isSerializableInJava(classFqName: FqName): Boolean {
            val fqNameUnsafe = classFqName.toUnsafe()
            if (fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.array || KotlinBuiltIns.isPrimitiveArray(fqNameUnsafe)) {
                return true
            }
            val javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(fqNameUnsafe) ?: return false
            val classViaReflection = try {
                Class.forName(javaClassId.asSingleFqName().asString())
            }
            catch (e: ClassNotFoundException) {
                return false
            }
            return Serializable::class.java.isAssignableFrom(classViaReflection)
        }

        private val BLACK_LIST_METHOD_SIGNATURES: Set<String> =
                buildPrimitiveValueMethodsSet() +

                "java/lang/annotation/Annotation.annotationType()${javaLang("Class").t}" +

                inJavaUtil(
                        "Collection", "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;"
                ) +

                inJavaLang("String",
                           "codePointAt(I)I", "codePointBefore(I)I", "codePointCount(II)I", "compareToIgnoreCase($stringType)I",
                           "concat($stringType)$stringType", "contains(Ljava/lang/CharSequence;)Z",
                           "contentEquals(Ljava/lang/CharSequence;)Z", "contentEquals(Ljava/lang/StringBuffer;)Z",
                           "endsWith($stringType)Z", "equalsIgnoreCase($stringType)Z", "getBytes()[B", "getBytes(II[BI)V",
                           "getBytes($stringType)[B", "getBytes(Ljava/nio/charset/Charset;)[B", "getChars(II[CI)V",
                           "indexOf(I)I", "indexOf(II)I", "indexOf($stringType)I", "indexOf(${stringType}I)I",
                           "intern()$stringType", "isEmpty()Z", "lastIndexOf(I)I", "lastIndexOf(II)I",
                           "lastIndexOf($stringType)I", "lastIndexOf(${stringType}I)I", "matches($stringType)Z",
                           "offsetByCodePoints(II)I", "regionMatches(I${stringType}II)Z", "regionMatches(ZI${stringType}II)Z",
                           "replaceAll($stringType$stringType)$stringType", "replace(CC)$stringType",
                           "replaceFirst($stringType$stringType)$stringType",
                           "replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)$stringType",
                           "split(${stringType}I)[$stringType", "split($stringType)[$stringType",
                           "startsWith(${stringType}I)Z", "startsWith($stringType)Z", "substring(II)$stringType",
                           "substring(I)$stringType", "toCharArray()[C", "toLowerCase()$stringType",
                           "toLowerCase(Ljava/util/Locale;)$stringType", "toUpperCase()$stringType",
                           "toUpperCase(Ljava/util/Locale;)$stringType", "trim()$stringType") +

                inJavaLang("Double", "isInfinite()Z", "isNaN()Z") +
                inJavaLang("Float", "isInfinite()Z", "isNaN()Z") +

                inJavaUtil("Collection", "toArray([$objectType)[$objectType", "toArray()[$objectType")


        private val MUTABLE_METHOD_SIGNATURES: Set<String> =
                inJavaUtil("Collection", "removeIf(Ljava/util/function/Predicate;)Z") +

                inJavaUtil("List",
                        "sort(Ljava/util/Comparator;)V", "replaceAll(Ljava/util/function/UnaryOperator;)V") +

                inJavaUtil("Map",
                           "computeIfAbsent(${objectType}Ljava/util/function/Function;)$objectType",
                           "computeIfPresent(${objectType}Ljava/util/function/BiFunction;)$objectType",
                           "compute(${objectType}Ljava/util/function/BiFunction;)$objectType",
                           "merge($objectType${objectType}Ljava/util/function/BiFunction;)$objectType",
                           "putIfAbsent($objectType$objectType)$objectType",
                           "remove($objectType$objectType)Z", "replaceAll(Ljava/util/function/BiFunction;)V",
                           "replace($objectType$objectType)$objectType",
                           "replace($objectType$objectType$objectType)Z")

        private fun buildPrimitiveValueMethodsSet(): Set<String> =
                JvmPrimitiveType.values().flatMapTo(LinkedHashSet()) {
                    inJavaLang(it.wrapperFqName.shortName().asString(), "${it.javaKeywordName}Value()${it.desc}")
                }
    }
}

private val ClassDescriptor.isAny: Boolean get() = fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.any

private val String.t: String
    get() = "L$this;"

private val stringType = javaLang("String").t
private val objectType = javaLang("Object").t

private fun javaLang(name: String) = "java/lang/$name"
private fun javaUtil(name: String) = "java/util/$name"

private fun inJavaLang(name: String, vararg signatures: String) = inClass(javaLang(name), *signatures)
private fun inJavaUtil(name: String, vararg signatures: String) = inClass(javaUtil(name), *signatures)

private fun inClass(internalName: String, vararg signatures: String) = signatures.mapTo(LinkedHashSet()) { internalName + "." + it }
