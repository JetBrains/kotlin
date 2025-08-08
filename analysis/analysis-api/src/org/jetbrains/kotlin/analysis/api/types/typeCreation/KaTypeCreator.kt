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