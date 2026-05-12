/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal fun IrBuilderWithScope.irAsNotNull(value: IrExpression): IrExpression {
    val nonNullType = value.type.makeNotNull()
    return IrTypeOperatorCallImpl(
        startOffset,
        endOffset,
        nonNullType,
        IrTypeOperator.IMPLICIT_NOTNULL,
        nonNullType,
        value
    )
}

internal fun IrBuilderWithScope.createSequenceWhile(): IrWhileLoop =
    irWhile(IrStatementOrigin.FOR_LOOP_INNER_WHILE)

internal fun IrElement.markAsSynthetic() {
    this.acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.startOffset = UNDEFINED_OFFSET
            element.endOffset = UNDEFINED_OFFSET
            element.acceptChildrenVoid(this)
        }
    })
}

private class BreakContinueUpdater(
    val newLoop: IrLoop,
    val oldLoop: IrLoop
) : IrElementTransformerVoidWithContext() {
    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        if (jump.loop == oldLoop)
            jump.loop = newLoop
        return super.visitBreakContinue(jump)
    }
}

private class LoopBodyTransformer(
    val builder: IrBuilderWithScope,
    val oldVariable: IrValueDeclaration,
    val newVariable: IrVariable,
) : IrElementTransformerVoidWithContext() {
    override fun visitGetValue(expression: IrGetValue): IrExpression {
        if (expression.symbol == oldVariable.symbol) {
            check(expression.type == newVariable.type)
            return builder.irGet(newVariable)
        }
        return super.visitGetValue(expression)
    }
}

internal sealed class LoweringStrategy {
    abstract fun lowerLoop(
        builderWithParent: IrBuilderWithParent,
        loopBody: (IrVariable) -> IrContainerExpression,
        sequenceData: SequenceData,
        newLoop: IrLoop,
        loopVariableName: Name?,
    ): IrContainerExpression?

    abstract fun lowerFunction(
        builderWithParent: IrBuilderWithParent,
        function: IrRichFunctionReference,
        sequenceData: SequenceData,
    ): IrExpression?

    abstract fun prepareLoopBody(
        loopBody: IrBlock,
        builder: IrBuilderWithScope,
        oldLoopVariable: IrVariable,
        oldLoop: IrLoop?,
    ): Pair<(IrVariable) -> IrContainerExpression, IrLoop>

    /**
     * Transforms loop body:
     * ```
     *  {
     *      val next = iterator.next()
     *      body(next)
     *  }
     * ```
     * into
     * ```
     *  {
     *      val mappedValue = mapReplacement(filterReplacement(initialValue))
     *      body(mappedValue)
     *  }
     * ```
     */
    protected fun addReplacementsToBody(
        builderWithParent: IrBuilderWithParent,
        bodyRewriter: (IrVariable) -> IrContainerExpression,
        sequenceData: SequenceData,
        initialValue: IrExpression,
        newBodyOrigin: IrStatementOrigin?,
        loop: IrLoop,
        innerLoopVariableName: Name?,
    ): IrExpression {
        return sequenceData.newLoopPrologue(
            builderWithParent,
            loop,
            initialValue,
        ) { filteredValue ->
            val builder = builderWithParent.first
            val mappedValue = sequenceData.mapReplacement(builderWithParent, filteredValue)
            builder.irBlock(origin = newBodyOrigin) {
                val valueAfterReplacements = scope.createTemporaryVariable(
                    mappedValue,
                    origin = IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                    nameHint = innerLoopVariableName?.asString(),
                    inventUniqueName = innerLoopVariableName == null,
                )
                +valueAfterReplacements
                +bodyRewriter(valueAfterReplacements)
            }
        }
    }

    protected fun addReplacementsToForEachCall(
        builderWithParent: IrBuilderWithParent,
        forEachFunction: IrRichFunctionReference,
        sequenceData: SequenceData,
        initialValue: IrExpression,
        loop: IrLoop,
    ): IrExpression {
        return sequenceData.newLoopPrologue(
            builderWithParent,
            loop,
            initialValue,
        ) { filteredValue ->
            val (builder, parent) = builderWithParent
            val mappedValue = sequenceData.mapReplacement(builderWithParent, filteredValue)
            builder.irBlock(origin = IrStatementOrigin.FOR_LOOP_INNER_WHILE) {
                val valueAfterReplacements = scope.createTemporaryVariable(
                    mappedValue,
                    origin = IrDeclarationOrigin.FOR_LOOP_VARIABLE,
                )
                +valueAfterReplacements
                +callRichFunctionReference(forEachFunction, parent, irGet(valueAfterReplacements))
            }
        }
    }

    protected fun updateLoopVariableInBody(
        builder: IrBuilderWithScope,
        oldLoopVariable: IrValueDeclaration,
        body: IrContainerExpression,
        newLoop: IrLoop,
        oldLoop: IrLoop?,
    ): (IrVariable) -> IrContainerExpression = { newInnerLoopVariable ->
        body.transformChildrenVoid(LoopBodyTransformer(builder, oldLoopVariable, newInnerLoopVariable))
        if (oldLoop != null) body.transformChildrenVoid(BreakContinueUpdater(newLoop, oldLoop))
        body
    }

    protected fun addTakeVariableDeclarations(
        oldLoop: IrContainerExpression,
        sequenceData: SequenceData,
        builder: IrBuilderWithScope
    ): IrContainerExpression =
        builder.irBlock {
            +sequenceData.declarationsBeforeLoop(builder)
            +oldLoop
        }

    /**
     * Given a loop body, iteratorDeclaration, and loopVariable definition, creates the outer shell of what is expected in a lowered for loop.
     */
    protected fun createLoweredLoop(
        iteratorDeclaration: IrVariable,
        outerLoopVariable: IrVariable,
        loopCondition: IrExpression,
        builder: IrBuilderWithScope,
        loopBody: IrExpression,
        sequenceData: SequenceData,
        newLoop: IrLoop,
    ): IrContainerExpression {
        newLoop.body = builder.irBlock {
            +outerLoopVariable
            +loopBody
        }
        newLoop.condition = loopCondition
        val newBlock = builder.irBlock {
            +iteratorDeclaration
            +newLoop
        }
        return addTakeVariableDeclarations(newBlock, sequenceData, builder)
    }
}
