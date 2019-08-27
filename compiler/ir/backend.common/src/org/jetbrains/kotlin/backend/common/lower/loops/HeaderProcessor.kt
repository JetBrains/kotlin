/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.common.ir.isTopLevel
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrDoWhileLoopImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrWhileLoopImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * Contains the loop and expression to replace the old loop.
 *
 * @param newLoop The new loop.
 * @param replacementExpression The expression to use in place of the old loop. It is either `newLoop`, or a container
 * that contains `newLoop`.
 */
internal data class LoopReplacement(
    val newLoop: IrLoop,
    val replacementExpression: IrExpression
)

/** Contains information about variables used in the loop. */
internal sealed class ForLoopHeader(
    protected open val headerInfo: HeaderInfo,
    val inductionVariable: IrVariable,
    val last: IrVariable,
    val step: IrVariable,
    var loopVariable: IrVariable? = null,
    val isLastInclusive: Boolean
) {
    /** Expression used to initialize the loop variable at the beginning of the loop. */
    abstract fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder): IrExpression

    /** Declarations used in the loop condition and body (e.g., induction variable). */
    abstract val declarations: List<IrStatement>

    /** Builds a new loop from the old loop. */
    abstract fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement

    /** Statement used to increment the induction variable. */
    fun incrementInductionVariable(builder: DeclarationIrBuilder): IrStatement = with(builder) {
        // inductionVariable = inductionVariable + step
        val plusFun = inductionVariable.type.getClass()!!.functions.first {
            it.name.asString() == "plus" &&
                    it.valueParameters.size == 1 &&
                    it.valueParameters[0].type == step.type
        }
        irSetVar(
            inductionVariable.symbol, irCallOp(
                plusFun.symbol, plusFun.returnType,
                irGet(inductionVariable),
                irGet(step)
            )
        )
    }

    protected fun buildLoopCondition(builder: DeclarationIrBuilder): IrExpression =
        with(builder) {
            val builtIns = context.irBuiltIns
            val progressionType = headerInfo.progressionType
            val progressionKotlinType = progressionType.elementType(builtIns).toKotlinType()
            val compFun =
                if (isLastInclusive) builtIns.lessOrEqualFunByOperandType[progressionKotlinType]!!
                else builtIns.lessFunByOperandType[progressionKotlinType]!!

            // The default condition depends on the direction.
            when (headerInfo.direction) {
                ProgressionDirection.DECREASING ->
                    // last <= inductionVar (use `<` if last is exclusive)
                    irCall(compFun).apply {
                        putValueArgument(0, irGet(last))
                        putValueArgument(1, irGet(inductionVariable))
                    }
                ProgressionDirection.INCREASING ->
                    // inductionVar <= last (use `<` if last is exclusive)
                    irCall(compFun).apply {
                        putValueArgument(0, irGet(inductionVariable))
                        putValueArgument(1, irGet(last))
                    }
                ProgressionDirection.UNKNOWN -> {
                    // If the direction is unknown, we check depending on the "step" value:
                    //   // (use `<` if last is exclusive)
                    //   (step > 0 && inductionVar <= last) || (step < 0 || last <= inductionVar)
                    val stepKotlinType = progressionType.stepType(builtIns).toKotlinType()
                    val zero = if (progressionType == ProgressionType.LONG_PROGRESSION) irLong(0) else irInt(0)
                    context.oror(
                        context.andand(
                            irCall(builtIns.greaterFunByOperandType[stepKotlinType]!!).apply {
                                putValueArgument(0, irGet(step))
                                putValueArgument(1, zero)
                            },
                            irCall(compFun).apply {
                                putValueArgument(0, irGet(inductionVariable))
                                putValueArgument(1, irGet(last))
                            }),
                        context.andand(
                            irCall(builtIns.lessFunByOperandType[stepKotlinType]!!).apply {
                                putValueArgument(0, irGet(step))
                                putValueArgument(1, zero)
                            },
                            irCall(compFun).apply {
                                putValueArgument(0, irGet(last))
                                putValueArgument(1, irGet(inductionVariable))
                            })
                    )
                }
            }
        }
}

internal class ProgressionLoopHeader(
    override val headerInfo: ProgressionHeaderInfo,
    inductionVariable: IrVariable,
    last: IrVariable,
    step: IrVariable
) : ForLoopHeader(headerInfo, inductionVariable, last, step, isLastInclusive = true) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        // loopVariable = inductionVariable
        irGet(inductionVariable)
    }

    // For this loop:
    //
    //   for (i in first()..last() step step())
    //
    // ...the functions may have side-effects so we need to call them in the following order: first() (inductionVariable), last(), step().
    // Additional variables come first as they may be needed to the subsequent variables.
    //
    // In the case of a reversed range, the `inductionVariable` and `last` variables are swapped, therefore the declaration order must be
    // swapped to preserve the correct evaluation order.
    override val declarations: List<IrStatement>
        get() = headerInfo.additionalVariables +
                (if (headerInfo.isReversed) listOf(last, inductionVariable) else listOf(inductionVariable, last)) +
                step

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        with(builder) {
            val newLoop = if (headerInfo.canOverflow) {
                // If the induction variable CAN overflow, we cannot use it in the loop condition. Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     // Loop is not empty
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (loopVar != last)
                //   }
                IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    label = oldLoop.label
                    condition = irNotEquals(irGet(loopVariable!!), irGet(last))
                    body = newBody
                }
            } else {
                // If the induction variable can NOT overflow, use a do-while loop. Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     do {
                //         val loopVar = inductionVar
                //         inductionVar += step
                //         // Loop body
                //     } while (inductionVar <= last)
                //   }
                //
                // Even though this can be simplified into a simpler while loop, using if + do-while (i.e., doing a loop inversion)
                // performs better in benchmarks. In cases where `last` is a constant, the `if` may be optimized away.
                IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    label = oldLoop.label
                    condition = buildLoopCondition(this@with)
                    body = newBody
                }
            }

            val loopCondition = buildLoopCondition(this@with)
            // Combine with the additional "not empty" condition, if any.
            val notEmptyCheck =
                irIfThen(headerInfo.additionalNotEmptyCondition?.let { context.andand(it, loopCondition) } ?: loopCondition, newLoop)
            LoopReplacement(newLoop, notEmptyCheck)
        }
}

internal class IndexedGetLoopHeader(
    override val headerInfo: IndexedGetHeaderInfo,
    inductionVariable: IrVariable,
    last: IrVariable,
    step: IrVariable
) : ForLoopHeader(headerInfo, inductionVariable, last, step, isLastInclusive = false) {

    override fun initializeLoopVariable(symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) = with(builder) {
        // inductionVar = loopVar[inductionVariable]
        val indexedGetFun = headerInfo.objectVariable.type.getClass()!!.functions.first { it.name.asString() == "get" }
        irCall(indexedGetFun).apply {
            dispatchReceiver = irGet(headerInfo.objectVariable)
            putValueArgument(0, irGet(inductionVariable))
        }
    }

    override val declarations: List<IrStatement>
        get() = listOf(headerInfo.objectVariable, inductionVariable, last, step)

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        // Loop is lowered into something like:
        //
        //   var inductionVar = 0
        //   var last = array.size
        //   while (inductionVar < last) {
        //       val loopVar = array[inductionVar]
        //       inductionVar++
        //       // Loop body
        //   }
        val newLoop = IrWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
            label = oldLoop.label
            condition = buildLoopCondition(this@with)
            body = newBody
        }
        LoopReplacement(newLoop, newLoop)
    }
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

        // Get the iterable expression, e.g., `someIterable` in the following loop variable declaration:
        //
        //   val it = someIterable.iterator()
        //
        // If the `iterator` method is an extension method, make sure that we are calling a known extension
        // method in the library such as `kotlin.text.StringsKt.iterator` with no value arguments. Other
        // extension methods could return user-defined iterators.
        val iterable = (variable.initializer as? IrCall)?.let {
            val extensionReceiver = it.extensionReceiver
            if (extensionReceiver != null) {
                val function = it.symbol.owner
                if (it.valueArgumentsCount == 0
                    && function.isTopLevel
                    && function.getPackageFragment()?.fqName == FqName("kotlin.text")
                    && function.name == OperatorNameConventions.ITERATOR) {
                    extensionReceiver
                } else {
                    null
                }
            } else {
                it.dispatchReceiver
            }
        }

        // Collect loop information from the iterable expression.
        val headerInfo = iterable?.accept(headerInfoBuilder, null)
            ?: return null  // If the iterable is not supported.

        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        with(builder) builder@{
            with(headerInfo) {
                // For this loop:
                //
                //   for (i in first()..last() step step())
                //
                // We need to cast first(), last(). and step() to conform to the progression type so
                // that operations on the induction variable within the loop are more efficient.
                //
                // In the above example, if first() is a Long and last() is an Int, this creates a
                // LongProgression so last() should be cast to a Long.
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
                    is IndexedGetHeaderInfo -> IndexedGetLoopHeader(
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

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
        if (expression.type is IrSimpleType && expression.type.isNullable()) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }
}
