/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.constantValue
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.incremental.components.InlineConstTracker
import org.jetbrains.kotlin.incremental.components.ConstantRef
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.load.kotlin.toSourceElement

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

class ConstLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    val inlineConstTracker =
        context.state.configuration[CommonConfigurationKeys.INLINE_CONST_TRACKER] ?: InlineConstTracker.DoNothing

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.lowerConstRead(receiver: IrExpression?, field: IrField?): IrExpression? {
        val value = field?.constantValue() ?: return null
        transformChildrenVoid()
        reportInlineConst(this@lowerConstRead, field)

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

    private fun reportInlineConst(irExpression: IrExpression, field: IrField?) {
        if (inlineConstTracker == InlineConstTracker.DoNothing) return
        val value = field?.constantValue() ?: return

        if (!field.isFinal || !field.isStatic) return

        val sourceFile = field.toIrBasedDescriptor().containingDeclaration.toSourceElement.containingFile
        if (sourceFile == SourceFile.NO_SOURCE_FILE || sourceFile.toString().lowercase().endsWith(".kt")) return

        for (file: KtFile in context.state.files) {
            val fileName = file.virtualFilePath
            val owner =
                ((irExpression as? IrGetFieldImpl)?.symbol?.signature as? IdSignature.CompositeSignature)?.container?.asPublic()?.firstNameSegment
                    ?: continue
            val name = field.name.asString()
            val constType = value.kind.toString()

            inlineConstTracker.report(
                fileName,
                listOf(ConstantRef(owner, name, constType))
            )
        }
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
