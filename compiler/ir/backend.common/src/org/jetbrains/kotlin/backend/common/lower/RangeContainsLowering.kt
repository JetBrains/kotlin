/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.loops.*
import org.jetbrains.kotlin.backend.common.lower.loops.handlers.*
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.andand
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

val rangeContainsLoweringPhase = makeIrFilePhase(
    ::RangeContainsLowering,
    name = "RangeContainsLowering",
    description = "Optimizes calls to contains() for ClosedRanges"
)

/**
 * This lowering pass optimizes calls to contains() (`in` operator) for ClosedRanges.
 *
 * For example, the expression `X in A..B` is transformed into `A <= X && X <= B`.
 */
class RangeContainsLowering(val context: CommonBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = Transformer(context, container as IrSymbolOwner)
        irBody.transformChildrenVoid(transformer)
    }
}

private class Transformer(
    val context: CommonBackendContext,
    val container: IrSymbolOwner
) : IrElementTransformerVoidWithContext() {
    private val headerInfoBuilder = RangeHeaderInfoBuilder(context, this::getScopeOwnerSymbol)
    fun getScopeOwnerSymbol() = currentScope?.scope?.scopeOwnerSymbol ?: container.symbol

    private fun matchStdlibExtensionContainsCall(expression: IrCall): Boolean {
        val callee = expression.symbol.owner
        return callee.valueParameters.size == 1 &&
                callee.extensionReceiverParameter?.type?.isSubtypeOfClass(context.ir.symbols.closedRange) == true &&
                callee.kotlinFqName == FqName("kotlin.ranges.${OperatorNameConventions.CONTAINS}")
    }

    override fun visitCall(expression: IrCall): IrExpression {
        // The call to contains() in `5 in 0..10` has origin=IN:
        //
        //   CALL 'public open fun contains (value: kotlin.Int): kotlin.Boolean [operator] declared in kotlin.ranges.IntRange' type=kotlin.Boolean origin=IN
        //
        // And when `!in` is used in `5 !in 0..10`, _both_ the not() and contains() calls have origin=NOT_IN:
        //
        //   CALL 'public final fun not (): kotlin.Boolean [operator] declared in kotlin.Boolean' type=kotlin.Boolean origin=NOT_IN
        //     $this: CALL 'public open fun contains (value: kotlin.Int): kotlin.Boolean [operator] declared in kotlin.ranges.IntRange' type=kotlin.Boolean origin=NOT_IN
        //
        // We only want to lower the call to contains(); in the `!in` case, the call to not() should be preserved.
        val origin = expression.origin
        if (origin != IrStatementOrigin.IN && origin != IrStatementOrigin.NOT_IN) {
            return super.visitCall(expression)  // The call is not an `in` expression.
        }
        if (origin == IrStatementOrigin.NOT_IN && expression.symbol == context.irBuiltIns.booleanNotSymbol) {
            return super.visitCall(expression)  // Preserve the call to not().
        }

        if (expression.extensionReceiver != null && !matchStdlibExtensionContainsCall(expression)) {
            // We can only optimize calls to the stdlib extension functions and not a user-defined extension.
            // TODO: This breaks the optimization for *Range.reversed().contains(). The function called there is the extension function
            // Iterable.contains(). Figure out if we can safely match on that as well.
            return super.visitCall(expression)
        }

        // The HeaderInfoBuilder extracts information (e.g., lower/upper bounds, direction) from the range expression, which is the
        // receiver for the contains() call.
        val receiver = expression.dispatchReceiver ?: expression.extensionReceiver
        val headerInfo = receiver?.accept(headerInfoBuilder, expression)
            ?: return super.visitCall(expression)  // The receiver is not a supported range (or not a range at all).

        val argument = expression.getValueArgument(0)!!
        if (argument.type.isNullable()) {
            // There are stdlib extension functions that return false for null arguments, e.g., IntRange.contains(Int?). We currently
            // do not optimize such calls.
            return super.visitCall(expression)
        }

        val builder = context.createIrBuilder(getScopeOwnerSymbol(), expression.startOffset, expression.endOffset)
        return builder.buildContainsComparison(headerInfo, argument, origin) ?: super.visitCall(expression)  // The call cannot be lowered.
    }

    private fun DeclarationIrBuilder.buildContainsComparison(
        headerInfo: HeaderInfo,
        argument: IrExpression,
        origin: IrStatementOrigin
    ): IrExpression? {
        // If the lower bound of the range is A, the upper bound is B, and the argument is X, the contains() call is generally transformed
        // into `A <= X && X <= B`. However, when any of these expressions (A/B/X) can have side-effects, they must resolve in the order
        // in the expression. E.g., for `X in A..B` the order is A -> B -> X (the equivalent call is `(A..B).contains(X)`), and for
        // `X in B downTo A` the order is B -> A -> X (the equivalent call is `(B.downTo(A)).contains(X)`).
        // Therefore, we need to know in which order the expressions appear in the contains() expression. `shouldUpperComeFirst` is true
        // when the expression or variable for `B` (upper) should appear in the lowered IR before `A` (lower).

        val lower: IrExpression
        val upper: IrExpression
        val isUpperInclusive: Boolean
        val shouldUpperComeFirst: Boolean
        val useCompareTo: Boolean
        val isNumericRange: Boolean
        val additionalStatements = mutableListOf<IrStatement>()

        when (headerInfo) {
            is NumericHeaderInfo -> {
                when (headerInfo) {
                    is ProgressionHeaderInfo -> {
                        additionalStatements.addAll(headerInfo.additionalStatements)
                    }
                    // None of the handlers in RangeHeaderInfoBuilder should return a IndexedGetHeaderInfo (those are only for loops).
                    is IndexedGetHeaderInfo -> error("Unexpected IndexedGetHeaderInfo returned by RangeHeaderInfoBuilder")
                }

                // TODO: Optimize contains() for progressions with |step| > 1 or unknown step and/or direction. These are also not optimized
                // in the old JVM backend. contains(x) for a stepped progression returns true if it is one of the elements/steps in the
                // progression. e.g., `4 in 0..10 step 2` is true, and `3 in 0..10 step 2` is false. This requires an additional condition
                // with a modulo.
                when (headerInfo.direction) {
                    ProgressionDirection.INCREASING -> {
                        if (headerInfo.step.constLongValue != 1L) return null

                        // There are 2 cases for an optimizable range with INCREASING direction:
                        //   1. `X in A..B`:
                        //      Produces HeaderInfo with first = A, last = B, isReversed = false (`first/A` is lower).
                        //      Expression for `lower/A` should come first.
                        //   2. `X in (B downTo A).reversed()`:
                        //      Produces HeaderInfo with first = A, last = B, isReversed = true (`first/A` is lower).
                        //      Expression for `upper/B` should come first.
                        lower = headerInfo.first
                        upper = headerInfo.last
                        shouldUpperComeFirst = headerInfo.isReversed
                    }
                    ProgressionDirection.DECREASING -> {
                        if (headerInfo.step.constLongValue != -1L) return null

                        // There are 2 cases for an optimizable range with DECREASING direction:
                        //   1. `X in B downTo A`:
                        //      Produces HeaderInfo with first = B, last = A, isReversed = false (`last/A` is lower).
                        //      Expression for `upper/B` should come first.
                        //   2. `X in (A..B).reversed()`:
                        //      Produces HeaderInfo with first = B, last = A, isReversed = true (`last/A` is lower).
                        //      Expression for `lower/A` should come first.
                        lower = headerInfo.last
                        upper = headerInfo.first
                        shouldUpperComeFirst = !headerInfo.isReversed
                    }
                    ProgressionDirection.UNKNOWN -> return null
                }

                // `compareTo` must be used for UInt/ULong; they don't have intrinsic comparison operators.
                useCompareTo = headerInfo.progressionType is UnsignedProgressionType
                isUpperInclusive = headerInfo.isLastInclusive
                isNumericRange = true
            }
            is FloatingPointRangeHeaderInfo -> {
                lower = headerInfo.start
                upper = headerInfo.endInclusive
                isUpperInclusive = true
                shouldUpperComeFirst = false
                useCompareTo = false
                isNumericRange = true
            }
            is ComparableRangeInfo -> {
                lower = headerInfo.start
                upper = headerInfo.endInclusive
                isUpperInclusive = true
                shouldUpperComeFirst = false
                useCompareTo = true
                isNumericRange = false
            }
            else -> return null
        }

        // The transformed expression is `A <= X && X <= B`. If the argument expression X can have side effects, it must be stored in a
        // temp variable before the expression so it does not get evaluated twice. If A and/or B can have side effects, they must also be
        // stored in temp variables BEFORE X.
        //
        // On the other hand, if X can NOT have side effects, it does NOT need to be stored in a temp variable. However, because of
        // short-circuit evaluation of &&, if A and/or B can have side effects, we need to make sure they get evaluated regardless.
        // We accomplish this be storing it in a temp variable (the alternative is to duplicate A/B in a block in the "else" branch before
        // returning false). We can also switch the order of the clauses to ensure evaluation. See below for the expected outcomes:
        //
        //   =======|=======|=======|======================|================|=======================
        //   Can have side effects? | (Note B is "upper")  |                |
        //      X   |   A   |   B   | shouldUpperComeFirst | Temp var order | Transformed expression
        //   =======|=======|=======|======================|================|=======================
        //    True  | True  | True  | False                | A -> B -> X    | A <= X && X <= B *
        //    True  | True  | True  | True                 | B -> A -> X    | A <= X && X <= B *
        //    True  | True  | False | False/True           | A -> X         | A <= X && X <= B *
        //    True  | False | True  | False/True           | B -> X         | A <= X && X <= B *
        //    True  | False | False | False/True           | X              | A <= X && X <= B *
        //   -------|-------|-------|----------------------|----------------|-----------------------
        //    False | True  | True  | False                | A **           | X <= B && A <= X ***
        //    False | True  | True  | True                 | B **           | A <= X && X <= B ***
        //    False | True  | False | False/True           | [None]         | A <= X && X <= B ***
        //    False | False | True  | False/True           | [None]         | X <= B && A <= X ***
        //    False | False | False | False/True           | [None]         | A <= X && X <= B *
        //   =======|=======|=======|======================|================|=======================
        //
        // *   - Order does not matter.
        // **  - Bound with side effect is stored in a temp variable to ensure evaluation even if right side is short-circuited.
        // *** - Bound with side effect is on left side of && to make sure it always gets evaluated.

        var arg = argument
        val builtIns = context.irBuiltIns
        val comparisonClass = if (isNumericRange) {
            computeComparisonClass(this@Transformer.context.ir.symbols, lower.type, upper.type, arg.type) ?: return null
        } else {
            assert(headerInfo is ComparableRangeInfo)
            this@Transformer.context.ir.symbols.comparable.owner
        }

        if (isNumericRange) {
            // Convert argument to the "widest" common numeric type for comparisons.
            // Note that we do the same for the bounds below. If it is necessary to convert the argument, it's better to do it once and
            // store in a temp variable, since it is used twice in the transformed expression (bounds are only used once).
            arg = arg.castIfNecessary(comparisonClass)
        }

        val (argVar, argExpression) = createTemporaryVariableIfNecessary(arg, "containsArg")
        var lowerExpression: IrExpression
        var upperExpression: IrExpression
        val useLowerClauseOnLeftSide: Boolean
        if (argVar != null) {
            val (lowerVar, tmpLowerExpression) = createTemporaryVariableIfNecessary(lower, "containsLower")
            val (upperVar, tmpUpperExpression) = createTemporaryVariableIfNecessary(upper, "containsUpper")
            if (shouldUpperComeFirst) {
                additionalStatements.addIfNotNull(upperVar)
                additionalStatements.addIfNotNull(lowerVar)
            } else {
                additionalStatements.addIfNotNull(lowerVar)
                additionalStatements.addIfNotNull(upperVar)
            }
            lowerExpression = tmpLowerExpression.shallowCopy()
            upperExpression = tmpUpperExpression.shallowCopy()
            useLowerClauseOnLeftSide = true
        } else if (lower.canHaveSideEffects && upper.canHaveSideEffects) {
            if (shouldUpperComeFirst) {
                val (upperVar, tmpUpperExpression) = createTemporaryVariableIfNecessary(upper, "containsUpper")
                additionalStatements.add(upperVar!!)
                lowerExpression = lower
                upperExpression = tmpUpperExpression.shallowCopy()
                useLowerClauseOnLeftSide = true
            } else {
                val (lowerVar, tmpLowerExpression) = createTemporaryVariableIfNecessary(lower, "containsLower")
                additionalStatements.add(lowerVar!!)
                lowerExpression = tmpLowerExpression.shallowCopy()
                upperExpression = upper
                useLowerClauseOnLeftSide = false
            }
        } else {
            lowerExpression = lower
            upperExpression = upper
            useLowerClauseOnLeftSide = true
        }
        additionalStatements.addIfNotNull(argVar)

        if (isNumericRange) {
            lowerExpression = lowerExpression.castIfNecessary(comparisonClass)
            upperExpression = upperExpression.castIfNecessary(comparisonClass)
        }

        val lowerCompFun = builtIns.lessOrEqualFunByOperandType.getValue(if (useCompareTo) builtIns.intClass else comparisonClass.symbol)
        val upperCompFun = if (isUpperInclusive) {
            builtIns.lessOrEqualFunByOperandType
        } else {
            builtIns.lessFunByOperandType
        }.getValue(if (useCompareTo) builtIns.intClass else comparisonClass.symbol)
        val compareToFun = comparisonClass.functions.singleOrNull {
            it.name == OperatorNameConventions.COMPARE_TO &&
                    it.dispatchReceiverParameter != null && it.extensionReceiverParameter == null &&
                    it.valueParameters.size == 1 && (!isNumericRange || it.valueParameters[0].type == comparisonClass.defaultType)
        } ?: return null

        // contains() function for ComparableRange is implemented as `value >= start && value <= endInclusive` (`value` is the argument).
        // Therefore the dispatch receiver for the compareTo() calls should be the argument. This is important since the implementation
        // for compareTo() may have side effects dependent on which expressions are the receiver and argument
        // (see evaluationOrderForComparableRange.kt test).
        val lowerClause = if (useCompareTo) {
            irCall(lowerCompFun).apply {
                putValueArgument(0, irInt(0))
                putValueArgument(1, irCall(compareToFun).apply {
                    dispatchReceiver = argExpression.shallowCopy()
                    putValueArgument(0, lowerExpression)
                })
            }
        } else {
            irCall(lowerCompFun).apply {
                putValueArgument(0, lowerExpression)
                putValueArgument(1, argExpression.shallowCopy())
            }
        }
        val upperClause = if (useCompareTo) {
            irCall(upperCompFun).apply {
                putValueArgument(0, irCall(compareToFun).apply {
                    dispatchReceiver = argExpression.shallowCopy()
                    putValueArgument(0, upperExpression)
                })
                putValueArgument(1, irInt(0))
            }
        } else {
            irCall(upperCompFun).apply {
                putValueArgument(0, argExpression.shallowCopy())
                putValueArgument(1, upperExpression)
            }
        }

        val contains = context.andand(
            if (useLowerClauseOnLeftSide) lowerClause else upperClause,
            if (useLowerClauseOnLeftSide) upperClause else lowerClause,
            origin
        )
        return if (additionalStatements.isEmpty()) {
            contains
        } else {
            irBlock {
                for (stmt in additionalStatements) {
                    +stmt
                }
                +contains
            }
        }
    }

    private fun computeComparisonClass(
        symbols: Symbols,
        lowerType: IrType,
        upperType: IrType,
        argumentType: IrType
    ): IrClass? {
        val commonBoundType = leastCommonPrimitiveNumericType(symbols, lowerType, upperType) ?: return null
        return leastCommonPrimitiveNumericType(symbols, argumentType, commonBoundType)?.getClass()
    }

    private fun leastCommonPrimitiveNumericType(symbols: Symbols, t1: IrType, t2: IrType): IrType? {
        val primitive1 = t1.getPrimitiveType()
        val primitive2 = t2.getPrimitiveType()
        val unsigned1 = t1.getUnsignedType()
        val unsigned2 = t2.getUnsignedType()

        return when {
            primitive1 == PrimitiveType.DOUBLE || primitive2 == PrimitiveType.DOUBLE -> symbols.double
            primitive1 == PrimitiveType.FLOAT || primitive2 == PrimitiveType.FLOAT -> symbols.float
            unsigned1 == UnsignedType.ULONG || unsigned2 == UnsignedType.ULONG -> symbols.uLong!!
            unsigned1.isPromotableToUInt() || unsigned2.isPromotableToUInt() -> symbols.uInt!!
            primitive1 == PrimitiveType.LONG || primitive2 == PrimitiveType.LONG -> symbols.long
            primitive1.isPromotableToInt() || primitive2.isPromotableToInt() -> symbols.int
            primitive1 == PrimitiveType.CHAR || primitive2 == PrimitiveType.CHAR -> symbols.char
            else -> error("Unexpected types: t1=${t1.render()}, t2=${t2.render()}")
        }.defaultType
    }

    private fun PrimitiveType?.isPromotableToInt(): Boolean =
        this == PrimitiveType.INT || this == PrimitiveType.SHORT || this == PrimitiveType.BYTE

    private fun UnsignedType?.isPromotableToUInt(): Boolean =
        this == UnsignedType.UINT || this == UnsignedType.USHORT || this == UnsignedType.UBYTE
}

internal open class RangeHeaderInfoBuilder(context: CommonBackendContext, scopeOwnerSymbol: () -> IrSymbol) :
    HeaderInfoBuilder(context, scopeOwnerSymbol, allowUnsignedBounds = true) {

    override val progressionHandlers = listOf(
        CollectionIndicesHandler(context),
        ArrayIndicesHandler(context),
        CharSequenceIndicesHandler(context),
        UntilHandler(context),
        RangeUntilHandler(context),
        DownToHandler(context),
        RangeToHandler(context)
    )

    override val callHandlers = listOf(
        FloatingPointRangeToHandler,
        ComparableRangeToHandler(context),
        ReversedHandler(context, this)
    )

    override val expressionHandlers = listOf(DefaultProgressionHandler(context, allowUnsignedBounds = true))
}

/** Builds a [HeaderInfo] for closed floating-point ranges built using the `rangeTo` function. */
internal object FloatingPointRangeToHandler : HeaderInfoHandler<IrCall, Nothing?> {
    override fun matchIterable(expression: IrCall): Boolean {
        val callee = expression.symbol.owner
        return callee.valueParameters.singleOrNull()?.type?.let { it.isFloat() || it.isDouble() } == true &&
                callee.extensionReceiverParameter?.type?.let { it.isFloat() || it.isDouble() } == true &&
                callee.kotlinFqName == FqName("kotlin.ranges.${OperatorNameConventions.RANGE_TO}")
    }

    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        FloatingPointRangeHeaderInfo(
            start = expression.extensionReceiver!!,
            endInclusive = expression.getValueArgument(0)!!
        )
}

/** Builds a [HeaderInfo] for ranges of Comparables built using the `rangeTo` extension function. */
internal class ComparableRangeToHandler(private val context: CommonBackendContext) : HeaderInfoHandler<IrCall, Nothing?> {
    override fun matchIterable(expression: IrCall): Boolean {
        val callee = expression.symbol.owner
        return callee.valueParameters.size == 1 &&
                callee.extensionReceiverParameter?.type?.isSubtypeOfClass(context.ir.symbols.comparable) == true &&
                callee.kotlinFqName == FqName("kotlin.ranges.${OperatorNameConventions.RANGE_TO}")
    }

    override fun build(expression: IrCall, data: Nothing?, scopeOwner: IrSymbol) =
        ComparableRangeInfo(
            start = expression.extensionReceiver!!,
            endInclusive = expression.getValueArgument(0)!!
        )
}
