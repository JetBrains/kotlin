/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.types

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.types.Variance

interface IrType {
    val annotations: List<IrCall>

    /**
     * @return true if this type is equal to [other] symbolically. Note that this is NOT EQUIVALENT to the full type checking algorithm
     * used in the compiler frontend. For example, this method will return `false` on the types `List<*>` and `List<Any?>`,
     * whereas the real type checker from the compiler frontend would return `true`.
     *
     * Classes are compared by FQ names, which means that even if two types refer to different symbols of the class with the same FQ name,
     * such types will be considered equal. Type annotations do not have any effect on the behavior of this method.
     */
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

interface IrErrorType : IrType

interface IrDynamicType : IrType

interface IrSimpleType : IrType {
    val classifier: IrClassifierSymbol
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
}

interface IrTypeArgument {
    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int
}

interface IrStarProjection : IrTypeArgument

interface IrTypeProjection : IrTypeArgument {
    val variance: Variance
    val type: IrType
}
