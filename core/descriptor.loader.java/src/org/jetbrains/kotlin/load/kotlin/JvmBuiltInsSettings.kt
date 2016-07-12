/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.BuiltInsInitializer
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.annotations.createDeprecatedAnnotation
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
import org.jetbrains.kotlin.serialization.deserialization.PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.serialization.deserialization.PlatformDependentDeclarationFilter
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.WrappedType
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.Serializable
import java.util.*

open class JvmBuiltInsSettings(
        private val moduleDescriptor: ModuleDescriptor,
        storageManager: StorageManager,
        deferredOwnerModuleDescriptor: () -> ModuleDescriptor
) : AdditionalClassPartsProvider, PlatformDependentDeclarationFilter {
    private val j2kClassMap = JavaToKotlinClassMap.INSTANCE

    private val ownerModuleDescriptor: ModuleDescriptor by lazy(deferredOwnerModuleDescriptor)

    private val mockSerializableType = createMockJavaIoSerializableType()

    private val javaAnalogueClassesWithCustomSupertypeCache = storageManager.createCacheWithNotNullValues<FqName, ClassDescriptor>()

    // Most this properties are lazy because they depends on KotlinBuiltIns initialization that depends on JvmBuiltInsSettings object
    private val notConsideredDeprecation by storageManager.createLazyValue {
        moduleDescriptor.builtIns.createDeprecatedAnnotation(
                "This member is not fully supported by Kotlin compiler, so it may be absent or have different signature in next major version"
        ).let { AnnotationsImpl(listOf(it)) }
    }

    private fun createMockJavaIoSerializableType(): KotlinType {
        val mockJavaIoPackageFragment = object : PackageFragmentDescriptorImpl(moduleDescriptor, FqName("java.io")) {
            override fun getMemberScope() = MemberScope.Empty
        }

        //NOTE: can't reference anyType right away, because this is sometimes called when JvmBuiltIns are initializing
        val superTypes = listOf(object : WrappedType() {
            override fun unwrap() = moduleDescriptor.builtIns.anyType
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
            .mapNotNull {
                additionalMember ->
                val substitutedWithKotlinTypeParameters =
                        additionalMember.substitute(
                                createMappedTypeParametersSubstitution
                                (additionalMember.containingDeclaration as ClassDescriptor, classDescriptor).buildSubstitutor()
                        ) as SimpleFunctionDescriptor

                substitutedWithKotlinTypeParameters.newCopyBuilder().apply {
                    setOwner(classDescriptor)
                    setDispatchReceiverParameter(classDescriptor.thisAsReceiverParameter)
                    setPreserveSourceElement()
                    setSubstitution(UnsafeVarianceTypeSubstitution(moduleDescriptor.builtIns))

                    val memberStatus = additionalMember.getJdkMethodStatus()
                    when (memberStatus) {
                        JDKMemberStatus.BLACK_LIST -> {
                            // Black list methods in final class can't be overridden or called with 'super'
                            if (classDescriptor.isFinalClass) return@mapNotNull null
                            setHiddenForResolutionEverywhereBesideSupercalls()
                        }

                        JDKMemberStatus.NOT_CONSIDERED -> {
                            setAdditionalAnnotations(notConsideredDeprecation)
                        }

                        JDKMemberStatus.DROP -> return@mapNotNull null

                        JDKMemberStatus.WHITE_LIST -> {} // Do nothing
                    }

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

        val kotlinClassDescriptors = j2kClassMap.mapPlatformClass(javaAnalogueDescriptor.fqNameSafe, FallbackBuiltIns.Instance)
        val kotlinMutableClassIfContainer = kotlinClassDescriptors.lastOrNull() ?: return emptyList()
        val kotlinVersions = SmartSet.create(kotlinClassDescriptors.map { it.fqNameSafe })

        val isMutable = j2kClassMap.isMutable(classDescriptor)

        val fakeJavaClassDescriptor = javaAnalogueClassesWithCustomSupertypeCache.computeIfAbsent(javaAnalogueDescriptor.fqNameSafe) {
            javaAnalogueDescriptor.copy(
                    javaResolverCache = JavaResolverCache.EMPTY,
                    additionalSupertypeClassDescriptor = kotlinMutableClassIfContainer)
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

    private fun SimpleFunctionDescriptor.isMutabilityViolation(isMutable: Boolean): Boolean {
        val owner = containingDeclaration as ClassDescriptor
        val jvmDescriptor = computeJvmDescriptor()

        if ((SignatureBuildingComponents.signature(owner, jvmDescriptor) in MUTABLE_METHOD_SIGNATURES) xor isMutable) return true

        return DFS.ifAny<CallableMemberDescriptor>(
                listOf(this),
                { it.original.overriddenDescriptors }
        ) {
            overridden ->
            overridden.kind == CallableMemberDescriptor.Kind.DECLARATION &&
                j2kClassMap.isMutable(overridden.containingDeclaration as ClassDescriptor)
        }
    }

    private fun FunctionDescriptor.getJdkMethodStatus(): JDKMemberStatus {
        val owner = containingDeclaration as ClassDescriptor
        val jvmDescriptor = computeJvmDescriptor()
        var result: JDKMemberStatus? = null
        return DFS.dfs<ClassDescriptor, JDKMemberStatus>(
                listOf(owner),
                {
                    // Search through mapped supertypes to determine that Set.toArray is in blacklist, while we have only
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
                            in BLACK_LIST_METHOD_SIGNATURES -> { result = JDKMemberStatus.BLACK_LIST }
                            in WHITE_LIST_METHOD_SIGNATURES -> { result = JDKMemberStatus.WHITE_LIST }
                            in DROP_LIST_METHOD_SIGNATURES -> { result = JDKMemberStatus.DROP }
                        }

                        return result == null
                    }

                    override fun result() = result ?: JDKMemberStatus.NOT_CONSIDERED
                })
    }

    private enum class JDKMemberStatus {
        BLACK_LIST, WHITE_LIST, NOT_CONSIDERED, DROP
    }

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
                j2kClassMap.mapJavaToKotlin(javaAnalogueDescriptor.fqNameSafe, FallbackBuiltIns.Instance) ?: return emptyList()

        val substitutor = createMappedTypeParametersSubstitution(defaultKotlinVersion, javaAnalogueDescriptor).buildSubstitutor()

        fun ConstructorDescriptor.isEffectivelyTheSameAs(javaConstructor: ConstructorDescriptor) =
                OverridingUtil.getBothWaysOverridability(this, javaConstructor.substitute(substitutor)) ==
                    OverridingUtil.OverrideCompatibilityInfo.Result.OVERRIDABLE

        return javaAnalogueDescriptor.constructors.filter {
            javaConstructor ->
            javaConstructor.visibility.isPublicAPI &&
                defaultKotlinVersion.constructors.none { it.isEffectivelyTheSameAs(javaConstructor) } &&
                !javaConstructor.isTrivialCopyConstructorFor(classDescriptor) &&
                !KotlinBuiltIns.isDeprecated(javaConstructor) &&
                SignatureBuildingComponents.signature(javaAnalogueDescriptor, javaConstructor.computeJvmDescriptor()) !in BLACK_LIST_CONSTRUCTOR_SIGNATURES
        }.map {
            javaConstructor ->
            javaConstructor.newCopyBuilder().apply {
                setOwner(classDescriptor)
                setReturnType(classDescriptor.defaultType)
                setPreserveSourceElement()
                setSubstitution(substitutor.substitution)

                if (SignatureBuildingComponents.signature(javaAnalogueDescriptor, javaConstructor.computeJvmDescriptor()) !in WHITE_LIST_CONSTRUCTOR_SIGNATURES) {
                    setAdditionalAnnotations(notConsideredDeprecation)
                }

            }.build() as ConstructorDescriptor
        }
    }

    override fun isFunctionAvailable(classDescriptor: DeserializedClassDescriptor, functionDescriptor: SimpleFunctionDescriptor): Boolean {
        if (!functionDescriptor.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)) return true
        val javaAnalogueClassDescriptor = classDescriptor.getJavaAnalogue() ?: return true

        val jvmDescriptor = functionDescriptor.computeJvmDescriptor()
        return javaAnalogueClassDescriptor
                    .unsubstitutedMemberScope
                    .getContributedFunctions(functionDescriptor.name, NoLookupLocation.FROM_BUILTINS)
                    .any { it.computeJvmDescriptor() == jvmDescriptor }
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

        val DROP_LIST_METHOD_SIGNATURES: Set<String> =
                SignatureBuildingComponents.inJavaUtil(
                        "Collection",
                        "toArray()[Ljava/lang/Object;", "toArray([Ljava/lang/Object;)[Ljava/lang/Object;") +

                "java/lang/annotation/Annotation.annotationType()Ljava/lang/Class;"

        val BLACK_LIST_METHOD_SIGNATURES: Set<String> =
            signatures {
                buildPrimitiveValueMethodsSet() +

                inJavaUtil("List", "sort(Ljava/util/Comparator;)V") +

                inJavaLang("String",
                           "codePointAt(I)I", "codePointBefore(I)I", "codePointCount(II)I", "compareToIgnoreCase(Ljava/lang/String;)I",
                           "concat(Ljava/lang/String;)Ljava/lang/String;", "contains(Ljava/lang/CharSequence;)Z",
                           "contentEquals(Ljava/lang/CharSequence;)Z", "contentEquals(Ljava/lang/StringBuffer;)Z",
                           "endsWith(Ljava/lang/String;)Z", "equalsIgnoreCase(Ljava/lang/String;)Z", "getBytes()[B", "getBytes(II[BI)V",
                           "getBytes(Ljava/lang/String;)[B", "getBytes(Ljava/nio/charset/Charset;)[B", "getChars(II[CI)V",
                           "indexOf(I)I", "indexOf(II)I", "indexOf(Ljava/lang/String;)I", "indexOf(Ljava/lang/String;I)I",
                           "intern()Ljava/lang/String;", "isEmpty()Z", "lastIndexOf(I)I", "lastIndexOf(II)I",
                           "lastIndexOf(Ljava/lang/String;)I", "lastIndexOf(Ljava/lang/String;I)I", "matches(Ljava/lang/String;)Z",
                           "offsetByCodePoints(II)I", "regionMatches(ILjava/lang/String;II)Z", "regionMatches(ZILjava/lang/String;II)Z",
                           "replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", "replace(CC)Ljava/lang/String;",
                           "replaceFirst(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;",
                           "replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;",
                           "split(Ljava/lang/String;I)[Ljava/lang/String;", "split(Ljava/lang/String;)[Ljava/lang/String;",
                           "startsWith(Ljava/lang/String;I)Z", "startsWith(Ljava/lang/String;)Z", "substring(II)Ljava/lang/String;",
                           "substring(I)Ljava/lang/String;", "toCharArray()[C", "toLowerCase()Ljava/lang/String;",
                           "toLowerCase(Ljava/util/Locale;)Ljava/lang/String;", "toUpperCase()Ljava/lang/String;",
                           "toUpperCase(Ljava/util/Locale;)Ljava/lang/String;", "trim()Ljava/lang/String;") +

                inJavaLang("Double", "isInfinite()Z", "isNaN()Z") +
                inJavaLang("Float", "isInfinite()Z", "isNaN()Z") +

                inJavaLang("Enum", "getDeclaringClass()Ljava/lang/Class;", "finalize()V")
            }

        private fun buildPrimitiveValueMethodsSet(): Set<String> =
            signatures {
                listOf(JvmPrimitiveType.BOOLEAN, JvmPrimitiveType.CHAR).flatMapTo(LinkedHashSet()) {
                    inJavaLang(it.wrapperFqName.shortName().asString(), "${it.javaKeywordName}Value()${it.desc}")
                }
            }

        val WHITE_LIST_METHOD_SIGNATURES: Set<String> =
                signatures {
                    inJavaLang("CharSequence",
                            "codePoints()Ljava/util/stream/IntStream;", "chars()Ljava/util/stream/IntStream;") +

                    inJavaUtil("Iterator",
                               "forEachRemaining(Ljava/util/function/Consumer;)V") +

                    inJavaLang("Iterable",
                               "forEach(Ljava/util/function/Consumer;)V", "spliterator()Ljava/util/Spliterator;") +

                    inJavaLang("Throwable",
                               "setStackTrace([Ljava/lang/StackTraceElement;)V", "fillInStackTrace()Ljava/lang/Throwable;",
                               "getLocalizedMessage()Ljava/lang/String;", "printStackTrace()V", "printStackTrace(Ljava/io/PrintStream;)V",
                               "printStackTrace(Ljava/io/PrintWriter;)V", "getStackTrace()[Ljava/lang/StackTraceElement;",
                               "initCause(Ljava/lang/Throwable;)Ljava/lang/Throwable;", "getSuppressed()[Ljava/lang/Throwable;",
                               "addSuppressed(Ljava/lang/Throwable;)V") +

                    inJavaUtil("Collection",
                               "spliterator()Ljava/util/Spliterator;", "parallelStream()Ljava/util/stream/Stream;",
                               "stream()Ljava/util/stream/Stream;", "removeIf(Ljava/util/function/Predicate;)Z") +

                    inJavaUtil("List",
                               "replaceAll(Ljava/util/function/UnaryOperator;)V") +

                    inJavaUtil("Map",
                               "getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                               "forEach(Ljava/util/function/BiConsumer;)V", "replaceAll(Ljava/util/function/BiFunction;)V",
                               "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                               "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                               "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                               "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z",
                               "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                               "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                               "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;")
                }

        val MUTABLE_METHOD_SIGNATURES: Set<String> =
            signatures {
                inJavaUtil("Collection", "removeIf(Ljava/util/function/Predicate;)Z") +

                inJavaUtil("List", "replaceAll(Ljava/util/function/UnaryOperator;)V", "sort(Ljava/util/Comparator;)V") +

                inJavaUtil("Map",
                           "computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;",
                           "computeIfPresent(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                           "compute(Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                           "merge(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/function/BiFunction;)Ljava/lang/Object;",
                           "putIfAbsent(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                           "remove(Ljava/lang/Object;Ljava/lang/Object;)Z", "replaceAll(Ljava/util/function/BiFunction;)V",
                           "replace(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                           "replace(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Z")
            }

        val BLACK_LIST_CONSTRUCTOR_SIGNATURES: Set<String> =
            signatures {
                buildPrimitiveStringConstructorsSet() +
                inJavaLang("Float", *constructors("D")) +
                inJavaLang("String", *constructors(
                        "[C", "[CII", "[III", "[BIILjava/lang/String;",
                        "[BIILjava/nio/charset/Charset;",
                        "[BLjava/lang/String;",
                        "[BLjava/nio/charset/Charset;",
                        "[BII", "[B",
                        "Ljava/lang/StringBuffer;",
                        "Ljava/lang/StringBuilder;"
                ))
            }

        val WHITE_LIST_CONSTRUCTOR_SIGNATURES: Set<String> =
                signatures {
                    inJavaLang("Throwable", *constructors("Ljava/lang/String;Ljava/lang/Throwable;ZZ"))
                }

        private fun buildPrimitiveStringConstructorsSet(): Set<String> =
            signatures {
                listOf(JvmPrimitiveType.BOOLEAN, JvmPrimitiveType.BYTE, JvmPrimitiveType.DOUBLE, JvmPrimitiveType.FLOAT,
                      JvmPrimitiveType.BYTE, JvmPrimitiveType.INT, JvmPrimitiveType.LONG, JvmPrimitiveType.SHORT
                ).flatMapTo(LinkedHashSet()) {
                    // java/lang/<Wrapper>.<init>(Ljava/lang/String;)V
                    inJavaLang(it.wrapperFqName.shortName().asString(), *constructors("Ljava/lang/String;"))
                }
            }
    }
}

private val ClassDescriptor.isAny: Boolean get() = fqNameUnsafe == KotlinBuiltIns.FQ_NAMES.any

private class FallbackBuiltIns private constructor() : KotlinBuiltIns(LockBasedStorageManager()) {
    companion object {
        private val initializer = BuiltInsInitializer {
            FallbackBuiltIns()
        }

        @JvmStatic
        val Instance: KotlinBuiltIns
            get() = initializer.get()
    }

    override fun getPlatformDependentDeclarationFilter() = PlatformDependentDeclarationFilter.All
}
