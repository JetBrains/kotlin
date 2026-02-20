/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.utils.newHashMapWithExpectedSize
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
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

internal fun getAllMembers(kClass: KClassImpl<*>): Collection<ReflectKCallable<*>> {
    val fakeOverrideMembers = kClass.data.value.fakeOverrideMembers
    // Kotlin doesn't have statics (unless it's enum), and it never inherits statics from Java
    val isKotlin = kClass.java.isKotlin
    val doNeedToFilterOutStatics =
        fakeOverrideMembers.containsInheritedStatics && kClass.classKind != ClassKind.ENUM_CLASS && isKotlin
    val doNeedToShrinkMembers = fakeOverrideMembers.containsPackagePrivate || doNeedToFilterOutStatics
    val membersMutable = when (doNeedToShrinkMembers) {
        true -> fakeOverrideMembers.members.filterNotTo(
            newHashMapWithExpectedSize(
                // We expect that all non-transitive operations below (like filtering out statics or adding privates)
                // do not change the final size of the collection significantly.
                // We expect the size to stay more or less the same.
                expectedSize = fakeOverrideMembers.members.size
            )
        ) { (_, member) ->
            doNeedToFilterOutStatics && member.isStatic ||
                    member.isPackagePrivate && member.originalContainer.jClass.`package` != kClass.java.`package`
        }
        false -> HashMap(fakeOverrideMembers.members)
    }
    return membersMutable.values + kClass.declaredReflectKCallableMembers.filter { isNonTransitiveMember(kClass, it) }
}

internal fun starProjectionInTopLevelTypeIsNotPossible(containerForDebug: Any): Nothing =
    error(
        "Star projection in top level type is not possible. " +
                "Star projection appeared in the following container: '$containerForDebug'"
    )

private object CovariantOverrideComparator : Comparator<ReflectKCallable<*>> {
    override fun compare(a: ReflectKCallable<*>, b: ReflectKCallable<*>): Int {
        val typeParametersEliminator = a.typeParameters.substitutedWith(b.typeParameters)
            ?: error(
                "Intersection overrides can't have different type parameters sizes. " +
                        "It must have been reported by the compiler. " +
                        "The following members appear to be violating intersection overrides: '$a' '$b'"
            )
        val aReturnType =
            typeParametersEliminator.substitute(a.returnType).type
                ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = a.name)
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

private typealias MembersJavaSignatureMap = Map<EquatableCallableSignature<EqualityMode.JavaSignature>, ReflectKCallable<*>>
private typealias MutableMembersJavaSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.JavaSignature>, ReflectKCallable<*>>
private typealias MutableMembersKotlinSignatureMap = MutableMap<EquatableCallableSignature<EqualityMode.KotlinSignature>, ReflectKCallable<*>>

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

private fun ReflectKCallable<*>.isStaticMethodInInterface(kClass: KClassImpl<*>): Boolean =
    isStatic && kClass.classKind == ClassKind.INTERFACE && !isJavaField

/**
 * Non-transitive members don't inherit transitively but appear in the 'members' list of the immediate KClass
 */
private fun isNonTransitiveMember(kClass: KClassImpl<*>, member: ReflectKCallable<*>): Boolean =
    member.visibility == KVisibility.PRIVATE ||
            // static methods (but not fields) in interfaces are never inherited (neither in Java nor in Kotlin)
            member.isStaticMethodInInterface(kClass)

internal fun computeFakeOverrideMembers(kClass: KClassImpl<*>): FakeOverrideMembers {
    val javaSignaturesMap: MutableMembersJavaSignatureMap = HashMap()
    var containsInheritedStatics = false
    var containsPackagePrivate = false
    val isKotlin = kClass.java.isKotlin
    val declaredKotlinMembers: MutableMembersKotlinSignatureMap = HashMap()
    if (isKotlin) {
        for (member in kClass.declaredReflectKCallableMembers) {
            if (isNonTransitiveMember(kClass, member)) continue
            declaredKotlinMembers[member.toEquatableCallableSignature(EqualityMode.KotlinSignature)] = member
        }
    }
    for (supertype in kClass.supertypes) {
        val supertypeKClass = supertype.classifier as? KClass<*>
            ?: error(
                "Non-denotable supertypes are not possible. " +
                        "Supertype '$supertype' appears non-denotable in class '$kClass'"
            )
        val substitutor = KTypeSubstitutor.create(supertype)
        val supertypeMembers = supertypeKClass.fakeOverrideMembers // Recursive call
        containsInheritedStatics = containsInheritedStatics || supertypeMembers.containsInheritedStatics
        containsPackagePrivate = containsPackagePrivate || supertypeMembers.containsPackagePrivate
        for ((_, notSubstitutedMember) in supertypeMembers.members) {
            val overriddenStorage = notSubstitutedMember.overriddenStorage
                .withChainedClassTypeParametersSubstitutor(substitutor)
                .copy(
                    originalContainerIfFakeOverride = notSubstitutedMember.originalContainer,
                    originalCallableTypeParameters = notSubstitutedMember.typeParameters,
                    isStatic = notSubstitutedMember.isStatic,
                )
            val member = notSubstitutedMember.shallowCopy(kClass, overriddenStorage)
            val kotlinSignature = member.toEquatableCallableSignature(EqualityMode.KotlinSignature)
            if (declaredKotlinMembers.contains(kotlinSignature)) continue
            // Inherited signatures are always compared by the JvmSignatures. Even for kotlin classes.
            javaSignaturesMap.mergeWith(kotlinSignature.withEqualityMode(EqualityMode.JavaSignature), member) { a, b ->
                val c = minOf(a, b, CovariantOverrideComparator)
                when (a is KFunction<*> && b is KFunction<*>) {
                    true -> c.shallowCopy(
                        c.container,
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
        for (member in kClass.declaredReflectKCallableMembers) {
            if (isNonTransitiveMember(kClass, member)) continue
            containsInheritedStatics = containsInheritedStatics || member.isStatic
            containsPackagePrivate = containsPackagePrivate || member.isPackagePrivate
            javaSignaturesMap[member.toEquatableCallableSignature(EqualityMode.JavaSignature)] = member
        }
    }
    return FakeOverrideMembers(javaSignaturesMap, containsInheritedStatics, containsPackagePrivate)
}

/**
 * Alternative `MutableMap.merge` implementation that is available on JDK < 8. Reflect has to be able to work in JDK 6
 *
 * Related test: `org.jetbrains.kotlin.tools.tests.JdkApiUsageTest`
 */
private inline fun <K, V : Any> MutableMap<K, V>.mergeWith(key: K, value: V, remappingFunction: (V, V) -> V): V =
    (get(key)?.let { remappingFunction(it, value) } ?: value).also { this[key] = it }

private val modalityIntersectionOverrideComparator: Comparator<ReflectKCallable<*>> = compareBy(
    // Deprioritize interfaces, prioritize classes
    { (it.originalContainer as? KClass<*>)?.java?.isInterface == true },
    // If there are multiple superclasses (not interfaces), deprioritize kotlin.Any.
    // For instance, equals/hashCode/toString which come from interfaces have kotlin.Any container.
    { it.originalContainer == Any::class },
)

private val ReflectKCallable<*>.originalContainer: KDeclarationContainerImpl
    get() = overriddenStorage.originalContainerIfFakeOverride ?: container

internal val ReflectKCallable<*>.isStatic: Boolean
    get() = overriddenStorage.isStatic ?: (allParameters.firstOrNull()?.kind != KParameter.Kind.INSTANCE)

private val ReflectKCallable<*>.isJavaField: Boolean
    get() = this is KProperty<*> && this.javaField?.declaringClass?.isKotlin == false

private val KClass<*>.fakeOverrideMembers: FakeOverrideMembers
    get() = when (this) {
        is KClassImpl<*> -> data.value.fakeOverrideMembers
        is MutableCollectionKClass<*> -> klass.fakeOverrideMembers
        else -> error("Unknown type ${this::class}")
    }

private fun <T : EqualityMode> ReflectKCallable<*>.toEquatableCallableSignature(equalityMode: T): EquatableCallableSignature<T> {
    val kotlinParameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when {
        isJavaField -> SignatureKind.FIELD_IN_JAVA_CLASS
        this is KProperty<*> -> SignatureKind.PROPERTY
        this is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val javaMethod = (this as? KFunction<*>)?.javaMethod
    val javaGenericParameterTypes = javaMethod?.genericParameterTypes.orEmpty().toList()
    val javaParameterTypes = javaMethod?.parameterTypes.orEmpty().toList()
    val jvmNameIfFunction = javaMethod?.name
    return EquatableCallableSignature(
        kind,
        name,
        jvmNameIfFunction,
        typeParameters,
        kotlinParameterTypes,
        javaParameterTypes,
        javaGenericParameterTypes,
        isStatic,
        equalityMode,
    )
}

internal val Class<*>.isKotlin: Boolean
    get() = getAnnotation(Metadata::class.java) != null

@Suppress("UNCHECKED_CAST")
private val KClass<*>.declaredReflectKCallableMembers: Collection<ReflectKCallable<*>>
    get() = declaredMembers as Collection<ReflectKCallable<*>>

internal fun List<KTypeParameter>.substitutedWith(arguments: List<KTypeParameter>): KTypeSubstitutor? {
    if (size != arguments.size) return null
    if (isEmpty()) return KTypeSubstitutor.EMPTY
    val substitutionMap = zip(arguments).associate { (x, y) -> Pair(x, KTypeProjection.invariant(y.createType())) }
    return KTypeSubstitutor(substitutionMap)
}

internal enum class SignatureKind {
    FUNCTION, PROPERTY, FIELD_IN_JAVA_CLASS
}

internal sealed class EqualityMode {
    /**
     * For declared members in Kotlin classes
     */
    data object KotlinSignature : EqualityMode()

    /**
     * For inherited members and declared members in Java classes; and for inherited members in Kotlin classes
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
    val jvmNameIfFunction: String?,
    val typeParameters: List<KTypeParameter>,
    val kotlinParameterTypes: List<KType>,
    val javaParameterTypesIfFunction: List<Class<*>>,
    val javaGenericParameterTypesIfFunction: List<Type>,
    val isStatic: Boolean,
    val equalityMode: T,
) {
    init {
        check(
            kind != SignatureKind.FIELD_IN_JAVA_CLASS ||
                    kotlinParameterTypes.isEmpty() && typeParameters.isEmpty() && javaParameterTypesIfFunction.isEmpty()
        ) {
            "Inconsistent combination of EquatableCallableSignature values. kind: ${kind}, " +
                    "kotlinParameterTypes.isEmpty(): ${kotlinParameterTypes.isEmpty()}," +
                    "typeParameters.isEmpty(): ${typeParameters.isEmpty()}, " +
                    "javaParameterTypesIfFunction.isEmpty(): ${javaParameterTypesIfFunction.isEmpty()}." +
                    "For member: '$name'"
        }
        check(javaParameterTypesIfFunction.size == javaGenericParameterTypesIfFunction.size) {
            "javaParameterTypesIfFunction.size (${javaParameterTypesIfFunction.size}) and " +
                    "javaGenericParameterTypesIfFunction.size (${javaGenericParameterTypesIfFunction.size}) must be equal. " +
                    "For member: '$name'"
        }
    }

    fun <T : EqualityMode> withEqualityMode(equalityMode: T): EquatableCallableSignature<T> =
        EquatableCallableSignature(
            kind,
            name,
            jvmNameIfFunction,
            typeParameters,
            kotlinParameterTypes,
            javaParameterTypesIfFunction,
            javaGenericParameterTypesIfFunction,
            isStatic,
            equalityMode
        )

    override fun hashCode(): Int = when (equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION) {
        true -> arrayOf<Any>(kind, kotlinParameterTypes.size, isStatic, jvmNameIfFunction ?: "").contentHashCode()
        false -> arrayOf<Any>(kind, kotlinParameterTypes.size, isStatic, name).contentHashCode()
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
            "Equality modes must be the same for member '$name'. Please recreate signatures on inheritance"
        }
        if (kind != other.kind) return false
        if (isStatic != other.isStatic) return false
        if (kotlinParameterTypes.size != other.kotlinParameterTypes.size) return false
        if (equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION) {
            if (jvmNameIfFunction != other.jvmNameIfFunction) return false
            if (javaParameterTypesIfFunction.size != other.javaParameterTypesIfFunction.size) return false
            check(javaParameterTypesIfFunction.size == kotlinParameterTypes.size) {
                "javaParameterTypesIfFunction.size (${javaParameterTypesIfFunction.size}) and " +
                        "kotlinParameterTypes.size (${kotlinParameterTypes.size}) must be equal for member '$name'"
            }
            for (i in javaParameterTypesIfFunction.indices) {
                val javaTypeA = javaGenericParameterTypesIfFunction[i]
                val javaClassA = javaParameterTypesIfFunction[i]
                val javaTypeB = other.javaGenericParameterTypesIfFunction[i]
                val javaClassB = other.javaParameterTypesIfFunction[i]
                val isATypeParameterFromClass = (javaTypeA as? TypeVariable<*>)?.genericDeclaration is Class<*>
                val isBTypeParameterFromClass = (javaTypeB as? TypeVariable<*>)?.genericDeclaration is Class<*>
                if (isATypeParameterFromClass || isBTypeParameterFromClass) {
                    if (javaClassA.isPrimitive != javaClassB.isPrimitive) return false

                    // Since we don't have type substitutors for Java types, here we abuse KTypes for this purpose
                    // Make types non-flexible and non-mutable to make 'equals' transitive
                    val kTypeA =
                        kotlinParameterTypes[i].coerceFlexibleTypesAndMutabilityRecursive(memberNameForDebug = name)
                    val kTypeB =
                        other.kotlinParameterTypes[i].coerceFlexibleTypesAndMutabilityRecursive(memberNameForDebug = other.name)
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
                        functionTypeParametersEliminator.substitute(it).type
                            ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = name)
                    }
                    .sortedUpperBounds(memberNameForDebug = name)
                    .zip(typeParameterB.upperBounds.sortedUpperBounds(memberNameForDebug = other.name))
                    .all { areEqualKTypes(it.first, it.second) }
                if (!equalUpperBounds) return false
            }
            for (i in kotlinParameterTypes.indices) {
                val a = functionTypeParametersEliminator.substitute(kotlinParameterTypes[i]).type
                    ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = name)
                val b = other.kotlinParameterTypes[i]
                if (!areEqualKTypes(a, b)) return false
            }
        }
        return true
    }
}

/**
 * This util function is used to make [areEqualKTypes] transitive and to get red of mutability in types
 * (e.g. MutableList becomes List).
 * Type equality is not transitive in Kotlin because of flexible types
 * (e.g. `String?` != `String` but (`String?` == `String!` and `String` == `String!`)
 */
private fun KType.coerceFlexibleTypesAndMutabilityRecursive(memberNameForDebug: String): KType {
    val self = this
    if (with(ReflectTypeSystemContext) { (self as? AbstractKType)?.isError() == true }) return self
    val classifier = classifier
        ?: error(
            "Non-denotable parameter types are not possible. " +
                    "Some parameter types appear non-denotable for type '$this' (${this::class}) which belongs to member '$memberNameForDebug'"
        )
    // Recreating type from classifiers erases mutability (e.g., MutableList becomes List)
    return classifier.createType(
        arguments.map { it.copy(type = it.type?.coerceFlexibleTypesAndMutabilityRecursive(memberNameForDebug)) },
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
private fun List<KType>.sortedUpperBounds(memberNameForDebug: String): List<KType> =
    sortedBy {
        when (
            val classifier = it.classifier ?: error(
                "Upper bounds are always denotable. " +
                        "Upper bounds appear non-denotable for member: '$memberNameForDebug'"
            )
        ) {
            is KClass<*> -> classifier.java.name
            is KTypeParameter -> classifier.name
            else -> error("Unknown upper bound classifier: ${classifier::class}")
        }
    }

private fun areEqualKTypes(a: KType, b: KType): Boolean = a.isSubtypeOf(b) && b.isSubtypeOf(a)
