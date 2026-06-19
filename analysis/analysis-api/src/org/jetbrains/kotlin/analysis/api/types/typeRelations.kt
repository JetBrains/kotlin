/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.name.ClassId

/**
 * Returns whether this [KaType] is semantically equal to [other].
 *
 * Semantic equality stands in contrast to the structural equality implemented by [KaType.equals]. See [KaType] for a detailed
 * discussion about structural vs. semantic type equality.
 */
context(session: KaSession)
public fun KaType.semanticallyEquals(other: KaType): Boolean =
    semanticallyEquals(other, KaSubtypingErrorTypePolicy.STRICT)

/**
 * Returns whether this [KaType] is semantically equal to [other].
 *
 * Semantic equality stands in contrast to the structural equality implemented by [KaType.equals]. See [KaType] for a detailed
 * discussion about structural vs. semantic type equality.
 *
 * The [errorTypePolicy] determines the treatment of error types in the equality check.
 */
context(session: KaSession)
public fun KaType.semanticallyEquals(other: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.typeRelationChecker.semanticallyEquals(this, other, errorTypePolicy)
}

/**
 * Returns whether this [KaType] is a subtype of [supertype]. The relation is non-strict, i.e. any type `t` is a subtype of itself.
 */
context(session: KaSession)
public fun KaType.isSubtypeOf(supertype: KaType): Boolean =
    isSubtypeOf(supertype, KaSubtypingErrorTypePolicy.STRICT)

/**
 * Returns whether this [KaType] is a subtype of [supertype]. The relation is non-strict, i.e. any type `t` is a subtype of itself.
 *
 * The [errorTypePolicy] determines the treatment of error types in the subtyping check.
 */
context(session: KaSession)
public fun KaType.isSubtypeOf(supertype: KaType, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.typeRelationChecker.isSubtypeOf(this, supertype, errorTypePolicy)
}

/**
 * Returns whether this [KaType] is a subtype of a class called [classId].
 *
 * This function provides a convenient way to check if a class extends a certain base class or interface while disregarding type
 * arguments. For example, one may check if this [KaType] is a subtype of
 * [StandardClassIds.Iterable][org.jetbrains.kotlin.name.StandardClassIds.Iterable].
 *
 * See the overload taking a [KaSubtypingErrorTypePolicy] for the treatment of error types.
 */
context(session: KaSession)
public fun KaType.isSubtypeOf(classId: ClassId): Boolean =
    isSubtypeOf(classId, KaSubtypingErrorTypePolicy.STRICT)

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
context(session: KaSession)
public fun KaType.isSubtypeOf(classId: ClassId, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.typeRelationChecker.isSubtypeOf(this, classId, errorTypePolicy)
}

/**
 * Returns whether this [KaType] is a subtype of a class represented by [symbol].
 *
 * This function provides a convenient way to check if a class extends a certain base class or interface while disregarding type
 * arguments.
 *
 * See the overload taking a [KaSubtypingErrorTypePolicy] for the treatment of error types.
 */
context(session: KaSession)
public fun KaType.isSubtypeOf(symbol: KaClassLikeSymbol): Boolean =
    isSubtypeOf(symbol, KaSubtypingErrorTypePolicy.STRICT)

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
context(session: KaSession)
public fun KaType.isSubtypeOf(symbol: KaClassLikeSymbol, errorTypePolicy: KaSubtypingErrorTypePolicy): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.typeRelationChecker.isSubtypeOf(this, symbol, errorTypePolicy)
}
