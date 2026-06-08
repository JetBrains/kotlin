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
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

internal class GenerateSequenceStrategy(val source: SequenceSource.GenerateSequence) : LoweringStrategy() {
    override fun lowerLoop(
        builderWithParent: IrBuilderWithParent,
        loopBody: (IrVariable) -> IrContainerExpression,
        sequenceData: SequenceData,
        newLoop: IrLoop,
        loopVariable: IrVariable?,
    ): IrContainerExpression {
        val builder = builderWithParent.first
        val iteratorReplacement =
            createIteratorReplacement(builderWithParent, source.sequenceElementType)
        val newBody = builder.irComposite {
            +iteratorReplacement.iteratorNextStatement
            +addReplacementsToBody(
                builderWithParent,
                loopBody,
                sequenceData,
                builder.irGet(iteratorReplacement.outerLoopVariable),
                IrStatementOrigin.FOR_LOOP_INNER_WHILE,
                newLoop,
                loopVariable,
            )
        }
        return createLoweredLoop(
            iteratorReplacement.iteratorVariable,
            iteratorReplacement.outerLoopVariable,
            iteratorReplacement.condition,
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
        val iteratorReplacement =
            createIteratorReplacement(builderWithParent, source.sequenceElementType)
        val newLoop = builder.createSequenceWhile()
        val newBody = builder.irComposite {
            +iteratorReplacement.iteratorNextStatement
            +addReplacementsToForEachCall(
                builderWithParent,
                function,
                sequenceData,
                builder.irGet(iteratorReplacement.outerLoopVariable),
                newLoop
            )
        }
        return createLoweredLoop(
            iteratorReplacement.iteratorVariable,
            iteratorReplacement.outerLoopVariable,
            iteratorReplacement.condition,
            builder,
            newBody,
            sequenceData,
            newLoop,
        )
    }

    override fun prepareLoopBody(
        loopBody: IrBlock,
        builderWithParent: IrBuilderWithParent,
        oldLoopVariable: IrVariable,
        oldLoop: IrLoop?
    ): Pair<(IrVariable) -> IrContainerExpression, IrLoop> {
        loopBody.statements.remove(oldLoopVariable)
        return updateLoopVariableInBody(
            builderWithParent.first,
            oldLoopVariable,
            loopBody,
            oldLoop,
            builderWithParent.second
        )
    }

    private fun createIteratorReplacement(
        builderWithParent: IrBuilderWithParent,
        elementType: IrType,
    ): IteratorReplacement {
        val builder = builderWithParent.first
        val parent = builderWithParent.second
        val generatingFunction = source.generatingFunction
        val oneArgumentIteratingFunction: (IrVariable) -> IrExpression = { variable ->
            builder.callRichFunctionReference(generatingFunction, parent, builder.irAsNotNull(builder.irGet(variable)))
        }
        val zeroArgumentIteratingFunction: (IrVariable) -> IrExpression = { _ ->
            builder.callRichFunctionReference(generatingFunction, parent)
        }

        val results = when (val initialValue = source.initialValue) {
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
        return IteratorReplacement.create(results.first, results.second, builder, elementType)
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
                elementType: IrType,
            ): IteratorReplacement = with(builder) {
                val iteratorVariable = scope.createTemporaryVariable(
                    initialExpression,
                    isMutable = true,
                    irType = elementType.makeNullable(),
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
