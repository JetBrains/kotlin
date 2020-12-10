/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCompositeImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isStringClassType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal val constPhase = makeIrFilePhase(
    ::ConstLowering,
    name = "Const",
    description = "Substitute calls to const properties with constant values"
)

fun IrField.constantValue(context: JvmBackendContext? = null): IrConst<*>? {
    val value = initializer?.expression as? IrConst<*> ?: return null
    // JVM has a ConstantValue attribute which does two things:
    //   1. allows the field to be inlined into other modules;
    //   2. implicitly generates an initialization of that field in <clinit>
    // It is only allowed on final fields of primitive/string types. Java and Kotlin < 1.4
    // apply it whenever possible; Kotlin >= 1.4 only applies it to `const val`s to avoid making
    // values part of the library's ABI unless explicitly requested by the author.
    val allowImplicitConst =
        context != null && !context.state.languageVersionSettings.supportsFeature(LanguageFeature.NoConstantValueAttributeForNonConstVals)
    val implicitConst = isFinal && ((isStatic && origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) ||
            (allowImplicitConst && (type.isPrimitiveType() || type.isStringClassType())))
    return if (implicitConst || correspondingPropertySymbol?.owner?.isConst == true) value else null
}

class ConstLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    private fun IrExpression.lowerConstRead(receiver: IrExpression?, field: IrField?): IrExpression? {
        val value = field?.constantValue() ?: return null
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
