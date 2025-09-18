/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeCreator : KaSessionComponent {
    /**
     * Builds a class type with the given class ID.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * For Kotlin built-in types, consider using the overload that accepts a [KaClassLikeSymbol] instead:
     * `buildClassType(builtinTypes.string)`.
     *
     *  #### Example
     *
     * ```kotlin
     * buildClassType(ClassId.fromString("kotlin/collections/List")) {
     *     argument(buildClassType(ClassId.fromString("kotlin/String")))
     * }
     * ```
     */
    public fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a class type with the given class symbol.
     *
     * A generic class type can be built by providing type arguments using the [init] block.
     * The caller is supposed to provide the correct number of type arguments for the class.
     *
     * #### Example
     *
     * ```kotlin
     * buildClassType(builtinTypes.string)
     * ```
     */
    public fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a boxed / primitive (depending on the [init] block) array type from the given [elementType].
     */
    @KaExperimentalApi
    public fun buildArrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds the underlying array type of [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
     * function parameter with the given [elementType].
     */
    @KaExperimentalApi
    public fun buildVarargArrayType(elementType: KaType): KaType

    /**
     * Builds a [KaTypeParameterType] with the given type parameter symbol.
     */
    public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType

    /**
     * Builds a [KaStarTypeProjection] (`*`).
     */
    @KaExperimentalApi
    public fun buildStarTypeProjection(): KaStarTypeProjection
}

@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeBuilder : KaLifetimeOwner

/**
 * A builder for class types.
 *
 * @see KaTypeCreator.buildClassType
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaClassTypeBuilder : KaTypeBuilder {
    /**
     * Default value: [KaTypeNullability.NON_NULLABLE].
     */
    @Deprecated("Use `isMarkedNullable` instead.", ReplaceWith("isMarkedNullable"))
    @Suppress("Deprecation")
    public var nullability: KaTypeNullability

    /**
     * Whether the type is marked as nullable, i.e. the type is represented as `T?`.
     *
     * Default value: `false`.
     *
     * @see KaTypeInformationProvider.isMarkedNullable
     */
    public var isMarkedNullable: Boolean

    public val arguments: List<KaTypeProjection>

    /**
     * Adds a type projection as an [argument] to the class type.
     */
    public fun argument(argument: KaTypeProjection)

    /**
     * Adds a [type] argument to the class type, with the given [variance].
     */
    public fun argument(type: KaType, variance: Variance = Variance.INVARIANT)
}

/**
 * A builder for type parameter types.
 *
 * @see KaTypeCreator.buildTypeParameterType
 */
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaTypeParameterTypeBuilder : KaTypeBuilder {
    /**
     * Default value: [KaTypeNullability.NON_NULLABLE].
     */
    @Deprecated("Use `isMarkedNullable` instead.", ReplaceWith("isMarkedNullable"))
    @Suppress("Deprecation")
    public var nullability: KaTypeNullability

    /**
     * Whether the type is marked as nullable, i.e. the type is represented as `T?`.
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
 * @see KaTypeCreator.buildTypeParameterType
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
 * Builds a class type with the given class ID.
 *
 * A generic class type can be built by providing type arguments using the [init] block.
 * The caller is supposed to provide the correct number of type arguments for the class.
 *
 * For Kotlin built-in types, consider using the overload that accepts a [KaClassLikeSymbol] instead:
 * `buildClassType(builtinTypes.string)`.
 *
 *  #### Example
 *
 * ```kotlin
 * buildClassType(ClassId.fromString("kotlin/collections/List")) {
 *     argument(buildClassType(ClassId.fromString("kotlin/String")))
 * }
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType {
    return with(s) {
        buildClassType(
            classId = classId,
            init = init,
        )
    }
}

/**
 * Builds a class type with the given class symbol.
 *
 * A generic class type can be built by providing type arguments using the [init] block.
 * The caller is supposed to provide the correct number of type arguments for the class.
 *
 * #### Example
 *
 * ```kotlin
 * buildClassType(builtinTypes.string)
 * ```
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType {
    return with(s) {
        buildClassType(
            symbol = symbol,
            init = init,
        )
    }
}

/**
 * Builds a boxed / primitive (depending on the [init] block) array type from the given [elementType].
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun buildArrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType {
    return with(s) {
        buildArrayType(
            elementType = elementType,
            init = init,
        )
    }
}

/**
 * Builds the underlying array type of [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
 * function parameter with the given [elementType].
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun buildVarargArrayType(elementType: KaType): KaType {
    return with(s) {
        buildVarargArrayType(
            elementType = elementType,
        )
    }
}

/**
 * Builds a [KaTypeParameterType] with the given type parameter symbol.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaContextParameterApi
context(s: KaSession)
public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType {
    return with(s) {
        buildTypeParameterType(
            symbol = symbol,
            init = init,
        )
    }
}

/**
 * Builds a [KaStarTypeProjection] (`*`).
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun buildStarTypeProjection(): KaStarTypeProjection {
    return with(s) {
        buildStarTypeProjection()
    }
}
