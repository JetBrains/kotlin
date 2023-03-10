/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.copyWithOffsets
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class InlineConstTransformer : IrElementTransformerVoid() {
    abstract val IrField.constantInitializer: IrConst<*>?
    private fun IrExpression.lowerConstRead(receiver: IrExpression?, field: IrField?): IrExpression? {
        val value = field?.constantInitializer ?: return null
        transformChildrenVoid()
        reportInlineConst(field, value)

        val resultExpression = value.copyWithOffsets(startOffset, endOffset)

        return if (receiver == null || receiver.shouldDropConstReceiver())
            resultExpression
        else
            IrCompositeImpl(
                startOffset, endOffset, resultExpression.type, null,
                listOf(receiver, resultExpression)
            )
    }

    abstract fun reportInlineConst(field: IrField, value: IrConst<*>)

    fun IrExpression.shouldDropConstReceiver(): Boolean {
        return this is IrConst<*> || this is IrGetValue || this is IrGetObjectValue
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val function = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val property = function.correspondingPropertySymbol?.owner ?: return super.visitCall(expression)
        // If `constantValue` is not null, `function` can only be the getter because the property is immutable.
        return expression.lowerConstRead(expression.dispatchReceiver, property.backingField) ?: super.visitCall(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression =
        expression.lowerConstRead(expression.receiver, expression.symbol.owner) ?: super.visitGetField(expression)
}