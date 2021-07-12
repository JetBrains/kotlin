/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.ir.builders.IrGeneratorContext
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicMemberExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDynamicOperatorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.left
import org.jetbrains.kotlin.ir.expressions.right
import org.jetbrains.kotlin.ir.types.IrType

class DynamicMemberLValue(
    private val context: IrGeneratorContext,
    private val startOffset: Int,
    private val endOffset: Int,
    override val type: IrType,
    private val memberName: String,
    private val receiver: CallReceiver
) : LValue, AssignmentReceiver {

    override fun load(): IrExpression =
        receiver.call { dispatchReceiverValue, extensionReceiverValue, _ ->
            val dynamicReceiver = getDynamicReceiver(dispatchReceiverValue, extensionReceiverValue)

            IrDynamicMemberExpressionImpl(
                startOffset, endOffset,
                type,
                memberName,
                dynamicReceiver
            )
        }

    override fun store(irExpression: IrExpression): IrExpression =
        receiver.call { dispatchReceiverValue, extensionReceiverValue, _ ->
            val dynamicReceiver = getDynamicReceiver(dispatchReceiverValue, extensionReceiverValue)

            IrDynamicOperatorExpressionImpl(
                startOffset, endOffset,
                context.irBuiltIns.unitType,
                IrDynamicOperator.EQ
            ).apply {
                left = IrDynamicMemberExpressionImpl(
                    startOffset, endOffset,
                    type,
                    memberName,
                    dynamicReceiver
                )
                right = irExpression
            }
        }

    override fun assign(withLValue: (LValue) -> IrExpression): IrExpression =
        receiver.call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            withLValue(
                DynamicMemberLValue(
                    context, startOffset, endOffset, type, memberName,
                    SimpleCallReceiver(dispatchReceiverValue, extensionReceiverValue, contextReceiverValues)
                )
            )
        }

    private fun getDynamicReceiver(dispatchReceiverValue: IntermediateValue?, extensionReceiverValue: IntermediateValue?): IrExpression {
        if (dispatchReceiverValue == null) throw AssertionError("Dynamic call $memberName should have a dispatch receiver")
        if (extensionReceiverValue != null) throw AssertionError("Dynamic call $memberName should have no extension receiver")
        return dispatchReceiverValue.load()
    }
}