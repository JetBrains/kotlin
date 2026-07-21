/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.GenerateSequenceStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.ProducerStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.SequenceOfStrategy
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.producers.UnknownVariableStrategy
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.IrType

internal typealias MapPredicate = (IrBuilderWithParent) -> (IrValueDeclaration) -> IrExpression
internal typealias MapIndexedPredicate = (IrBuilderWithParent) -> (IrValueDeclaration, IrValueDeclaration) -> IrExpression

internal sealed class MapPredicateCall {
    class Indexed(val predicate: MapIndexedPredicate) : MapPredicateCall()
    class NonIndexed(val predicate: MapPredicate) : MapPredicateCall()
}

sealed class TakeType {
    object Take : TakeType()
    object Drop : TakeType()
}

internal sealed class SequenceTransformer {
    class Map(
        val predicateCall: MapPredicateCall,
        val isIndexed: Boolean,
        val isNotNull: Boolean,
        val startOffset: Int,
        val endOffset: Int,
    ) :
        SequenceTransformer()

    class Filter(
        val predicateCall: (IrBuilderWithParent) -> (IrValueDeclaration) -> IrExpression,
        val startOffset: Int,
        val endOffset: Int,
    ) : SequenceTransformer()

    class Take(
        val argument: IrExpression,
        val startOffset: Int,
        val endOffset: Int,
        val takeType: TakeType,
    ) : SequenceTransformer()

    class TakeWhile(
        val predicateCall: (IrBuilderWithParent) -> (IrValueDeclaration) -> IrExpression,
        val startOffset: Int,
        val endOffset: Int,
        val takeType: TakeType,
    ) : SequenceTransformer()
}

internal class SequenceData(
    val sequenceSource: SequenceSource,
    val transformers: List<SequenceTransformer>
)

// sequenceSource is what the sequence was created from, to be substituted if the loop is to be fused
internal sealed class SequenceSource {
    class SequenceOf(val elements: List<IrExpression>, val type: IrType) : SequenceSource()
    class Variable(val variable: IrValueSymbol) : SequenceSource()
    class AsSequence(val iterable: IrExpression) : SequenceSource()
    class GenerateSequence(
        val initialValue: GenerateSequenceInitialValue,
        val generatingFunction: IrExpression,
        val sequenceElementType: IrType
    ) : SequenceSource()

    internal fun createProducerStrategy(
        builder: IrBuilderWithScope,
        context: JvmBackendContext,
    ): ProducerStrategy = when (this) {
        is AsSequence -> UnknownVariableStrategy(this.iterable)
        is GenerateSequence -> GenerateSequenceStrategy(this)
        is SequenceOf -> SequenceOfStrategy(this)
        is Variable -> UnknownVariableStrategy(builder.irGet(this.variable.owner))
    }
}
