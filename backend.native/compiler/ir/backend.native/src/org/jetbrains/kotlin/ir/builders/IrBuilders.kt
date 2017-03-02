package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.types.KotlinType

inline fun IrBuilderWithScope.irLetSequence(
        value: IrExpression,
        nameHint: String? = null,
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset,
        origin: IrStatementOrigin? = null,
        resultType: KotlinType? = null,
        body: IrBlockBuilder.(VariableDescriptor) -> Unit
): IrExpression = irBlock(startOffset, endOffset, origin, resultType) {
    val irTemporary = defineTemporary(value, nameHint)
    this.body(irTemporary)
}

fun IrBuilderWithScope.irWhile(origin: IrStatementOrigin? = null) =
        IrWhileLoopImpl(startOffset, endOffset, context.builtIns.unitType, origin)

fun IrBuilderWithScope.irBreak(loop: IrLoop) =
        IrBreakImpl(startOffset, endOffset, context.builtIns.nothingType, loop)

fun IrBuilderWithScope.irContinue(loop: IrLoop) =
        IrContinueImpl(startOffset, endOffset, context.builtIns.nothingType, loop)

fun IrBuilderWithScope.irTrue() = IrConstImpl.boolean(startOffset, endOffset, context.builtIns.booleanType, true)

fun IrBuilderWithScope.irGet(value: ValueDescriptor) =
        IrGetValueImpl(startOffset, endOffset, value)
