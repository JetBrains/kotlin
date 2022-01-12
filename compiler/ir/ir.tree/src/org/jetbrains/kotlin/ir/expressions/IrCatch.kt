/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

abstract class IrCatch : IrElementBase() {
    abstract var catchParameter: IrVariable
    abstract var result: IrExpression

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrCatch =
        super.transform(transformer, data) as IrCatch
}
