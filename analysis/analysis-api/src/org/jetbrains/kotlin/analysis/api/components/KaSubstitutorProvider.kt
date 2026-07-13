/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.analysis.api.types.KaType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSubstitutorProvider : KaSessionComponent {
    /**
     * Creates a [KaSubstitutor] based on the given [mappings].
     *
     * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
     *
     * @see KaSubstitutor
     */
    @KaExperimentalApi
    public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor

    /**
     * Creates a [KaSubstitutor] based on the inheritance relationship between [subClass] and [superClass]. [subClass] must inherit from
     * [superClass] and there may not be any error types in the inheritance path. Otherwise, `null` is returned.
     *
     * The semantics of the resulting [KaSubstitutor] are as follows: When applied to a member of [superClass], such as a function, its type
     * parameters are substituted in such a way that the resulting member can be used with an instance of [subClass].
     *
     * In other words, the substitutor is a composition of inheritance-based substitutions incorporating the whole inheritance chain.
     *
     * #### Example
     *
     * ```
     * class A : B<String>
     * class B<T> : C<T, Int>
     * class C<X, Y>
     * ```
     *
     * - `createInheritanceTypeSubstitutor(A, B)` returns `KaSubstitutor { T -> String }`
     * - `createInheritanceTypeSubstitutor(B, C)` returns `KaSubstitutor { X -> T, Y -> Int }`
     * - `createInheritanceTypeSubstitutor(A, C)` returns `KaSubstitutor { X -> T, Y -> Int } and then KaSubstitutor { T -> String }`
     */
    @KaExperimentalApi
    public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor?

    /**
     * Creates a [KaSubstitutor] which assigns type arguments such that, for each pair in [leftTypesToRightTypes],
     * the substituted left type is a subtype of the substituted right type.
     * Returns `null` if such an assignment is not possible.
     *
     * Note that when one type parameter is shared across several constraint pairs, all these pairs affect the resulting substitution
     * for this parameter.
     *
     * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for 'leftType <: rightType'
     * from each [leftTypesToRightTypes] pair and tries to solve the given constraint system:
     * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
     * - If there are no type parameters involved in the provided types and every left type is a subtype of its right type,
     *   [KaSubstitutor.Empty] is returned.
     * - If [leftTypesToRightTypes] is empty, [KaSubstitutor.Empty] is returned as there can be no contradictions with no constraints.
     *
     * [isFreeTypeParameter] is called on every type parameter involved in the provided types
     * and controls the set of free type parameters registered in the constraint system.
     * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
     * The constraint system will only adjust the values of these free type parameters,
     * and the produced substitutor will only contain mappings for these parameters.
     */
    @KaIdeApi
    @OptIn(KaExperimentalApi::class)
    public fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        isFreeTypeParameter: (KaTypeParameterSymbol) -> Boolean
    ): KaSubstitutor?

    /**
     * Creates a [KaSubstitutor] which assigns type arguments such that the substituted [leftType] is a subtype of the substituted [rightType].
     * Returns `null` if such an assignment is not possible.
     *
     * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for '[leftType] <: [rightType]' and
     * tries to solve the given constraint system:
     * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
     * - If there are no type parameters involved in the provided types and [leftType] is a subtype of [rightType],
     *   [KaSubstitutor.Empty] is returned.
     *
     * [constructionPolicy] controls the way the unification substitutor is constructed.
     * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
     * See [KaUnificationSubstitutorPolicy] for more information and code examples.
     *
     * #### Examples:
     *
     * ```
     * interface MyClass<A>
     *
     * fun <T: X, X: R, R: Number> someFun(leftType: MyClass<Int>, rightType: MyClass<T>) {}
     * ```
     *
     * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_LEFT)` returns
     *   `null`, as `T` is fixed and not guaranteed to be exactly `Int` to satisfy the constraint.
     * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)` returns
     *   `KaSubstitutor { T -> kotlin/Int, X -> kotlin/Int, R -> kotlin/Int }`.
     * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_ALL)` returns the exact same
     *   substitutor `KaSubstitutor { T -> kotlin/Int, X -> kotlin/Int, R -> kotlin/Int }`.
     *
     * ```
     * fun <C: Any, T: Int> foo(leftType: List<C>, rightType: List<T>) {}
     * ```
     *
     * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_LEFT)` returns
     *   `KaSubstitutor { C -> T }`.
     * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)` returns `null`,
     *   as `C` is fixed and there is no assignment for `T` to satisfy the constraint
     *   (e.g., with `C = kotlin/Any`, while `T` is bounded by `kotlin/Int`).
     * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_ALL)` returns
     *   `KaSubstitutor { C -> kotlin/Int, T -> kotlin/Int }`, as with such a substitution,
     *   `List<C>` is a subtype of `List<T>`.
     *
     * @see KaUnificationSubstitutorPolicy.ASSIGN_LEFT
     * @see KaUnificationSubstitutorPolicy.ASSIGN_RIGHT
     * @see KaUnificationSubstitutorPolicy.ASSIGN_ALL
     */
    @KaIdeApi
    @OptIn(KaExperimentalApi::class)
    public fun createSubtypingUnificationSubstitutor(
        leftType: KaType,
        rightType: KaType,
        constructionPolicy: KaUnificationSubstitutorPolicy
    ): KaSubstitutor?

    /**
     * Creates a [KaSubstitutor] which assigns type arguments such that, for each pair in [leftTypesToRightTypes],
     * the substituted left type is a subtype of the substituted right type.
     * Returns `null` if such an assignment is not possible.
     *
     * Note that when one type parameter is shared across several constraint pairs, all these pairs affect the resulting substitution
     * for this parameter.
     *
     * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for 'leftType <: rightType'
     * from each [leftTypesToRightTypes] pair and tries to solve the given constraint system:
     * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
     * - If there are no type parameters involved in the provided types and every left type is a subtype of its right type,
     *   [KaSubstitutor.Empty] is returned.
     * - If [leftTypesToRightTypes] is empty, [KaSubstitutor.Empty] is returned as there can be no contradictions with no constraints.
     *
     * [constructionPolicy] controls the way the unification substitutor is constructed.
     * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
     * See [KaUnificationSubstitutorPolicy] for more information and code examples.
     *
     * #### Examples:
     *
     * ```
     * fun <X> rights(right1: List<X>, right2: List<X>) {}
     *
     * fun lefts(left1: List<Int>, left2: List<String>) {}
     * ```
     *
     * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_LEFT)`
     *   returns `null` as the left types contain no type parameters and the concrete left types are not subtypes of their generic right types.
     * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)`
     *   returns `KaSubstitutor { X -> intersection(kotlin/Comparable<*> & java/io/Serializable) }`.
     * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_ALL)`
     *   returns the same substitutor `KaSubstitutor { X -> intersection(kotlin/Comparable<*> & java/io/Serializable) }`.
     *
     * @see KaUnificationSubstitutorPolicy.ASSIGN_LEFT
     * @see KaUnificationSubstitutorPolicy.ASSIGN_RIGHT
     * @see KaUnificationSubstitutorPolicy.ASSIGN_ALL
     */
    @KaIdeApi
    @OptIn(KaExperimentalApi::class)
    public fun createSubtypingUnificationSubstitutor(
        leftTypesToRightTypes: List<Pair<KaType, KaType>>,
        constructionPolicy: KaUnificationSubstitutorPolicy
    ): KaSubstitutor?
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.types.KaUnificationSubstitutorPolicy] instead.**
 *
 * [KaUnificationSubstitutorPolicy] determines the way unification [KaSubstitutor]s are created in [KaSubstitutorProvider.createSubtypingUnificationSubstitutor].
 * Note that the policy only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
 */
@KaObsoleteComponentApi
@KaIdeApi
public enum class KaUnificationSubstitutorPolicy {
    /**
     * Requires that there exists an instantiation of the left type parameters
     * such that the left type is a subtype of the right type when substituted.
     * Type parameters of the right type are treated as fixed.
     *
     * The constructed substitutor contains mappings for all type parameters of the left type
     * such that the substituted left type is a subtype of the right type.
     * If a correct instantiation doesn't exist, no substitutor is produced.
     *
     * ### Examples:
     * ```kotlin
     * interface A<T> : B<T>
     * interface B<T> : C<Int, T>
     * interface C<X, Y>
     *
     * fun <K> test(leftType: A<K>, rightType: C<Int, String>) {}
     * ```
     *
     * The left type here is generic `A<K>` and the right type is a fixed supertype `C<Int, String>`.
     * For this case, [ASSIGN_LEFT] will produce `{ K -> String }` as with such a substitution, `A<String>` is a subtype of `C<Int, String>`.
     *
     * ```kotlin
     * fun <T, R: Int> example(leftType: List<T>, rightType: List<R>) {}
     * ```
     *
     * [ASSIGN_LEFT] produces `{ T -> R }` mapping as with such a substitution, `List<T>` is a subtype of `List<R>`.
     *
     * ```kotlin
     * fun <T: Number> example(leftType: Number, rightType: T) {}
     * ```
     *
     * There are no free type parameters as the left type `Number` is concrete.
     * `T` is not guaranteed to be exactly `Number` to satisfy the constraint (e.g., with `{ T -> kotlin/Int }`),
     * so [ASSIGN_LEFT] produces no substitutor.
     */
    ASSIGN_LEFT,

    /**
     * Requires that there exists an instantiation of the right type parameters
     * such that the right type is a supertype of the left type when substituted.
     * Type parameters of the left type are treated as fixed.
     *
     * The constructed substitutor contains mappings for all type parameters of the right type
     * such that the substituted right type is a supertype of the left type.
     * If a correct instantiation doesn't exist, no substitutor is produced.
     *
     * ### Examples:
     * ```kotlin
     * fun <T: Number> example(leftType: T, rightType: Number) {}
     * ```
     *
     * `T: Number` is always a subtype of `Number` with any possible instantiation of `T`.
     * The [ASSIGN_RIGHT] unification substitutor here is empty as the right type doesn't have any type parameters.
     *
     * ```kotlin
     * fun <T: Int, R: Number> example(leftType: List<T>, rightType: List<R>) {}
     * ```
     *
     * The [ASSIGN_RIGHT] unification substitutor here is `{ R -> T }`.
     *
     * ```kotlin
     * fun <T, R: Int> example(leftType: List<T>, rightType: List<R>) {}
     * ```
     *
     * Since `List<T>` is not guaranteed to be a subtype of `List<R>` for all possible instantiations of `T` (consider `{ T -> kotlin/Number }`),
     * [ASSIGN_RIGHT] unification fails and no substitutor is constructed.
     */
    ASSIGN_RIGHT,

    /**
     * Requires that there exists an instantiation of the left and right type parameters
     * such that the substituted left type is a subtype of the substituted right type.
     *
     * The constructed substitutor contains mappings for all type parameters of both the left type and the right type
     * such that the substituted left type is a subtype of the substituted right type.
     * If a correct instantiation doesn't exist, no substitutor is produced.
     *
     * ### Examples:
     * ```kotlin
     * fun <A> rightTypes(rightType1: List<Int>, rightType2: List<A>) {}
     *
     * fun <B> leftTypes(leftType1: List<B>, leftType2: List<Int>) {}
     * ```
     *
     * Both [ASSIGN_RIGHT] and [ASSIGN_LEFT] here return no substitutor, as these pairs are inverses of each other.
     * However, [ASSIGN_ALL] is able to freely assign all type parameters, so `{ A -> kotlin/Int, B -> kotlin/Int }` is produced.
     * With this substitution, both constraints are satisfied.
     */
    ASSIGN_ALL,
}

/**
 * Builds a new [KaSubstitutor] from substitutions specified inside [build].
 */
@KaExperimentalApi
@OptIn(ExperimentalContracts::class, KaImplementationDetail::class)
@JvmName("buildSubstitutorExtension")
public inline fun KaSession.buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return createSubstitutor(KaSubstitutorBuilder(token).apply(build).mappings)
}

/**
 * Builds a new [KaSubstitutor] from substitutions specified inside [build].
 */
@KaCustomContextParameterBridge
@KaExperimentalApi
context(session: KaSession)
public inline fun buildSubstitutor(
    build: KaSubstitutorBuilder.() -> Unit,
): KaSubstitutor {
    @OptIn(ExperimentalContracts::class)
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }

    return session.buildSubstitutor(build)
}

@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
public class KaSubstitutorBuilder
@KaImplementationDetail constructor(override val token: KaLifetimeToken) : KaLifetimeOwner {
    private val backingMapping = mutableMapOf<KaTypeParameterSymbol, KaType>()

    /**
     * A map of the type substitutions that have so far been accumulated by the builder.
     */
    public val mappings: Map<KaTypeParameterSymbol, KaType>
        get() = withValidityAssertion { backingMapping }

    /**
     * Adds a new [typeParameter] -> [type] substitution to the substitutor.
     *
     * If there already was a substitution with a [typeParameter], the function replaces the corresponding substitution with a new one.
     */
    public fun substitution(typeParameter: KaTypeParameterSymbol, type: KaType): Unit = withValidityAssertion {
        backingMapping[typeParameter] = type
    }

    /**
     * Adds new [KaTypeParameterSymbol] -> [KaType] substitutions to the substitutor.
     *
     * If there already was a substitution with a [KaTypeParameterSymbol], the function replaces the corresponding substitution with a new
     * one.
     */
    public fun substitutions(substitutions: Map<KaTypeParameterSymbol, KaType>): Unit = withValidityAssertion {
        backingMapping += substitutions
    }
}

/**
 * Creates a [KaSubstitutor] based on the given [mappings].
 *
 * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
 *
 * @see KaSubstitutor
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith(
        "createSubstitutor(mappings)",
        "org.jetbrains.kotlin.analysis.api.types.createSubstitutor",
    ),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor {
    return with(session) {
        createSubstitutor(
            mappings = mappings,
        )
    }
}

/**
 * Creates a [KaSubstitutor] based on the inheritance relationship between [subClass] and [superClass]. [subClass] must inherit from
 * [superClass] and there may not be any error types in the inheritance path. Otherwise, `null` is returned.
 *
 * The semantics of the resulting [KaSubstitutor] are as follows: When applied to a member of [superClass], such as a function, its type
 * parameters are substituted in such a way that the resulting member can be used with an instance of [subClass].
 *
 * In other words, the substitutor is a composition of inheritance-based substitutions incorporating the whole inheritance chain.
 *
 * #### Example
 *
 * ```
 * class A : B<String>
 * class B<T> : C<T, Int>
 * class C<X, Y>
 * ```
 *
 * - `createInheritanceTypeSubstitutor(A, B)` returns `KaSubstitutor { T -> String }`
 * - `createInheritanceTypeSubstitutor(B, C)` returns `KaSubstitutor { X -> T, Y -> Int }`
 * - `createInheritanceTypeSubstitutor(A, C)` returns `KaSubstitutor { X -> T, Y -> Int } and then KaSubstitutor { T -> String }`
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith(
        "createInheritanceTypeSubstitutor(subClass, superClass)",
        "org.jetbrains.kotlin.analysis.api.types.createInheritanceTypeSubstitutor",
    ),
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
    return with(session) {
        createInheritanceTypeSubstitutor(
            subClass = subClass,
            superClass = superClass,
        )
    }
}

/**
 * Creates a [KaSubstitutor] which assigns type arguments such that, for each pair in [leftTypesToRightTypes],
 * the substituted left type is a subtype of the substituted right type.
 * Returns `null` if such an assignment is not possible.
 *
 * Note that when one type parameter is shared across several constraint pairs, all these pairs affect the resulting substitution
 * for this parameter.
 *
 * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for 'leftType <: rightType'
 * from each [leftTypesToRightTypes] pair and tries to solve the given constraint system:
 * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
 * - If there are no type parameters involved in the provided types and every left type is a subtype of its right type,
 *   [KaSubstitutor.Empty] is returned.
 * - If [leftTypesToRightTypes] is empty, [KaSubstitutor.Empty] is returned as there can be no contradictions with no constraints.
 *
 * [isFreeTypeParameter] is called on every type parameter involved in the provided types
 * and controls the set of free type parameters registered in the constraint system.
 * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
 * The constraint system will only adjust the values of these free type parameters,
 * and the produced substitutor will only contain mappings for these parameters.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith(
        "createSubtypingUnificationSubstitutor(leftTypesToRightTypes, isFreeTypeParameter)",
        "org.jetbrains.kotlin.analysis.api.types.createSubtypingUnificationSubstitutor",
    ),
)
@KaIdeApi
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftTypesToRightTypes: List<Pair<KaType, KaType>>,
    isFreeTypeParameter: (KaTypeParameterSymbol) -> Boolean
): KaSubstitutor? {
    return with(session) {
        createSubtypingUnificationSubstitutor(
            leftTypesToRightTypes = leftTypesToRightTypes,
            isFreeTypeParameter = isFreeTypeParameter,
        )
    }
}

/**
 * Creates a [KaSubstitutor] which assigns type arguments such that the substituted [leftType] is a subtype of the substituted [rightType].
 * Returns `null` if such an assignment is not possible.
 *
 * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for '[leftType] <: [rightType]' and
 * tries to solve the given constraint system:
 * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
 * - If there are no type parameters involved in the provided types and [leftType] is a subtype of [rightType],
 *   [KaSubstitutor.Empty] is returned.
 *
 * [constructionPolicy] controls the way the unification substitutor is constructed.
 * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
 * See [KaUnificationSubstitutorPolicy] for more information and code examples.
 *
 * #### Examples:
 *
 * ```
 * interface MyClass<A>
 *
 * fun <T: X, X: R, R: Number> someFun(leftType: MyClass<Int>, rightType: MyClass<T>) {}
 * ```
 *
 * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_LEFT)` returns
 *   `null`, as `T` is fixed and not guaranteed to be exactly `Int` to satisfy the constraint.
 * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)` returns
 *   `KaSubstitutor { T -> kotlin/Int, X -> kotlin/Int, R -> kotlin/Int }`.
 * - `createSubtypingUnificationSubstitutor(MyClass<Int>, MyClass<T>, KaUnificationSubstitutorPolicy.ASSIGN_ALL)` returns the exact same
 *   substitutor `KaSubstitutor { T -> kotlin/Int, X -> kotlin/Int, R -> kotlin/Int }`.
 *
 * ```
 * fun <C: Any, T: Int> foo(leftType: List<C>, rightType: List<T>) {}
 * ```
 *
 * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_LEFT)` returns
 *   `KaSubstitutor { C -> T }`.
 * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)` returns `null`,
 *   as `C` is fixed and there is no assignment for `T` to satisfy the constraint
 *   (e.g., with `C = kotlin/Any`, while `T` is bounded by `kotlin/Int`).
 * - `createSubtypingUnificationSubstitutor(List<C>, List<T>, KaUnificationSubstitutorPolicy.ASSIGN_ALL)` returns
 *   `KaSubstitutor { C -> kotlin/Int, T -> kotlin/Int }`, as with such a substitution,
 *   `List<C>` is a subtype of `List<T>`.
 *
 * @see KaUnificationSubstitutorPolicy.ASSIGN_LEFT
 * @see KaUnificationSubstitutorPolicy.ASSIGN_RIGHT
 * @see KaUnificationSubstitutorPolicy.ASSIGN_ALL
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith(
        "createSubtypingUnificationSubstitutor(leftType, rightType, constructionPolicy)",
        "org.jetbrains.kotlin.analysis.api.types.createSubtypingUnificationSubstitutor",
    ),
)
@KaIdeApi
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftType: KaType,
    rightType: KaType,
    constructionPolicy: KaUnificationSubstitutorPolicy
): KaSubstitutor? {
    return with(session) {
        createSubtypingUnificationSubstitutor(
            leftType = leftType,
            rightType = rightType,
            constructionPolicy = constructionPolicy,
        )
    }
}

/**
 * Creates a [KaSubstitutor] which assigns type arguments such that, for each pair in [leftTypesToRightTypes],
 * the substituted left type is a subtype of the substituted right type.
 * Returns `null` if such an assignment is not possible.
 *
 * Note that when one type parameter is shared across several constraint pairs, all these pairs affect the resulting substitution
 * for this parameter.
 *
 * [createSubtypingUnificationSubstitutor] creates a constraint system, adds all the required bounds for 'leftType <: rightType'
 * from each [leftTypesToRightTypes] pair and tries to solve the given constraint system:
 * - If there were no contradictions found in the constraint system, the resulting substitutor is non-null. Otherwise, `null` is returned.
 * - If there are no type parameters involved in the provided types and every left type is a subtype of its right type,
 *   [KaSubstitutor.Empty] is returned.
 * - If [leftTypesToRightTypes] is empty, [KaSubstitutor.Empty] is returned as there can be no contradictions with no constraints.
 *
 * [constructionPolicy] controls the way the unification substitutor is constructed.
 * Only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
 * See [KaUnificationSubstitutorPolicy] for more information and code examples.
 *
 * #### Examples:
 *
 * ```
 * fun <X> rights(right1: List<X>, right2: List<X>) {}
 *
 * fun lefts(left1: List<Int>, left2: List<String>) {}
 * ```
 *
 * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_LEFT)`
 *   returns `null` as the left types contain no type parameters and the concrete left types are not subtypes of their generic right types.
 * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_RIGHT)`
 *   returns `KaSubstitutor { X -> intersection(kotlin/Comparable<*> & java/io/Serializable) }`.
 * - `createSubtypingUnificationSubstitutor(listOf(List<Int> to List<X>, List<String> to List<X>), KaUnificationSubstitutorPolicy.ASSIGN_ALL)`
 *   returns the same substitutor `KaSubstitutor { X -> intersection(kotlin/Comparable<*> & java/io/Serializable) }`.
 *
 * @see KaUnificationSubstitutorPolicy.ASSIGN_LEFT
 * @see KaUnificationSubstitutorPolicy.ASSIGN_RIGHT
 * @see KaUnificationSubstitutorPolicy.ASSIGN_ALL
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.types' endpoint instead.",
    replaceWith = ReplaceWith(
        "createSubtypingUnificationSubstitutor(leftTypesToRightTypes, constructionPolicy)",
        "org.jetbrains.kotlin.analysis.api.types.createSubtypingUnificationSubstitutor",
    ),
)
@KaIdeApi
@OptIn(KaExperimentalApi::class)
@KaContextParameterApi
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftTypesToRightTypes: List<Pair<KaType, KaType>>,
    constructionPolicy: KaUnificationSubstitutorPolicy
): KaSubstitutor? {
    return with(session) {
        createSubtypingUnificationSubstitutor(
            leftTypesToRightTypes = leftTypesToRightTypes,
            constructionPolicy = constructionPolicy,
        )
    }
}
