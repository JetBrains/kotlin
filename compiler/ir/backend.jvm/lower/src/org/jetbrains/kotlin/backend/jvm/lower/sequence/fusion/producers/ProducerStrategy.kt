/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceData
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irWhile
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrBreakContinue
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrWhileLoop
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal typealias UpdateLoopVariableResult = Pair<(IrValueDeclaration) -> IrContainerExpression, IrLoop>

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

class VariableSubstitutionTransformer(
    private val oldSymbol: IrValueSymbol,
    private val newSymbol: IrValueSymbol,
    val newLoop: IrLoop,
    val oldLoop: IrLoop?,
) : IrElementTransformerVoidWithContext() {

    override fun visitGetValue(expression: IrGetValue): IrGetValue {
        val copied = super.visitGetValue(expression) as IrGetValue
        if (expression.symbol == oldSymbol) {
            copied.symbol = newSymbol
        }
        return copied
    }

    override fun visitSetValue(expression: IrSetValue): IrSetValue {
        val copied = super.visitSetValue(expression) as IrSetValue
        if (expression.symbol == oldSymbol) {
            copied.symbol = newSymbol
        }
        return copied
    }

    override fun visitBreakContinue(jump: IrBreakContinue): IrExpression {
        val copied = super.visitBreakContinue(jump) as IrBreakContinue
        if (copied.loop == oldLoop) {
            copied.loop = newLoop
        }
        return copied
    }
}

internal sealed class ProducerStrategy {
    abstract fun fuseConsumer(
        builderWithParent: IrBuilderWithParent,
        sequenceData: SequenceData,
        sequenceReplacement: SequenceReplacement,
    ): IrContainerExpression?
}

internal fun updateLoopVariableInBody(
    builder: IrBuilderWithScope,
    oldLoopVariable: IrValueDeclaration,
    body: IrContainerExpression,
    oldLoop: IrLoop?,
    parent: IrDeclarationParent,
): UpdateLoopVariableResult {
    val newLoop = builder.createSequenceWhile().apply {
        this.label = oldLoop?.label ?: "sequence_while_loop"
    }
    return { newInnerLoopVariable: IrValueDeclaration ->
        body.transformChildrenVoid(
            VariableSubstitutionTransformer(
                oldLoopVariable.symbol,
                newInnerLoopVariable.symbol,
                newLoop,
                oldLoop,
            )
        )
        body.patchDeclarationParents(parent)
    } to newLoop
}
