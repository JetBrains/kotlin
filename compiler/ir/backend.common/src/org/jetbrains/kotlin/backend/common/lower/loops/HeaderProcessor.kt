/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
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
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.functions
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

internal interface ForLoopHeader {
    /** Statements used to initialize the entire loop (e.g., declare induction variable). */
    val loopInitStatements: List<IrStatement>

    /** Statements used to initialize an iteration of the loop (e.g., assign loop variable). */
    fun initializeIteration(
        loopVariable: IrVariable,
        symbols: Symbols<CommonBackendContext>,
        builder: DeclarationIrBuilder
    ): List<IrStatement>

    /** Builds a new loop from the old loop. */
    fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement
}

internal abstract class NumericForLoopHeader(
    headerInfo: NumericHeaderInfo,
    builder: DeclarationIrBuilder,
    protected val isLastInclusive: Boolean
) : ForLoopHeader {

    protected val inductionVariable: IrVariable
    protected val lastVariableIfCanCacheLast: IrVariable?
    protected val stepVariable: IrVariable
    protected val lastExpression: IrExpression
        // Always copy `lastExpression` is it may be used in multiple conditions.
        get() = field.deepCopyWithSymbols()

    init {
        with(builder) {
            // For this loop:
            //
            //   for (i in first()..last() step step())
            //
            // We need to cast first(), last(). and step() to conform to the progression type so
            // that operations on the induction variable within the loop are more efficient.
            //
            // In the above example, if first() is a Long and last() is an Int, this creates a
            // LongProgression so last() should be cast to a Long.
            inductionVariable = scope.createTemporaryVariable(
                headerInfo.first.castIfNecessary(
                    headerInfo.progressionType.elementType(context.irBuiltIns),
                    headerInfo.progressionType.elementCastFunctionName
                ),
                nameHint = "inductionVariable",
                isMutable = true
            )

            // Due to features of PSI2IR we can obtain nullable arguments here while actually
            // they are non-nullable (the frontend takes care about this). So we need to cast
            // them to non-nullable.
            // TODO: Confirm if casting to non-nullable is still necessary
            val last = ensureNotNullable(
                headerInfo.last.castIfNecessary(
                    headerInfo.progressionType.elementType(context.irBuiltIns),
                    headerInfo.progressionType.elementCastFunctionName
                )
            )

            lastVariableIfCanCacheLast = if (headerInfo.canCacheLast) {
                scope.createTemporaryVariable(
                    last,
                    nameHint = "last"
                )
            } else null

            lastExpression = if (headerInfo.canCacheLast) irGet(lastVariableIfCanCacheLast!!) else last

            stepVariable = headerInfo.progressionType.stepType(context.irBuiltIns).let {
                scope.createTemporaryVariable(
                    ensureNotNullable(
                        headerInfo.step.castIfNecessary(
                            it,
                            headerInfo.progressionType.stepCastFunctionName
                        )
                    ),
                    nameHint = "step",
                    irType = it
                )
            }
        }
    }

    // This cannot be declared and initialized directly in the constructor. At the time of the base class (ForLoopHeader) constructor
    // execution, the `headerInfo` property overridden in the derived class (e.g., NumericForLoopHeader) is not yet initialized.
    // Therefore, we must use the `headerInfo` constructor parameter in the "init" block above instead of using the property.
    protected open val headerInfo: NumericHeaderInfo = headerInfo

    private fun DeclarationIrBuilder.ensureNotNullable(expression: IrExpression) =
        if (expression.type is IrSimpleType && expression.type.isNullable()) {
            irImplicitCast(expression, expression.type.makeNotNull())
        } else {
            expression
        }

    /** Statement used to increment the induction variable. */
    protected fun incrementInductionVariable(builder: DeclarationIrBuilder): IrStatement = with(builder) {
        // inductionVariable = inductionVariable + step
        val plusFun = inductionVariable.type.getClass()!!.functions.single {
            it.name == OperatorNameConventions.PLUS &&
                    it.valueParameters.size == 1 &&
                    it.valueParameters[0].type == stepVariable.type
        }
        irSetVar(
            inductionVariable.symbol, irCallOp(
                plusFun.symbol, plusFun.returnType,
                irGet(inductionVariable),
                irGet(stepVariable)
            )
        )
    }

    protected fun buildLoopCondition(builder: DeclarationIrBuilder): IrExpression =
        with(builder) {
            val builtIns = context.irBuiltIns
            val progressionType = headerInfo.progressionType
            val progressionElementType = progressionType.elementType(builtIns)
            val compFun =
                if (isLastInclusive) builtIns.lessOrEqualFunByOperandType[progressionElementType.classifierOrFail]!!
                else builtIns.lessFunByOperandType[progressionElementType.classifierOrFail]!!

            // The default condition depends on the direction.
            when (headerInfo.direction) {
                ProgressionDirection.DECREASING ->
                    // last <= inductionVar (use `<` if last is exclusive)
                    irCall(compFun).apply {
                        putValueArgument(0, lastExpression)
                        putValueArgument(1, irGet(inductionVariable))
                    }
                ProgressionDirection.INCREASING ->
                    // inductionVar <= last (use `<` if last is exclusive)
                    irCall(compFun).apply {
                        putValueArgument(0, irGet(inductionVariable))
                        putValueArgument(1, lastExpression)
                    }
                ProgressionDirection.UNKNOWN -> {
                    // If the direction is unknown, we check depending on the "step" value:
                    //   // (use `<` if last is exclusive)
                    //   (step > 0 && inductionVar <= last) || (step < 0 || last <= inductionVar)
                    val stepType = progressionType.stepType(builtIns)
                    val isLong = progressionType == ProgressionType.LONG_PROGRESSION
                    context.oror(
                        context.andand(
                            irCall(builtIns.greaterFunByOperandType[stepType.classifierOrFail]!!).apply {
                                putValueArgument(0, irGet(stepVariable))
                                putValueArgument(1, if (isLong) irLong(0) else irInt(0))
                            },
                            irCall(compFun).apply {
                                putValueArgument(0, irGet(inductionVariable))
                                putValueArgument(1, lastExpression)
                            }),
                        context.andand(
                            irCall(builtIns.lessFunByOperandType[stepType.classifierOrFail]!!).apply {
                                putValueArgument(0, irGet(stepVariable))
                                putValueArgument(1, if (isLong) irLong(0) else irInt(0))
                            },
                            irCall(compFun).apply {
                                putValueArgument(0, lastExpression)
                                putValueArgument(1, irGet(inductionVariable))
                            })
                    )
                }
            }
        }
}

internal class ProgressionLoopHeader(
    override val headerInfo: ProgressionHeaderInfo,
    builder: DeclarationIrBuilder
) : NumericForLoopHeader(headerInfo, builder, isLastInclusive = true) {

    // For this loop:
    //
    //   for (i in first()..last() step step())
    //
    // ...the functions may have side-effects so we need to call them in the following order: first() (inductionVariable), last(), step().
    // Additional variables come first as they may be needed to the subsequent variables.
    //
    // In the case of a reversed range, the `inductionVariable` and `last` variables are swapped, therefore the declaration order must be
    // swapped to preserve the correct evaluation order.
    override val loopInitStatements = headerInfo.additionalVariables + (
            if (headerInfo.isReversed)
                listOfNotNull(lastVariableIfCanCacheLast, inductionVariable)
            else
                listOfNotNull(inductionVariable, lastVariableIfCanCacheLast)
            ) +
            stepVariable

    private var loopVariable: IrVariable? = null

    override fun initializeIteration(loopVariable: IrVariable, symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) =
        with(builder) {
            this@ProgressionLoopHeader.loopVariable = loopVariable
            // loopVariable = inductionVariable
            // inductionVariable = inductionVariable + step
            loopVariable.initializer = irGet(inductionVariable)
            listOf(loopVariable, incrementInductionVariable(this))
        }

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
                    condition = irNotEquals(irGet(loopVariable!!), lastExpression)
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
    builder: DeclarationIrBuilder
) : NumericForLoopHeader(headerInfo, builder, isLastInclusive = false) {

    override val loopInitStatements = listOfNotNull(headerInfo.objectVariable, inductionVariable, lastVariableIfCanCacheLast, stepVariable)

    override fun initializeIteration(loopVariable: IrVariable, symbols: Symbols<CommonBackendContext>, builder: DeclarationIrBuilder) =
        with(builder) {
            // loopVariable = objectVariable[inductionVariable]
            val indexedGetFun = with(headerInfo.expressionHandler) { headerInfo.objectVariable.type.getFunction }
            loopVariable.initializer = irCall(indexedGetFun).apply {
                dispatchReceiver = irGet(headerInfo.objectVariable)
                putValueArgument(0, irGet(inductionVariable))
            }
            listOf(loopVariable, incrementInductionVariable(this))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?): LoopReplacement = with(builder) {
        // Loop is lowered into something like:
        //
        //   var inductionVar = 0
        //   var last = objectVariable.size
        //   while (inductionVar < last) {
        //       val loopVar = objectVariable.get(inductionVar)
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
        val iteratorCall = variable.initializer as? IrCall
        val iterable = iteratorCall?.run {
            if (extensionReceiver != null) {
                extensionReceiver
            } else {
                dispatchReceiver
            }
        }

        // Collect loop information from the iterable expression.
        val headerInfo = iterable?.accept(headerInfoBuilder, iteratorCall)
            ?: return null  // If the iterable is not supported.

        val builder = context.createIrBuilder(scopeOwnerSymbol(), variable.startOffset, variable.endOffset)
        return when (headerInfo) {
            is IndexedGetHeaderInfo -> IndexedGetLoopHeader(headerInfo, builder)
            is ProgressionHeaderInfo -> ProgressionLoopHeader(headerInfo, builder)
        }
    }
}
