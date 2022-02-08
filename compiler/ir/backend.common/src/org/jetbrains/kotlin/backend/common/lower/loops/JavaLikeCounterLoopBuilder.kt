/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.util.OperatorNameConventions

class JavaLikeCounterLoopBuilder(private val context: CommonBackendContext) {
    private val booleanNot =
        context.irBuiltIns.booleanClass.owner.findDeclaration<IrSimpleFunction> {
            it.name == OperatorNameConventions.NOT
        } ?: error("No '${OperatorNameConventions.NOT}' in ${context.irBuiltIns.booleanClass.owner.render()}")

    fun buildJavaLikeDoWhileCounterLoop(
        oldLoop: IrLoop,
        newLoopCondition: IrExpression,
        newBody: IrExpression?,
        loopOrigin: IrStatementOrigin?
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

        val doWhileLoop = IrDoWhileLoopImpl(oldLoop.startOffset, oldLoop.endOffset, oldLoop.type, loopOrigin)
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


    private fun createNegatedConditionCheck(
        newLoopCondition: IrExpression,
        doWhileLoop: IrDoWhileLoop
    ): IrWhenImpl {
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

    fun moveInductionVariableUpdateToLoopCondition(doWhileLoop: IrDoWhileLoop) {
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
}