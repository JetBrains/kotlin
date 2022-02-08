/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrErrorCallExpression : IrErrorExpression() {
    abstract var explicitReceiver: IrExpression?
    abstract val arguments: MutableList<IrExpression>

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitErrorCallExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        explicitReceiver?.accept(visitor, data)
        arguments.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        explicitReceiver = explicitReceiver?.transform(transformer, data)
        arguments.forEachIndexed { i, irExpression ->
            arguments[i] = irExpression.transform(transformer, data)
        }
    }
}
