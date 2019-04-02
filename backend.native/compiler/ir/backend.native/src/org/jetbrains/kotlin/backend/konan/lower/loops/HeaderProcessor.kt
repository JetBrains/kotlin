package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.isSubtypeOf
import org.jetbrains.kotlin.backend.konan.ir.typeWithStarProjections
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.isSimpleTypeWithQuestionMark
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Contains information about variables used in the loop. */
internal sealed class ForLoopHeader(
        val inductionVariable: IrVariable,
        val bound: IrVariable,
        val last: IrVariable,
        val step: IrVariable,
        val progressionType: ProgressionType
) {
    abstract fun initializeLoopVariable(symbols: KonanSymbols, builder: DeclarationIrBuilder): IrExpression

    abstract val declarations: List<IrStatement>

    abstract fun buildBody(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop
}

internal class ProgressionLoopHeader(
        headerInfo: ProgressionHeaderInfo,
        inductionVariable: IrVariable,
        bound: IrVariable,
        last: IrVariable,
        step: IrVariable,
        var loopVariable: IrVariable? = null
) : ForLoopHeader(inductionVariable, bound, last, step, headerInfo.progressionType) {

    val closed = headerInfo.closed

    private val increasing = headerInfo.increasing

    fun comparingFunction(builtIns: IrBuiltIns) = if (increasing)
        builtIns.lessOrEqualFunByOperandType[builtIns.int]
    else
        builtIns.greaterOrEqualFunByOperandType[builtIns.int]

    override fun initializeLoopVariable(symbols: KonanSymbols, builder: DeclarationIrBuilder) = with(builder) {
        irGet(inductionVariable)
    }

    override val declarations: List<IrStatement>
        get() = listOf(inductionVariable, step, bound, last)

    override fun buildBody(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with (builder) {
        assert(loopVariable != null)
        val newCondition = irCall(context.irBuiltIns.booleanNotSymbol).apply {
            dispatchReceiver = irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irGet(loopVariable!!))
                putValueArgument(1, irGet(last))
            }
        }
        IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
            label = loop.label
            condition = newCondition
            body = newBody
        }
    }
}

internal class ArrayLoopHeader(
        headerInfo: ArrayHeaderInfo,
        inductionVariable: IrVariable,
        bound: IrVariable,
        last: IrVariable,
        step: IrVariable
) : ForLoopHeader(inductionVariable, bound, last, step, ProgressionType.INT_PROGRESSION) {

    private val arrayDeclaration = headerInfo.arrayDeclaration

    override fun initializeLoopVariable(symbols: KonanSymbols, builder: DeclarationIrBuilder) = with(builder) {
        val callee = symbols.arrayGet[arrayDeclaration.type.classifierOrFail]!!
        irCall(callee).apply {
            dispatchReceiver = irGet(arrayDeclaration)
            putValueArgument(0, irGet(inductionVariable))
        }
    }

    override val declarations: List<IrStatement>
        get() = listOf(arrayDeclaration, inductionVariable, step, bound, last)

    override fun buildBody(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with (builder) {
        val builtIns = context.irBuiltIns
        val callee = builtIns.lessOrEqualFunByOperandType[builtIns.int]
        val newCondition = irCall(callee as IrFunctionSymbol).apply {
            putValueArgument(0, irGet(inductionVariable))
            putValueArgument(1, irGet(last))
        }
        IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
            label = loop.label
            condition = newCondition
            body = newBody
        }
    }
}

private fun ProgressionType.elementType(context: Context): IrType = when (this) {
    ProgressionType.INT_PROGRESSION -> context.irBuiltIns.intType
    ProgressionType.LONG_PROGRESSION -> context.irBuiltIns.longType
    ProgressionType.CHAR_PROGRESSION -> context.irBuiltIns.charType
}

// Given the for loop iterator variable, extract information about iterable subject
// and create ForLoopHeader from it.
internal class HeaderProcessor(
        private val context: Context,
        private val headerInfoBuilder: HeaderInfoBuilder,
        private val scopeOwnerSymbol: () -> IrSymbol) {

    private val symbols = context.ir.symbols

    private val progressionElementClasses = (symbols.integerClasses + symbols.char).toSet()

    fun processHeader(variable: IrVariable): ForLoopHeader? {

        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        val iteratorType = symbols.iterator.typeWithStarProjections

        if (!variable.type.isSubtypeOf(iteratorType)) {
            return null
        }
        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        // Collect loop info and form the loop header composite.
        val progressionInfo = headerInfoBuilder.build(variable)
                ?: return null
        if (progressionInfo is ArrayHeaderInfo) {
            progressionInfo.arrayDeclaration.parent = variable.parent
        }
        with(builder) {
            with(progressionInfo) {
                /**
                 * For this loop:
                 * `for (i in a() .. b() step c() step d())`
                 * We need to call functions in the following order: a, b, c, d.
                 * So we call b() before step calculations and then call last element calculation function (if required).
                 */
                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast them to non-nullable.
                val inductionVariable = scope.createTemporaryVariable(lowerBound.castIfNecessary(progressionType),
                        nameHint = "inductionVariable",
                        isMutable = true,
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)

                val upperBoundTmpVariable = scope.createTemporaryVariable(ensureNotNullable(upperBound.castIfNecessary(progressionType)),
                        nameHint = "upperBound",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)

                val stepExpression = if (step != null) {
                    ensureNotNullable(if (increasing) step else step.unaryMinus())
                } else {
                    defaultStep(startOffset, endOffset)
                }

                val stepValue = scope.createTemporaryVariable(stepExpression,
                        nameHint = "step",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)

                // Calculate the last element of the progression
                // The last element can be:
                //    boundValue, if step is 1 and the range is closed.
                //    boundValue - 1, if step is 1 and the range is open.
                //    getProgressionLast(inductionVariable, boundValue, step), if step != 1 and the range is closed.
                //    getProgressionLast(inductionVariable, boundValue - 1, step), if step != 1 and the range is open.
                val lastExpression = if (closed) {
                    irGet(upperBoundTmpVariable)
                } else {
                    val decrementSymbol = symbols.getUnaryOperator(OperatorNameConventions.DEC, upperBoundTmpVariable.type.toKotlinType())
                    irCall(decrementSymbol.owner).apply {
                        dispatchReceiver = irGet(upperBoundTmpVariable)
                    }
                }
                // In case of `step` we need to calculate the last element.
                val lastElement = if (needLastCalculation) {
                    irGetProgressionLast(progressionType, inductionVariable, lastExpression, stepValue)
                } else {
                    lastExpression
                }
                val lastValue = scope.createTemporaryVariable(lastElement,
                        nameHint = "last",
                        origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE)

                return when (progressionInfo) {
                    is ArrayHeaderInfo -> ArrayLoopHeader(
                            progressionInfo,
                            inductionVariable,
                            upperBoundTmpVariable,
                            lastValue,
                            stepValue)
                    is ProgressionHeaderInfo -> ProgressionLoopHeader(
                            progressionInfo,
                            inductionVariable,
                            upperBoundTmpVariable,
                            lastValue,
                            stepValue)
                }
            }
        }
    }

    private fun IrExpression.castIfNecessary(progressionType: ProgressionType): IrExpression {
        assert(type.classifierOrNull in progressionElementClasses)
        return if (type.classifierOrNull == progressionType.elementType(context).classifierOrNull) {
            this
        } else {
            val function = symbols.getNoParamFunction(progressionType.numberCastFunctionName, type.toKotlinType())
            IrCallImpl(startOffset, endOffset, function.owner.returnType, function)
                    .apply { dispatchReceiver = this@castIfNecessary }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
            if (expression.type.isSimpleTypeWithQuestionMark) {
                irImplicitCast(expression, expression.type.makeNotNull())
            } else {
                expression
            }

    private fun IrExpression.unaryMinus(): IrExpression {
        val unaryOperator = symbols.getUnaryOperator(OperatorNameConventions.UNARY_MINUS, type.toKotlinType())
        return IrCallImpl(startOffset, endOffset, unaryOperator.owner.returnType, unaryOperator).apply {
            dispatchReceiver = this@unaryMinus
        }
    }

    private fun HeaderInfo.defaultStep(startOffset: Int, endOffset: Int): IrExpression {
        val type = progressionType.elementType(context)
        val step = if (increasing) 1 else -1
        return when {
            type.isInt() || type.isChar() ->
                IrConstImpl.int(startOffset, endOffset, context.irBuiltIns.intType, step)
            type.isLong() ->
                IrConstImpl.long(startOffset, endOffset, context.irBuiltIns.longType, step.toLong())
            else -> throw IllegalArgumentException()
        }
    }

    private fun irGetProgressionLast(progressionType: ProgressionType,
                                     first: IrVariable,
                                     lastExpression: IrExpression,
                                     step: IrVariable): IrExpression {
        val symbol = symbols.getProgressionLast[progressionType.elementType(context).toKotlinType()]
                ?: throw IllegalArgumentException("No `getProgressionLast` for type ${step.type} ${lastExpression.type}")
        val startOffset = lastExpression.startOffset
        val endOffset = lastExpression.endOffset
        return IrCallImpl(startOffset, endOffset, symbol.owner.returnType, symbol).apply {
            putValueArgument(0, IrGetValueImpl(startOffset, endOffset, first.type, first.symbol))
            putValueArgument(1, lastExpression)
            putValueArgument(2, IrGetValueImpl(startOffset, endOffset, step.type, step.symbol))
        }
    }
}
