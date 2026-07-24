/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaIdeApi

/**
 * [KaUnificationSubstitutorPolicy] determines the way unification [KaSubstitutor]s are created in [createSubtypingUnificationSubstitutor].
 * Note that the policy only affects the construction when at least one of the involved types is generic, i.e., depends on a type parameter.
 */
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
