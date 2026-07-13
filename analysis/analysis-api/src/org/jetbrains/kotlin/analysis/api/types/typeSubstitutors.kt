/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.buildSubstitutor
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol

/**
 * Creates a [KaSubstitutor] based on the given [mappings].
 *
 * Usually, [buildSubstitutor] should be preferred to build a new substitutor from scratch.
 *
 * @see KaSubstitutor
 */
@KaExperimentalApi
context(session: KaSession)
public fun createSubstitutor(mappings: Map<KaTypeParameterSymbol, KaType>): KaSubstitutor {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createSubstitutor(mappings)
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
@KaExperimentalApi
context(session: KaSession)
public fun createInheritanceTypeSubstitutor(subClass: KaClassSymbol, superClass: KaClassSymbol): KaSubstitutor? {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createInheritanceTypeSubstitutor(subClass, superClass)
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
@KaIdeApi
@OptIn(KaExperimentalApi::class)
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftTypesToRightTypes: List<Pair<KaType, KaType>>,
    isFreeTypeParameter: (KaTypeParameterSymbol) -> Boolean,
): KaSubstitutor? {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createSubtypingUnificationSubstitutor(leftTypesToRightTypes, isFreeTypeParameter)
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
@KaIdeApi
@OptIn(KaExperimentalApi::class)
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftType: KaType,
    rightType: KaType,
    constructionPolicy: KaUnificationSubstitutorPolicy,
): KaSubstitutor? {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createSubtypingUnificationSubstitutor(leftType, rightType, constructionPolicy)
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
@KaIdeApi
@OptIn(KaExperimentalApi::class)
context(session: KaSession)
public fun createSubtypingUnificationSubstitutor(
    leftTypesToRightTypes: List<Pair<KaType, KaType>>,
    constructionPolicy: KaUnificationSubstitutorPolicy,
): KaSubstitutor? {
    @OptIn(KaImplementationDetail::class)
    return internals.substitutorProvider.createSubtypingUnificationSubstitutor(leftTypesToRightTypes, constructionPolicy)
}
