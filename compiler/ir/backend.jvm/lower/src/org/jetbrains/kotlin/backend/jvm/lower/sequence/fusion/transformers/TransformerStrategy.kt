/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression

sealed class TakeOrDrop {
    object Take : TakeOrDrop()
    object Drop : TakeOrDrop()
}

internal typealias UnaryPredicate = (IrBuilderWithParent) -> (IrValueDeclaration) -> IrExpression
internal typealias BinaryPredicate = (IrBuilderWithParent) -> (IrValueDeclaration, IrValueDeclaration) -> IrExpression

internal sealed class MapPredicateCall {
    class Indexed(val predicate: BinaryPredicate) : MapPredicateCall()
    class NonIndexed(val predicate: UnaryPredicate) : MapPredicateCall()
}

internal sealed class SequenceTransformer {
    class Map(
        val predicateCall: MapPredicateCall,
        val isIndexed: Boolean,
        val isNotNull: Boolean,
    ) : SequenceTransformer()

    class Filter(
        val predicateCall: UnaryPredicate
    ) : SequenceTransformer()

    class Take(
        val argument: IrExpression,
        val takeType: TakeOrDrop,
    ) : SequenceTransformer()

    class TakeWhile(
        val predicateCall: UnaryPredicate,
        val takeType: TakeOrDrop,
    ) : SequenceTransformer()
}

internal abstract class TransformerStrategy(val builderWithParent: IrBuilderWithParent) {
    abstract fun addTransformerToBodyBuilder(
        sequenceReplacement: SequenceReplacement,
    ): SequenceReplacement

    companion object {
        fun create(sequenceTransformer: SequenceTransformer, builderWithParent: IrBuilderWithParent): TransformerStrategy =
            when (sequenceTransformer) {
                is SequenceTransformer.Map -> MapStrategy(sequenceTransformer, builderWithParent)
                is SequenceTransformer.Filter -> FilterStrategy(sequenceTransformer, builderWithParent)
                is SequenceTransformer.Take -> TakeStrategy(sequenceTransformer, builderWithParent)
                is SequenceTransformer.TakeWhile -> TakeWhileStrategy(sequenceTransformer, builderWithParent)
            }
    }
}
