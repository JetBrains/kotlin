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
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class InterfaceLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state = context.state

    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)
        irClass.declarations.add(defaultImplsIrClass)
        val members = defaultImplsIrClass.declarations

        irClass.declarations.filterIsInstance<IrFunction>().forEach {
            if (it is IrSimpleFunction && it.modality != Modality.ABSTRACT && it.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                val element = context.declarationFactory.getDefaultImplsFunction(it)
                members.add(element)
                element.body = it.body
                it.body = null
                //TODO reset modality to abstract
            }
        }

        irClass.transformChildrenVoid(this)

        //REMOVE private methods
        val privateToRemove = irClass.declarations.filterIsInstance<IrFunction>().filter {
            Visibilities.isPrivate(it.visibility) && (it as? IrSimpleFunction)?.name != clinitName
        }

        val defaultBodies = irClass.declarations.filterIsInstance<IrFunction>().filter {
            it.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER
        }
        irClass.declarations.removeAll(privateToRemove)
        irClass.declarations.removeAll(defaultBodies)
    }
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
    }
}
