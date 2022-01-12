/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

abstract class IrSpreadElement : IrElementBase(), IrVarargElement {
    abstract var expression: IrExpression

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrElement =
        accept(transformer, data) as IrSpreadElement
}
