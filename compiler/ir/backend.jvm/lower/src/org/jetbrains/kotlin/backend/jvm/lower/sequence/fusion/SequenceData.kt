/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.EmptySequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.ProducerStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.SequenceOfStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers.SequenceTransformer
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType


internal class SequenceData(
    val sequenceSource: SequenceSource,
    val transformers: List<SequenceTransformer> = emptyList()
)

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
internal sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>, val type: IrType) : SequenceSource()
    object Empty : SequenceSource()


    internal fun createProducerStrategy(
        builder: IrBuilderWithScope,
        context: JvmBackendContext,
    ): ProducerStrategy = when (this) {
        is SequenceOf -> SequenceOfStrategy(this)
        is Empty -> EmptySequenceStrategy
    }
}
