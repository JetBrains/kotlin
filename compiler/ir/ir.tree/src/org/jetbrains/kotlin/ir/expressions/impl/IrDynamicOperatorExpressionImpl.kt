/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperatorExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

class IrDynamicOperatorExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    override val operator: IrDynamicOperator,
    override val valueArgumentsCount: Int
) :
    IrExpressionBase(startOffset, endOffset, type),
    IrDynamicOperatorExpression {

    private val valueArguments = arrayOfNulls<IrExpression>(valueArgumentsCount)

    override fun getValueArgument(index: Int): IrExpression? {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        return valueArguments[index]
    }

    override fun putValueArgument(index: Int, valueArgument: IrExpression?) {
        if (index >= valueArgumentsCount) {
            throw AssertionError("$this: No such value argument slot: $index")
        }
        valueArguments[index] = valueArgument
    }

    override fun removeValueArgument(index: Int) {
        putValueArgument(index, null)
    }


    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitDynamicOperatorExpression(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        for (valueArgument in valueArguments) {
            valueArgument?.accept(visitor, data)
        }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        for (i in 0 until valueArgumentsCount) {
            valueArguments[i] = valueArguments[i]?.transform(transformer, data)
        }
    }
}