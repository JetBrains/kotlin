/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.name.ClassId

public interface KaTypeRelationChecker {
    @Deprecated("Use 'semanticallyEquals()' instead", replaceWith = ReplaceWith("semanticallyEquals(other)"))
    public fun KaType.isEqualTo(other: KaType): Boolean = semanticallyEquals(other)

    /**
     * Returns whether this [KaType] is semantically equal to [other].
     *
     * Semantic equality stands in contrast to the structural equality implemented by [KaType.equals]. See [KaType] for a detailed
     * discussion about structural vs. semantic type equality.
     */
    public fun KaType.semanticallyEquals(
        other: KaType,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean

    @Deprecated("Use 'semanticallyEquals()' instead.", replaceWith = ReplaceWith("semanticallyEquals(other, errorTypePolicy)"))
    public fun KaType.isEqualTo(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean =
        semanticallyEquals(other, errorTypePolicy)

    /**
     * Returns whether this [KaType] is a subtype of [supertype]. The relation is non-strict, i.e. any type `t` is a subtype of itself.
     */
    public fun KaType.isSubtypeOf(
        supertype: KaType,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean

    @Deprecated("Use 'isSubtypeOf' instead.", replaceWith = ReplaceWith("isSubtypeOf(other, errorTypePolicy)"))
    public fun KaType.isSubTypeOf(
        superType: KaType,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean = isSubtypeOf(superType, errorTypePolicy)

    @Deprecated(
        "Use negated 'isSubtypeOf()' instead.",
        replaceWith = ReplaceWith("!isSubtypeOf(other, errorTypePolicy)")
    )
    public fun KaType.isNotSubTypeOf(
        superType: KaType,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean = withValidityAssertion {
        return !isSubtypeOf(superType, errorTypePolicy)
    }

    /**
     * Returns whether this [KaType] is a subtype of a class called [classId].
     *
     * This function provides a convenient way to check if a class extends a certain base class or interface while disregarding type
     * arguments. For example, one may check if this [KaType] is a subtype of
     * [StandardClassIds.Iterable][org.jetbrains.kotlin.name.StandardClassIds.Iterable].
     *
     * The [errorTypePolicy] is applied as such: If this [KaType] is an error type, the [LENIENT][KaSubtypingErrorTypePolicy.LENIENT] policy
     * leads to a trivially `true` result. Errors in type arguments are not considered, as the subclass check is concerned with the applied
     * class type and not its type arguments.
     *
     * This function for [ClassId]s is a convenient dual to other [isSubtypeOf] functions. As such, its result is the same as a call to
     * [isSubtypeOf] with the following right-hand [KaType]: `a.b.Class<*, *, ...>?` given a class ID `a.b.Class` with all type arguments
     * instantiated to a star projection.
     *
     * This has the following interesting implications:
     *
     * - If the [classId] points to or actualizes to a type alias, subclassing is checked for the expanded type, as other [isSubtypeOf]
     *   implementations also take expansion into account. If the type alias doesn't expand to a
     *   [KaClassType][org.jetbrains.kotlin.analysis.api.types.KaClassType], [isSubtypeOf] is trivially `false`.
     * - If the [classId] cannot be resolved, it effectively means that we would have an "unresolved symbol" error [KaType] on the
     *   right-hand side of [isSubtypeOf]. Hence, with a [LENIENT][KaSubtypingErrorTypePolicy.LENIENT] error type policy, [isSubtypeOf]
     *   is `true` for all unresolved class IDs.
     */
    public fun KaType.isSubtypeOf(
        classId: ClassId,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean

    /**
     * Returns whether this [KaType] is a subtype of a class represented by [symbol].
     *
     * This function provides a convenient way to check if a class extends a certain base class or interface while disregarding type
     * arguments.
     *
     * The [errorTypePolicy] is applied as such: If this [KaType] is an error type, the [LENIENT][KaSubtypingErrorTypePolicy.LENIENT] policy
     * leads to a trivially `true` result. Errors in type arguments are not considered, as the subclass check is concerned with the applied
     * class type and not its type arguments.
     *
     * This function for [KaClassLikeSymbol]s is a convenient dual to other [isSubtypeOf] functions. As such, its result is the same as a
     * call to [isSubtypeOf] with the following right-hand [KaType]: `a.b.Class<*, *, ...>?` given a class called `a.b.Class` with all type
     * arguments instantiated to a star projection.
     *
     * This has the following interesting implication: If the [symbol] points to or actualizes to a type alias, subclassing is checked for
     * the expanded type, as other [isSubtypeOf] implementations also take expansion into account. If the type alias doesn't expand to a
     * [KaClassType][org.jetbrains.kotlin.analysis.api.types.KaClassType], [isSubtypeOf] is trivially `false`.
     */
    public fun KaType.isSubtypeOf(
        symbol: KaClassLikeSymbol,
        errorTypePolicy: KaSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy.STRICT,
    ): Boolean
}

/**
 * [KaSubtypingErrorTypePolicy] determines the treatment of error types in type equality and subtyping checks.
 */
public enum class KaSubtypingErrorTypePolicy {
    /**
     * Error types are not considered equal to or subtypes of any other type.
     *
     * [STRICT] is the default policy for the following reasons:
     *
     *  1. In general, considering an error type equal to all other types is unintuitive. It essentially turns error types into dynamic `Any`
     *     or `Nothing` types (depending on the typing position). This can produce a lot of typing relationships which do not make any sense,
     *     such as `Int = UnresolvedClass`.
     *  2. It forces the user to handle error types explicitly, which reduces the risk of false positives.
     *  3. It is consistent with most of the behavior of the Kotlin compiler.
     */
    STRICT,

    /**
     * Error types are equal to and subtypes of all types.
     *
     * [LENIENT] should be chosen if type errors are expected and should be treated as type holes that can be satisfied by any other type.
     * However, caution should be applied when using [LENIENT], as the policy can swallow type errors which should have been dealt with
     * explicitly. In most cases, explicit type error handling should be preferred.
     *
     * As a motivating example, consider that we want to find all functions which can be called with an argument of a certain type and show
     * them to a user. The user has provided a type `List<>` with an unspecified type argument, which translates to `List<ERROR>`. We still
     * want to show functions like `foo(list: List<Int>)`, as the user hasn't filled out the type argument yet. With the [LENIENT] policy,
     * `List<ERROR>` is a subtype of `List<Int>`, meaning that `foo(list: List<Int>)` is a valid candidate for our purposes.
     */
    LENIENT,
}

@Deprecated("Use 'KaSubtypingErrorTypePolicy' instead.", replaceWith = ReplaceWith("KaSubtypingErrorTypePolicy"))
public typealias KtSubtypingErrorTypePolicy = KaSubtypingErrorTypePolicy
