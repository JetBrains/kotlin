/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.ir.builders.primitiveOp1
import org.jetbrains.kotlin.ir.builders.primitiveOp2
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.getSimpleFunction
import java.util.*

internal class OperatorExpressionGenerator(
    private val components: Fir2IrComponents,
    private val visitor: Fir2IrVisitor,
    private val callGenerator: CallAndReferenceGenerator
) : Fir2IrComponents by components {

    companion object {
        private val NEGATED_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY)

        private val UNARY_OPERATIONS: Set<FirOperation> = EnumSet.of(FirOperation.EXCL)
    }

    fun convertComparisonExpression(comparisonExpression: FirComparisonExpression): IrExpression {
        return comparisonExpression.convertWithOffsets { startOffset, endOffset ->
            generateComparisonCall(startOffset, endOffset, comparisonExpression)
        }
    }

    fun convertOperatorCall(operatorCall: FirOperatorCall): IrExpression {
        return operatorCall.convertWithOffsets { startOffset, endOffset ->
            generateOperatorCall(startOffset, endOffset, operatorCall.operation, operatorCall.arguments)
        }
    }

    private fun generateComparisonCall(
        startOffset: Int, endOffset: Int,
        comparisonExpression: FirComparisonExpression
    ): IrExpression {
        val operation = comparisonExpression.operation

        fun fallbackToRealCall(): IrExpression {
            val (symbol, origin) = getSymbolAndOriginForComparison(operation, irBuiltIns.intType.classifierOrFail)
            return primitiveOp2(
                startOffset, endOffset,
                symbol!!,
                irBuiltIns.booleanType,
                origin,
                comparisonExpression.compareToCall.accept(visitor, null) as IrExpression,
                IrConstImpl.int(startOffset, endOffset, irBuiltIns.intType, 0)
            )
        }

        val comparisonInfo = comparisonExpression.inferPrimitiveNumericComparisonInfo() ?: return fallbackToRealCall()
        val comparisonType = comparisonInfo.comparisonType

        val comparisonIrType = typeConverter.classIdToTypeMap[comparisonType.lookupTag.classId] ?: return fallbackToRealCall()
        val (symbol, origin) = getSymbolAndOriginForComparison(operation, comparisonIrType.classifierOrFail)

        return primitiveOp2(
            startOffset, endOffset, symbol!!, irBuiltIns.booleanType, origin,
            visitor.convertToIrExpression(comparisonExpression.left).asComparisonOperand(comparisonInfo.leftType, comparisonType),
            visitor.convertToIrExpression(comparisonExpression.right).asComparisonOperand(comparisonInfo.rightType, comparisonType),
        )
    }

    private fun getSymbolAndOriginForComparison(
        operation: FirOperation,
        classifier: IrClassifierSymbol
    ): Pair<IrSimpleFunctionSymbol?, IrStatementOriginImpl> {
        return when (operation) {
            FirOperation.LT -> irBuiltIns.lessFunByOperandType[classifier] to IrStatementOrigin.LT
            FirOperation.GT -> irBuiltIns.greaterFunByOperandType[classifier] to IrStatementOrigin.GT
            FirOperation.LT_EQ -> irBuiltIns.lessOrEqualFunByOperandType[classifier] to IrStatementOrigin.LTEQ
            FirOperation.GT_EQ -> irBuiltIns.greaterOrEqualFunByOperandType[classifier] to IrStatementOrigin.GTEQ
            else -> error("Unexpected comparison operation: $operation")
        }
    }

    private fun generateOperatorCall(
        startOffset: Int, endOffset: Int, operation: FirOperation, arguments: List<FirExpression>
    ): IrExpression {
        val (type, symbol, origin) = when (operation) {
            FirOperation.EQ -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EQEQ)
            FirOperation.NOT_EQ -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqSymbol, IrStatementOrigin.EXCLEQ)
            FirOperation.IDENTITY -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EQEQEQ)
            FirOperation.NOT_IDENTITY -> Triple(irBuiltIns.booleanType, irBuiltIns.eqeqeqSymbol, IrStatementOrigin.EXCLEQEQ)
            FirOperation.EXCL -> Triple(irBuiltIns.booleanType, irBuiltIns.booleanNotSymbol, IrStatementOrigin.EXCL)
            FirOperation.LT, FirOperation.GT,
            FirOperation.LT_EQ, FirOperation.GT_EQ,
            FirOperation.OTHER, FirOperation.ASSIGN, FirOperation.PLUS_ASSIGN,
            FirOperation.MINUS_ASSIGN, FirOperation.TIMES_ASSIGN,
            FirOperation.DIV_ASSIGN, FirOperation.REM_ASSIGN,
            FirOperation.IS, FirOperation.NOT_IS,
            FirOperation.AS, FirOperation.SAFE_AS
            -> {
                TODO("Should not be here: incompatible operation in FirOperatorCall: $operation")
            }
        }
        val result = if (operation in UNARY_OPERATIONS) {
            primitiveOp1(startOffset, endOffset, symbol, type, origin, visitor.convertToIrExpression(arguments[0]))
        } else {
            val comparisonInfo = inferPrimitiveNumericComparisonInfo(arguments[0], arguments[1])
            val comparisonType = comparisonInfo?.comparisonType
            primitiveOp2(
                startOffset, endOffset, symbol, type, origin,
                visitor.convertToIrExpression(arguments[0]).asComparisonOperand(comparisonInfo?.leftType, comparisonType),
                visitor.convertToIrExpression(arguments[1]).asComparisonOperand(comparisonInfo?.rightType, comparisonType)
            )
        }
        if (operation !in NEGATED_OPERATIONS) return result
        return primitiveOp1(startOffset, endOffset, irBuiltIns.booleanNotSymbol, irBuiltIns.booleanType, origin, result)
    }

    private fun IrExpression.asComparisonOperand(
        operandType: ConeClassLikeType?,
        targetType: ConeClassLikeType?
    ): IrExpression {
        if (targetType == null) return this
        if (operandType == null) throw AssertionError("operandType should be non-null")

        val operandClassId = operandType.lookupTag.classId
        val targetClassId = targetType.lookupTag.classId
        if (operandClassId == targetClassId) return this
        val conversionFunction =
            typeConverter.classIdToSymbolMap[operandClassId]?.getSimpleFunction("to${targetType.lookupTag.classId.shortClassName.asString()}")
                ?: throw AssertionError("No conversion function for $operandType ~> $targetType")

        val dispatchReceiver = this@asComparisonOperand
        val unsafeIrCall = IrCallImpl(startOffset, endOffset, conversionFunction.owner.returnType, conversionFunction).also {
            it.dispatchReceiver = dispatchReceiver
        }
        return if (operandType.isNullable) {
            callGenerator.convertToSafeIrCall(unsafeIrCall, dispatchReceiver, isDispatch = true)
        } else {
            unsafeIrCall
        }
    }
}