/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.util.OperatorNameConventions

class ProgressionLoopHeader(
    headerInfo: ProgressionHeaderInfo,
    builder: DeclarationIrBuilder,
    context: CommonBackendContext
) : NumericForLoopHeader<ProgressionHeaderInfo>(headerInfo, builder, context) {

    private val preferJavaLikeCounterLoop = context.preferJavaLikeCounterLoop

    // For this loop:
    //
    //   for (i in first()..last() step step())
    //
    // ...the functions may have side effects, so we need to call them in the following order: first() (inductionVariable), last(), step().
    // Additional variables come first as they may be needed to the subsequent variables.
    //
    // In the case of a reversed range, the `inductionVariable` and `last` variables are swapped, therefore the declaration order must be
    // swapped to preserve the correct evaluation order.
    override val loopInitStatements = headerInfo.additionalStatements + (
            if (headerInfo.isReversed)
                listOfNotNull(lastVariableIfCanCacheLast, inductionVariable)
            else
                listOfNotNull(inductionVariable, lastVariableIfCanCacheLast)
            ) +
            listOfNotNull(stepVariable)

    private var loopVariable: IrVariable? = null

    override fun initializeIteration(
        loopVariable: IrVariable?,
        loopVariableComponents: Map<Int, IrVariable>,
        builder: DeclarationIrBuilder,
        backendContext: CommonBackendContext,
    ): List<IrStatement> =
        with(builder) {
            // loopVariable is used in the loop condition if it can overflow. If no loopVariable was provided, create one.
            this@ProgressionLoopHeader.loopVariable = if (headerInfo.canOverflow && loopVariable == null) {
                scope.createTmpVariable(
                    irGet(inductionVariable),
                    nameHint = "loopVariable",
                    isMutable = true
                )
            } else {
                loopVariable?.initializer = irGet(inductionVariable).let {
                    headerInfo.progressionType.run {
                        if (this is UnsignedProgressionType) {
                            // The induction variable is signed for unsigned progressions but the loop variable should be unsigned.
                            it.asUnsigned()
                        } else it
                    }
                }
                loopVariable
            }

            // loopVariable = inductionVariable
            // inductionVariable = inductionVariable + step
            listOfNotNull(this@ProgressionLoopHeader.loopVariable, incrementInductionVariable(this))
        }

    override fun buildLoop(builder: DeclarationIrBuilder, oldLoop: IrLoop, newBody: IrExpression?) =
        with(builder) {
            if (headerInfo.canOverflow ||
                preferJavaLikeCounterLoop && headerInfo.progressionType is UnsignedProgressionType && headerInfo.isLastInclusive
            ) {
                // If the induction variable CAN overflow, we cannot use it in the loop condition.
                // Loop is lowered into something like:
                //
                //   if (inductionVar <= last) {
                //     // Loop is not empty
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (loopVar != last)
                //   }
                //
                // This loop form is also preferable for loops over unsigned progressions on JVM,
                // because HotSpot doesn't recognize unsigned integer comparison as a counter loop condition.
                // Unsigned integer equality is fine, though.
                // See KT-49444 for performance comparison example.
                val newLoopOrigin = if (preferJavaLikeCounterLoop)
                    this@ProgressionLoopHeader.context.doWhileCounterLoopOrigin
                else
                    oldLoop.origin
                val newLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, newLoopOrigin).apply {
                    val loopVariableExpression = irGet(loopVariable!!).let {
                        headerInfo.progressionType.run {
                            if (this is UnsignedProgressionType) {
                                // The loop variable is signed but bounds are signed for unsigned progressions.
                                it.asSigned()
                            } else it
                        }
                    }
                    label = oldLoop.label
                    condition = irNotEquals(loopVariableExpression, lastExpression)
                    body = newBody
                }

                if (preferJavaLikeCounterLoop) {
                    moveInductionVariableUpdateToLoopCondition(newLoop)
                }

                val loopCondition = buildLoopCondition(this@with)
                LoopReplacement(newLoop, irIfThen(loopCondition, newLoop))
            } else if (preferJavaLikeCounterLoop && !headerInfo.isLastInclusive) {
                // It is critically important for loop code performance on JVM to "look like" a simple counter loop in Java when possible
                // (`for (int i = first; i < lastExclusive; ++i) { ... }`).
                // Otherwise loop-related optimizations will not kick in, resulting in significant performance degradation.
                //
                // Use a do-while loop:
                //   do {
                //       if ( !( inductionVariable < last ) ) break
                //       val loopVariable = inductionVariable
                //       <body>
                //   } while ( { inductionVariable += step; true } )
                // This loop form is equivalent to the Java counter loop shown above.

                val newLoopCondition = buildLoopCondition(this@with)

                buildJavaLikeDoWhileCounterLoop(oldLoop, newLoopCondition, newBody)
            } else {
                // Use an if-guarded do-while loop (note the difference in loop condition):
                //
                //   if (inductionVar <= last) {
                //     do {
                //       val loopVar = inductionVar
                //       inductionVar += step
                //       // Loop body
                //     } while (inductionVar <= last)
                //   }
                //
                val newLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, oldLoop.origin).apply {
                    label = oldLoop.label
                    condition = buildLoopCondition(this@with)
                    body = newBody
                }
                val loopCondition = buildLoopCondition(this@with)
                LoopReplacement(newLoop, irIfThen(loopCondition, newLoop))
            }
        }

    private val booleanNot =
        context.irBuiltIns.booleanClass.owner.findDeclaration<IrSimpleFunction> {
            it.name == OperatorNameConventions.NOT
        } ?: error("No '${OperatorNameConventions.NOT}' in ${context.irBuiltIns.booleanClass.owner.render()}")

    private fun moveInductionVariableUpdateToLoopCondition(doWhileLoop: IrDoWhileLoop) {
        // On JVM, it's important that induction variable update happens in the end of the loop
        // (otherwise HotSpot will not treat it as a counter loop).
        // Moving induction variable update to loop condition (instead of just placing it in the end of loop body)
        // also allows reusing loop variable as induction variable later.
        //
        // Transform a loop in the form:
        //      do {
        //          { <next> }
        //          <body>
        //      } while (<condition>)
        // to
        //      do {
        //          { <next'> }
        //          <body>
        //      } while ( { if (!<condition>) break; <updateInductionVar>; true } )
        val doWhileBody = doWhileLoop.body as? IrContainerExpression ?: return
        if (doWhileBody.origin != IrStatementOrigin.FOR_LOOP_INNER_WHILE) return
        val doWhileLoopNext = doWhileBody.statements[0] as? IrContainerExpression ?: return
        if (doWhileLoopNext.origin != IrStatementOrigin.FOR_LOOP_NEXT) return

        val updateInductionVarIndex = doWhileLoopNext.statements
            .indexOfFirst { it is IrSetValue && it.symbol.owner.isInductionVariable(context) }
        if (updateInductionVarIndex < 0) return
        val updateInductionVar = doWhileLoopNext.statements[updateInductionVarIndex]
        doWhileLoopNext.statements.removeAt(updateInductionVarIndex)

        val loopCondition = doWhileLoop.condition
        val loopConditionStartOffset = loopCondition.startOffset
        val loopConditionEndOffset = loopCondition.endOffset
        doWhileLoop.condition = IrCompositeImpl(
            loopConditionStartOffset, loopConditionEndOffset, loopCondition.type,
            origin = null,
            statements = listOf(
                createNegatedConditionCheck(doWhileLoop.condition, doWhileLoop),
                updateInductionVar,
                IrConstImpl.boolean(loopConditionStartOffset, loopConditionEndOffset, context.irBuiltIns.booleanType, true)
            )
        )
    }

    private fun buildJavaLikeDoWhileCounterLoop(
        oldLoop: IrLoop,
        newLoopCondition: IrExpression,
        newBody: IrExpression?
    ): LoopReplacement {
        // Transform loop:
        //      while (<newLoopCondition>) {
        //          { // FOR_LOOP_NEXT
        //              <initializeLoopIteration>
        //              <inductionVariableUpdate>
        //          }
        //          <originalLoopBody>
        //      }
        // to:
        //      do {
        //          { // FOR_LOOP_NEXT
        //              if (!(<newLoopCondition>)) break
        //              <initializeLoopIteration>
        //          }
        //          <originalLoopBody>
        //      } while (
        //          {
        //              <inductionVariableUpdate>
        //              true
        //          }
        //      )
        val bodyBlock = newBody as? IrContainerExpression
            ?: throw AssertionError("newBody: ${newBody?.dump()}")
        val forLoopNextBlock = bodyBlock.statements[0] as? IrContainerExpression
            ?: throw AssertionError("bodyBlock[0]: ${bodyBlock.statements[0].dump()}")
        if (forLoopNextBlock.origin != IrStatementOrigin.FOR_LOOP_NEXT)
            throw AssertionError("FOR_LOOP_NEXT expected: ${forLoopNextBlock.dump()}")
        val inductionVariableUpdate = forLoopNextBlock.statements.last() as? IrSetValue
            ?: throw AssertionError("forLoopNextBlock.last: ${forLoopNextBlock.statements.last().dump()}")

        val doWhileLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, context.doWhileCounterLoopOrigin)
        doWhileLoop.label = oldLoop.label

        bodyBlock.statements[0] = IrCompositeImpl(
            forLoopNextBlock.startOffset, forLoopNextBlock.endOffset,
            forLoopNextBlock.type,
            forLoopNextBlock.origin,
        ).apply {
            statements.add(createNegatedConditionCheck(newLoopCondition, doWhileLoop))
            if (forLoopNextBlock.statements.size >= 2)
                statements.addAll(forLoopNextBlock.statements.subList(0, forLoopNextBlock.statements.lastIndex))
        }

        doWhileLoop.body = bodyBlock

        val stepStartOffset = inductionVariableUpdate.startOffset
        val stepEndOffset = inductionVariableUpdate.endOffset
        val doWhileCondition =
            IrCompositeImpl(
                stepStartOffset, stepEndOffset, context.irBuiltIns.booleanType, null,
                listOf(
                    inductionVariableUpdate,
                    IrConstImpl.boolean(stepStartOffset, stepEndOffset, context.irBuiltIns.booleanType, true)
                )
            )
        doWhileLoop.condition = doWhileCondition

        return LoopReplacement(doWhileLoop, doWhileLoop)
    }

    private fun createNegatedConditionCheck(newLoopCondition: IrExpression, doWhileLoop: IrDoWhileLoop): IrWhenImpl {
        val conditionStartOffset = newLoopCondition.startOffset
        val conditionEndOffset = newLoopCondition.endOffset
        val negatedCondition =
            IrCallImpl.fromSymbolOwner(conditionStartOffset, conditionEndOffset, booleanNot.symbol).apply {
                dispatchReceiver = newLoopCondition
            }

        return IrWhenImpl(
            conditionStartOffset, conditionEndOffset, context.irBuiltIns.unitType, null,
            listOf(
                IrBranchImpl(
                    negatedCondition,
                    IrBreakImpl(conditionStartOffset, conditionEndOffset, context.irBuiltIns.nothingType, doWhileLoop)
                )
            )
        )
    }

}