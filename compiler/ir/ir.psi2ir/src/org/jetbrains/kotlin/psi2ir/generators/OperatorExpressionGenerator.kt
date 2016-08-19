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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.values.*
import org.jetbrains.kotlin.psi2ir.load
import org.jetbrains.kotlin.psi2ir.toExpectedType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.types.typeUtil.makeNullable


class OperatorExpressionGenerator(val statementGenerator: StatementGenerator): IrGenerator {
    override val context: GeneratorContext get() = statementGenerator.context

    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> createDummyExpression(expression, ktOperator.toString())
            in PREFIX_INCREMENT_DECREMENT_OPERATORS -> generatePrefixIncrementDecrementOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generatePostfixExpression(expression: KtPostfixExpression): IrExpression {
        TODO("not implemented")
    }

    fun generateCastExpression(expression: KtBinaryExpressionWithTypeRHS): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)
        val rhsType = getOrFail(BindingContext.TYPE, expression.right!!)

        val resultType = when (irOperator) {
            IrTypeOperator.CAST ->
                rhsType
            IrTypeOperator.SAFE_CAST ->
                rhsType.makeNullable()
            else ->
                throw AssertionError("Unexpected IrTypeOperator: $irOperator")
        }

        return IrTypeOperatorExpressionImpl(expression.startOffset, expression.endOffset, resultType, irOperator, rhsType,
                                            statementGenerator.generateExpression(expression.left))
    }

    fun generateInstanceOfExpression(expression: KtIsExpression): IrStatement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)!!
        val againstType = getOrFail(BindingContext.TYPE, expression.typeReference)

        return IrTypeOperatorExpressionImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType, irOperator,
                                            againstType, statementGenerator.generateExpression(expression.leftHandSide))
    }

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            return generateBinaryOperatorWithConventionalCall(expression, null)
        }

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
        val irArgument0 = statementGenerator.generateExpression(expression.left!!).toExpectedType(returnType.makeNullable())
        val irArgument1 = statementGenerator.generateExpression(expression.right!!).toExpectedType(returnType)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, returnType,
                IrOperator.ELVIS, null, irArgument0, irArgument1
        )
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!).toExpectedType(context.builtIns.booleanType)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!).toExpectedType(context.builtIns.booleanType)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, getResolvedCall(expression)?.resultingDescriptor, irArgument0, irArgument1
        )
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val operatorCall = getResolvedCall(expression)!!

        val irOperatorCall = CallGenerator(statementGenerator).generateCall(expression, operatorCall, irOperator)

        return if (irOperator == IrOperator.IN)
            irOperatorCall
        else
            IrUnaryOperatorExpressionImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                                          IrOperator.EXCL, null, irOperatorCall)
    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)
        return IrBinaryOperatorExpressionImpl(
                expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                irOperator, null, irArgument0, irArgument1
        )
    }

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val relatedCall = getResolvedCall(expression)!!
        val relatedDescriptor = relatedCall.resultingDescriptor

        val irCallGenerator = CallGenerator(statementGenerator)

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

    private fun generateComparisonOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val compareToCall = getResolvedCall(expression)!!
        val compareToDescriptor = compareToCall.resultingDescriptor

        val irCallGenerator = CallGenerator(statementGenerator)

        return if (shouldKeepComparisonAsBinaryOperation(compareToDescriptor)) {
            val irArgument0 = irCallGenerator.generateReceiver(expression.left!!, compareToCall.dispatchReceiver, compareToDescriptor.dispatchReceiverParameter!!)!!
            val valueParameter0 = compareToDescriptor.valueParameters[0]
            val irArgument1 = irCallGenerator.generateValueArgument(compareToCall.valueArgumentsByIndex!![0], valueParameter0)!!
            IrBinaryOperatorExpressionImpl(
                    expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                    irOperator, compareToDescriptor, irArgument0, irArgument1
            )
        }
        else {
            val irCompareToCall = irCallGenerator.generateCall(expression, compareToCall, irOperator)
            IrBinaryOperatorExpressionImpl(
                    expression.startOffset, expression.endOffset, context.builtIns.booleanType,
                    irOperator, null, irCompareToCall,
                    IrLiteralExpressionImpl.int(expression.startOffset, expression.endOffset, context.builtIns.intType, 0)
            )
        }
    }

    private fun shouldKeepComparisonAsBinaryOperation(compareToDescriptor: CallableDescriptor) =
            compareToDescriptor.dispatchReceiverParameter?.type.let {
                it != null && (KotlinBuiltIns.isPrimitiveType(it) || KotlinBuiltIns.isString(it))
            }

    private fun generateBinaryOperatorWithConventionalCall(expression: KtBinaryExpression, irOperator: IrOperator?): IrExpression {
        val operatorCall = getResolvedCall(expression)!!
        return CallGenerator(statementGenerator).generateCall(expression, operatorCall, irOperator)
    }

    private fun generatePrefixIncrementDecrementOperator(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val ktBaseExpression = expression.baseExpression!!
        val irLValue = generateLValue(ktBaseExpression, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        if (irLValue is IrLValueWithAugmentedStore) {
            return irLValue.prefixAugmentedStore(operatorCall, irOperator)
        }

        val opCallGenerator = CallGenerator(statementGenerator).apply { putValue(ktBaseExpression, irLValue) }
        val irBlock = IrBlockExpressionImpl(expression.startOffset, expression.endOffset, irLValue.type, true, irOperator)
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)
        val irTmp = statementGenerator.temporaryVariableFactory.createTemporaryVariable(irOpCall)
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
            return irLValue.augmentedStore(operatorCall, irOperator, statementGenerator.generateExpression(expression.right!!))
        }

        val opCallGenerator = CallGenerator(statementGenerator).apply { putValue(ktLeft, irLValue) }
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
        return irLValue.store(statementGenerator.generateExpression(ktRight))
    }

    private fun generateLValue(ktLeft: KtExpression, irOperator: IrOperator?): IrLValue {
        if (ktLeft is KtArrayAccessExpression) {
            val irArrayValue = statementGenerator.generateExpression(ktLeft.arrayExpression!!)
            val indexExpressions = ktLeft.indexExpressions.map { it to statementGenerator.generateExpression(it) }
            val indexedGetCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
            val indexedSetCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
            val type = indexedGetCall?.run { resultingDescriptor.returnType }
                       ?: indexedSetCall?.run { resultingDescriptor.valueParameters.last().type }
                       ?: throw AssertionError("Either 'get' or 'set' call should be present for an indexed LValue: ${ktLeft.text}")
            return IndexedLValue(statementGenerator, ktLeft, irOperator,
                                 irArrayValue, type, indexExpressions, indexedGetCall, indexedSetCall)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    VariableLValue(ktLeft.startOffset, ktLeft.endOffset, descriptor, irOperator)
            is PropertyDescriptor ->
                CallGenerator(statementGenerator).run {
                    PropertyLValue(
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
