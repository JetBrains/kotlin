/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceTransformer

internal abstract class TransformerStrategy(val builderWithParent: IrBuilderWithParent) {
    abstract fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement

    companion object {
        fun create(sequenceTransformer: SequenceTransformer, builderWithParent: IrBuilderWithParent): TransformerStrategy =
            when (sequenceTransformer) {
                is SequenceTransformer.Map -> MapStrategy(sequenceTransformer, builderWithParent)
                is SequenceTransformer.Filter -> FilterStrategy(sequenceTransformer, builderWithParent)
            }
    }
}
