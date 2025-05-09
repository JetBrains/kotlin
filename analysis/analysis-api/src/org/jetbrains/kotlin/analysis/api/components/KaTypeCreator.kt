/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

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
     * Builds a [KaTypeParameterType] with the given type parameter symbol.
     */
    public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType

    /**
     * Builds a [KaStarTypeProjection] (`*`).
     */
    @KaExperimentalApi
    public fun buildStarTypeProjection(): KaStarTypeProjection
}

public interface KaTypeBuilder : KaLifetimeOwner

/**
 * A builder for class types.
 *
 * @see KaTypeCreator.buildClassType
 */
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
     * See [KaTypeInformationProvider.isMarkedNullable].
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
public interface KaTypeParameterTypeBuilder : KaTypeBuilder {
    /**
     * Default value: [KaTypeNullability.NULLABLE].
     */
    @Deprecated("Use `isMarkedNullable` instead.", ReplaceWith("isMarkedNullable"))
    @Suppress("Deprecation")
    public var nullability: KaTypeNullability

    /**
     * Whether the type is marked as nullable, i.e. the type is represented as `T?`.
     *
     * Default value: `true`.
     *
     * See [KaTypeInformationProvider.isMarkedNullable].
     */
    public var isMarkedNullable: Boolean
}
