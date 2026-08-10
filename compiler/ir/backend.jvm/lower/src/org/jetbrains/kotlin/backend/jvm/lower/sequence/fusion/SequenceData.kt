/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.EmptySequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.GenerateSequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.ProducerStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.SequenceOfStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.UnknownVariableStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers.SequenceTransformer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.irFlag
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

internal class SequenceData(
    val sequenceSource: SequenceSource,
    val transformers: List<SequenceTransformer> = emptyList(),
    val isMovable: Boolean = true,
    // avoid exponential blowup by detecting copies of sequence variables
    val containsVariableCopies: Boolean = sequenceSource.containsVariableCopies(),
    val isVariableCopy: Boolean = false,
) {
    // whether reading a variable with this data copies anything to the place of the read
    val isCopiedToReads: Boolean
        get() = sequenceSource !is SequenceSource.Variable || transformers.isNotEmpty()

    fun asVariableCopy(): SequenceData =
        SequenceData(sequenceSource, transformers, isMovable, containsVariableCopies, isVariableCopy = true)
}

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
internal sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>, val type: IrType) : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
    class GenerateSequence(
        val initialValue: GenerateSequenceInitialValue,
        val generatingFunction: IrExpression,
        val sequenceElementType: IrType
    ) : SequenceSource()

    object Empty : SequenceSource()

    internal fun containsVariableCopies(): Boolean = when (this) {
        is SequenceOf -> elements.any { it.containsFusedVariableCopy() }
        is GenerateSequence -> generatingFunction.containsFusedVariableCopy() || when (initialValue) {
            is GenerateSequenceInitialValue.Seed -> initialValue.expression.containsFusedVariableCopy()
            is GenerateSequenceInitialValue.SeedFunction -> initialValue.function.containsFusedVariableCopy()
            GenerateSequenceInitialValue.NextFunction -> false
        }
        is Variable, Empty -> false
    }

    internal fun createProducerStrategy(
        builder: IrBuilderWithScope,
        context: JvmBackendContext,
    ): ProducerStrategy = when (this) {
        is GenerateSequence -> GenerateSequenceStrategy(this)
        is SequenceOf -> SequenceOfStrategy(this, context)
        is Empty -> EmptySequenceStrategy
        is Variable -> UnknownVariableStrategy(builder.irGet(this.variable.owner), context)
    }
}

// marks the fused code of a sequence copied from a sequence variable; copied along with the code
internal var IrElement.isFusedVariableCopy: Boolean by irFlag(copyByDefault = true)

// the consumers are fused bottom-up, so the reads of sequence variables in an expression are already replaced with their fused copies
internal fun IrElement.containsFusedVariableCopy(): Boolean {
    var result = false
    acceptVoid(object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            if (element.isFusedVariableCopy) result = true
            if (!result) element.acceptChildrenVoid(this)
        }
    })
    return result
}
