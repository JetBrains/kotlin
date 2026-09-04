/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.transformers

import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.FilterVersion
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.IrBuilderWithParent
import org.jetbrains.kotlin.backend.jvm.lower.sequence.fusion.SequenceReplacement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfThen
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols

sealed class TakeOrDrop {
    object Take : TakeOrDrop()
    object Drop : TakeOrDrop()
}

internal sealed class SequenceTransformer {
    class Map(
        val function: IrExpression,
        val isIndexed: Boolean,
        val isNotNull: Boolean,
    ) : SequenceTransformer()

    class Filter(
        // null for `filterNotNull`
        val function: IrExpression?,
        val version: FilterVersion,
    ) : SequenceTransformer()

    class Take(
        val argument: IrExpression,
    ) : SequenceTransformer()

    class Drop(
        val argument: IrExpression,
    ) : SequenceTransformer()

    class TakeWhile(
        val function: IrExpression,
        val takeOrDrop: TakeOrDrop,
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
                is SequenceTransformer.Filter -> FilterStrategy(sequenceTransformer, builderWithParent)
                is SequenceTransformer.Take -> TakeStrategy(sequenceTransformer, builderWithParent, context)
                is SequenceTransformer.Drop -> DropStrategy(sequenceTransformer, builderWithParent, context)
                is SequenceTransformer.TakeWhile -> TakeWhileStrategy(sequenceTransformer, builderWithParent)
            }
    }
}

/**
 * The state shared by `take(n)` and `drop(n)`: the number of elements seen so far and the evaluated `n`,
 * together with the check that `n` is not negative, which has to be evaluated before the sequence is iterated.
 */
internal class ElementCounter(
    val count: IrVariable,
    val argument: IrVariable,
    val negativeArgumentCheck: IrExpression,
)

internal fun IrBuilderWithParent.createElementCounter(argument: IrExpression): ElementCounter {
    val [builder, parent] = this
    with(builder) {
        val count = scope.createTemporaryVariable(
            irInt(0),
            isMutable = true,
            nameHint = "takeVar"
        )
        val argumentVariable = scope.createTemporaryVariable(
            argument.deepCopyWithSymbols(parent),
            nameHint = "takeArgument",
        )
        val throwExpression = irThrow(
            irCall(context.irBuiltIns.illegalArgumentExceptionSymbol.owner).apply {
                arguments[0] = irConcat().apply {
                    addArgument(irString("Requested element count "))
                    addArgument(irGet(argumentVariable))
                    addArgument(irString(" is less than zero."))
                }
            }
        )
        val negativeArgumentCheck = irIfThen(
            type = context.irBuiltIns.unitType,
            irIntLess(irGet(argumentVariable), irInt(0)),
            throwExpression
        )
        return ElementCounter(count, argumentVariable, negativeArgumentCheck)
    }
}

internal fun IrBuilderWithScope.irIntLess(left: IrExpression, right: IrExpression): IrExpression =
    irCall(context.irBuiltIns.lessFunByOperandType.getValue(context.irBuiltIns.intClass)).apply {
        arguments[0] = left
        arguments[1] = right
    }

internal fun IrBuilderWithScope.irIntLessOrEqual(left: IrExpression, right: IrExpression): IrExpression =
    irCall(context.irBuiltIns.lessOrEqualFunByOperandType.getValue(context.irBuiltIns.intClass)).apply {
        arguments[0] = left
        arguments[1] = right
    }

internal fun IrBuilderWithScope.irOrOr(left: IrExpression, right: IrExpression): IrExpression =
    irCall(context.irBuiltIns.ororSymbol).apply {
        arguments[0] = left
        arguments[1] = right
    }

/** `previous || condition`, or just `condition` if there is no previous check. */
internal fun IrBuilderWithScope.orWithPrevious(previous: IrExpression?, condition: IrExpression): IrExpression =
    previous?.let { irOrOr(it, condition) } ?: condition
