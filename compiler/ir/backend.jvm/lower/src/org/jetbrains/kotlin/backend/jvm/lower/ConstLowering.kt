/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.copyWithOffsets
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal val constPhase1 = makeIrFilePhase(
    ::ConstLowering,
    name = "Const1",
    description = "Substitute calls to const properties with constant values"
)

internal val constPhase2 = makeIrFilePhase(
    ::ConstLowering,
    name = "Const2",
    description = "Substitute calls to const properties with constant values"
)

class ConstLowering(val context: JvmBackendContext) : FileLoweringPass {
    val inlineConstTracker =
        context.state.configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER]

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid(ConstTransformer(irFile, context, inlineConstTracker))
}

private class ConstTransformer(
    val irFile: IrFile,
    val context: JvmBackendContext,
    val inlineConstTracker: InlineConstTracker?
) : IrElementTransformerVoid() {
    private fun IrExpression.lowerConstRead(receiver: IrExpression?, field: IrField?): IrExpression? {
        val value = field?.constantValue() ?: return null
        transformChildrenVoid()
        reportInlineConst(field, value)

        val resultExpression = if (context.state.shouldInlineConstVals)
            value.copyWithOffsets(startOffset, endOffset)
        else
            IrGetFieldImpl(startOffset, endOffset, field.symbol, field.type)

        return if (receiver == null || receiver.shouldDropConstReceiver())
            resultExpression
        else
            IrCompositeImpl(
                startOffset, endOffset, resultExpression.type, null,
                listOf(receiver, resultExpression)
            )
    }

    private fun reportInlineConst(field: IrField, value: IrConst<*>) {
        if (inlineConstTracker == null) return
        if (field.origin != IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) return

        val path = irFile.path
        val owner = field.parentAsClass.classId?.asString()?.replace(".", "$")?.replace("/", ".") ?: return
        val name = field.name.asString()
        val constType = value.kind.asString

        inlineConstTracker.report(path, owner, name, constType)
    }

    private fun IrExpression.shouldDropConstReceiver() =
        this is IrConst<*> || this is IrGetValue ||
                this is IrGetObjectValue

    override fun visitCall(expression: IrCall): IrExpression {
        val function = (expression.symbol.owner as? IrSimpleFunction) ?: return super.visitCall(expression)
        val property = function.correspondingPropertySymbol?.owner ?: return super.visitCall(expression)
        // If `constantValue` is not null, `function` can only be the getter because the property is immutable.
        return expression.lowerConstRead(expression.dispatchReceiver, property.backingField) ?: super.visitCall(expression)
    }

    override fun visitGetField(expression: IrGetField): IrExpression =
        expression.lowerConstRead(expression.receiver, expression.symbol.owner) ?: super.visitGetField(expression)
}