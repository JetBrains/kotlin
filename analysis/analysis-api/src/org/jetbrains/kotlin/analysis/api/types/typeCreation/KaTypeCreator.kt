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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance

/**
 * An entry point for type building facilities.
 *
 * Must only be accessed via [org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider].
 */
@KaExperimentalApi
public interface KaTypeCreator : KaLifetimeOwner {
    /**
     * Builds a class type with the given class ID.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * For Kotlin built-in types, consider using the overload that accepts a [KaClassLikeSymbol] instead:
     * `classType(builtinTypes.string)`.
     *
     *  #### Example
     *
     * ```kotlin
     * classType(ClassId.fromString("kotlin/collections/List")) {
     *     argument(classType(ClassId.fromString("kotlin/String")))
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
     * #### Example
     *
     * ```kotlin
     * classType(builtinTypes.string)
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
     * Builds a boxed / primitive (depending on the [init] block) array type from the given [elementType].
     */
    @KaExperimentalApi
    public fun arrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds the underlying array type of [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
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
    public fun dynamicType(init: KaDynamicTypeBuilder.() -> Unit = {}): KaDynamicType

    /**
     * Builds a [KaFunctionType] based on the given [init] block.
     */
    @KaExperimentalApi
    public fun functionType(init: KaFunctionTypeBuilder.() -> Unit = {}): KaFunctionType

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
     * Builds a [KaFunctionValueParameter] with the given [name] and [type].
     */
    @KaExperimentalApi
    public fun functionValueParameter(name: Name?, type: KaType): KaFunctionValueParameter

    /**
     * Builds a [KaFunctionValueParameter] with the given [name] and [type].
     */
    @KaExperimentalApi
    public fun functionValueParameter(name: Name?, type: KaTypeCreator.() -> KaType): KaFunctionValueParameter

    /**
     * Builds a [KaStarTypeProjection] (`*`).
     */
    @KaExperimentalApi
    public fun starTypeProjection(): KaStarTypeProjection
}

/**
 * A base interface for all concrete type builders.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeBuilder : KaTypeCreator {
    /**
     * A list of annotation [ClassId]s, which are used to construct annotations of the resulting type.
     *
     * @see KaType.annotations
     */
    public val annotations: List<ClassId>

    /**
     * Adds [annotationClassId] to [annotations].
     *
     * @see KaType.annotations
     */
    public fun annotation(annotationClassId: ClassId)

    /**
     * Adds the annotation produced by [annotationClassId] to [annotations].
     *
     * @see KaType.annotations
     */
    public fun annotation(annotationClassId: () -> ClassId)

    /**
     * Adds [annotationClassIds] to [annotations].
     *
     * @see KaType.annotations
     */
    public fun annotations(annotationClassIds: Iterable<ClassId>)

    /**
     * Adds [annotationClassIds] to [annotations].
     *
     * @see KaType.annotations
     */
    public fun annotations(annotationClassIds: () -> Iterable<ClassId>)
}

/**
 * A builder for class types.
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
     * A type argument can be added using [typeArgument] or [invariantTypeArgument].
     */
    public val typeArguments: List<KaTypeProjection>

    /**
     * Adds an invariant [type] argument to the class type with [Variance.INVARIANT] variance.
     */
    public fun invariantTypeArgument(type: KaType)

    /**
     * Adds a [type] argument to the class type with the given [variance].
     */
    public fun typeArgument(variance: Variance, type: KaType)

    /**
     * Adds an invariant type argument produced by [type] to the class type with [Variance.INVARIANT] variance.
     */
    public fun invariantTypeArgument(type: () -> KaType)

    /**
     * Adds a type argument produced by [type] to the class type with the given [variance].
     */
    public fun typeArgument(variance: Variance, type: () -> KaType)

    /**
     * Adds type [argument] to the class type.
     */
    public fun typeArgument(argument: KaTypeProjection)

    /**
     * Adds a type argument produced by [typeProjection] to the class type.
     */
    public fun typeArgument(typeProjection: () -> KaTypeProjection)

    /**
     * Adds type [arguments] to the class type.
     */
    public fun typeArguments(arguments: () -> Iterable<KaTypeProjection>)
}

/**
 * A builder for type parameter types.
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
     * Whether the resulting array type is marked as nullable, i.e., the type is represented as `T?`.
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

/**
 * A builder for dynamic types.
 *
 * @see KaTypeCreator.dynamicType
 */
@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
public interface KaDynamicTypeBuilder : KaTypeBuilder

/**
 * A builder for function types.
 *
 * @see KaTypeCreator.functionType
 */
@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
public interface KaFunctionTypeBuilder : KaTypeBuilder {
    /**
     * Whether the type is marked as nullable, i.e., the type is represented as `T?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean

    /**
     * Whether the function type is a [suspend type]
     * (https://kotlinlang.org/spec/asynchronous-programming-with-coroutines.html#suspending-functions).
     *
     * Default value: `false`.
     *
     * @see KaFunctionType.isSuspend
     */
    public var isSuspend: Boolean

    /**
     * Whether the function type is a [reflection type]
     * (https://kotlinlang.org/docs/reflection.html#function-references)
     *
     * Default value: `false`.
     *
     * Note that Kotlin prohibits context parameters in reflection types.
     * So all context parameters passed to the builder are discarded when [isReflectType] is `true`.
     *
     * @see KaFunctionType.isReflectType
     */
    public var isReflectType: Boolean

    /**
     * List of context receivers for the function type.
     *
     * Note that Kotlin prohibits context parameters in reflection types.
     * So all context parameters passed to the builder are discarded when [isReflectType] is `true`.
     *
     * @see KaFunctionType.contextReceivers
     */
    public val contextParameters: List<KaType>

    /**
     * Adds the given [contextParameter] to the [contextParameters] list.
     *
     * Note that Kotlin prohibits context parameters in reflection types.
     * So all context parameters passed to the builder are discarded when [isReflectType] is `true`.
     */
    public fun contextParameter(contextParameter: KaType)

    /**
     * Adds the type produced by [contextParameter] to the [contextParameters] list.
     *
     * Note that Kotlin prohibits context parameters in reflection types.
     * So all context parameters passed to the builder are discarded when [isReflectType] is `true`.
     */
    public fun contextParameter(contextParameter: () -> KaType)

    /**
     * Function receiver type.
     *
     * @see KaFunctionType.receiverType
     */
    public var receiverType: KaType?

    /**
     * Function value parameters.
     *
     * @see KaFunctionType.parameters
     */
    public val valueParameters: List<KaFunctionValueParameter>

    /**
     * Adds the given [parameter] to the [valueParameters] list.
     */
    public fun valueParameter(parameter: KaFunctionValueParameter)

    /**
     * Adds the value parameter produced by [parameter] to the [valueParameters] list.
     */
    public fun valueParameter(parameter: () -> KaFunctionValueParameter)

    /**
     * Adds a value parameter with the given [name] and [type] to the [valueParameters] list.
     */
    public fun valueParameter(name: Name?, type: KaType)

    /**
     * Adds a value parameter with the given [name] and type produced by [type] to the [valueParameters] list.
     */
    public fun valueParameter(name: Name?, type: () -> KaType)

    /**
     * Function return type.
     *
     * Default value: [Unit][org.jetbrains.kotlin.analysis.api.components.KaBuiltinTypes.unit].
     *
     * @see KaFunctionType.returnType
     */
    public var returnType: KaType
}