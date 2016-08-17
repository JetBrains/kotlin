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

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrBinaryOperatorExpressionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.values.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.types.expressions.OperatorConventions

val KT_OPERATOR_TO_IR_OPERATOR: Map<IElementType, IrOperator> =
        hashMapOf(
                KtTokens.PLUSEQ to IrOperator.PLUSEQ,
                KtTokens.MINUSEQ to IrOperator.MINUSEQ,
                KtTokens.MULTEQ to IrOperator.MULTEQ,
                KtTokens.DIVEQ to IrOperator.DIVEQ,
                KtTokens.PERCEQ to IrOperator.PERCEQ,

                KtTokens.PLUS to IrOperator.PLUS,
                KtTokens.MINUS to IrOperator.MINUS,
                KtTokens.MUL to IrOperator.MUL,
                KtTokens.DIV to IrOperator.DIV,
                KtTokens.PERC to IrOperator.PERC,
                KtTokens.RANGE to IrOperator.RANGE,

                KtTokens.LT to IrOperator.LT,
                KtTokens.LTEQ to IrOperator.LTEQ,
                KtTokens.GT to IrOperator.GT,
                KtTokens.GTEQ to IrOperator.GTEQ
        )

val AUGMENTED_ASSIGNMENTS = KtTokens.AUGMENTED_ASSIGNMENTS
val BINARY_OPERATORS_WITH_CALLS = OperatorConventions.BINARY_OPERATION_NAMES.keys
val COMPARISON_OPERATORS = OperatorConventions.COMPARISON_OPERATIONS

class IrOperatorExpressionGenerator(val irStatementGenerator: IrStatementGenerator): IrGenerator {
    override val context: IrGeneratorContext get() = irStatementGenerator.context

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()

        return when (ktOperator) {
            KtTokens.EQ -> generateAssignment(expression)
            in AUGMENTED_ASSIGNMENTS -> generateAugmentedAssignment(expression, ktOperator)
            in BINARY_OPERATORS_WITH_CALLS -> generateBinaryOperatorWithConventionalCall(expression, ktOperator)
            in COMPARISON_OPERATORS -> generateComparisonOperator(expression, ktOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, ktOperator: IElementType): IrExpression {
        val irOperator = getIrOperator(ktOperator)

        val compareToCall = getResolvedCall(expression)!!
        val compareToDescriptor = compareToCall.resultingDescriptor

        val irCallGenerator = IrCallGenerator(irStatementGenerator)
        val irArgument0 = irCallGenerator.generateReceiver(expression.left!!, compareToCall.dispatchReceiver, compareToDescriptor.dispatchReceiverParameter)!!
        val irArgument1 = irCallGenerator.generateValueArgument(compareToCall.valueArgumentsByIndex!![0], compareToDescriptor.valueParameters[0])!!
        return IrBinaryOperatorExpressionImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                                              irOperator, compareToDescriptor, irArgument0, irArgument1)
    }

    private fun generateBinaryOperatorWithConventionalCall(expression: KtBinaryExpression, ktOperator: IElementType): IrExpression {
        val irOperator = getIrOperator(ktOperator)
        val operatorCall = getResolvedCall(expression)!!
        return IrCallGenerator(irStatementGenerator).generateCall(expression, operatorCall, irOperator)
    }

    private fun generateAugmentedAssignment(expression: KtBinaryExpression, ktOperator: IElementType): IrExpression {
        val ktLeft = expression.left!!

        val irOperator = getIrOperator(ktOperator)
        val irLhs = generateLValue(ktLeft, irOperator)

        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false

        val operatorCall = getResolvedCall(expression)!!

        if (isSimpleAssignment && irLhs is IrLValueWithAugmentedStore) {
            return irLhs.augmentedStore(operatorCall, irStatementGenerator.generateExpression(expression.right!!))
        }

        val opCallGenerator = IrCallGenerator(irStatementGenerator).apply { putValue(ktLeft, irLhs) }
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)

        return if (isSimpleAssignment) {
            // Set( Op( Get(), RHS ) )
            irLhs.store(irOpCall)
        }
        else {
            // Op( Get(), RHS )
            irOpCall
        }
    }

    private fun getIrOperator(ktOperator: IElementType): IrOperator =
            KT_OPERATOR_TO_IR_OPERATOR[ktOperator] ?: TODO("Operator: $ktOperator")

    private fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val lhsReference = generateLValue(ktLeft, IrOperator.EQ)
        return lhsReference.store(irStatementGenerator.generateExpression(ktRight))
    }

    private fun generateLValue(ktLeft: KtExpression, irOperator: IrOperator?): IrLValue {
        if (ktLeft is KtArrayAccessExpression) {
            val irArrayValue = irStatementGenerator.generateExpression(ktLeft.arrayExpression!!)
            val indexExpressions = ktLeft.indexExpressions.map { it to irStatementGenerator.generateExpression(it) }
            val indexedGetCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
            val indexedSetCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
            return IrIndexedLValue(irStatementGenerator, ktLeft, irOperator,
                                   irArrayValue, indexExpressions, indexedGetCall, indexedSetCall)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    IrVariableLValueValue(ktLeft, irOperator, descriptor)
            is PropertyDescriptor ->
                IrCallGenerator(irStatementGenerator).run {
                    IrPropertyLValueValue(
                            ktLeft, irOperator, descriptor,
                            generateReceiver(ktLeft, resolvedCall.dispatchReceiver, descriptor.dispatchReceiverParameter),
                            generateReceiver(ktLeft, resolvedCall.extensionReceiver, descriptor.extensionReceiverParameter),
                            resolvedCall.call.isSafeCall()
                    )
                }
            else ->
                TODO("Other cases of LHS")
        }
    }
}
