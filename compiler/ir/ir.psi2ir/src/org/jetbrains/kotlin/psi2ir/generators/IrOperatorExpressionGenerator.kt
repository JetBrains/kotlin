/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall

class IrOperatorExpressionGenerator(val irStatementGenerator: IrStatementGenerator): IrGenerator {
    override val context: IrGeneratorContext get() = irStatementGenerator.context

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()

        return when (ktOperator) {
            KtTokens.EQ -> generateAssignment(expression)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    private fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val lhs = generateAssignmentLHS(ktLeft)
        return lhs.generateAssignment(expression, IrOperator.EQ, irStatementGenerator.generateExpression(ktRight, lhs.type))
    }

    private fun generateAssignmentLHS(ktLeft: KtExpression): AssignmentLHS {
        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    VariableLHS(descriptor)
            is PropertyDescriptor ->
                IrCallGenerator(irStatementGenerator).run {
                    PropertyLHS(
                            descriptor,
                            generateReceiver(ktLeft, resolvedCall.dispatchReceiver,
                                             descriptor.dispatchReceiverParameter?.type),
                            generateReceiver(ktLeft, resolvedCall.extensionReceiver,
                                             descriptor.extensionReceiverParameter?.type),
                            resolvedCall.call.isSafeCall()
                    )
                }
            else ->
                TODO("Other cases of LHS")
        }
    }

}
