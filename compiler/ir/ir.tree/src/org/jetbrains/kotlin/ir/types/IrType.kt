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
}

interface IrErrorType : IrType

interface IrDynamicType : IrType

interface IrSimpleType : IrType {
    val classifier: IrClassifierSymbol
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
}

interface IrTypeArgument

interface IrStarProjection : IrTypeArgument

interface IrTypeProjection : IrTypeArgument {
    val variance: Variance
    val type: IrType
}