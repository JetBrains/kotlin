/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.utils.convertWithOffsets
import org.jetbrains.kotlin.fir.backend.utils.createSafeCallConstruction
import org.jetbrains.kotlin.fir.backend.utils.createTemporaryVariableForSafeCallConstruction
import org.jetbrains.kotlin.fir.backend.utils.unsubstitutedScope
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.ir.builders.primitiveOp1
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.Name

internal class OperatorExpressionGenerator(
    private val c: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val conversionScope: Fir2IrConversionScope
) : Fir2IrComponents by c {

    fun convertComparisonExpression(comparisonExpression: FirComparisonExpression): IrExpression {
        return comparisonExpression.convertWithOffsets { startOffset, endOffset ->
            generateComparisonCall(startOffset, endOffset, comparisonExpression)
        }
    }

    fun convertEqualityOperatorCall(equalityOperatorCall: FirEqualityOperatorCall): IrExpression {
        return equalityOperatorCall.convertWithOffsets { startOffset, endOffset ->
            generateEqualityOperatorCall(startOffset, endOffset, equalityOperatorCall.operation, equalityOperatorCall.arguments)
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int,
        comparisonExpression: FirComparisonExpression
    ): IrExpression {
        val operation = comparisonExpression.operation
        val receiver = comparisonExpression.compareToCall.explicitReceiver

        if (receiver?.resolvedType is ConeDynamicType) {
            val dynamicOperator = operation.toIrDynamicOperator()
                ?: throw Exception("Can't convert to the corresponding IrDynamicOperator")
            val argument = comparisonExpression.compareToCall.dynamicVarargArguments?.firstOrNull()
                ?: throw Exception("Comparison with a dynamic function should have a vararg with the rhs-argument")

            return IrDynamicOperatorExpressionImpl(
                startOffset,
                endOffset,
                builtins.booleanType,
                dynamicOperator,
            ).apply {
                this.receiver = receiver.accept(visitor, null) as IrExpression
                arguments.add(argument.accept(visitor, null) as IrExpression)
            }
        }

        fun fallbackToRealCall(): IrExpression {
            val (symbol, origin) = getSymbolAndOriginForComparison(operation, builtins.intType.classifierOrFail)
            val irCompareToCall = comparisonExpression.compareToCall.accept(visitor, null) as IrCall
            irCompareToCall.origin = origin
            return IrCallImplWithShape(
                startOffset = startOffset,
                endOffset = endOffset,
                type = builtins.booleanType,
                symbol = symbol!!,
                typeArgumentsCount = 0,
                valueArgumentsCount = 2,
                contextParameterCount = 0,
                hasDispatchReceiver = false,
                hasExtensionReceiver = false,
                origin = origin,
            ).apply {
                putValueArgument(0, irCompareToCall)
                putValueArgument(1, IrConstImpl.int(startOffset, endOffset, builtins.intType, 0))
            }
        }

        if (comparisonExpression.compareToCall.toResolvedCallableSymbol()?.fir?.receiverParameter != null) {
            return fallbackToRealCall()
        }

        val comparisonInfo = comparisonExpression.inferPrimitiveNumericComparisonInfo(c) ?: return fallbackToRealCall()
        val comparisonType = comparisonInfo.comparisonType

        val comparisonIrType = typeConverter.classIdToTypeMap[comparisonType.lookupTag.classId] ?: return fallbackToRealCall()
        val (symbol, origin) = getSymbolAndOriginForComparison(operation, comparisonIrType.classifierOrFail)

        return IrCallImplWithShape(
            startOffset = startOffset,
            endOffset = endOffset,
            type = builtins.booleanType,
            symbol = symbol!!,
            typeArgumentsCount = 0,
            valueArgumentsCount = 2,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
            origin = origin,
        ).apply {
            putValueArgument(0, comparisonExpression.left.convertToIrExpression(comparisonInfo, isLeftType = true))
            putValueArgument(1,  comparisonExpression.right.convertToIrExpression(comparisonInfo, isLeftType = false))
        }
    }

    private fun getSymbolAndOriginForComparison(
        operation: FirOperation,
        classifier: IrClassifierSymbol
    ): Pair<IrSimpleFunctionSymbol?, IrStatementOriginImpl> {
        return when (operation) {
            FirOperation.LT -> builtins.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> builtins.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> builtins.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> builtins.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> error("Unexpected comparison operation: $operation")
        }
    }

    private fun FirOperation.toIrDynamicOperator() = when (this) {
        FirOperation.LT -> IrDynamicOperator.LT
        FirOperation.LT_EQ -> IrDynamicOperator.LE
        FirOperation.GT -> IrDynamicOperator.GT
        FirOperation.GT_EQ -> IrDynamicOperator.GE
        else -> null
    }

    private fun generateEqualityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression = when (operation) {
        FirOperation.EQ, FirOperation.NOT_EQ -> transformEqualityOperatorCall(startOffset, endOffset, operation, arguments)
        FirOperation.IDENTITY, FirOperation.NOT_IDENTITY -> transformIdentityOperatorCall(startOffset, endOffset, operation, arguments)
        else -> error("Unexpected operation: $operation")
    }

    private fun IrStatementOrigin.toIrDynamicOperator() = when (this) {
        IrStatementOrigin.EQEQ -> IrDynamicOperator.EQEQ
        IrStatementOrigin.EXCLEQ -> IrDynamicOperator.EXCLEQ
        IrStatementOrigin.EQEQEQ -> IrDynamicOperator.EQEQEQ
        IrStatementOrigin.EXCLEQEQ -> IrDynamicOperator.EXCLEQEQ
        else -> null
    }

    private fun tryGenerateDynamicOperatorCall(
        startOffset: Int,
        endOffset: Int,
        firstArgument: IrExpression,
        secondArgument: IrExpression,
        origin: IrStatementOrigin,
    ) = if (firstArgument.type is IrDynamicType) {
        val dynamicOperator = origin.toIrDynamicOperator()
            ?: throw Exception("Couldn't convert to the corresponding IrDynamicOperator")

        IrDynamicOperatorExpressionImpl(
            startOffset,
            endOffset,
            builtins.booleanType,
            dynamicOperator,
        ).apply {
            receiver = firstArgument
            arguments.add(secondArgument)
        }
    } else {
        null
    }

    private fun transformEqualityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val origin = when (operation) {
            FirOperation.EQ -> IrStatementOrigin.EQEQ
            FirOperation.NOT_EQ -> IrStatementOrigin.EXCLEQ
            else -> error("Not an equality operation: $operation")
        }
        val comparisonInfo = inferPrimitiveNumericComparisonInfo(arguments[0], arguments[1], c)

        val convertedLeft = arguments[0].convertToIrExpression(comparisonInfo, isLeftType = true)
        val convertedRight = arguments[1].convertToIrExpression(comparisonInfo, isLeftType = false)

        tryGenerateDynamicOperatorCall(
            startOffset,
            endOffset,
            convertedLeft,
            convertedRight,
            origin,
        )?.let {
            return it
        }

        val comparisonType = comparisonInfo?.comparisonType
        val eqeqSymbol = comparisonType?.let { typeConverter.classIdToSymbolMap[it.lookupTag.classId] }
            ?.let { builtins.ieee754equalsFunByOperandType[it] } ?: builtins.eqeqSymbol

        val equalsCall = IrCallImplWithShape(
            startOffset = startOffset,
            endOffset = endOffset,
            type = builtins.booleanType,
            symbol = eqeqSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 2,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
            origin = origin
        ).apply {
            putValueArgument(0, convertedLeft)
            putValueArgument(1, convertedRight)
        }
        return if (operation == FirOperation.EQ) {
            equalsCall
        } else {
            equalsCall.negate(origin)
        }
    }

    private fun transformIdentityOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val origin = when (operation) {
            FirOperation.IDENTITY -> IrStatementOrigin.EQEQEQ
            FirOperation.NOT_IDENTITY -> IrStatementOrigin.EXCLEQEQ
            else -> error("Not an identity operation: $operation")
        }
        val convertedLeft = visitor.convertToIrExpression(arguments[0])
        val convertedRight = visitor.convertToIrExpression(arguments[1])
        tryGenerateDynamicOperatorCall(
            startOffset,
            endOffset,
            convertedLeft,
            convertedRight,
            origin,
        )?.let {
            return it
        }
        val identityCall = IrCallImplWithShape(
            startOffset = startOffset,
            endOffset = endOffset,
            type = builtins.booleanType,
            symbol = builtins.eqeqeqSymbol,
            typeArgumentsCount = 0,
            valueArgumentsCount = 2,
            contextParameterCount = 0,
            hasDispatchReceiver = false,
            hasExtensionReceiver = false,
            origin = origin,
        ).apply {
            putValueArgument(0, convertedLeft)
            putValueArgument(1, convertedRight)
        }

        return if (operation == FirOperation.IDENTITY) {
            identityCall
        } else {
            identityCall.negate(origin)
        }
    }

    private fun IrExpression.negate(origin: IrStatementOrigin) =
        primitiveOp1(startOffset, endOffset, builtins.booleanNotSymbol, builtins.booleanType, origin, this)

    private fun FirExpression.convertToIrExpression(
        comparisonInfo: PrimitiveConeNumericComparisonInfo?,
        isLeftType: Boolean
    ): IrExpression {
        val isOriginalNullable = (this as? FirSmartCastExpression)?.originalExpression?.resolvedType?.isMarkedNullable ?: false
        val irExpression = visitor.convertToIrExpression(this)
        val operandType = if (isLeftType) comparisonInfo?.leftType else comparisonInfo?.rightType
        val targetType = comparisonInfo?.comparisonType
        val noImplicitCast = comparisonInfo?.leftType == comparisonInfo?.rightType

        fun eraseImplicitCast(): IrExpression {
            if (irExpression is IrTypeOperatorCall) {
                val isDoubleOrFloatWithoutNullability = irExpression.type.isDoubleOrFloatWithoutNullability()
                if (noImplicitCast && !isDoubleOrFloatWithoutNullability && irExpression.operator == IrTypeOperator.IMPLICIT_CAST) {
                    return irExpression.argument
                } else {
                    val expressionType = irExpression.type
                    if (isDoubleOrFloatWithoutNullability &&
                        isOriginalNullable &&
                        expressionType is IrSimpleType &&
                        !expressionType.isNullable()
                    ) {
                        // Make it compatible with IR lowering
                        val nullableDoubleOrFloatType = expressionType.makeNullable()
                        return IrTypeOperatorCallImpl(
                            irExpression.startOffset,
                            irExpression.endOffset,
                            nullableDoubleOrFloatType,
                            irExpression.operator,
                            nullableDoubleOrFloatType,
                            irExpression.argument
                        )
                    }
                }
            }

            return irExpression
        }

        if (targetType == null) {
            return eraseImplicitCast()
        }

        if (operandType == null) error("operandType should be non-null if targetType is non-null")

        val operandClassId = operandType.lookupTag.classId
        val targetClassId = targetType.lookupTag.classId
        if (operandClassId == targetClassId) return eraseImplicitCast()
        val operandFirClass = session.getRegularClassSymbolByClassId(operandClassId)
            ?: error("No symbol for $operandClassId")
        val conversionFirFunction = operandFirClass.unsubstitutedScope(c)
            .getFunctions(Name.identifier("to${targetType.lookupTag.classId.shortClassName.asString()}"))
            .singleOrNull()
            ?: error("No conversion function for $operandType ~> $targetType")
        val conversionFunctionSymbol = declarationStorage.getIrFunctionSymbol(conversionFirFunction, operandFirClass.toLookupTag())

        val unsafeIrCall = IrCallImpl(
            irExpression.startOffset, irExpression.endOffset,
            conversionFirFunction.resolvedReturnType.toIrType(c),
            conversionFunctionSymbol as IrSimpleFunctionSymbol,
            typeArgumentsCount = 0
        ).also {
            it.dispatchReceiver = irExpression
        }
        return if (operandType.isMarkedNullable) {
            val (receiverVariable, receiverVariableSymbol) =
                conversionScope.createTemporaryVariableForSafeCallConstruction(irExpression)

            unsafeIrCall.dispatchReceiver = IrGetValueImpl(irExpression.startOffset, irExpression.endOffset, receiverVariableSymbol)

            c.createSafeCallConstruction(receiverVariable, receiverVariableSymbol, unsafeIrCall)
        } else {
            unsafeIrCall
        }
    }
}
