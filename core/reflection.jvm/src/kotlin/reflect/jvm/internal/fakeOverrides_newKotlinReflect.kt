/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.metadata.ClassKind
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.internal.types.MutableCollectionKClass
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaMethod

internal fun getAllMembers_newKotlinReflectImpl(kClass: KClassImpl<*>): Collection<DescriptorKCallable<*>> {
    val membersReadOnly = kClass.data.value.fakeOverrideMembers
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    val isKotlin = kClass.java.isKotlin
    val doNeedToFilterOutStatics =
        membersReadOnly.containsInheritedStatics && kClass.classKind != ClassKind.ENUM_CLASS && isKotlin
    val doNeedToShrinkMembers = membersReadOnly.containsPackagePrivate || doNeedToFilterOutStatics
    val membersMutable = when (doNeedToShrinkMembers) {
        true -> lazyOf(
            membersReadOnly.members.filterNotTo(HashMap(membersReadOnly.members.size)) { (_, member) ->
                doNeedToFilterOutStatics && member.isStatic ||
                    member.isPackagePrivate && member.container.jClass.`package` != kClass.java.`package`
            }
        )
        false -> lazy(LazyThreadSafetyMode.NONE) { HashMap(membersReadOnly.members) }
    }
    // Privates don't override anything, so it's fine to collect them in a separate map
    val kotlinDeclaredPrivates: MutableMembersKotlinSignatureMap = HashMap()
    // Populating the 'members' list with things which are not inherited but appear in the 'members' list
    for (declaredMember in kClass.declaredDescriptorKCallableMembers) {
        when {
            // static members are not inherited,
            // but the immediate statics in interfaces must appear in the 'members' list
            declaredMember.isStaticMethodInInterface(kClass) -> {
                check(!isKotlin) { "Kotlin doesn't have statics" }
                val signature = declaredMember.toEquatableCallableSignature(EqualityMode.JavaSignature)
                membersMutable.value[signature] = declaredMember
            }

            // private members are not inherited, but immediate private members must appear in the 'members' list
            declaredMember.visibility == KVisibility.PRIVATE -> {
                if (isKotlin) {
                    val signature = declaredMember.toEquatableCallableSignature(EqualityMode.KotlinSignature)
                    kotlinDeclaredPrivates[signature] = declaredMember
                } else {
                    val signature = declaredMember.toEquatableCallableSignature(EqualityMode.JavaSignature)
                    membersMutable.value[signature] = declaredMember
                }
            }
        }
    }
    val result = (if (membersMutable.isInitialized()) membersMutable.value else membersReadOnly.members).values
    return addCollectionsOptimizingEmpty(result, kotlinDeclaredPrivates.values)
}

private fun <T> addCollectionsOptimizingEmpty(a: Collection<T>, b: Collection<T>): Collection<T> = when {
    a.isEmpty() -> b
    b.isEmpty() -> a
    else -> a + b
}

private fun nonDenotableSupertypesAreNotPossible(): Nothing = error("Non-denotable supertypes are not possible")
internal fun starProjectionSupertypesAreNotPossible(): Nothing = error("Star projection supertypes are not possible")

private object CovariantOverrideComparator : Comparator<DescriptorKCallable<*>> {
    override fun compare(a: DescriptorKCallable<*>, b: DescriptorKCallable<*>): Int {
        val typeParametersEliminator = a.typeParameters.substitutedWith(b.typeParameters)
            ?: error("Intersection overrides can't have different type parameters sizes. It must be a compiler diagnostic")
        val aReturnType =
            typeParametersEliminator.substitute(a.returnType).type ?: starProjectionSupertypesAreNotPossible()
        val bReturnType = b.returnType

        val aIsSubtypeOfB = aReturnType.isSubtypeOf(bReturnType)
        val bIsSubtypeOfA = bReturnType.isSubtypeOf(aReturnType)
        if (aIsSubtypeOfB && !bIsSubtypeOfA) return -1
        if (bIsSubtypeOfA && !aIsSubtypeOfB) return 1

        val isAFlexible = with(ReflectTypeSystemContext) { (aReturnType as? AbstractKType)?.isFlexible() == true }
        val isBFlexible = with(ReflectTypeSystemContext) { (bReturnType as? AbstractKType)?.isFlexible() == true }
        if (isBFlexible && !isAFlexible) return -1
        if (isAFlexible && !isBFlexible) return 1

        return 0
    }
}

private typealias MembersJavaSignatureMap = Map<EquatableCallableSignature<EqualityMode.JavaSignature>, DescriptorKCallable<*>>
private typealias MutableMembersJavaSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.JavaSignature>, DescriptorKCallable<*>>
private typealias MutableMembersKotlinSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.KotlinSignature>, DescriptorKCallable<*>>

/**
 * Auxiliary class to help build 'KClass.members' for every KClass.
 *
 * User facing 'KClass.members' is not "transitive". This auxiliary class is "transitive".
 *
 * By "transitive" we mean that 'KClass.members' of every inheritor class/interface are a strict superset
 * of their parent classes' `KClass.members`
 */
internal data class FakeOverrideMembers(
    val members: MembersJavaSignatureMap,
    val containsInheritedStatics: Boolean,
    val containsPackagePrivate: Boolean,
)

private fun DescriptorKCallable<*>.isStaticMethodInInterface(kClass: KClassImpl<*>): Boolean =
    isStatic && kClass.classKind == ClassKind.INTERFACE && !isJavaField

private fun skipDeclaredMember(kClass: KClassImpl<*>, member: DescriptorKCallable<*>): Boolean =
    member.visibility == KVisibility.PRIVATE ||
        // static methods (but not fields) in interfaces are never inherited (neither in Java nor in Kotlin)
        member.isStaticMethodInInterface(kClass)

internal fun computeFakeOverrideMembers(kClass: KClassImpl<*>): FakeOverrideMembers {
    val javaSignaturesMap: MutableMembersJavaSignatureMap = HashMap()
    val thisReceiver = kClass.descriptor.thisAsReceiverParameter
    var containsInheritedStatics = false
    var containsPackagePrivate = false
    val isKotlin = kClass.java.isKotlin
    val declaredKotlinMembers: MutableMembersKotlinSignatureMap = HashMap()
    if (isKotlin) {
        for (member in kClass.declaredDescriptorKCallableMembers) {
            if (skipDeclaredMember(kClass, member)) continue
            declaredKotlinMembers[member.toEquatableCallableSignature(EqualityMode.KotlinSignature)] = member
        }
    }
    for (supertype in kClass.supertypes) {
        val supertypeKClass = supertype.classifier as? KClass<*> ?: nonDenotableSupertypesAreNotPossible()
        val substitutor = KTypeSubstitutor.create(supertype)
        val supertypeMembers = supertypeKClass.fakeOverrideMembers // Recursive call
        containsInheritedStatics = containsInheritedStatics || supertypeMembers.containsInheritedStatics
        containsPackagePrivate = containsPackagePrivate || supertypeMembers.containsPackagePrivate
        for ((_, notSubstitutedMember) in supertypeMembers.members) {
            val overriddenStorage = notSubstitutedMember.overriddenStorage.copy(
                instanceReceiverParameter = if (notSubstitutedMember.isStatic) null else thisReceiver,
                typeSubstitutor = notSubstitutedMember.overriddenStorage.typeSubstitutor.combinedWith(substitutor),
                isFakeOverride = true,
            )
            val member = notSubstitutedMember.shallowCopy(overriddenStorage)
            val kotlinSignature = member.toEquatableCallableSignature(EqualityMode.KotlinSignature)
            if (declaredKotlinMembers.contains(kotlinSignature)) continue
            // Inherited signatures are always compared by JvmSignatures. Even for kotlin classes.
            javaSignaturesMap.merge_Jdk6Compatibility(
                kotlinSignature.withEqualityMode(EqualityMode.JavaSignature),
                member
            ) { a, b ->
                val c = minOf(a, b, CovariantOverrideComparator)
                when (a is KFunction<*> && b is KFunction<*>) {
                    true -> c.shallowCopy(
                        c.overriddenStorage.copy(
                            forceIsOperator = a.isOperator || b.isOperator,
                            forceIsInfix = a.isInfix || b.isInfix,
                            forceIsInline = a.isInline || b.isInline,
                            forceIsExternal = a.isExternal || b.isExternal,
                            modality = minOf(a, b, modalityIntersectionOverrideComparator).modality
                        )
                    )
                    else -> c
                }
            }
        }
    }
    for ((kotlinSignature, member) in declaredKotlinMembers) {
        containsInheritedStatics = containsInheritedStatics || member.isStatic
        containsPackagePrivate = containsPackagePrivate || member.isPackagePrivate
        javaSignaturesMap[kotlinSignature.withEqualityMode(EqualityMode.JavaSignature)] = member
    }
    if (!isKotlin) {
        for (member in kClass.declaredDescriptorKCallableMembers) {
            if (skipDeclaredMember(kClass, member)) continue
            containsInheritedStatics = containsInheritedStatics || member.isStatic
            containsPackagePrivate = containsPackagePrivate || member.isPackagePrivate
            javaSignaturesMap[member.toEquatableCallableSignature(EqualityMode.JavaSignature)] = member
        }
    }
    return FakeOverrideMembers(javaSignaturesMap, containsInheritedStatics, containsPackagePrivate)
}

/**
 * MutableMap.merge that is available on JDK < 8. Reflect has to be able to work in JDK 6
 */
private fun <K, V> MutableMap<K, V>.merge_Jdk6Compatibility(key: K, value: V, remappingFunction: (V, V) -> V): V {
    val newValue = when (val oldValue = get(key)) {
        null -> value
        else -> remappingFunction(oldValue, value)
    }
    if (newValue == null) {
        remove(key)
    } else {
        this[key] = newValue
    }
    return newValue
}

private val modalityIntersectionOverrideComparator: Comparator<DescriptorKCallable<*>> = compareBy(
    // Deprioritize interfaces, prioritize classes
    { (it.container as? KClass<*>)?.java?.isInterface == true },
    // If there are multiple superclasses (not interfaces), deprioritize kotlin.Any.
    // For instance, equals/hashCode/toString which come from interfaces have kotlin.Any container.
    { it.container == Any::class },
)

internal val DescriptorKCallable<*>.isStatic: Boolean
    get() = instanceReceiverParameter == null

private val DescriptorKCallable<*>.isJavaField: Boolean
    get() = this is KProperty<*> && this.javaField?.declaringClass?.isKotlin == false

private val KClass<*>.fakeOverrideMembers: FakeOverrideMembers
    get() = when (this) {
        is KClassImpl<*> -> data.value.fakeOverrideMembers
        is MutableCollectionKClass<*> -> klass.fakeOverrideMembers
        else -> error("Unknown type ${this::class}")
    }

private fun <T : EqualityMode> DescriptorKCallable<*>.toEquatableCallableSignature(equalityMode: T): EquatableCallableSignature<T> {
    val parameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when (this) {
        is KProperty<*> if javaField?.declaringClass?.isKotlin == false -> SignatureKind.FIELD_IN_JAVA_CLASS
        is KProperty<*> -> SignatureKind.PROPERTY
        is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val javaMethod = (this as? KFunction<*>)?.javaMethod
    val javaGenericParameterTypes = javaMethod?.genericParameterTypes.orEmpty().toList()
    val javaParameterTypes = javaMethod?.parameterTypes.orEmpty()
    check(javaParameterTypes.size == javaGenericParameterTypes.size)
    val jvmNameIfFunction = javaMethod?.name.orEmpty()
    return EquatableCallableSignature(
        kind,
        name,
        jvmNameIfFunction,
        typeParameters,
        parameterTypes,
        javaGenericParameterTypes.zip(javaParameterTypes),
        isStatic,
        equalityMode,
    )
}

internal val Class<*>.isKotlin: Boolean
    get() = this == Enum::class.java || // Enum and Comparable JDK classes are immediately mapped to Kotlin classes
        this == Comparable::class.java ||
        getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredDescriptorKCallableMembers: Collection<DescriptorKCallable<*>>
    get() = declaredMembers as Collection<DescriptorKCallable<*>>

private fun List<KTypeParameter>.substitutedWith(arguments: List<KTypeParameter>): KTypeSubstitutor? {
    if (size != arguments.size) return null
    if (arguments.isEmpty() || isEmpty()) return KTypeSubstitutor.EMPTY
    val substitutionMap = zip(arguments).associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

internal enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

sealed class EqualityMode {
    /**
     * For declared members in Kotlin classes
     * (which include comparison of declared members in Kotlin classes and inherited members)
     */
    data object KotlinSignature : EqualityMode()

    /**
     * For inherited members and declared members in Java classes
     *
     * There is also the third kind of signatures: JVM signatures
     * JVM signature is a plain triple: (jvmName: String, parameters: List<Class<*>>, returnType: Class<*>)
     * Contrary to JVM signature, Java signature doesn't include `returnType`,
     * and Java signatures respect class generics (but not method generics)
     */
    data object JavaSignature : EqualityMode()
}

// Signatures that you can test for equality
internal data class EquatableCallableSignature<T : EqualityMode>(
    val kind: SignatureKind,
    val name: String,
    val jvmNameIfFunction: String,
    val typeParameters: List<KTypeParameter>,
    val parameterTypes: List<KType>,
    val javaParameterTypesIfFunction: List<Pair<Type, Class<*>>>,
    val isStatic: Boolean,
    val equalityMode: T,
) {
    init {
        check(
            kind != SignatureKind.FIELD_IN_JAVA_CLASS ||
                parameterTypes.isEmpty() && typeParameters.isEmpty() && javaParameterTypesIfFunction.isEmpty()
        )
    }

    fun <T : EqualityMode> withEqualityMode(equalityMode: T): EquatableCallableSignature<T> =
        EquatableCallableSignature(
            kind,
            name,
            jvmNameIfFunction,
            typeParameters,
            parameterTypes,
            javaParameterTypesIfFunction,
            isStatic,
            equalityMode
        )

    override fun hashCode(): Int = when (equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION) {
        true -> arrayOf<Any>(kind, parameterTypes.size, isStatic, jvmNameIfFunction).contentHashCode()
        false -> arrayOf<Any>(kind, parameterTypes.size, isStatic, name).contentHashCode()
    }

    /**
     * Generally, [areEqualKTypes] is unsafe to use inside [equals] implementations since [areEqualKTypes] is not transitive.
     *
     * But when we compare by Kotlin signatures ([EqualityMode.KotlinSignature]),
     * on the one-hand side of the comparison there are always declared
     * Kotlin members, which doesn't allow to observe [equals] non-transitivity
     * (because Kotlin declared members cannot have flexible parameter types)
     *
     * When we compare by Java signatures ([EqualityMode.JavaSignature]),
     * we [coerceFlexibleTypesAndMutabilityRecursive] the types,
     * which makes [areEqualKTypes] transitive
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EquatableCallableSignature<*>) return false
        check(equalityMode == other.equalityMode) {
            "Equality modes must be the same. Please recreate signatures on inheritance"
        }
        if (kind != other.kind) return false
        if (isStatic != other.isStatic) return false
        if (parameterTypes.size != other.parameterTypes.size) return false
        if (equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION) {
            if (jvmNameIfFunction != other.jvmNameIfFunction) return false
            if (javaParameterTypesIfFunction.size != other.javaParameterTypesIfFunction.size) return false
            check(javaParameterTypesIfFunction.size == parameterTypes.size)
            for (i in javaParameterTypesIfFunction.indices) {
                val (javaTypeA, javaClassA) = javaParameterTypesIfFunction[i]
                val (javaTypeB, javaClassB) = other.javaParameterTypesIfFunction[i]
                val isATypeParameterFromClass = (javaTypeA as? TypeVariable<*>)?.genericDeclaration is Class<*>
                val isBTypeParameterFromClass = (javaTypeB as? TypeVariable<*>)?.genericDeclaration is Class<*>
                if (isATypeParameterFromClass || isBTypeParameterFromClass) {
                    if (javaClassA.isPrimitive != javaClassB.isPrimitive) return false

                    // Since we don't have type substitutors for Java types, here we abuse KTypes for this purpose
                    // Make types non-flexible and non-mutable to make 'equals' transitive
                    val kTypeA = parameterTypes[i].coerceFlexibleTypesAndMutabilityRecursive()
                    val kTypeB = other.parameterTypes[i].coerceFlexibleTypesAndMutabilityRecursive()
                    if (!areEqualKTypes(kTypeA, kTypeB)) return false
                } else {
                    if (javaClassA != javaClassB) return false
                }
            }
        } else {
            if (name != other.name) return false
            val functionTypeParametersEliminator = typeParameters.substitutedWith(other.typeParameters) ?: return false
            for (i in typeParameters.indices) {
                val typeParameterA = typeParameters[i]
                val typeParameterB = other.typeParameters[i]
                if (typeParameterA.upperBounds.size != typeParameterB.upperBounds.size) return false
                val equalUpperBounds = typeParameterA.upperBounds
                    .map {
                        functionTypeParametersEliminator.substitute(it).type ?: starProjectionSupertypesAreNotPossible()
                    }
                    .sortedUpperBounds()
                    .zip(typeParameterB.upperBounds.sortedUpperBounds())
                    .all { areEqualKTypes(it.first, it.second) }
                if (!equalUpperBounds) return false
            }
            for (i in parameterTypes.indices) {
                val a = functionTypeParametersEliminator.substitute(parameterTypes[i]).type
                    ?: starProjectionSupertypesAreNotPossible()
                val b = other.parameterTypes[i]
                if (!areEqualKTypes(a, b)) return false
            }
        }
        return true
    }
}

// Make KType.equals transitive
private fun KType.coerceFlexibleTypesAndMutabilityRecursive(): KType {
    val classifier = classifier ?: nonDenotableSupertypesAreNotPossible()
    // Recreating type from classifiers erases mutability (e.g., MutableList becomes List)
    return classifier.createType(
        arguments.map { it.copy(type = it.type?.coerceFlexibleTypesAndMutabilityRecursive()) },
        nullable = false,
        annotations
    )
}

/**
 * Those upper bounds are already substituted, so equal lists of upper bounds must also have equal names.
 * The necessary condition for equal upper bounds is equal names.
 *
 * The only false negative case that we are afraid of is when different upper bounds accidentally have the same name.
 * In that case, the list of bounds will be discarded later by areEqualTypes anyway.
 */
private fun List<KType>.sortedUpperBounds(): List<KType> =
    sortedBy {
        when (val classifier = it.classifier ?: error("upper bounds are always denotable")) {
            is KClass<*> -> classifier.java.name
            is KTypeParameter -> classifier.name
            else -> error("Unknown upper bound classifier: ${classifier::class}")
        }
    }

private fun areEqualKTypes(a: KType, b: KType): Boolean = a.isSubtypeOf(b) && b.isSubtypeOf(a)
