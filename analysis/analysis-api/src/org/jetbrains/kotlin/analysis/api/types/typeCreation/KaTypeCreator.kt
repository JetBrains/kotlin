/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.types.typeCreation

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.KaTypeInformationProvider
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

/**
 * An entry point for type building facilities.
 *
 * Must only be accessed via [org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider].
 */
@KaExperimentalApi
@KaTypeCreatorDslMarker
public interface KaTypeCreator : KaLifetimeOwner {
    /**
     * Builds a class type with the given class ID.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller should provide the correct number of type arguments for the class.
     * If the number of provided type arguments is lower, missing types arguments are
     * filled with [star projections][KaStarTypeProjection].
     * If there are more type arguments than required, extra arguments are ignored.
     *
     *  #### Example
     *
     * ```kotlin
     * classType(StandardClassIds.List) {
     *     argument(builtinTypes.string)
     * }
     * ```
     */
    @KaExperimentalApi
    public fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a class type with the given class symbol.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * For Kotlin built-in types, consider using the overload that accepts a [ClassId] instead:
     * `classType(StandardClassIds.String)`.
     *
     * #### Example
     *
     * ```kotlin
     * classType(builtinTypes.string.symbol)
     * ```
     */
    @KaExperimentalApi
    public fun classType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a [KaTypeParameterType] with the given type parameter symbol.
     */
    @KaExperimentalApi
    public fun typeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType

    /**
     * Builds an array type from the given [elementType].
     * For primitive element types, the [KaArrayTypeBuilder.shouldPreferPrimitiveTypes] option determines
     * whether the array type will be a [primitive array type](https://kotlinlang.org/docs/arrays.html#primitive-type-arrays).
     *
     * Array types are essentially class types and could be built manually via [classType].
     * This builder just provides a more convenient way to construct them.
     */
    @KaExperimentalApi
    public fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds the underlying array type of a [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
     * function parameter with the given [elementType].
     */
    @KaExperimentalApi
    public fun varargArrayType(elementType: KaType): KaType

    /**
     * Builds a [KaCapturedType] based on the given [type].
     */
    @KaExperimentalApi
    public fun capturedType(type: KaCapturedType, init: KaCapturedTypeBuilder.() -> Unit = {}): KaCapturedType

    /**
     * Builds a [KaCapturedType] with the given [projection].
     *
     * Note that if [projection] is [KaTypeArgumentWithVariance],
     * it's [KaTypeArgumentWithVariance.variance] must not be [Variance.INVARIANT].
     * Captured types are only intended to capture non-invariant projections.
     * Otherwise, an exception is thrown.
     */
    @KaExperimentalApi
    public fun capturedType(projection: KaTypeProjection, init: KaCapturedTypeBuilder.() -> Unit = {}): KaCapturedType

    /**
     * Builds a [KaDefinitelyNotNullType] wrapping the given [type].
     *
     * [type] can only be [KaCapturedType] or [KaTypeParameterType].
     * Otherwise, an exception is thrown.
     */
    @KaExperimentalApi
    public fun definitelyNotNullType(
        type: KaType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit = {}
    ): KaDefinitelyNotNullType

    /**
     * Builds a [KaFlexibleType] based on the given [type].
     *
     * The caller is supposed to provide a correct pair of bounds, i.e., these types must be different, and
     * the lower bound must be a subtype of the upper bound.
     * If either of the bounds is [KaFlexibleType] itself, then the corresponding bound of this type is considered instead.
     * I.e., if the upperbound is a [KaFlexibleType], then it's upperbound is taken as the resulting upperbound.
     */
    @KaExperimentalApi
    public fun flexibleType(type: KaFlexibleType, init: KaFlexibleTypeBuilder.() -> Unit = {}): KaFlexibleType

    /**
     * Builds a [KaFlexibleType] with the given [lowerBound] and [upperBound] bounds.
     *
     * The caller is supposed to provide a correct pair of bounds, i.e., these types must be different, and
     * the lower bound must be a subtype of the upper bound.
     * If either of the bounds is [KaFlexibleType] itself, then the corresponding bound of this type is considered instead.
     * I.e., if the [upperBound] is a [KaFlexibleType], then it's upperbound is taken as the resulting upperbound.
     */
    @KaExperimentalApi
    public fun flexibleType(lowerBound: KaType, upperBound: KaType, init: KaFlexibleTypeBuilder.() -> Unit = {}): KaFlexibleType

    /**
     * Builds an [KaIntersectionType] based on the given [type].
     *
     * [KaIntersectionType] must be flat by its contract, i.e., not contain any other intersection type.
     * To achieve this, all intersection types passed to the builder are unwrapped.
     *
     * The set of conjuncts must contain at least one element.
     * Otherwise, an exception is thrown.
     */
    @KaExperimentalApi
    public fun intersectionType(type: KaIntersectionType, init: KaIntersectionTypeBuilder.() -> Unit = {}): KaIntersectionType

    /**
     * Builds an [KaIntersectionType] with the provided [conjuncts].
     *
     * [KaIntersectionType] must be flat by its contract, i.e., not contain any other intersection type.
     * To achieve this, all intersection types passed to the builder are unwrapped.
     *
     * The set of conjuncts must contain at least one element.
     * Otherwise, an exception is thrown.
     */
    @KaExperimentalApi
    public fun intersectionType(conjuncts: List<KaType> = listOf(), init: KaIntersectionTypeBuilder.() -> Unit = {}): KaIntersectionType

    /**
     * Builds a [KaDynamicType].
     */
    @KaExperimentalApi
    public fun dynamicType(): KaDynamicType

    /**
     * Builds a [KaTypeArgumentWithVariance].
     */
    @KaExperimentalApi
    public fun typeArgumentWithVariance(variance: Variance, type: KaType): KaTypeArgumentWithVariance

    /**
     * Builds a [KaTypeArgumentWithVariance].
     */
    @KaExperimentalApi
    public fun typeArgumentWithVariance(variance: Variance, type: KaTypeCreator.() -> KaType): KaTypeArgumentWithVariance

    /**
     * Builds a [KaStarTypeProjection] (`*`).
     */
    @KaExperimentalApi
    public fun starTypeProjection(): KaStarTypeProjection
}

/**
 * A DSL marker used to annotate entities related to the type building infrastructure in [KaTypeCreator].
 */
@DslMarker
@KaExperimentalApi
public annotation class KaTypeCreatorDslMarker

/**
 * A base interface for all concrete type builders.
 *
 * [KaTypeBuilder] is derived from [KaTypeCreator] to extend its type building API
 * while keeping all base endpoints accessible.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeBuilder : KaTypeCreator

/**
 * A builder for [KaClassType].
 *
 * @see KaTypeCreator.classType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaClassTypeBuilder : KaTypeBuilder {
    /**
     * Whether the type is marked as nullable, i.e., the type is represented as `T?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean

    /**
     * List of type arguments that are currently registered in the builder.
     * A type argument can be added using [typeArgument], [typeArguments] or [invariantTypeArgument].
     */
    public val typeArguments: List<KaTypeProjection>

    /**
     * Adds [type] as an [invariant][Variance.INVARIANT] type argument to the class type.
     */
    public fun invariantTypeArgument(type: KaType)

    /**
     * Adds [type] as a type argument to the class type with the given [variance].
     */
    public fun typeArgument(variance: Variance, type: KaType)

    /**
     * Adds [type] as an [invariant][Variance.INVARIANT] type argument to the class type.
     */
    public fun invariantTypeArgument(type: () -> KaType)

    /**
     * Adds [type] as a type argument to the class type with the given [variance].
     */
    public fun typeArgument(variance: Variance, type: () -> KaType)

    /**
     * Adds [argument] as a type argument to the class type.
     */
    public fun typeArgument(argument: KaTypeProjection)

    /**
     * Adds [argument] as a type argument to the class type.
     */
    public fun typeArgument(argument: () -> KaTypeProjection)

    /**
     * Adds [arguments] as type arguments to the class type.
     */
    public fun typeArguments(arguments: () -> Iterable<KaTypeProjection>)
}

/**
 * A builder for [KaTypeParameterType].
 *
 * @see KaTypeCreator.typeParameterType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeParameterTypeBuilder : KaTypeBuilder {
    /**
     * Whether the type is marked as nullable, i.e., the type is represented as `T?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean
}

/**
 * A builder for array types.
 *
 * @see KaTypeCreator.arrayType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaArrayTypeBuilder : KaTypeBuilder {
    /**
     * Whether the resulting array type is marked as nullable, i.e., the type is represented as `Array<T>?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean

    /**
     * Variance that should be used for the resulting boxed array (`Array<T>`).
     * This doesn't affect anything if [shouldPreferPrimitiveTypes] is set to `true` and the given element type is primitive.
     *
     * Default value: [Variance.INVARIANT].
     */
    public var variance: Variance

    /**
     * Whether the builder should try to construct [primitive arrays](https://kotlinlang.org/docs/arrays.html#primitive-type-arrays)
     * (e.g. `IntArray`) for primitive types (e.g. `Int`).
     *
     * Note that nullable primitive types (e.g. `Int?`) are not considered primitive, as they are represented as objects.
     * Thus, for these types, the builder will always produce boxed arrays (e.g. `Array<Int?>`).
     *
     * Default value: `true`.
     */
    public var shouldPreferPrimitiveTypes: Boolean
}

/**
 * A builder for captured types.
 *
 * @see KaTypeCreator.capturedType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCapturedTypeBuilder : KaTypeBuilder {
    /**
     * Whether the type is marked as nullable, i.e., the type is represented as `T?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean
}

/**
 * A builder for definitely-not-null types.
 *
 * @see KaTypeCreator.definitelyNotNullType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDefinitelyNotNullTypeBuilder : KaTypeBuilder

/**
 * A builder for flexible types.
 *
 * @see KaTypeCreator.flexibleType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaFlexibleTypeBuilder : KaTypeBuilder {
    /**
     * The lower bound, such as `String` in `String!`.
     */
    public var lowerBound: KaType

    /**
     * The upper bound, such as `String?` in `String!`.
     */
    public var upperBound: KaType
}

/**
 * A builder for intersection types.
 *
 * @see KaTypeCreator.intersectionType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaIntersectionTypeBuilder : KaTypeBuilder {
    /**
     * A set of individual types participating in the intersection.
     */
    public val conjuncts: Set<KaType>

    /**
     * Adds a [conjunct] to the [conjuncts] set.
     */
    public fun conjunct(conjunct: KaType)

    /**
     * Adds a conjunct produced by [conjunct] to the [conjuncts] set.
     */
    public fun conjunct(conjunct: () -> KaType)

    /**
     * Adds a list of conjuncts to the [conjuncts] set.
     */
    public fun conjuncts(conjuncts: () -> Iterable<KaType>)

    /**
     * Adds a list of conjuncts to the [conjuncts] set.
     */
    public fun conjuncts(conjuncts: Iterable<KaType>)
}
