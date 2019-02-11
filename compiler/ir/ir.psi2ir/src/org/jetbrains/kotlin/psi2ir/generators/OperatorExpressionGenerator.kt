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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrDynamicOperator
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.referenceClassifier
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.containsNull
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.psi2ir.intermediate.safeCallOnDispatchReceiver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.checkers.PrimitiveNumericComparisonInfo
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.types.typeUtil.makeNullable


class OperatorExpressionGenerator(statementGenerator: StatementGenerator) : StatementGeneratorExtension(statementGenerator) {

    private fun createErrorExpression(ktExpression: KtExpression, text: String) =
        IrErrorExpressionImpl(
            ktExpression.startOffsetSkippingComments,
            ktExpression.endOffset,
            context.irBuiltIns.nothingType,
            text
        )

    fun generatePrefixExpression(expression: KtPrefixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPrefixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected prefix operator: $ktOperator")

            in INCREMENT_DECREMENT_OPERATORS ->
                AssignmentGenerator(statementGenerator).generatePrefixIncrementDecrement(expression, irOperator)

            in OPERATORS_DESUGARED_TO_CALLS -> generatePrefixOperatorAsCall(expression, irOperator)

            else -> createErrorExpression(expression, ktOperator.toString())
        }
    }

    fun generatePostfixExpression(expression: KtPostfixExpression): IrExpression {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getPostfixOperator(ktOperator)

        return when (irOperator) {
            null -> throw AssertionError("Unexpected postfix operator: $ktOperator")

            in INCREMENT_DECREMENT_OPERATORS ->
                AssignmentGenerator(statementGenerator).generatePostfixIncrementDecrement(expression, irOperator)

            IrStatementOrigin.EXCLEXCL -> generateExclExclOperator(expression, irOperator)

            else -> createErrorExpression(expression, ktOperator.toString())
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
            expression.startOffsetSkippingComments, expression.endOffset, resultType.toIrType(), irOperator, rhsType.toIrType(),
            context.symbolTable.referenceClassifier(rhsType.constructor.declarationDescriptor!!),
            expression.left.genExpr()
        )
    }

    fun generateInstanceOfExpression(expression: KtIsExpression): IrStatement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val irOperator = getIrTypeOperator(ktOperator)!!
        val againstType = getOrFail(BindingContext.TYPE, expression.typeReference)

        return IrTypeOperatorCallImpl(
            expression.startOffsetSkippingComments, expression.endOffset, context.irBuiltIns.booleanType, irOperator,
            againstType.toIrType(),
            context.symbolTable.referenceClassifier(againstType.constructor.declarationDescriptor!!),
            expression.leftHandSide.genExpr()
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
            else -> createErrorExpression(expression, ktOperator.toString())
        }
    }

    private fun generateElvis(expression: KtBinaryExpression): IrExpression {
        val specialCallForElvis = getResolvedCall(expression)!!
        val resultType = specialCallForElvis.resultingDescriptor.returnType!!.toIrType()
        val irArgument0 = expression.left!!.genExpr()
        val irArgument1 = expression.right!!.genExpr()

        return irBlock(expression.startOffsetSkippingComments, expression.endOffset, IrStatementOrigin.ELVIS, resultType) {
            val temporary = irTemporary(irArgument0, "elvis_lhs")
            +irIfNull(
                resultType,
                irGet(temporary.type, temporary.symbol),
                irArgument1,
                irGet(temporary.type, temporary.symbol)
            )
        }
    }

    private fun generateBinaryBooleanOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val irArgument0 = expression.left!!.genExpr()
        val irArgument1 = expression.right!!.genExpr()
        return when (irOperator) {
            IrStatementOrigin.OROR ->
                context.oror(expression.startOffsetSkippingComments, expression.endOffset, irArgument0, irArgument1)
            IrStatementOrigin.ANDAND ->
                context.andand(expression.startOffsetSkippingComments, expression.endOffset, irArgument0, irArgument1)
            else ->
                throw AssertionError("Unexpected binary boolean operator $irOperator")
        }
    }

    private fun generateInOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val containsCall = getResolvedCall(expression)!!

        val irContainsCall = generateCall(containsCall, expression, irOperator)

        return when (irOperator) {
            IrStatementOrigin.IN ->
                irContainsCall
            IrStatementOrigin.NOT_IN ->
                IrUnaryPrimitiveImpl(
                    expression.startOffsetSkippingComments, expression.endOffset,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.NOT_IN,
                    context.irBuiltIns.booleanNotSymbol,
                    irContainsCall
                )
            else ->
                throw AssertionError("Unexpected in-operator $irOperator")
        }

    }

    private fun generateIdentityOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val irArgument0 = expression.left!!.genExpr()
        val irArgument1 = expression.right!!.genExpr()

        val irIdentityEquals = IrBinaryPrimitiveImpl(
            expression.startOffsetSkippingComments, expression.endOffset,
            context.irBuiltIns.booleanType,
            irOperator,
            context.irBuiltIns.eqeqeqSymbol,
            irArgument0, irArgument1
        )

        return when (irOperator) {
            IrStatementOrigin.EQEQEQ ->
                irIdentityEquals
            IrStatementOrigin.EXCLEQEQ ->
                IrUnaryPrimitiveImpl(
                    expression.startOffsetSkippingComments, expression.endOffset,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCLEQEQ,
                    context.irBuiltIns.booleanNotSymbol,
                    irIdentityEquals
                )
            else ->
                throw AssertionError("Unexpected identity operator $irOperator")
        }
    }

    private fun KtExpression.generateAsPrimitiveNumericComparisonOperand(
        expressionType: KotlinType?,
        comparisonType: KotlinType?
    ) = genExpr().promoteToPrimitiveNumericType(expressionType, comparisonType)

    private fun getPrimitiveNumericComparisonInfo(ktExpression: KtBinaryExpression) =
        context.bindingContext[BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, ktExpression]

    private fun generateEqualityOperator(expression: KtBinaryExpression, irOperator: IrStatementOrigin): IrExpression {
        val comparisonInfo = getPrimitiveNumericComparisonInfo(expression)
        val comparisonType = comparisonInfo?.comparisonType

        val eqeqSymbol = context.irBuiltIns.ieee754equalsFunByOperandType[comparisonType]?.symbol
            ?: context.irBuiltIns.eqeqSymbol

        val irEquals = IrBinaryPrimitiveImpl(
            expression.startOffsetSkippingComments, expression.endOffset,
            context.irBuiltIns.booleanType,
            irOperator,
            eqeqSymbol,
            expression.left!!.generateAsPrimitiveNumericComparisonOperand(comparisonInfo?.leftType, comparisonType),
            expression.right!!.generateAsPrimitiveNumericComparisonOperand(comparisonInfo?.rightType, comparisonType)
        )

        return when (irOperator) {
            IrStatementOrigin.EQEQ ->
                irEquals
            IrStatementOrigin.EXCLEQ ->
                IrUnaryPrimitiveImpl(
                    expression.startOffsetSkippingComments, expression.endOffset,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCLEQ,
                    context.irBuiltIns.booleanNotSymbol,
                    irEquals
                )
            else ->
                throw AssertionError("Unexpected equality operator $irOperator")
        }
    }

    fun generateEquality(
        startOffset: Int,
        endOffset: Int,
        irOperator: IrStatementOrigin,
        arg1: IrExpression,
        arg2: IrExpression,
        comparisonInfo: PrimitiveNumericComparisonInfo?
    ): IrExpression =
        if (comparisonInfo != null) {
            val comparisonType = comparisonInfo.comparisonType
            val eqeqSymbol =
                context.irBuiltIns.ieee754equalsFunByOperandType[comparisonType]?.symbol
                    ?: context.irBuiltIns.eqeqSymbol
            IrBinaryPrimitiveImpl(
                startOffset, endOffset,
                context.irBuiltIns.booleanType,
                irOperator,
                eqeqSymbol,
                arg1.promoteToPrimitiveNumericType(comparisonInfo.leftType, comparisonType),
                arg2.promoteToPrimitiveNumericType(comparisonInfo.rightType, comparisonType)
            )
        } else {
            IrBinaryPrimitiveImpl(
                startOffset, endOffset,
                context.irBuiltIns.booleanType,
                irOperator,
                context.irBuiltIns.eqeqSymbol,
                arg1, arg2
            )
        }

    private fun IrExpression.promoteToPrimitiveNumericType(operandType: KotlinType?, targetType: KotlinType?): IrExpression {
        if (targetType == null) return this
        if (operandType == null) throw AssertionError("operandType should be non-null")

        val operandNNType = operandType.makeNotNullable()

        val conversionFunction = operandNNType.findConversionFunctionTo(targetType)

        return when {
            !operandNNType.isPrimitiveNumberType() ->
                throw AssertionError("Primitive number type or nullable primitive number type expected: $type")

            operandType == targetType || operandNNType == targetType ->
                this

            // TODO: don't rely on originalKotlinType.
            type.originalKotlinType!!.containsNull() ->
                safeCallOnDispatchReceiver(this@OperatorExpressionGenerator, startOffset, endOffset) { dispatchReceiver ->
                    invokeConversionFunction(
                        startOffset, endOffset,
                        conversionFunction ?: throw AssertionError("No conversion function for $type ~> $targetType"),
                        dispatchReceiver
                    )
                }

            else ->
                invokeConversionFunction(
                    startOffset, endOffset,
                    conversionFunction ?: throw AssertionError("No conversion function for $type ~> $targetType"),
                    this
                )
        }
    }

    private fun invokeConversionFunction(
        startOffset: Int,
        endOffset: Int,
        functionDescriptor: FunctionDescriptor,
        receiver: IrExpression
    ): IrExpression =
        IrCallImpl(
            startOffset,
            endOffset,
            functionDescriptor.returnType!!.toIrType(),
            context.symbolTable.referenceFunction(functionDescriptor.original),
            functionDescriptor,
            origin = null, // TODO origin for widening conversions?
            superQualifierSymbol = null
        ).apply {
            dispatchReceiver = receiver
        }

    private fun KotlinType.findConversionFunctionTo(targetType: KotlinType): FunctionDescriptor? {
        val targetTypeName = targetType.constructor.declarationDescriptor?.name?.asString() ?: return null
        return memberScope.findSingleFunction(Name.identifier("to$targetTypeName"))
    }

    private fun generateComparisonOperator(expression: KtBinaryExpression, origin: IrStatementOrigin): IrExpression {
        val startOffset = expression.startOffsetSkippingComments
        val endOffset = expression.endOffset

        val comparisonInfo = getPrimitiveNumericComparisonInfo(expression)

        return if (comparisonInfo != null) {
            IrBinaryPrimitiveImpl(
                startOffset, endOffset,
                context.irBuiltIns.booleanType,
                origin,
                getComparisonOperatorSymbol(origin, comparisonInfo.comparisonType),
                expression.left!!.generateAsPrimitiveNumericComparisonOperand(comparisonInfo.leftType, comparisonInfo.comparisonType),
                expression.right!!.generateAsPrimitiveNumericComparisonOperand(comparisonInfo.rightType, comparisonInfo.comparisonType)
            )
        } else {
            IrBinaryPrimitiveImpl(
                startOffset, endOffset,
                context.irBuiltIns.booleanType,
                origin,
                getComparisonOperatorSymbol(origin, context.irBuiltIns.int),
                generateCall(getResolvedCall(expression)!!, expression, origin),
                IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, 0)
            )
        }
    }

    private fun generateCall(
        resolvedCall: ResolvedCall<*>,
        ktExpression: KtExpression,
        origin: IrStatementOrigin?
    ) =
        CallGenerator(statementGenerator).generateCall(ktExpression, statementGenerator.pregenerateCall(resolvedCall), origin)

    private fun getComparisonOperatorSymbol(origin: IrStatementOrigin, primitiveNumericType: KotlinType): IrSimpleFunctionSymbol =
        when (origin) {
            IrStatementOrigin.LT -> context.irBuiltIns.lessFunByOperandType
            IrStatementOrigin.LTEQ -> context.irBuiltIns.lessOrEqualFunByOperandType
            IrStatementOrigin.GT -> context.irBuiltIns.greaterFunByOperandType
            IrStatementOrigin.GTEQ -> context.irBuiltIns.greaterOrEqualFunByOperandType
            else -> throw AssertionError("Unexpected comparison operator: $origin")
        }[primitiveNumericType]!!.symbol

    private fun generateExclExclOperator(expression: KtPostfixExpression, origin: IrStatementOrigin): IrExpression {
        val ktArgument = expression.baseExpression!!
        val irArgument = ktArgument.genExpr()
        val ktOperator = expression.operationReference

        val resultType = irArgument.type.makeNotNull()

        return irBlock(ktOperator.startOffsetSkippingComments, ktOperator.endOffset, origin, resultType) {
            val temporary = irTemporary(irArgument, "notnull")
            +irIfNull(
                resultType,
                irGet(temporary.type, temporary.symbol),
                irThrowNpe(origin),
                irGet(temporary.type, temporary.symbol)
            )
        }
    }

    private fun generateBinaryOperatorAsCall(expression: KtBinaryExpression, origin: IrStatementOrigin?): IrExpression =
        generateCall(getResolvedCall(expression)!!, expression, origin)

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

        return generateCall(resolvedCall, expression, origin)
    }

    fun generateDynamicOperatorExpression(
        irOperator: IrDynamicOperator,
        irType: IrType,
        ktOperatorExpression: KtExpression,
        ktReceiverExpression: KtExpression,
        ktArgumentExpressions: List<KtExpression>
    ): IrExpression =
        IrDynamicOperatorExpressionImpl(
            ktOperatorExpression.startOffsetSkippingComments,
            ktOperatorExpression.endOffset,
            irType,
            irOperator
        ).apply {
            receiver = statementGenerator.generateExpression(ktReceiverExpression)
            ktArgumentExpressions.mapTo(arguments) { statementGenerator.generateExpression(it) }
        }
}
