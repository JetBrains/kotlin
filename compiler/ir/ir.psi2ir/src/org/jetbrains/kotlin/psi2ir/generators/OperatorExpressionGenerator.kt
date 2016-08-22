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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.defaultLoad
import org.jetbrains.kotlin.psi2ir.generators.operators.*
import org.jetbrains.kotlin.psi2ir.generators.values.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.types.typeUtil.makeNullable



class OperatorExpressionGenerator(parentGenerator: StatementGenerator) : IrChildBodyGeneratorBase<StatementGenerator>(parentGenerator) {
    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected prefix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> generatePrefixIncrementDecrementOperator(expression, irOperator)
            in OPERATORS_DESUGARED_TO_CALLS -> generatePrefixOperatorAsCall(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generatePostfixExpression(expression: KtPostfixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPostfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected postfix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> generatePostfixIncrementDecrementOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
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

        return IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset, resultType, irOperator, rhsType,
                                      parentGenerator.generateExpression(expression.left))
    }

    fun generateInstanceOfExpression(expression: KtIsExpression): IrStatement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)!!
        val againstType = getOrFail(BindingContext.TYPE, expression.typeReference)

        return IrTypeOperatorCallImpl(expression.startOffset, expression.endOffset, context.builtIns.booleanType, irOperator,
                                      againstType, parentGenerator.generateExpression(expression.leftHandSide))
    }

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            return generateBinaryOperatorAsCall(expression, null)
        }

        val irOperator = getInfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected infix operator: $ktOperator")
            IrOperator.EQ -> generateAssignment(expression)
            IrOperator.ELVIS -> generateElvis(expression)
            in AUGMENTED_ASSIGNMENTS -> generateAugmentedAssignment(expression, irOperator)
            in OPERATORS_DESUGARED_TO_CALLS -> generateBinaryOperatorAsCall(expression, irOperator)
            in COMPARISON_OPERATORS -> generateComparisonOperator(expression, irOperator)
            in EQUALITY_OPERATORS -> generateEqualityOperator(expression, irOperator)
            in IDENTITY_OPERATORS -> generateIdentityOperator(expression, irOperator)
            in IN_OPERATORS -> generateInOperator(expression, irOperator)
            in BINARY_BOOLEAN_OPERATORS -> generateBinaryBooleanOperator(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    private fun generateElvis(expression: KtBinaryExpression): IrExpression {
        val specialCallForElvis = getResolvedCall(expression)!!
        val returnType = specialCallForElvis.resultingDescriptor.returnType!!
        val irArgument0 = parentGenerator.generateExpressionWithExpectedType(expression.left!!, returnType.makeNullable())
        val irArgument1 = parentGenerator.generateExpressionWithExpectedType(expression.right!!, returnType)
        return block(expression, IrOperator.ELVIS) {
            add(scope.introduceTemporary(irArgument0))
            result(ifThenElse(returnType,
                              equalsNull(scope.valueOf(irArgument0)!!),
                              irArgument1,
                              scope.valueOf(irArgument0)!!))
        }
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = parentGenerator.generateExpressionWithExpectedType(expression.left!!, context.builtIns.booleanType)
        val irArgument1 = parentGenerator.generateExpressionWithExpectedType(expression.right!!, context.builtIns.booleanType)
        return when (irOperator) {
            IrOperator.OROR ->
                IrIfThenElseImpl.oror(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            IrOperator.ANDAND ->
                IrIfThenElseImpl.andand(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            else ->
                throw AssertionError("Unexpected binary boolean operator $irOperator")
        }
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val containsCall = getResolvedCall(expression)!!

        val irContainsCall = CallGenerator(parentGenerator).generateCall(expression, containsCall, irOperator)

        return when (irOperator) {
            IrOperator.IN ->
                irContainsCall
            IrOperator.NOT_IN ->
                IrUnaryOperatorImpl(expression.startOffset, expression.endOffset, IrOperator.EXCL, context.irBuiltIns.booleanNot,
                                    irContainsCall)
            else ->
                throw AssertionError("Unexpected in-operator $irOperator")
        }

    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = parentGenerator.generateExpression(expression.left!!)
        val irArgument1 = parentGenerator.generateExpression(expression.right!!)


        val irIdentityEquals = IrBinaryOperatorImpl(expression.startOffset, expression.endOffset, irOperator, context.irBuiltIns.eqeqeq,
                                                    irArgument0, irArgument1)

        return when (irOperator) {
            IrOperator.EQEQEQ ->
                irIdentityEquals
            IrOperator.EXCLEQEQ ->
                IrUnaryOperatorImpl(expression.startOffset, expression.endOffset, IrOperator.EXCL, context.irBuiltIns.booleanNot,
                                    irIdentityEquals)
            else ->
                throw AssertionError("Unexpected identity operator $irOperator")
        }

    }

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val irArgument0 = parentGenerator.generateExpression(expression.left!!)
        val irArgument1 = parentGenerator.generateExpression(expression.right!!)

        val irEquals = IrBinaryOperatorImpl(expression.startOffset, expression.endOffset,
                                            irOperator, context.irBuiltIns.eqeq, irArgument0, irArgument1)

        return when (irOperator) {
            IrOperator.EQEQ ->
                irEquals
            IrOperator.EXCLEQ ->
                IrUnaryOperatorImpl(expression.startOffset, expression.endOffset, IrOperator.EXCLEQ,
                                    context.irBuiltIns.booleanNot, irEquals)
            else ->
                throw AssertionError("Unexpected equality operator $irOperator")
        }

    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val compareToCall = getResolvedCall(expression)!!

        val irCallGenerator = CallGenerator(parentGenerator)
        val irCompareToCall = irCallGenerator.generateCall(expression, compareToCall, irOperator)

        val compareToZeroDescriptor = when (irOperator) {
            IrOperator.LT -> context.irBuiltIns.lt0
            IrOperator.LTEQ -> context.irBuiltIns.lteq0
            IrOperator.GT -> context.irBuiltIns.gt0
            IrOperator.GTEQ -> context.irBuiltIns.gteq0
            else -> throw AssertionError("Unexpected comparison operator: $irOperator")
        }

        return IrUnaryOperatorImpl(expression.startOffset, expression.endOffset, irOperator, compareToZeroDescriptor, irCompareToCall)
    }


    private fun generateBinaryOperatorAsCall(expression: KtBinaryExpression, irOperator: IrOperator?): IrExpression {
        val operatorCall = getResolvedCall(expression)!!
        return CallGenerator(parentGenerator).generateCall(expression, operatorCall, irOperator)
    }

    private fun generatePrefixIncrementDecrementOperator(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val ktBaseExpression = expression.baseExpression!!
        val irLValue = generateLValue(ktBaseExpression, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        if (irLValue is IrLValueWithAugmentedStore) {
            return irLValue.prefixAugmentedStore(operatorCall, irOperator)
        }

        val opCallGenerator = CallGenerator(parentGenerator).apply { scope.putValue(ktBaseExpression, irLValue) }
        val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irLValue.type, true, irOperator)
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)
        val irTmp = parentGenerator.scope.createTemporaryVariable(irOpCall)
        irBlock.addStatement(irTmp)
        irBlock.addStatement(irLValue.store(irTmp.defaultLoad()))
        irBlock.addStatement(irTmp.defaultLoad())
        return irBlock
    }

    private fun generatePostfixIncrementDecrementOperator(expression: KtPostfixExpression, irOperator: IrOperator): IrExpression {
        val ktBaseExpression = expression.baseExpression!!
        val irLValue = generateLValue(ktBaseExpression, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        if (irLValue is IrLValueWithAugmentedStore) {
            return irLValue.postfixAugmentedStore(operatorCall, irOperator)
        }

        val irBlock = IrBlockImpl(expression.startOffset, expression.endOffset, irLValue.type, true, irOperator)
        val opCallGenerator = CallGenerator(parentGenerator)

        val irTmp = parentGenerator.scope.createTemporaryVariable(irLValue.load())
        irBlock.addStatement(irTmp)

        opCallGenerator.scope.putValue(ktBaseExpression, VariableLValue(this, irTmp, irOperator))
        val irOpCall = opCallGenerator.generateCall(expression, operatorCall, irOperator)
        irBlock.addStatement(irLValue.store(irOpCall))

        irBlock.addStatement(irTmp.defaultLoad())

        return irBlock
    }

    private fun generatePrefixOperatorAsCall(expression: KtPrefixExpression, irOperator: IrOperator): IrExpression {
        val resolvedCall = getResolvedCall(expression)!!
        return CallGenerator(parentGenerator).generateCall(expression, resolvedCall, irOperator)
    }

    private fun generateAugmentedAssignment(expression: KtBinaryExpression, irOperator: IrOperator): IrExpression {
        val ktLeft = expression.left!!
        val irLValue = generateLValue(ktLeft, irOperator)
        val operatorCall = getResolvedCall(expression)!!

        val isSimpleAssignment = get(BindingContext.VARIABLE_REASSIGNMENT, expression) ?: false

        if (isSimpleAssignment && irLValue is IrLValueWithAugmentedStore) {
            return irLValue.augmentedStore(operatorCall, irOperator, parentGenerator.generateExpression(expression.right!!))
        }

        val opCallGenerator = CallGenerator(parentGenerator).apply { scope.putValue(ktLeft, irLValue) }
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
        return irLValue.store(parentGenerator.generateExpression(ktRight))
    }

    private fun generateLValue(ktLeft: KtExpression, irOperator: IrOperator?): IrLValue {
        if (ktLeft is KtArrayAccessExpression) {
            val irArrayValue = parentGenerator.generateExpression(ktLeft.arrayExpression!!)
            val indexExpressions = ktLeft.indexExpressions.map { it to parentGenerator.generateExpression(it) }
            val indexedGetCall = get(BindingContext.INDEXED_LVALUE_GET, ktLeft)
            val indexedSetCall = get(BindingContext.INDEXED_LVALUE_SET, ktLeft)
            val type = indexedGetCall?.run { resultingDescriptor.returnType }
                       ?: indexedSetCall?.run { resultingDescriptor.valueParameters.last().type }
                       ?: throw AssertionError("Either 'get' or 'set' call should be present for an indexed LValue: ${ktLeft.text}")
            return IndexedLValue(parentGenerator, ktLeft, irOperator,
                                 irArrayValue, type, indexExpressions, indexedGetCall, indexedSetCall)
        }

        val resolvedCall = getResolvedCall(ktLeft) ?: TODO("no resolved call for LHS")
        val descriptor = resolvedCall.candidateDescriptor

        return when (descriptor) {
            is LocalVariableDescriptor ->
                if (descriptor.isDelegated)
                    TODO("Delegated local variable")
                else
                    VariableLValue(this, ktLeft.startOffset, ktLeft.endOffset, descriptor, irOperator)
            is PropertyDescriptor ->
                CallGenerator(parentGenerator).run {
                    PropertyLValue(
                            this,
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
