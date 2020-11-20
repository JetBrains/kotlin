/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspendableExpression
import org.jetbrains.kotlin.ir.expressions.IrSuspensionPoint
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrSuspensionPointImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var suspensionPointIdParameter: IrVariable,
    override var result: IrExpression,
    override var resumeResult: IrExpression
) : IrSuspensionPoint() {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSuspensionPoint(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointIdParameter.accept(visitor, data)
        result.accept(visitor, data)
        resumeResult.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointIdParameter = suspensionPointIdParameter.transform(transformer, data) as IrVariable
        result = result.transform(transformer, data)
        resumeResult = resumeResult.transform(transformer, data)
    }
}

class IrSuspendableExpressionImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var type: IrType,
    override var suspensionPointId: IrExpression,
    override var result: IrExpression
) : IrSuspendableExpression() {
    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitSuspendableExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        suspensionPointId.accept(visitor, data)
        result.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        suspensionPointId = suspensionPointId.transform(transformer, data)
        result = result.transform(transformer, data)
    }
}
