package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression

inline fun IrBlockBuilder.irLetSequence(
        value: IrExpression,
        nameHint: String? = null,
        body: IrBlockBuilder.(VariableDescriptor) -> Unit
): IrExpression  = block {
    val irTemporary = defineTemporary(value, nameHint)
    this.body(irTemporary)
}
