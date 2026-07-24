/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * [KaType] instances for built-in types.
 */
context(session: KaSession)
public val builtinTypes: KaBuiltinTypes
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.builtinTypes()
    }

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] supertype.
 *
 * The function returns `null` if the type is already denotable and does not need approximation. Otherwise, for a type `T`, returns a
 * denotable type `S` such that `T <: S`, with all type arguments of `S` also being denotable.
 *
 * @param allowLocalDenotableTypes Whether locally declared types should be approximated to local supertypes instead of non-local ones.
 * Local type approximation is sensible when the resulting [KaType] is analyzed in the same local context.
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSupertype(allowLocalDenotableTypes: Boolean): KaType? {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.approximateToDenotableSupertype(this, allowLocalDenotableTypes)
}

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] supertype, or returns the given type itself if it is
 * already denotable.
 *
 * @see approximateToDenotableSupertype
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSupertypeOrSelf(allowLocalDenotableTypes: Boolean): KaType =
    approximateToDenotableSupertype(allowLocalDenotableTypes) ?: this

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] subtype.
 *
 * The function returns `null` if the type is already denotable and does not need approximation. Otherwise, for a type `T`, returns a
 * denotable type `S` such that `S <: T`, with all type arguments of `S` also being denotable.
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSubtype(): KaType? {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.approximateToDenotableSubtype(this)
}

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] subtype, or returns the given type itself if it is
 * already denotable.
 *
 * @see approximateToDenotableSupertype
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSubtypeOrSelf(): KaType =
    approximateToDenotableSubtype() ?: this

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] supertype based on the given [position].
 *
 * This [position] is used when approximating local types.
 * If the given type is local, then the function returns the first supertype, which is visible from the given [position].
 * Note that [position] is required to be within [KaAnalysisScopeProvider.analysisScope][org.jetbrains.kotlin.analysis.api.components.KaAnalysisScopeProvider.analysisScope],
 * otherwise, an exception is thrown.
 *
 * The function returns `null` if the type is already denotable and does not need approximation.
 * Otherwise, for a type `T`, returns a
 * denotable type `S` such that `T <: S`, with all type arguments of `S` also being denotable.
 *
 * Example:
 * ```kotlin
 * <position_1>
 * fun foo() {
 *     open class <position_2> A
 *
 *     fun bar() = <expr>object: A() {}</expr>
 * }
 * ```
 *
 * In the example above we are trying to approximate the type of `object: A() {}` expression,
 * which is a local type `<anonymous>: A`.
 * When this type is approximated using `<position_2>` the function returns `A`, as this type is visible from this position.
 * However, when approximating from `<position_1>`, the function returns `Any`, as `A` is not visible from this position,
 * so the only option left is `Any`.
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSupertype(position: KtElement): KaType? {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.approximateToDenotableSupertype(this, position)
}

/**
 * Approximates [KaType] to a [denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] subtype based on the given [position],
 * or returns the given type itself if it is already denotable.
 *
 * @see approximateToDenotableSupertype
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaType.approximateToDenotableSupertypeOrSelf(position: KtElement): KaType =
    approximateToDenotableSupertype(position) ?: this

/**
 * A [KaType] derived from the given type by enforcing warning-level nullability annotations.
 * If the derived type doesn't differ from the original type, the original type is used.
 *
 * In general, Java type enhancement allows the Kotlin compiler to infer a more specific nullability for a Java type based on its
 * [nullability annotations](https://kotlinlang.org/docs/java-interop.html#nullability-annotations). Normally,
 * only [strict][org.jetbrains.kotlin.load.java.ReportLevel.STRICT]
 * nullability annotations have an impact on a resolved type's nullability.
 * These annotations are already taken into account in [KaType].
 *
 * However, there are also [warning-level][org.jetbrains.kotlin.load.java.ReportLevel.WARN] nullability annotations,
 * such as Android's `RecentlyNullable` and `RecentlyNonNull`.
 * These annotations have weaker constraints and don't affect a resolved type's nullability.
 * [augmentedByWarningLevelAnnotations] is a [KaType] with weak annotations treated as strict ones.
 *
 * See the list of default report levels for different nullability annotations in
 * [NULLABILITY_ANNOTATION_SETTINGS][org.jetbrains.kotlin.load.java.NULLABILITY_ANNOTATION_SETTINGS]
 *
 * ### Examples
 *
 * - For `@androidx.annotation.RecentlyNullable X!` [augmentedByWarningLevelAnnotations] is `X?`.
 * - For `@androidx.annotation.RecentlyNonNull X!` [augmentedByWarningLevelAnnotations] is `X`.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaType.augmentedByWarningLevelAnnotations: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.augmentedByWarningLevelAnnotations(this)
    }

/**
 * The representation of [this] in terms of [KaType].
 *
 * Type parameters are substituted with matching type parameter types, e.g. `List<T>` for the `List` class.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
 */
context(session: KaSession)
public val KaClassifierSymbol.defaultType: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.defaultType(this)
    }

/**
 * The representation of [this] in terms of [KaType].
 *
 * Type parameters are substituted with [KaStarTypeProjection], e.g. `List<*>` for the `List` class.
 *
 * @see org.jetbrains.kotlin.analysis.api.components.KaTypeCreator
 */
@KaExperimentalApi
context(session: KaSession)
public val KaClassifierSymbol.defaultTypeWithStarProjections: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.defaultTypeWithStarProjections(this)
    }

/**
 * The array type that represents the list of arguments passed to this parameter if [this] is a
 * [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs) parameter.
 *
 * If [this] is not a `vararg` parameter, [varargArrayType] is `null`.
 * If [this] is an invalid (e.g., in case of multiple `vararg` parameters) or useless (in anonymous functions) `vararg` parameter,
 * [varargArrayType] still contains a type for it.
 */
@KaExperimentalApi
context(session: KaSession)
public val KaValueParameterSymbol.varargArrayType: KaType?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.varargArrayType(this)
    }

/**
 * The common supertype of the given [KaType]s.
 *
 * @throws IllegalArgumentException If the collection of types is empty.
 */
context(session: KaSession)
public val Iterable<KaType>.commonSupertype: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.commonSupertype(this)
    }

/**
 * The common supertype of the given [KaType]s.
 *
 * @throws IllegalArgumentException If the array of types is empty.
 */
context(session: KaSession)
public val Array<KaType>.commonSupertype: KaType
    get() = asList().commonSupertype

/**
 * Resolves the given [KtTypeReference] to its corresponding [KaType].
 *
 * This may raise an exception if the resolution ends up with an unexpected result.
 */
context(session: KaSession)
public val KtTypeReference.type: KaType
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.type(this)
    }

/**
 * Resolves the given [KtDoubleColonExpression] to the [KaType] of its receiver.
 *
 * The result may be `null` if the resolution fails or the resolved callable reference is not a reflection type.
 *
 * #### Example
 *
 * ```kotlin
 * class Foo {
 *     fun bar() { }
 * }
 *
 * val foo = Foo()
 * foo::bar
 * ```
 *
 * Here, `receiverType` for `foo::bar` is `Foo` (the type of `foo`).
 */
context(session: KaSession)
public val KtDoubleColonExpression.receiverType: KaType?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.receiverType(this)
    }

/**
 * Creates a new [KaType] based on the given type with the updated nullability specified by [isMarkedNullable].
 */
context(session: KaSession)
public fun KaType.withNullability(isMarkedNullable: Boolean): KaType {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.withNullability(this, isMarkedNullable)
}

/**
 * Returns the [KaFlexibleType]'s upper bound, or the type itself if it is not flexible.
 */
context(session: KaSession)
public fun KaType.upperBoundIfFlexible(): KaType =
    (this as? KaFlexibleType)?.upperBound ?: this

/**
 * Returns the [KaFlexibleType]'s lower bound, or the type itself if it is not flexible.
 */
context(session: KaSession)
public fun KaType.lowerBoundIfFlexible(): KaType =
    (this as? KaFlexibleType)?.lowerBound ?: this

/**
 * Checks whether this [KaType] is compatible with [that] other type. If they are compatible, the types can have a common subtype.
 */
context(session: KaSession)
public fun KaType.hasCommonSubtypeWith(that: KaType): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.hasCommonSubtypeWith(this, that)
}

/**
 * Returns the direct supertypes of the given [KaType].
 *
 * For flexible types, direct supertypes of both the upper and lower bounds are returned. If that's not desirable, use
 * [directSupertypes] on [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound].
 *
 * #### Example
 *
 * Given `MutableList<String>`, [directSupertypes] returns `List<String>` and `MutableCollection<String>`
 *
 * @param shouldApproximate Whether to approximate [non-denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] types. For example, the
 * supertype of `List<out String>` is `Collection<CAPTURED out String>`. With approximation set to `true`, `Collection<out String>` is
 * returned instead.
 */
context(session: KaSession)
public fun KaType.directSupertypes(shouldApproximate: Boolean): Sequence<KaType> {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.directSupertypes(this, shouldApproximate)
}

/**
 * The direct supertypes of the given [KaType].
 *
 * For flexible types, direct supertypes of both the upper and lower bounds are included. If that's not desirable, use
 * [directSupertypes] on [KaFlexibleType.upperBound] or [KaFlexibleType.lowerBound].
 *
 * [Denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] types are not approximated.
 *
 * #### Example
 *
 * Given `MutableList<String>`, [directSupertypes] contains `List<String>` and `MutableCollection<String>`
 */
context(session: KaSession)
public val KaType.directSupertypes: Sequence<KaType>
    get() = directSupertypes(shouldApproximate = false)

/**
 * Returns all supertypes of the given [KaType]. The resulting sequence is ordered by a breadth-first traversal of the class hierarchy,
 * without duplicates.
 *
 * @param shouldApproximate Whether to approximate [non-denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] types. See [directSupertypes]
 *  for more information.
 */
context(session: KaSession)
public fun KaType.allSupertypes(shouldApproximate: Boolean): Sequence<KaType> {
    @OptIn(KaImplementationDetail::class)
    return internals.typeProvider.allSupertypes(this, shouldApproximate)
}

/**
 * All supertypes of the given [KaType]. The resulting sequence is ordered by a breadth-first traversal of the class hierarchy, without
 * duplicates.
 *
 * [Denotable][org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider.isDenotable] types are not approximated.
 */
context(session: KaSession)
public val KaType.allSupertypes: Sequence<KaType>
    get() = allSupertypes(shouldApproximate = false)

/**
 * The array type's element type if the given [KaType] is a primitive type array or [Array], and `null` otherwise.
 */
context(session: KaSession)
public val KaType.arrayElementType: KaType?
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.typeProvider.arrayElementType(this)
    }
