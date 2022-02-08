/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrCatch : IrElementBase() {
    abstract var catchParameter: IrVariable
    abstract var result: IrExpression

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitCatch(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        catchParameter.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrCatch =
        super.transform(transformer, data) as IrCatch

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        catchParameter = catchParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
    }
}
