/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.Name

/** Contains information about variables used in the loop. */
internal sealed class ForLoopHeader(
    val inductionVariable: IrVariable,
    val last: IrVariable,
    val step: IrVariable,
    val progressionType: ProgressionType,
    var loopVariable: IrVariable? = null
) {
    /** Expression used to initialize the loop variable at the beginning of the loop. */
    abstract fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder): IrExpression

    /** Declarations used in the loop condition and body (e.g., induction variable). */
    abstract val declarations: List<IrStatement>

    /** Builds a new loop from the old loop. */
    abstract fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop

    /**
     * A condition used in a check surrounding the loop checking for an empty loop. The expression
     * should evaluate to true if the loop is NOT empty.
     *
     * Returns null if no check is needed for the for-loop.
     */
    abstract fun buildNotEmptyConditionIfNecessary(builder: DeclarationIrBuilder): IrExpression?
}

internal class ProgressionLoopHeader(
    private val headerInfo: ProgressionHeaderInfo,
    inductionVariable: IrVariable,
    last: IrVariable,
    step: IrVariable
) : ForLoopHeader(inductionVariable, last, step, headerInfo.progressionType) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        // loopVariable = inductionVariable
        irGet(inductionVariable)
    }

    override val declarations: List<IrStatement>
        get() = headerInfo.additionalVariables + listOf(inductionVariable, last, step)

    override fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with(builder) {
        // Condition: loopVariable != last
        assert(loopVariable != null)
        val booleanNotFun = context.irBuiltIns.booleanClass.functions.first { it.owner.name.asString() == "not" }
        val newCondition = irCallOp(booleanNotFun, booleanNotFun.owner.returnType, irCall(context.irBuiltIns.eqeqSymbol).apply {
            putValueArgument(0, irGet(loopVariable!!))
            putValueArgument(1, irGet(last))
        })


        // TODO: Build while loop (instead of do-while) where possible, e.g., in "until" ranges, or when "last" is known to be < MAX_VALUE
        IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
            label = loop.label
            condition = newCondition
            body = newBody
        }
    }

    override fun buildNotEmptyConditionIfNecessary(builder: DeclarationIrBuilder): IrExpression? =
        with(builder) {
            val builtIns = context.irBuiltIns
            val progressionKotlinType = progressionType.elementType(builtIns).toKotlinType()
            val lessOrEqualFun = builtIns.lessOrEqualFunByOperandType[progressionKotlinType]!!

            // The default "not empty" condition depends on the direction.
            val notEmptyCondition = when (headerInfo.direction) {
                ProgressionDirection.DECREASING ->
                    // last <= inductionVariable
                    irCall(lessOrEqualFun).apply {
                        putValueArgument(0, irGet(last))
                        putValueArgument(1, irGet(inductionVariable))
                    }
                ProgressionDirection.INCREASING ->
                    // inductionVariable <= last
                    irCall(lessOrEqualFun).apply {
                        putValueArgument(0, irGet(inductionVariable))
                        putValueArgument(1, irGet(last))
                    }
                ProgressionDirection.UNKNOWN -> {
                    // If the direction is unknown, we check depending on the "step" value:
                    //   (step > 0 && inductionVariable <= last) || (step < 0 || last <= inductionVariable)
                    val stepKotlinType = progressionType.stepType(builtIns).toKotlinType()
                    val zero = if (progressionType == ProgressionType.LONG_PROGRESSION) irLong(0) else irInt(0)
                    context.oror(
                        context.andand(
                            irCall(builtIns.greaterFunByOperandType[stepKotlinType]!!).apply {
                                putValueArgument(0, irGet(step))
                                putValueArgument(1, zero)
                            },
                            irCall(lessOrEqualFun).apply {
                                putValueArgument(0, irGet(inductionVariable))
                                putValueArgument(1, irGet(last))
                            }),
                        context.andand(
                            irCall(builtIns.lessFunByOperandType[stepKotlinType]!!).apply {
                                putValueArgument(0, irGet(step))
                                putValueArgument(1, zero)
                            },
                            irCall(lessOrEqualFun).apply {
                                putValueArgument(0, irGet(last))
                                putValueArgument(1, irGet(inductionVariable))
                            })
                    )
                }
            }

            if (headerInfo.additionalNotEmptyCondition != null) context.andand(
                headerInfo.additionalNotEmptyCondition,
                notEmptyCondition
            ) else notEmptyCondition
        }
}

internal class ArrayLoopHeader(
    private val headerInfo: ArrayHeaderInfo,
    inductionVariable: IrVariable,
    last: IrVariable,
    step: IrVariable
) : ForLoopHeader(inductionVariable, last, step, ProgressionType.INT_PROGRESSION) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        // loopVariable = array[inductionVariable]
        val arrayGetFun = headerInfo.arrayVariable.type.getClass()!!.functions.first { it.name.asString() == "get" }
        irCall(arrayGetFun).apply {
            dispatchReceiver = irGet(headerInfo.arrayVariable)
            putValueArgument(0, irGet(inductionVariable))
        }
    }

    override val declarations: List<IrStatement>
        get() = listOf(headerInfo.arrayVariable, inductionVariable, last, step)

    override fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with(builder) {
        // Condition: loopVariable != last
        val builtIns = context.irBuiltIns
        val callee = builtIns.lessOrEqualFunByOperandType[builtIns.int]!!
        val newCondition = irCall(callee).apply {
            putValueArgument(0, irGet(inductionVariable))
            putValueArgument(1, irGet(last))
        }

        IrWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
            label = loop.label
            condition = newCondition
            body = newBody
        }
    }

    // No surrounding emptiness check is needed with a while loop.
    override fun buildNotEmptyConditionIfNecessary(builder: DeclarationIrBuilder): IrExpression? = null
}

/**
 * Given the for-loop iterator variable, extract information about the iterable subject
 * and create a [ForLoopHeader] from it.
 */
internal class HeaderProcessor(
    private val context: CommonBackendContext,
    private val headerInfoBuilder: HeaderInfoBuilder,
    private val scopeOwnerSymbol: () -> IrSymbol
) {

    private val symbols = context.ir.symbols

    /**
     * Extracts information for building the for-loop (as a [ForLoopHeader]) from the given
     * "header" statement that stores the iterator into the loop variable
     * (e.g., `val it = someIterable.iterator()`).
     *
     * Returns null if the for-loop cannot be lowered.
     */
    fun processHeader(variable: IrVariable): ForLoopHeader? {
        // Verify the variable type is a subtype of Iterator<*>.
        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        if (!variable.type.isSubtypeOfClass(symbols.iterator)) {
            return null
        }

        // Collect loop information.
        val headerInfo = headerInfoBuilder.build(variable)
            ?: return null  // If the iterable is not supported.

        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        with(builder) builder@{
            with(headerInfo) {
                // For this loop:
                //
                // ```
                // for (i in first()..last() step step())
                // ```
                //
                // ...the functions may have side-effects so we need to call them in the following
                // order: first(), last(), step().
                //
                // We also need to cast them to conform to the progression type so that operations
                // in the induction variable within the loop are more efficient.
                //
                // In the above example, if first() is a Long and last() is an Int, this creates a
                // LongProgression so last(), should be cast to a Long.
                val inductionVariable = scope.createTemporaryVariable(
                    first.castIfNecessary(
                        progressionType.elementType(context.irBuiltIns),
                        progressionType.elementCastFunctionName
                    ),
                    nameHint = "inductionVariable",
                    isMutable = true,
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
                )

                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast
                // them to non-nullable.
                // TODO: Confirm if casting to non-nullable is still necessary
                val lastValue = scope.createTemporaryVariable(
                    ensureNotNullable(
                        last.castIfNecessary(
                            progressionType.elementType(context.irBuiltIns),
                            progressionType.elementCastFunctionName
                        )
                    ),
                    nameHint = "last",
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
                )

                val stepValue = scope.createTemporaryVariable(
                    ensureNotNullable(
                        step.castIfNecessary(
                            progressionType.stepType(context.irBuiltIns),
                            progressionType.stepCastFunctionName
                        )
                    ),
                    nameHint = "step",
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
                )

                return when (headerInfo) {
                    is ArrayHeaderInfo -> ArrayLoopHeader(
                        headerInfo,
                        inductionVariable,
                        lastValue,
                        stepValue
                    )
                    is ProgressionHeaderInfo -> ProgressionLoopHeader(
                        headerInfo,
                        inductionVariable,
                        lastValue,
                        stepValue
                    )
                }
            }
        }
    }
}

internal fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
    if (expression.type is IrSimpleType && expression.type.isNullable()) {
        irImplicitCast(expression, expression.type.makeNotNull())
    } else {
        expression
    }

internal fun IrExpression.castIfNecessary(targetType: IrType, numberCastFunctionName: Name): IrExpression {
    return if (type.toKotlinType() == targetType.toKotlinType()) {
        this
    } else {
        val function = type.getClass()!!.functions.first { it.name == numberCastFunctionName }
        IrCallImpl(startOffset, endOffset, function.returnType, function.symbol)
            .apply { dispatchReceiver = this@castIfNecessary }
    }
}