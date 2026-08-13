/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.expressions.IrExpression


internal sealed class SequenceTransformer {
    class Map(
        val function: IrExpression,
        val isIndexed: Boolean,
        val isNotNull: Boolean,
    ) : SequenceTransformer()
}

internal abstract class TransformerStrategy(val builderWithParent: IrBuilderWithParent) {
    abstract fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement

    companion object {
        fun create(
            sequenceTransformer: SequenceTransformer,
            builderWithParent: IrBuilderWithParent,
            context: JvmBackendContext
        ): TransformerStrategy =
            when (sequenceTransformer) {
                is SequenceTransformer.Map -> MapStrategy(sequenceTransformer, builderWithParent, context)
            }
    }
}
