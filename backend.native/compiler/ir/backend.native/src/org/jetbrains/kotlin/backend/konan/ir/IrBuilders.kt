package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.types.KotlinType

inline fun IrBuilderWithScope.irLetSequence(
        value: IrExpression,
        nameHint: String? = null,
        startOffset: Int = this.startOffset,
        endOffset: Int = this.endOffset,
        origin: IrStatementOrigin? = null,
        resultType: KotlinType? = null,
        body: IrBlockBuilder.(VariableDescriptor) -> Unit
): IrExpression  = irBlock(startOffset, endOffset, origin, resultType) {
    val irTemporary = defineTemporary(value, nameHint)
    this.body(irTemporary)
}