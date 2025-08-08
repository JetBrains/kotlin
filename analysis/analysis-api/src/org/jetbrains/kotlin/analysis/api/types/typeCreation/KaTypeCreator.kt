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
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeCreator : KaLifetimeOwner {
    /**
     * Builds a class type with the given [classId].
     *
     * If there are no classes available by [classId], returns [KaClassErrorType].
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
     *     invariantTypeArgument(builtinTypes.string)
     * }
     * ```
     */
    @KaExperimentalApi
    public fun classType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a class type from the given class [symbol].
     *
     * If it's impossible to construct a type from [symbol], returns [KaClassErrorType].
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
     * classType(ktClass.classSymbol as KaClassSymbol) {
     *     invariantTypeArgument(builtinTypes.string)
     * }
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
     *
     * If the type was constructed successfully, returns [KaClassType].
     * Otherwise, returns [KaClassErrorType].
     */
    @KaExperimentalApi
    public fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds the underlying array type of a [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
     * function parameter with the given [elementType].
     *
     * If the type was constructed successfully, returns [KaClassType].
     * Otherwise, returns [KaClassErrorType].
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
     * its [KaTypeArgumentWithVariance.variance] must not be [Variance.INVARIANT].
     * Captured types are only intended to capture non-invariant projections.
     * Otherwise, an exception is thrown.
     */
    @KaExperimentalApi
    public fun capturedType(projection: KaTypeProjection, init: KaCapturedTypeBuilder.() -> Unit = {}): KaCapturedType

    /**
     * Builds a [KaDefinitelyNotNullType] wrapping the given [type].
     *
     * If [type] is not nullable, the original type is returned,
     * as wrapping it in [KaDefinitelyNotNullType] is unnecessary.
     */
    @KaExperimentalApi
    public fun definitelyNotNullType(
        type: KaCapturedType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit = {}
    ): KaType

    /**
     * Builds a [KaDefinitelyNotNullType] wrapping the given [type].
     *
     * If [type] is not nullable, the original type is returned,
     * as wrapping it in [KaDefinitelyNotNullType] is unnecessary.
     */
    @KaExperimentalApi
    public fun definitelyNotNullType(
        type: KaTypeParameterType,
        init: KaDefinitelyNotNullTypeBuilder.() -> Unit = {}
    ): KaType

    /**
     * Builds a [KaFlexibleType] with initial bounds taken from the given [type].
     *
     * If either of the bounds is [KaFlexibleType] itself, then the corresponding bound of this type is considered instead.
     * I.e., if the upper bound is a [KaFlexibleType], then it's upper bound is taken as the resulting upper bound.
     *
     * If the lower bound is not a subtype of the upper bound, `null` is returned.
     *
     * If both bounds are equal, the bound type is returned,
     * as it's unnecessary to create a flexible type in this case.
     */
    @KaExperimentalApi
    public fun flexibleType(type: KaFlexibleType, init: KaFlexibleTypeBuilder.() -> Unit = {}): KaType?

    /**
     * Builds a [KaFlexibleType].
     *
     * If either of the bounds is [KaFlexibleType] itself, then the corresponding bound of this type is considered instead.
     * I.e., if the upper bound is a [KaFlexibleType], then it's upper bound is taken as the resulting upper bound.
     *
     * If the lower bound is not a subtype of the upper bound, `null` is returned.
     *
     * If both bounds are equal, the bound type is returned,
     * as it's unnecessary to create a flexible type in this case.
     */
    @KaExperimentalApi
    public fun flexibleType(init: KaFlexibleTypeBuilder.() -> Unit = {}): KaType?

    /**
     * Builds a [KaFlexibleType] with [lowerBound] and [upperBound] as bounds.
     *
     * If either of the bounds is [KaFlexibleType] itself, then the corresponding bound of this type is considered instead.
     * I.e., if the upper bound is a [KaFlexibleType], then it's upper bound is taken as the resulting upper bound.
     *
     * If the lower bound is not a subtype of the upper bound, `null` is returned.
     *
     * If both bounds are equal, the bound type is returned,
     * as it's unnecessary to create a flexible type in this case.
     */
    @KaExperimentalApi
    public fun flexibleType(lowerBound: KaType, upperBound: KaType): KaType?

    /**
     * Builds an [KaIntersectionType].
     *
     * The builder returns a normalized version of the intersection,
     * i.e., all duplicated types are removed, nested intersection types are unwrapped, etc.
     *
     * This normalized version isn't always [KaIntersectionType].
     * For example, if there is a [KaFlexibleType] among conjuncts,
     * the intersector might return another [KaFlexibleType] with [KaIntersectionType]s as bounds.
     * That's due to the distributive property of intersection as a mathematical operation: `(A..B) & C = (A & C)..(B & C)`.
     *
     * If there are no conjuncts, returns [Any?][org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes.nullableAny]
     * as a neutral element of intersection operation.
     * If a single conjunct is passed, returns that conjunct.
     *
     * Note that currently it's impossible to provide annotations in [KaIntersectionTypeBuilder] due to KT-80749.
     */
    @KaExperimentalApi
    public fun intersectionType(init: KaIntersectionTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a [KaDynamicType].
     */
    @KaExperimentalApi
    public fun dynamicType(init: KaDynamicTypeBuilder.() -> Unit = {}): KaDynamicType

    /**
     * Builds a [KaTypeArgumentWithVariance].
     */
    @KaExperimentalApi
    public fun typeProjection(variance: Variance, type: KaType): KaTypeArgumentWithVariance

    /**
     * Builds a [KaTypeArgumentWithVariance].
     */
    @KaExperimentalApi
    public fun typeProjection(variance: Variance, type: KaTypeCreator.() -> KaType): KaTypeArgumentWithVariance

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
 * A builder interface derived from [KaTypeBuilder] allowing creating annotated types.
 *
 * At the moment, the builder only supports annotations that accept no value arguments.
 *
 * Should only be used for types, for which constructing annotations makes sense.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeBuilderWithAnnotations : KaTypeBuilder {
    /**
     * A list of annotation [ClassId]s, which are used to construct annotations of the resulting type.
     *
     * @see KaType.annotations
     */
    public val annotations: List<ClassId>

    /**
     * Adds [annotationClassId] to [annotations].
     *
     * Note that the builder only supports annotations that accept no value arguments.
     * All annotation classes requiring value arguments are discarded.
     *
     * @see KaType.annotations
     */
    public fun annotation(annotationClassId: ClassId)

    /**
     * Adds the annotation produced by [annotationClassId] to [annotations].
     *
     * Note that the builder only supports annotations that accept no value arguments.
     * All annotation classes requiring value arguments are discarded.
     *
     * @see KaType.annotations
     */
    public fun annotation(annotationClassId: () -> ClassId)

    /**
     * Adds [annotationClassIds] to [annotations].
     *
     * Note that the builder only supports annotations that accept no value arguments.
     * All annotation classes requiring value arguments are discarded.
     *
     * @see KaType.annotations
     */
    public fun annotations(annotationClassIds: Iterable<ClassId>)

    /**
     * Adds [annotationClassIds] to [annotations].
     *
     * Note that the builder only supports annotations that accept no value arguments.
     * All annotation classes requiring value arguments are discarded.
     *
     * @see KaType.annotations
     */
    public fun annotations(annotationClassIds: () -> Iterable<ClassId>)
}

/**
 * A builder for [KaClassType].
 *
 * @see KaTypeCreator.classType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaClassTypeBuilder : KaTypeBuilderWithAnnotations {
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
public interface KaTypeParameterTypeBuilder : KaTypeBuilderWithAnnotations {
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
public interface KaArrayTypeBuilder : KaTypeBuilderWithAnnotations {
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
 * A builder for [KaCapturedType].
 *
 * @see KaTypeCreator.capturedType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaCapturedTypeBuilder : KaTypeBuilderWithAnnotations {
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
 * A builder for [KaDefinitelyNotNullType].
 *
 * @see KaTypeCreator.definitelyNotNullType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDefinitelyNotNullTypeBuilder : KaTypeBuilderWithAnnotations

/**
 * A builder for [KaFlexibleType].
 *
 * @see KaTypeCreator.flexibleType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaFlexibleTypeBuilder : KaTypeBuilderWithAnnotations {
    /**
     * The lower bound, such as `String` in `String!`.
     *
     * Default value: [Nothing][org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes.nothing].
     *
     * @see KaFlexibleType.lowerBound
     */
    public var lowerBound: KaType

    /**
     * The upper bound, such as `String?` in `String!`.
     *
     * Default value: [Any?][org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes.nullableAny].
     *
     * @see KaFlexibleType.upperBound
     */
    public var upperBound: KaType
}

/**
 * A builder for [KaIntersectionType].
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

/**
 * A builder for [KaDynamicType].
 *
 * @see KaTypeCreator.dynamicType
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaDynamicTypeBuilder : KaTypeBuilderWithAnnotations
