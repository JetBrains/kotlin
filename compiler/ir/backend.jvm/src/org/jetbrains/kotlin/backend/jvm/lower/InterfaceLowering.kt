/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering.Companion.clinitName
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

internal val interfacePhase = makeIrFilePhase(
    ::InterfaceLowering,
    name = "Interface",
    description = "Move default implementations of interface members to DefaultImpls class"
)

private class InterfaceLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state = context.state

    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)
        irClass.declarations.add(defaultImplsIrClass)
        val members = defaultImplsIrClass.declarations

        for (function in irClass.declarations) {
            if (function !is IrSimpleFunction) continue

            if (function.modality != Modality.ABSTRACT && function.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                val element = context.declarationFactory.getDefaultImplsFunction(function)
                members.add(element)
                element.body = function.body
                function.body = null
                //TODO reset modality to abstract
            }
        }

        irClass.transformChildrenVoid(this)

        irClass.declarations.removeAll {
            it is IrFunction && shouldRemoveFunction(it)
        }
    }

    private fun shouldRemoveFunction(function: IrFunction): Boolean =
        Visibilities.isPrivate(function.visibility) && function.name != clinitName ||
                function.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER ||
                function.origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS
}


internal fun createStaticFunctionWithReceivers(
    irParent: IrDeclarationParent,
    name: Name,
    oldFunction: IrFunction,
    dispatchReceiverType: IrType? = oldFunction.dispatchReceiverParameter?.type,
    origin: IrDeclarationOrigin = oldFunction.origin
): IrSimpleFunction {
    val descriptor = WrappedSimpleFunctionDescriptor(Annotations.EMPTY, oldFunction.descriptor.source)
    return IrFunctionImpl(
        oldFunction.startOffset, oldFunction.endOffset,
        origin,
        IrSimpleFunctionSymbolImpl(descriptor),
        name,
        oldFunction.visibility,
        Modality.FINAL,
        oldFunction.returnType,
        isInline = false, isExternal = false, isTailrec = false, isSuspend = false
    ).apply {
        descriptor.bind(this)
        parent = irParent

        copyTypeParametersFrom(oldFunction)

        var offset = 0
        val dispatchReceiver = oldFunction.dispatchReceiverParameter?.copyTo(
            this,
            name = Name.identifier("this"),
            index = offset++,
            type = dispatchReceiverType!!
        )
        val extensionReceiver = oldFunction.extensionReceiverParameter?.copyTo(
            this,
            name = Name.identifier("receiver"),
            index = offset++
        )
        valueParameters.addAll(listOfNotNull(dispatchReceiver, extensionReceiver) +
                                       oldFunction.valueParameters.map { it.copyTo(this, index = it.index + offset) }
        )

        val mapping: Map<IrValueParameter, IrValueParameter> =
            (listOfNotNull(oldFunction.dispatchReceiverParameter, oldFunction.extensionReceiverParameter) + oldFunction.valueParameters)
                .zip(valueParameters).toMap()
        body = oldFunction.body?.transform(VariableRemapper(mapping), null)

        metadata = oldFunction.metadata
    }
}
