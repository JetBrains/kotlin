/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.strategies

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.GenerateSequenceInitialValue
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceSource
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.callRichFunctionReference
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

internal class GenerateSequenceStrategy(val source: SequenceSource.GenerateSequence) : LoweringStrategy() {
    override fun lowerLoop(
        builderWithParent: IrBuilderWithParent,
        loopBody: IrBlock,
        sequenceData: SequenceData,
        oldLoop: IrLoop?,
        oldLoopVariable: IrVariable,
    ): IrExpression {
        loopBody.statements.remove(oldLoopVariable)
        val builder = builderWithParent.first
        val newLoop = builder.createSequenceWhile()
        val bodyRewriter = updateLoopVariableInBody(builder, oldLoopVariable, loopBody, newLoop, oldLoop)
        val (iteratorDeclaration, outerLoopVariable, iteratorNextReplacement, newCondition) =
            createIteratorReplacement(builderWithParent)
        val newBody = builder.irComposite {
            +iteratorNextReplacement
            +addReplacementsToBody(
                builderWithParent,
                bodyRewriter,
                sequenceData,
                builder.irGet(outerLoopVariable),
                IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                newLoop,
                oldLoopVariable.name,
            )
        }
        return createLoweredLoop(
            iteratorDeclaration,
            outerLoopVariable,
            newCondition,
            builder,
            newBody,
            sequenceData,
            newLoop,
        )
    }

    override fun lowerFunction(
        builderWithParent: IrBuilderWithParent,
        function: IrRichFunctionReference,
        sequenceData: SequenceData
    ): IrExpression {
        val builder = builderWithParent.first
        val (iteratorDeclaration, outerLoopVariable, iteratorNextReplacement, newCondition) =
            createIteratorReplacement(builderWithParent)
        val newLoop = builder.createSequenceWhile()
        val newBody = builder.irComposite {
            +iteratorNextReplacement
            +addReplacementsToForEachCall(
                builderWithParent,
                function,
                sequenceData,
                builder.irGet(outerLoopVariable),
                newLoop
            )
        }
        return createLoweredLoop(
            iteratorDeclaration,
            outerLoopVariable,
            newCondition,
            builder,
            newBody,
            sequenceData,
            newLoop,
        )
    }

    private fun createIteratorReplacement(
        builderWithParent: IrBuilderWithParent,
    ): IteratorReplacement {
        val (builder, parent) = builderWithParent
        val generatingFunction = source.generatingFunction
        val oneArgumentIteratingFunction: (IrVariable) -> IrExpression = { variable ->
            builder.callRichFunctionReference(generatingFunction, parent, builder.irAsNotNull(builder.irGet(variable)))
        }
        val zeroArgumentIteratingFunction: (IrVariable) -> IrExpression = { _ ->
            builder.callRichFunctionReference(generatingFunction, parent)
        }

        val (initialExpression, iteratingFunction: (IrVariable) -> IrExpression) = when (val initialValue = source.initialValue) {
            is GenerateSequenceInitialValue.InitialValue -> {
                initialValue.expression.deepCopyWithSymbols(parent) to oneArgumentIteratingFunction
            }
            is GenerateSequenceInitialValue.InitialFunction -> {
                builder.callRichFunctionReference(initialValue.function, parent) to oneArgumentIteratingFunction
            }
            is GenerateSequenceInitialValue.NoInitialValue -> {
                builder.callRichFunctionReference(generatingFunction, parent) to zeroArgumentIteratingFunction
            }
        }
        return IteratorReplacement.create(initialExpression, iteratingFunction, builder)
    }

    private data class IteratorReplacement(
        val iteratorVariable: IrVariable,
        val outerLoopVariable: IrVariable,
        val iteratorNextStatement: IrStatement,
        val condition: IrExpression,
    ) {
        companion object {
            fun create(
                initialExpression: IrExpression,
                evaluateNext: (IrVariable) -> IrExpression,
                builder: IrBuilderWithScope,
            ): IteratorReplacement = with(builder) {
                val iteratorVariable = scope.createTemporaryVariable(
                    initialExpression,
                    isMutable = true,
                    irType = initialExpression.type.makeNullable(),
                    origin = IrDeclarationOrigin.FOR_LOOP_ITERATOR
                )
                val condition = irNotEquals(irGet(iteratorVariable), irNull())
                val next = evaluateNext(iteratorVariable)
                val outerLoopVariable =
                    builder.scope.createTemporaryVariable(
                        builder.irAsNotNull(builder.irGet(iteratorVariable)),
                        nameHint = "outerLoopVariable",
                    )
                val iteratorNextStatement = irSet(iteratorVariable, next)
                return IteratorReplacement(iteratorVariable, outerLoopVariable, iteratorNextStatement, condition)
            }
        }
    }
}
