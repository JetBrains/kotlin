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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.load
import org.jetbrains.kotlin.psi2ir.generators.values.*
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.types.typeUtil.makeNullable


class IrOperatorExpressionGenerator(val irStatementGenerator: IrStatementGenerator): IrGenerator {
    override val context: IrGeneratorContext get() = irStatementGenerator.context

    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> createDummyExpression(expression, ktOperator.toString())
            in PREFIX_INCREMENT_DECREMENT_OPERATORS -> generatePrefixIncrementDecrementOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrBinaryOperator(ktOperator)

        return when (irOperator) {
            null -> createDummyExpression(expression, ktOperator.toString())
            IrOperator.EQ -> generateAssignment(expression)
            IrOperator.ELVIS -> generateElvis(expression)
            in AUGMENTED_ASSIGNMENTS -> generateAugmentedAssignment(expression, irOperator)
            in BINARY_OPERATORS_DESUGARED_TO_CALLS -> generateBinaryOperatorWithConventionalCall(expression, irOperator)
            in COMPARISON_OPERATORS -> generateComparisonOperator(expression, irOperator)
            in EQUALITY_OPERATORS -> generateEqualityOperator(expression, irOperator)
            in IDENTITY_OPERATORS -> generateIdentityOperator(expression, irOperator)
            in IN_OPERATORS -> generateInOperator(expression, irOperator)
            in BINARY_BOOLEAN_OPERATORS -> generateBinaryBooleanOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    private fun generateElvis(expression: KtBinaryExpression): IrExpression {
        // TODO desugar '?:' to 'if'?
        val specialCallForElvis = getResolvedCall(expression)!!
        val returnType = specialCallForElvis.resultingDescriptor.returnType!!
        val irArgument0 = irStatementGenerator.generateExpression(expression.left!!).toExpectedType(returnType.makeNullable())
        val irArgument1 = irStatementGenerator.generateExpression(expression.right!!).toExpectedType(returnType)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, returnType,
                IrOperator.ELVIS, null, irArgument0, irArgument1
        )
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrBinaryOperator): IrExpression {
        val irArgument0 = irStatementGenerator.generateExpression(expression.left!!).toExpectedType(context.builtIns.booleanType)
        val irArgument1 = irStatementGenerator.generateExpression(expression.right!!).toExpectedType(context.builtIns.booleanType)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, null, irArgument0, irArgument1
        )
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val operatorCall = getResolvedCall(expression)!!

        val irOperatorCall = IrCallGenerator(irStatementGenerator).generateCall(expression, operatorCall, irOperator)

        return if (irOperator == IrOperator.IN)
            irOperatorCall
        else
            IrUnaryOperatorExpressionImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                                          IrOperator.EXCL, null, irOperatorCall)
    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrBinaryOperator): IrExpression {
        val irArgument0 = irStatementGenerator.generateExpression(expression.left!!)
        val irArgument1 = irStatementGenerator.generateExpression(expression.right!!)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, null, irArgument0, irArgument1
        )
    }

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrBinaryOperator): IrExpression {
        val relatedCall = getResolvedCall(expression)!!
        val relatedDescriptor = relatedCall.resultingDescriptor

        val irCallGenerator = IrCallGenerator(irStatementGenerator)

        // NB special typing rules for equality operators: both arguments are nullable

        val irArgument0 =
                irCallGenerator.generateReceiver(expression.left!!, relatedCall.dispatchReceiver)!!
                        .toExpectedType(relatedDescriptor.dispatchReceiverParameter!!.type.makeNullable())

        val valueParameter0 = relatedDescriptor.valueParameters[0]
        val irArgument1 =
                irCallGenerator.generateValueArgument(
                        relatedCall.valueArgumentsByIndex!![0],
                        valueParameter0, valueParameter0.type.makeNullable()
                )!!

        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, relatedDescriptor, irArgument0, irArgument1
        )
    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, irOperator: IrBinaryOperator): IrExpression {
        val relatedCall = getResolvedCall(expression)!!
        val relatedDescriptor = relatedCall.resultingDescriptor

        val irCallGenerator = IrCallGenerator(irStatementGenerator)
        val irArgument0 = irCallGenerator.generateReceiver(expression.left!!, relatedCall.dispatchReceiver, relatedDescriptor.dispatchReceiverParameter!!)!!
        val valueParameter0 = relatedDescriptor.valueParameters[0]
        val irArgument1 = irCallGenerator.generateValueArgument(relatedCall.valueArgumentsByIndex!![0], valueParameter0)!!
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, relatedDescriptor, irArgument0, irArgument1
        )
    }

    private fun generateBinaryOperatorWithConventionalCall(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val operatorCall = getResolvedCall(expression)!!
        return IrCallGenerator(irStatementGenerator).generateCall(expression, operatorCall, irOperator)
    }

    private fun generatePrefixIncrementDecrementOperator(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val ktBaseExpression = expression.baseExpression!!
        val irLValue = generateLValue(ktBaseExpression, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        if (irLValue is IrLValueWithAugmentedStore) {
            return irLValue.prefixAugmentedStore(operatorCall, irOperator)
        }

        val opCallGenerator = IrCallGenerator(irStatementGenerator).apply { putValue(ktBaseExpression, irLValue) }
        val irBlock = IrBlockExpressionImpl(expression.startOffset, expression.endOffset, irLValue.type, true, irOperator)
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)
        val irTmp = irStatementGenerator.temporaryVariableFactory.createTemporaryVariable(irOpCall)
        irBlock.addStatement(irTmp)
        irBlock.addStatement(irLValue.store(irTmp.load()))
        irBlock.addStatement(irTmp.load())
        return irBlock
    }

    private fun generateAugmentedAssignment(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val ktLeft = expression.left!!
        val irLValue = generateLValue(ktLeft, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false

        if (isSimpleAssignment && irLValue is IrLValueWithAugmentedStore) {
            return irLValue.augmentedStore(operatorCall, irOperator, irStatementGenerator.generateExpression(expression.right!!))
        }

        val opCallGenerator = IrCallGenerator(irStatementGenerator).apply { putValue(ktLeft, irLValue) }
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)

        return if (isSimpleAssignment) {
            // Set( Op( Get(), RHS ) )
            irLValue.store(irOpCall)
        }
        else {
            // Op( Get(), RHS )
            irOpCall
        }
    }

    private fun generateAssignment(expression: KtBinaryExpression): IrExpression {
        val ktLeft = expression.left!!
        val ktRight = expression.right!!
        val irLValue = generateLValue(ktLeft, IrOperator.EQ)
        return irLValue.store(irStatementGenerator.generateExpression(ktRight))
    }

    private fun generateLValue(ktLeft: KtExpression, irOperator: IrOperator?): IrLValue {
        if (ktLeft is KtArrayAccessExpression) {
            val irArrayValue = irStatementGenerator.generateExpression(ktLeft.arrayExpression!!)
            val indexExpressions = ktLeft.indexExpressions.map { it to irStatementGenerator.generateExpression(it) }
            val indexedGetCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
            val indexedSetCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
            val type = indexedGetCall?.run { resultingDescriptor.returnType }
                       ?: indexedSetCall?.run { resultingDescriptor.valueParameters.last().type }
                       ?: throw AssertionError("Either 'get' or 'set' call should be present for an indexed LValue: ${ktLeft.text}")
            return IrIndexedLValue(irStatementGenerator, ktLeft, irOperator,
                                   irArrayValue, type, indexExpressions, indexedGetCall, indexedSetCall)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    IrVariableValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, irOperator)
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
