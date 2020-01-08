/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.getNameForDestructuredParameterOrNull
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal val renameAnonymousParametersLowering = makeIrFilePhase(
    ::RenameAnonymousParametersLowering,
    name = "RenameAnonymousParameters",
    description = "Mangles variable names for anonymous parameters (only allowed in lambdas)"
)

private class RenameAnonymousParametersLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoidWithContext() {

    val oldParameterToNew: MutableMap<IrValueParameter, IrValueParameter> = mutableMapOf()

    override fun lower(irFile: IrFile) = irFile.transformChildrenVoid()

    override fun visitValueParameter(declaration: IrValueParameter) =
        declaration.computeNewParameterName()?.let { name ->
            val descriptor = if (declaration.descriptor is ReceiverParameterDescriptor) {
                WrappedReceiverParameterDescriptor(declaration.descriptor.annotations)
            } else {
                WrappedValueParameterDescriptor(declaration.descriptor.annotations)
            }
            IrValueParameterImpl(
                declaration.startOffset, declaration.endOffset,
                declaration.origin,
                IrValueParameterSymbolImpl(descriptor),
                name,
                declaration.index,
                declaration.type,
                declaration.varargElementType,
                declaration.isCrossinline,
                declaration.isNoinline
            ).apply {
                descriptor.bind(this)
                parent = declaration.parent
                defaultValue = declaration.defaultValue
                annotations += declaration.annotations
                oldParameterToNew[declaration] = this
            }
        } ?: super.visitValueParameter(declaration)

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val oldParameter = expression.symbol.owner
        return oldParameterToNew[oldParameter]?.let {
            IrGetValueImpl(expression.startOffset, expression.endOffset, it.type, it.symbol)
        } ?: super.visitGetValue(expression)
    }

    private fun IrValueParameter.computeNewParameterName(): Name? {
        // Consistent with naming on old (non-IR) backend; see FunctionCodegen.java.
        descriptor.safeAs<ValueParameterDescriptor>()?.let {
            getNameForDestructuredParameterOrNull(it)?.let { return Name.identifier(it) }
            if (DescriptorToSourceUtils.getSourceFromDescriptor(it)?.safeAs<KtParameter>()?.isSingleUnderscore == true) {
                return Name.identifier("\$noName_${it.index}")
            }
        }
        return null
    }
}
