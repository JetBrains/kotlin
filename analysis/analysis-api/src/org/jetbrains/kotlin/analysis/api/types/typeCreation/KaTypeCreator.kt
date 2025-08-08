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
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.name.ClassId
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
public interface KaTypeBuilder : KaTypeCreator

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