/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val receiverParameterNameManglingPhase = makeIrFilePhase(
    ::ReceiverParameterNameManglingLowering,
    name = "ReceiverParameterNameMangling",
    description = "Mangles variable names for (captured) receiver parameters of objects and lambdas"
)

private class ReceiverParameterNameManglingLowering(val context: JvmBackendContext) : FileLoweringPass,
    IrElementTransformerVoidWithContext() {

    val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitValueParameterNew(declaration: IrValueParameter) =
        declaration.descriptor.safeAs<ReceiverParameterDescriptor>()?.let { oldDescriptor ->
            val newDescriptor = WrappedReceiverParameterDescriptor(oldDescriptor.annotations)
            IrValueParameterImpl(
                declaration.startOffset, declaration.endOffset,
                declaration.origin,
                IrValueParameterSymbolImpl(oldDescriptor),
                mangleReceiverName(declaration),
                declaration.index,
                declaration.type,
                declaration.varargElementType,
                declaration.isCrossinline,
                declaration.isNoinline
            ).apply {
                newDescriptor.bind(this)
                parent = declaration.parent
                defaultValue = declaration.defaultValue
                annotations += declaration.annotations
                oldParameterToNew[declaration] = this
            }
        } ?: super.visitValueParameterNew(declaration)

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val oldParameter = expression.symbol.owner
        return oldParameterToNew[oldParameter]?.let {
            IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol)
        } ?: super.visitGetValue(expression)
    }

    private fun mangleReceiverName(declaration: IrValueParameter): Name {
        if (context.state.languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
            declaration.variableNameHint?.let {
                return Name.identifier("\$this\$$it")
            }
            return declaration.name
        } else {
            return declaration.name
        }
    }
}
