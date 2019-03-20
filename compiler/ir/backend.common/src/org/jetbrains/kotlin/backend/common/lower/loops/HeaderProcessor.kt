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
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable

/** Contains information about variables used in the loop. */
internal sealed class ForLoopHeader(
    val inductionVariable: IrVariable,
    val last: IrVariable,
    val step: IrVariable,
    val progressionType: ProgressionType,
    val needsEmptinessCheck: Boolean,
    var loopVariable: IrVariable? = null
) {
    abstract fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder): IrExpression

    abstract val declarations: List<IrStatement>

    abstract fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop

    abstract fun buildNotEmptyCondition(builder: DeclarationIrBuilder): IrExpression?
}

internal class ProgressionLoopHeader(
    private val headerInfo: ProgressionHeaderInfo,
    inductionVariable: IrVariable,
    last: IrVariable,
    step: IrVariable
) : ForLoopHeader(inductionVariable, last, step, headerInfo.progressionType, needsEmptinessCheck = true) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        irGet(inductionVariable)
    }

    override val declarations: List<IrStatement>
        get() = headerInfo.additionalVariables + listOf(inductionVariable, step, last)

    override fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with(builder) {
        assert(loopVariable != null)
        val newCondition = irCall(context.irBuiltIns.booleanNotSymbol).apply {
            putValueArgument(0, irCall(context.irBuiltIns.eqeqSymbol).apply {
                putValueArgument(0, irGet(loopVariable!!))
                putValueArgument(1, irGet(last))
            })
        }
        IrDoWhileLoopImpl(loop.startOffset, loop.endOffset, loop.type, loop.origin).apply {
            label = loop.label
            condition = newCondition
            body = newBody
        }
    }

    override fun buildNotEmptyCondition(builder: DeclarationIrBuilder): IrExpression? =
        with(builder) {
            val builtIns = context.irBuiltIns
            val progressionKotlinType = progressionType.elementType(builtIns).toKotlinType()
            val lessOrEqualFun = builtIns.lessOrEqualFunByOperandType[progressionKotlinType]!!

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
) : ForLoopHeader(inductionVariable, last, step, ProgressionType.INT_PROGRESSION, needsEmptinessCheck = false) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        val arrayClass = (headerInfo.arrayVariable.type.classifierOrNull) as IrClassSymbol
        val arrayGetFun = arrayClass.owner.functions.find { it.name.toString() == "get" }!!
        irCall(arrayGetFun).apply {
            dispatchReceiver = irGet(headerInfo.arrayVariable)
            putValueArgument(0, irGet(inductionVariable))
        }
    }

    override val declarations: List<IrStatement>
        get() = listOf(headerInfo.arrayVariable, inductionVariable, step, last)

    override fun buildInnerLoop(builder: DeclarationIrBuilder, loop: IrLoop, newBody: IrExpression?): IrLoop = with(builder) {
        val builtIns = context.irBuiltIns
        val callee = builtIns.lessOrEqualFunByOperandType[builtIns.int]?.symbol!!
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
    override fun buildNotEmptyCondition(builder: DeclarationIrBuilder): IrExpression? = null
}

// Given the for loop iterator variable, extract information about iterable subject
// and create ForLoopHeader from it.
internal class HeaderProcessor(
    private val context: CommonBackendContext,
    private val headerInfoBuilder: HeaderInfoBuilder,
    private val scopeOwnerSymbol: () -> IrSymbol
) {

    private val symbols = context.ir.symbols

    fun processHeader(variable: IrVariable): ForLoopHeader? {

        assert(variable.origin == IrDeclarationOrigin.FOR_LOOP_ITERATOR)
        if (!variable.type.isSubtypeOfClass(symbols.iterator)) {
            return null
        }

        // Collect loop information.
        val headerInfo = headerInfoBuilder.build(variable)
            ?: return null // If the iterable is not supported.

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
                    first.castIfNecessary(progressionType),
                    nameHint = "inductionVariable",
                    isMutable = true,
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
                )

                // Due to features of PSI2IR we can obtain nullable arguments here while actually
                // they are non-nullable (the frontend takes care about this). So we need to cast
                // them to non-nullable.
                // TODO: Confirm if casting to non-nullable is still necessary
                val lastValue = scope.createTemporaryVariable(
                    ensureNotNullable(last.castIfNecessary(progressionType)),
                    nameHint = "last",
                    origin = IrDeclarationOrigin.FOR_LOOP_IMPLICIT_VARIABLE
                )

                val stepValue = scope.createTemporaryVariable(
                    ensureNotNullable(step),
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


    private fun IrExpression.castIfNecessary(progressionType: ProgressionType): IrExpression {
        return if (type.toKotlinType() == progressionType.elementType(context.irBuiltIns).toKotlinType()) {
            this
        } else {
            val function = type.getClass()!!.functions.first { it.name == progressionType.numberCastFunctionName }
            IrCallImpl(startOffset, endOffset, function.returnType, function.symbol)
                .apply { dispatchReceiver = this@castIfNecessary }
        }
    }

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
        if (expression.type is IrSimpleType && expression.type.isNullable()) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }
}