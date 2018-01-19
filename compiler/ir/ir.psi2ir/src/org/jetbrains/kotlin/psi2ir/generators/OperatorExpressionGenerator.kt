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
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrBinaryPrimitiveImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrUnaryPrimitiveImpl
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import java.lang.AssertionError


class OperatorExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected prefix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> AssignmentGenerator(statementGenerator).generatePrefixIncrementDecrement(
                expression,
                irOperator
            )
            in OPERATORS_DESUGARED_TO_CALLS -> generatePrefixOperatorAsCall(expression, irOperator)
            else -> createDummyExpression(expression, ktOperator.toString())
        }
    }

    fun generatePostfixExpression(expression: KtPostfixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPostfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected postfix operator: $ktOperator")
            in INCREMENT_DECREMENT_OPERATORS -> AssignmentGenerator(statementGenerator).generatePostfixIncrementDecrement(
                expression,
                irOperator
            )
            IrStatementOrigin.EXCLEXCL -> generateExclExclOperator(expression, irOperator)
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

        return IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset, resultType, irOperator, rhsType,
            statementGenerator.generateExpression(expression.left)
        )
    }

    fun generateInstanceOfExpression(expression: KtIsExpression): IrStatement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)!!
        val againstType = getOrFail(BindingContext.TYPE, expression.typeReference)

        return IrTypeOperatorCallImpl(
            expression.startOffset, expression.endOffset, context.builtIns.booleanType, irOperator,
            againstType, statementGenerator.generateExpression(expression.leftHandSide)
        )
    }

    fun generateBinaryExpression(expression: KtBinaryExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            return generateBinaryOperatorAsCall(expression, null)
        }

        val irOperator = getInfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected infix operator: $ktOperator")
            IrStatementOrigin.EQ -> AssignmentGenerator(statementGenerator).generateAssignment(expression)
            in AUGMENTED_ASSIGNMENTS -> AssignmentGenerator(statementGenerator).generateAugmentedAssignment(expression, irOperator)
            IrStatementOrigin.ELVIS -> generateElvis(expression)
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
        val resultType = specialCallForElvis.resultingDescriptor.returnType!!
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)

        return irBlock(expression, IrStatementOrigin.ELVIS, resultType) {
            val temporary = irTemporary(irArgument0, "elvis_lhs")
            +irIfNull(resultType, irGet(temporary.symbol), irArgument1, irGet(temporary.symbol))
        }
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)
        return when (irOperator) {
            IrStatementOrigin.OROR ->
                context.oror(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            IrStatementOrigin.ANDAND ->
                context.andand(expression.startOffset, expression.endOffset, irArgument0, irArgument1)
            else ->
                throw AssertionError("Unexpected binary boolean operator $irOperator")
        }
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val containsCall = getResolvedCall(expression)!!

        val irContainsCall =
            CallGenerator(statementGenerator).generateCall(expression, statementGenerator.pregenerateCall(containsCall), irOperator)

        return when (irOperator) {
            IrStatementOrigin.IN ->
                irContainsCall
            IrStatementOrigin.NOT_IN ->
                IrUnaryPrimitiveImpl(
                    expression.startOffset, expression.endOffset, IrStatementOrigin.NOT_IN,
                    context.irBuiltIns.booleanNotSymbol,
                    irContainsCall
                )
            else ->
                throw AssertionError("Unexpected in-operator $irOperator")
        }

    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)


        val irIdentityEquals = IrBinaryPrimitiveImpl(
            expression.startOffset, expression.endOffset, irOperator,
            context.irBuiltIns.eqeqeqSymbol,
            irArgument0, irArgument1
        )

        return when (irOperator) {
            IrStatementOrigin.EQEQEQ ->
                irIdentityEquals
            IrStatementOrigin.EXCLEQEQ ->
                IrUnaryPrimitiveImpl(
                    expression.startOffset, expression.endOffset, IrStatementOrigin.EXCLEQEQ,
                    context.irBuiltIns.booleanNotSymbol,
                    irIdentityEquals
                )
            else ->
                throw AssertionError("Unexpected identity operator $irOperator")
        }

    }

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val irArgument0 = statementGenerator.generateExpression(expression.left!!)
        val irArgument1 = statementGenerator.generateExpression(expression.right!!)

        val irEquals = IrBinaryPrimitiveImpl(
            expression.startOffset, expression.endOffset,
            irOperator,
            context.irBuiltIns.eqeqSymbol,
            irArgument0, irArgument1
        )

        return when (irOperator) {
            IrStatementOrigin.EQEQ ->
                irEquals
            IrStatementOrigin.EXCLEQ ->
                IrUnaryPrimitiveImpl(
                    expression.startOffset, expression.endOffset, IrStatementOrigin.EXCLEQ,
                    context.irBuiltIns.booleanNotSymbol,
                    irEquals
                )
            else ->
                throw AssertionError("Unexpected equality operator $irOperator")
        }

    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, origin: IrStatementOrigin): IrExpression {
        val compareToCall = getResolvedCall(expression)!!

        val irCompareToCall =
            CallGenerator(statementGenerator).generateCall(expression, statementGenerator.pregenerateCall(compareToCall), origin)

        val compareToZeroSymbol = when (origin) {
            IrStatementOrigin.LT -> context.irBuiltIns.lt0Symbol
            IrStatementOrigin.LTEQ -> context.irBuiltIns.lteq0Symbol
            IrStatementOrigin.GT -> context.irBuiltIns.gt0Symbol
            IrStatementOrigin.GTEQ -> context.irBuiltIns.gteq0Symbol
            else -> throw AssertionError("Unexpected comparison operator: $origin")
        }

        return IrUnaryPrimitiveImpl(expression.startOffset, expression.endOffset, origin, compareToZeroSymbol, irCompareToCall)
    }

    private fun generateExclExclOperator(expression: KtPostfixExpression, origin: IrStatementOrigin): IrExpression {
        val ktArgument = expression.baseExpression!!
        val irArgument = statementGenerator.generateExpression(ktArgument)
        val ktOperator = expression.operationReference

        val resultType = irArgument.type.makeNotNullable()

        return irBlock(ktOperator, origin, resultType) {
            val temporary = irTemporary(irArgument, "notnull")
            +irIfNull(resultType, irGet(temporary.symbol), irThrowNpe(origin), irGet(temporary.symbol))
        }
    }

    private fun generateBinaryOperatorAsCall(expression: KtBinaryExpression, origin: IrStatementOrigin?): IrExpression {
        val operatorCall = getResolvedCall(expression)!!
        return CallGenerator(statementGenerator).generateCall(expression, statementGenerator.pregenerateCall(operatorCall), origin)
    }

    private fun generatePrefixOperatorAsCall(expression: KtPrefixExpression, origin: IrStatementOrigin): IrExpression {
        val resolvedCall = getResolvedCall(expression)!!

        if (expression.baseExpression is KtConstantExpression) {
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)?.let { constant ->
                val receiverType = resolvedCall.dispatchReceiver?.type
                if (receiverType != null && KotlinBuiltIns.isPrimitiveType(receiverType)) {
                    return statementGenerator.generateConstantExpression(expression, constant)
                }
            }
        }

        return CallGenerator(statementGenerator).generateCall(expression, statementGenerator.pregenerateCall(resolvedCall), origin)
    }
}
