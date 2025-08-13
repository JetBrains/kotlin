/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance

/**
 * [KaTypeCreator] is deprecated and soon will no longer be a session component.
 * It will be replaced by another component [KaTypeCreatorProvider], which provides a single entry point
 * for all the type-building infrastructure [org.jetbrains.kotlin.analysis.api.types.typeCreation.KaTypeCreator].
 *
 * It has the same set of type building APIs as [KaTypeCreator] and some newer APIs covering all [KaType]s.
 *
 * See KT-65912, KT-66566, KT-66043
 */
@SubclassOptInRequired(KaImplementationDetail::class)
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
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
    @Deprecated(
        "Use `typeCreator.classType` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.classType")
    )
    @Suppress("DEPRECATION")
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
    @Deprecated(
        "Use `typeCreator.classType` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.classType")
    )
    @Suppress("DEPRECATION")
    public fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds a boxed / primitive (depending on the [init] block) array type from the given [elementType].
     */
    @Deprecated(
        "Use `typeCreator.arrayType` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.arrayType")
    )
    @KaExperimentalApi
    @Suppress("DEPRECATION")
    public fun buildArrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType

    /**
     * Builds the underlying array type of [vararg](https://kotlinlang.org/docs/functions.html#variable-number-of-arguments-varargs)
     * function parameter with the given [elementType].
     */
    @Deprecated(
        "Use `typeCreator.varargArrayType` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.varargArrayType")
    )
    @KaExperimentalApi
    public fun buildVarargArrayType(elementType: KaType): KaType

    /**
     * Builds a [KaTypeParameterType] with the given type parameter symbol.
     */
    @Deprecated(
        "Use `typeCreator.typeParameterType` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.typeParameterType")
    )
    @Suppress("DEPRECATION")
    public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType

    /**
     * Builds a [KaStarTypeProjection] (`*`).
     */
    @Deprecated(
        "Use `typeCreator.starTypeProjection` instead. " +
                "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide.",
        ReplaceWith("typeCreator.starTypeProjection")
    )
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
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
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
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
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
 * @see KaTypeCreator.buildArrayType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
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
 * @see KaTypeCreator.buildClassType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildClassType(classId: ClassId, init: KaClassTypeBuilder.() -> Unit = {}): KaType {
    return with(context) { buildClassType(classId, init) }
}

/**
 * @see KaTypeCreator.buildClassType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildClassType(symbol: KaClassLikeSymbol, init: KaClassTypeBuilder.() -> Unit = {}): KaType {
    return with(context) { buildClassType(symbol, init) }
}

/**
 * @see KaTypeCreator.buildArrayType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaExperimentalApi
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildArrayType(elementType: KaType, init: KaArrayTypeBuilder.() -> Unit = {}): KaType {
    return with(context) { buildArrayType(elementType, init) }
}

/**
 * @see KaTypeCreator.buildVarargArrayType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaExperimentalApi
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildVarargArrayType(elementType: KaType): KaType {
    return with(context) { buildVarargArrayType(elementType) }
}

/**
 * @see KaTypeCreator.buildTypeParameterType
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildTypeParameterType(symbol: KaTypeParameterSymbol, init: KaTypeParameterTypeBuilder.() -> Unit = {}): KaTypeParameterType {
    return with(context) { buildTypeParameterType(symbol, init) }
}

/**
 * @see KaTypeCreator.buildStarTypeProjection
 */
@Deprecated(
    "Use `org.jetbrains.kotlin.analysis.api.components.KaTypeCreatorProvider.typeCreator` instead. " +
            "See the KDoc for `org.jetbrains.kotlin.analysis.api.components.KaTypeCreator` for the migration guide."
)
@KaExperimentalApi
@KaContextParameterApi
@Suppress("DEPRECATION")
context(context: KaTypeCreator)
public fun buildStarTypeProjection(): KaStarTypeProjection {
    return with(context) { buildStarTypeProjection() }
}
