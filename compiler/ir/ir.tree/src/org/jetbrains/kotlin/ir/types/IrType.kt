/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.mpp.TypeRefMarker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.*

sealed class IrType : IrTypeProjection, KotlinTypeMarker, TypeRefMarker, IrAnnotationContainer {
    final override val type: IrType
        get() = this

    open val originalKotlinType: KotlinType?
        get() = null

    /**
     * @return true if this type is equal to [other] symbolically. Note that this is NOT EQUIVALENT to the full type checking algorithm
     * used in the compiler frontend. For example, this method will return `false` on the types `List<*>` and `List<Any?>`,
     * whereas the real type checker from the compiler frontend would return `true`.
     *
     * Classes are compared by FQ names, which means that even if two types refer to different symbols of the class with the same FQ name,
     * such types will be considered equal. Type annotations do not have any effect on the behavior of this method.
     */
    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int
}

abstract class IrErrorType(
    private val errorClassStubSymbol: IrClassSymbol,
    val isMarkedNullable: Boolean = false
) : IrType(), SimpleTypeMarker {
    val symbol: IrClassSymbol
        get() = errorClassStubSymbol
}

abstract class IrDynamicType : IrType(), DynamicTypeMarker

enum class SimpleTypeNullability {
    MARKED_NULLABLE,
    NOT_SPECIFIED,
    DEFINITELY_NOT_NULL;

    companion object {
        fun fromHasQuestionMark(hasQuestionMark: Boolean) = if (hasQuestionMark) MARKED_NULLABLE else NOT_SPECIFIED
    }
}

abstract class IrSimpleType : IrType(), SimpleTypeMarker, TypeArgumentListMarker {
    abstract val classifier: IrClassifierSymbol

    /**
     * If type is explicitly marked as nullable, [nullability] is [SimpleTypeNullability.MARKED_NULLABLE]
     *
     * If classifier is type parameter, not marked as nullable, but can store null values,
     * if corresponding argument would be nullable, [nullability] is [SimpleTypeNullability.NOT_SPECIFIED]
     *
     * If type can't store null values, [nullability] is [SimpleTypeNullability.DEFINITELY_NOT_NULL]
     *
     * Direct usages of this property should be avoided in most cases. Use relevant util functions instead.
     *
     * In most cases one of following is needed:
     *
     * Use [IrType.isNullable] to check if null value is possible for this type
     *
     * Use [IrType.isMarkedNullable] to check if type is marked with question mark in code
     *
     * Use [IrType.mergeNullability] to apply nullability of type parameter to actual type argument in type substitutions
     *
     * Use [IrType.makeNotNull] or [IrType.makeNullable] to transfer nullability from one type to another
     */
    abstract val nullability: SimpleTypeNullability
    abstract val arguments: List<IrTypeArgument>
    abstract val abbreviation: IrTypeAbbreviation?

    override val variance: Variance
        get() = Variance.INVARIANT
}

/**
 * An argument for a generic parameter. Can be either [IrTypeProjection], or [IrStarProjection].
 */
sealed interface IrTypeArgument : TypeArgumentMarker {
    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}

interface IrStarProjection : IrTypeArgument

interface IrTypeProjection : IrTypeArgument {
    val variance: Variance
    val type: IrType
}

interface IrTypeAbbreviation : IrAnnotationContainer {
    val typeAlias: IrTypeAliasSymbol
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
}
