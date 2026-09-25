/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.name.Name
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.ClassKind
import kotlin.metadata.Modality
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.jvm.internal.types.AbstractKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext
import kotlin.reflect.jvm.internal.types.areEqualKTypes
import kotlin.reflect.jvm.javaField

private object CovariantOverrideComparator : Comparator<ReflectKCallable<*>> {
    override fun compare(a: ReflectKCallable<*>, b: ReflectKCallable<*>): Int {
        val typeParametersEliminator = a.typeParameters.substitutedWith(b.typeParameters) ?: return 0
        val aReturnType = typeParametersEliminator.substituteTopLevelType(a.returnType, a.name)
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

/**
 * Members of a class with a given name, keyed by their signatures. Normally, the key is the Java signature of the member
 * ([EquatableCallableSignature] with [EqualityMode.JavaSignature]), but it can also be the Kotlin signature ([KotlinSignatureKey]) for
 * overrides of built-in functions with erased value parameters in Java, see [computeFakeOverrideMembersForName].
 * The keys are only used to group inherited members while computing the map, the users of the map should only rely on its values.
 */
internal typealias MembersJavaSignatureMap = Map<Any, ReflectKCallable<*>>
private typealias MutableMembersJavaSignatureMap = MutableMap<Any, ReflectKCallable<*>>

// A wrapper is needed so that the key never compares equal to a Java signature key, see `EquatableCallableSignature.equals`.
private data class KotlinSignatureKey(val signature: EquatableCallableSignature<EqualityMode.KotlinSignature>)

private fun ReflectKCallable<*>.isStaticMethodInInterface(kClass: MemberContainer<*>): Boolean =
    isStatic && kClass.classKind == ClassKind.INTERFACE && !isJavaField

/**
 * Non-transitive members don't inherit transitively but appear in the 'members' list of the immediate KClass
 */
internal fun isNonTransitiveMember(kClass: MemberContainer<*>, member: ReflectKCallable<*>): Boolean =
    member.visibility == KVisibility.PRIVATE ||
            // static methods (but not fields) in interfaces are never inherited (neither in Java nor in Kotlin)
            member.isStaticMethodInInterface(kClass)

/**
 * Builds the "transitive" member map, for a single member [name], used to compute 'KClass.members' for every KClass.
 *
 * User facing 'KClass.members' is not "transitive". This map is "transitive".
 *
 * By "transitive" we mean that the map of every inheritor class/interface is a strict superset
 * of their parent classes' maps.
 */
internal fun computeFakeOverrideMembersForName(kClass: MemberContainer<*>, name: String): MembersJavaSignatureMap {
    val declaredMembers = kClass.getDeclaredMembersByName(name).filterNot { isNonTransitiveMember(kClass, it) }
    val declaredKotlinSignatures =
        if (kClass.isKotlin) declaredMembers.mapTo(HashSet()) { it.toEquatableCallableSignature(EqualityMode.KotlinSignature) }
        else emptySet()
    val result: MutableMembersJavaSignatureMap = HashMap()
    // Keys in `result` of inherited members by their Kotlin signatures, needed to handle overrides of built-in functions with erased value
    // parameters (see below). Only needed for Kotlin classes.
    val keysByKotlinSignature: MutableMap<EquatableCallableSignature<EqualityMode.KotlinSignature>, Any>? =
        if (kClass.isKotlin && Name.identifier(name) in SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SHORT_NAMES) HashMap() else null
    for (supertype in kClass.supertypes) {
        val supertypeKClass = supertype.memberContainer
            ?: error(
                "Non-denotable supertypes are not possible. " +
                        "Supertype '$supertype' appears non-denotable in class '$kClass'"
            )
        val substitutor = KTypeSubstitutor.create(supertype)
        for (supertypeMember in getSupertypeMembersByName(supertype, supertypeKClass, name)) { // Recursive call
            val member = supertypeMember.createFakeOverride(kClass, substitutor)
            val kotlinSignature = member.toEquatableCallableSignature(EqualityMode.KotlinSignature)
            if (kotlinSignature in declaredKotlinSignatures) continue
            // Inherited signatures are always compared by the JvmSignatures. Even for kotlin classes.
            var key: Any = kotlinSignature.withEqualityMode(EqualityMode.JavaSignature)
            if (keysByKotlinSignature != null) {
                // The exception is an override of a built-in function with erased value parameters in Java (e.g. `MutableCollection.remove(E)`
                // itself, or `remove(Object)` in a Java class overriding it, which is loaded with the value parameter types of the built-in
                // function: `remove(E)`). In a Kotlin class, such member is compared with other inherited members by the Kotlin signature,
                // just like the compiler does. For example, `remove(E)` is merged with `remove(Integer)` from another Java supertype even
                // though their Java signatures differ, and it's not merged with `remove(Object)` from another Java supertype even though
                // their Java signatures are the same. Note that it's still not merged with `remove(int)`, because a Java method with a
                // primitive parameter cannot override a method with a non-primitive parameter, see
                // `JavaIncompatibilityRulesOverridabilityCondition.doesJavaOverrideHaveIncompatibleValueParameterKinds` in the compiler.
                val existingKey = keysByKotlinSignature[kotlinSignature]
                if (existingKey != null) {
                    val existingSignature = (existingKey as? KotlinSignatureKey)?.signature ?: existingKey as EquatableCallableSignature<*>
                    if ((member.isOverrideOfBuiltinWithErasedValueParameters ||
                                result[existingKey]?.isOverrideOfBuiltinWithErasedValueParameters == true) &&
                        kotlinSignature.hasSameValueParameterKinds(existingSignature)
                    ) {
                        key = existingKey
                    }
                } else {
                    if (member.isOverrideOfBuiltinWithErasedValueParameters) {
                        key = KotlinSignatureKey(kotlinSignature)
                    }
                    keysByKotlinSignature[kotlinSignature] = key
                }
            }
            val existingMember = result[key]
            result[key] = if (existingMember == null) member else createIntersectionOverride(existingMember, member)
        }
    }
    for (member in declaredMembers) {
        result[member.toEquatableCallableSignature(EqualityMode.JavaSignature)] = member
    }
    return result
}

private val ReflectKCallable<*>.isOverrideOfBuiltinWithErasedValueParameters: Boolean
    get() = this is ReflectKFunction && overriddenBuiltinWithErasedValueParameters() != null

internal fun ReflectKCallable<*>.createFakeOverride(subclass: MemberContainer<*>, substitutor: KTypeSubstitutor): ReflectKCallable<*> =
    shallowCopy(
        subclass,
        overriddenStorage.withChainedClassTypeParametersSubstitutor(substitutor).copy(
            isStatic = isStatic,
            originalContainerIfFakeOverride = originalContainer,
            originalCallableTypeParameters = typeParameters,
            overridden = listOf(this),
        ),
    )

private fun createIntersectionOverride(a: ReflectKCallable<*>, b: ReflectKCallable<*>): ReflectKCallable<*> {
    val result = minOf(a, b, CovariantOverrideComparator)
    val other = if (result === a) b else a
    val storage = result.overriddenStorage.copy(
        modality = computeIntersectionOverrideModality(a, b),
        overridden = result.overriddenStorage.overridden + other.overriddenStorage.overridden,
    )
    if (a !is ReflectKFunction || b !is ReflectKFunction) return result.shallowCopy(result.container, storage)
    return result.shallowCopy(
        result.container,
        storage.copy(
            forceIsExternal = a.isExternal || b.isExternal,
            forceIsOperator = a.isOperator || b.isOperator,
            forceIsInfix = a.isInfix || b.isInfix,
            forceIsInline = a.isInline || b.isInline,
        ),
    )
}

/**
 * Returns true if this is a builtin function whose JVM name differs from the Kotlin name (e.g. `kotlin.Number.toInt`), or its override
 * which has the JVM name in the bytecode (a fake override, or a Java method `intValue` in a subclass of `Number`).
 */
private val ReflectKCallable<*>.isBuiltinWithDifferentJvmName: Boolean
    get() = when (this) {
        is JavaKNamedFunction -> name != jMethod.name
        is KotlinKNamedFunction -> {
            val jvmName = signature.substringBeforeLast('(')
            jvmName != name && getBuiltinSpecialFunctionJvmName(name, signature.substring(jvmName.length), originalContainer) == jvmName
        }
        else -> false
    }

// See `OverridingUtil.determineModalityForFakeOverride`.
private fun computeIntersectionOverrideModality(a: ReflectKCallable<*>, b: ReflectKCallable<*>): Modality {
    val aModality = a.modality
    val bModality = b.modality
    if (aModality == bModality) return aModality
    if (aModality == Modality.FINAL || bModality == Modality.FINAL) return Modality.FINAL

    // One of the members is open, and the other is abstract. The result is open if there's an open declaration in the hierarchy which is
    // not overridden by any abstract declaration. Otherwise, it's abstract.
    val declarations = LinkedHashSet<ReflectKCallable<*>>()
    a.collectOverriddenDeclarations(declarations, transitively = false)
    b.collectOverriddenDeclarations(declarations, transitively = false)
    val overriddenByOtherDeclarations = HashSet<ReflectKCallable<*>>()
    for (declaration in declarations) {
        for (overridden in declaration.overriddenCallables) {
            overridden.collectOverriddenDeclarations(overriddenByOtherDeclarations, transitively = true)
        }
    }
    return if (declarations.any { it.modality == Modality.OPEN && it !in overriddenByOtherDeclarations })
        Modality.OPEN
    else
        Modality.ABSTRACT
}

private fun ReflectKCallable<*>.collectOverriddenDeclarations(result: MutableSet<ReflectKCallable<*>>, transitively: Boolean) {
    if (!overriddenStorage.isFakeOverride) {
        if (!result.add(this) || !transitively) return
    }
    for (overridden in overriddenCallables) {
        overridden.collectOverriddenDeclarations(result, transitively)
    }
}

// Callables overridden by this callable. Unlike for functions, overridden properties are computed only for fake overrides and for Java
// methods overriding Kotlin properties, because there's no general way to find overridden properties yet.
private val ReflectKCallable<*>.overriddenCallables: Collection<ReflectKCallable<*>>
    get() = when {
        this is ReflectKFunction -> overridden
        overriddenStorage.isFakeOverride -> overriddenStorage.overridden
        this is JavaForKotlinOverrideKProperty<*> -> listOf(overriddenProperty)
        else -> emptyList()
    }

internal fun computeOverriddenFunctions(callable: ReflectKFunction): Collection<ReflectKFunction> {
    if (callable.overriddenStorage.isFakeOverride) {
        return callable.overriddenStorage.overridden.map { it as ReflectKFunction }
    }

    val container = callable.container as? MemberContainer<*> ?: return emptyList()
    val thisKotlinSignature = callable.toEquatableCallableSignature(EqualityMode.KotlinSignature)
    return computeOverriddenFunctions(container, thisKotlinSignature)
}

/**
 * Finds functions in supertypes of [container] which are overridden by a function with the given Kotlin [signature] declared in [container].
 *
 * If [substituted] is true, the result contains copies of the overridden functions with types substituted to the type parameters of
 * [container] (in other words, fake overrides of the overridden functions in [container]), otherwise the original functions declared in
 * supertypes. Substituted copies are needed when types of the overridden functions are compared with or applied to types of the overriding
 * function, e.g. during type enhancement.
 *
 * [supertypes] are the supertypes of [container] where the overridden functions are looked for. They are different from `container.supertypes`
 * only for additional functions of mapped built-in classes, see `javaAnalogueSupertypes`.
 */
internal fun computeOverriddenFunctions(
    container: MemberContainer<*>,
    signature: EquatableCallableSignature<EqualityMode.KotlinSignature>,
    substituted: Boolean = false,
    supertypes: List<KType> = container.supertypes,
): Collection<ReflectKFunction> {
    val result = mutableListOf<ReflectKFunction>()
    for (supertype in supertypes) {
        val supertypeKClass = supertype.memberContainer ?: continue
        val substitutor = KTypeSubstitutor.create(supertype)
        for (supertypeMember in getSupertypeMembersByName(supertype, supertypeKClass, signature.name)) {
            if (supertypeMember !is ReflectKFunction) continue
            val fakeOverride = supertypeMember.createFakeOverride(container, substitutor) as ReflectKFunction
            if (signature == fakeOverride.toEquatableCallableSignature(EqualityMode.KotlinSignature)) {
                result.add(if (substituted) fakeOverride else supertypeMember)
            }
        }
    }
    return result
}

private fun getSupertypeMembersByName(
    supertype: KType,
    supertypeKClass: MemberContainer<*>,
    name: String,
): Collection<ReflectKCallable<*>> {
    // There are no KClass instances for suspend function types before KT-79225, so we create suspend invoke manually.
    if (name == "invoke" && (supertype as? AbstractKType)?.isSuspendFunctionType == true) {
        val functionKmClass = supertypeKClass.kmClass
            ?: throw KotlinReflectionInternalError("No metadata found for function class '$supertypeKClass'")
        val invokeKmFunction = createSuspendFunctionInvoke(supertype.arguments.size - 1, functionKmClass)
        return listOf(createUnboundFunction(invokeKmFunction, supertypeKClass))
    }
    return supertypeKClass.getFakeOverrideMembersByName(name).values
}

internal val ReflectKCallable<*>.originalContainer: KDeclarationContainerImpl
    get() = overriddenStorage.originalContainerIfFakeOverride ?: container

internal val ReflectKCallable<*>.isStatic: Boolean
    get() = overriddenStorage.isStatic ?: run {
        val parameters = (this as? JavaKFunction)?.originalParameters ?: allParameters
        parameters.firstOrNull()?.kind != KParameter.Kind.INSTANCE
    }

private val ReflectKCallable<*>.isJavaField: Boolean
    get() = this is KProperty<*> && this.javaField?.declaringClass?.isKotlinClassOrPackage == false

internal fun <T : EqualityMode> ReflectKCallable<*>.toEquatableCallableSignature(equalityMode: T): EquatableCallableSignature<T> {
    val parameters = (this as? JavaKFunction)?.originalParameters ?: allParameters
    val kotlinParameterTypes = parameters.filter { it.kind != KParameter.Kind.INSTANCE }.map { it.type }
    val kind = when {
        isJavaField -> SignatureKind.FIELD_IN_JAVA_CLASS
        this is KProperty<*> -> SignatureKind.PROPERTY
        this is KFunction<*> -> SignatureKind.FUNCTION
        else -> error("Unknown kind for ${this::class}")
    }
    val isSuspend = (this as? KFunction<*>)?.isSuspend == true
    val functionJvmSignature = (this as? ReflectKFunction)?.signature
    val jvmNameIfFunction = functionJvmSignature?.substringBeforeLast('(')
    val functionJvmDescriptor = functionJvmSignature?.substring(jvmNameIfFunction!!.length)
    // JVM signature of suspend functions has a continuation parameter, which is absent in `kotlinParameterTypes`, so we drop it to keep
    // Java and Kotlin parameter lists aligned.
    val javaParameterTypes = functionJvmDescriptor?.let {
        container.jClass.safeClassLoader.parseAndLoadDescriptor(it, loadReturnType = false).parameters.dropContinuationIfSuspend(isSuspend)
    }.orEmpty()
    return EquatableCallableSignature(
        kind,
        name,
        javaNameIfFunction = jvmNameIfFunction?.let { if (isBuiltinWithDifferentJvmName) name else it },
        typeParameters,
        kotlinParameterTypes,
        javaParameterTypes,
        {
            // Workaround KT-13077: `javaMethod` doesn't work for builtins, so find the method manually, falling back to erased types.
            val method = when (this) {
                is JavaKNamedFunction -> jMethod
                is ReflectKFunction -> findOriginalJavaMethod(jvmNameIfFunction!!, javaParameterTypes)
                else -> null
            }
            method?.genericParameterTypes?.toList()?.dropContinuationIfSuspend(isSuspend) ?: javaParameterTypes
        },
        isSuspend,
        isStatic,
        equalityMode,
    )
}

// Returns the same method as `javaMethod`, unless the latter is a bridge method, in which case, for a fake override, returns the Java
// method of the original declaration in the class where the member is really declared. For example, for the fake override `invoke` in
// a Java class implementing `Function1<String, Integer>`, `javaMethod` returns the synthetic bridge `invoke(Object): Object` declared
// in the Java class, whose generic parameter types are erased, while this property returns `Function1.invoke(P1)` whose generic
// parameter type is the type variable `P1`.
private fun ReflectKFunction.findOriginalJavaMethod(name: String, parameterTypes: List<Class<*>>): Method? {
    val method = container.findMethodBySignature(name, parameterTypes, returnType = null) ?: return null
    if (!method.isBridge) return method
    val originalContainer = overriddenStorage.originalContainerIfFakeOverride ?: return method
    val jvmName = signature.substringBeforeLast('(')
    return originalContainer.findMethodBySignature(jvmName, signature.substring(jvmName.length)) ?: method
}

private fun <T> List<T>.dropContinuationIfSuspend(isSuspend: Boolean): List<T> = if (isSuspend) dropLast(1) else this

internal val Class<*>.isKotlinClassOrPackage: Boolean
    get() = getAnnotation(Metadata::class.java) != null

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
     * and Java signatures respect class generics (but not method generics).
     * Also, for builtin functions whose JVM name differs from the Kotlin name (and their overrides), Java signature has the Kotlin name,
     * see `isBuiltinWithDifferentJvmName`.
     */
    data object JavaSignature : EqualityMode()
}

// Signatures that you can test for equality
internal class EquatableCallableSignature<T : EqualityMode>(
    val kind: SignatureKind,
    val name: String,
    // The name to compare functions by in [EqualityMode.JavaSignature]: the JVM name, or the Kotlin name for builtins with a different
    // JVM name and their overrides.
    val javaNameIfFunction: String?,
    val typeParameters: List<KTypeParameter>,
    val kotlinParameterTypes: List<KType>,
    val javaErasedParameterTypes: List<Class<*>>,
    private val computeJavaGenericParameterTypes: () -> List<Type>,
    val isSuspend: Boolean,
    val isStatic: Boolean,
    val equalityMode: T,
) {
    private val javaGenericParameterTypes: List<Type> by lazy(PUBLICATION) {
        computeJavaGenericParameterTypes().also {
            check(it.size == javaErasedParameterTypes.size && javaErasedParameterTypes.size == kotlinParameterTypes.size) {
                "javaGenericParameterTypes.size (${it.size}), javaErasedParameterTypes.size (${javaErasedParameterTypes.size}) and " +
                        "kotlinParameterTypes.size (${kotlinParameterTypes.size}) must be equal. " +
                        "For member: '$name'"
            }
        }
    }

    private val isJavaFunctionSignature: Boolean
        get() = equalityMode == EqualityMode.JavaSignature && kind == SignatureKind.FUNCTION

    init {
        check(
            kind != SignatureKind.FIELD_IN_JAVA_CLASS ||
                    kotlinParameterTypes.isEmpty() && typeParameters.isEmpty() && javaErasedParameterTypes.isEmpty()
        ) {
            "Inconsistent combination of EquatableCallableSignature values. kind: ${kind}, " +
                    "kotlinParameterTypes.isEmpty(): ${kotlinParameterTypes.isEmpty()}," +
                    "typeParameters.isEmpty(): ${typeParameters.isEmpty()}, " +
                    "javaErasedParameterTypes.isEmpty(): ${javaErasedParameterTypes.isEmpty()}." +
                    "For member: '$name'"
        }
    }

    fun <T : EqualityMode> withEqualityMode(equalityMode: T): EquatableCallableSignature<T> =
        EquatableCallableSignature(
            kind,
            name,
            javaNameIfFunction,
            typeParameters,
            kotlinParameterTypes,
            javaErasedParameterTypes,
            computeJavaGenericParameterTypes,
            isSuspend,
            isStatic,
            equalityMode
        )

    // Whether the corresponding value parameters of this and [other] signatures are both primitive or both non-primitive in Java.
    fun hasSameValueParameterKinds(other: EquatableCallableSignature<*>): Boolean =
        javaErasedParameterTypes.size == other.javaErasedParameterTypes.size &&
                javaErasedParameterTypes.indices.all {
                    javaErasedParameterTypes[it].isPrimitive == other.javaErasedParameterTypes[it].isPrimitive
                }

    override fun hashCode(): Int =
        arrayOf<Any>(kind, kotlinParameterTypes.size, isStatic, if (isJavaFunctionSignature) javaNameIfFunction ?: "" else name)
            .contentHashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EquatableCallableSignature<*>) return false
        check(equalityMode == other.equalityMode) {
            "Equality modes must be the same for member '$name'. Please recreate signatures on inheritance"
        }
        if (kind != other.kind) return false
        if (isSuspend != other.isSuspend) return false
        if (isStatic != other.isStatic) return false
        if (kotlinParameterTypes.size != other.kotlinParameterTypes.size) return false
        return if (isJavaFunctionSignature) equalsByJavaSignature(other) else equalsByKotlinSignature(other)
    }

    private fun equalsByJavaSignature(other: EquatableCallableSignature<*>): Boolean =
        javaNameIfFunction == other.javaNameIfFunction &&
                javaErasedParameterTypes.indices.all { i -> areEqualJavaParameterTypes(i, other) }

    private fun areEqualJavaParameterTypes(i: Int, other: EquatableCallableSignature<*>): Boolean =
        if (javaGenericParameterTypes[i].isClassTypeParameter || other.javaGenericParameterTypes[i].isClassTypeParameter) {
            javaErasedParameterTypes[i].isPrimitive == other.javaErasedParameterTypes[i].isPrimitive &&
                    // Since we don't have type substitutors for Java types, here we abuse KTypes for this purpose
                    areEqualKTypes(kotlinParameterTypes[i], other.kotlinParameterTypes[i])
        } else {
            javaErasedParameterTypes[i] == other.javaErasedParameterTypes[i]
        }

    private fun equalsByKotlinSignature(other: EquatableCallableSignature<*>): Boolean {
        if (name != other.name) return false
        val functionTypeParametersEliminator = typeParameters.substitutedWith(other.typeParameters) ?: return false
        if (!areEqualTypeParameterBounds(functionTypeParametersEliminator, other)) return false
        for (i in kotlinParameterTypes.indices) {
            val a = functionTypeParametersEliminator.substituteTopLevelType(kotlinParameterTypes[i], name)
            val b = other.kotlinParameterTypes[i]
            if (!areEqualKTypes(a, b)) return false
        }
        return true
    }

    private fun areEqualTypeParameterBounds(substitutor: KTypeSubstitutor, other: EquatableCallableSignature<*>): Boolean {
        for (i in typeParameters.indices) {
            val typeParameterA = typeParameters[i]
            val typeParameterB = other.typeParameters[i]
            if (typeParameterA.upperBounds.size != typeParameterB.upperBounds.size) return false
            val equalUpperBounds = typeParameterA.upperBounds
                .map { substitutor.substituteTopLevelType(it, name) }
                .sortedUpperBounds(memberNameForDebug = name)
                .zip(typeParameterB.upperBounds.sortedUpperBounds(memberNameForDebug = other.name))
                .all { areEqualKTypes(it.first, it.second) }
            if (!equalUpperBounds) return false
        }
        return true
    }
}

private val Type.isClassTypeParameter: Boolean
    get() = this is TypeVariable<*> && genericDeclaration is Class<*>

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
