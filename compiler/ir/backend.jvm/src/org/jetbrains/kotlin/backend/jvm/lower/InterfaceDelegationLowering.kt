/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptor
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptorImpl
import org.jetbrains.kotlin.codegen.isDefinitelyNotDefaultImplsMethod
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.types.toIrType
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class InterfaceDelegationLowering(val state: GenerationState) : IrElementTransformerVoid(), ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface) return

        irClass.transformChildrenVoid(this)
        generateInterfaceMethods(irClass, irClass.descriptor)
    }


    private fun generateInterfaceMethods(irClass: IrClass, descriptor: ClassDescriptor) {
        val classDescriptor = (descriptor as? DefaultImplsClassDescriptor)?.correspondingInterface ?: descriptor
        for ((interfaceFun, value) in CodegenUtil.getNonPrivateTraitMethods(classDescriptor)) {
            //skip java 8 default methods
            if (!interfaceFun.isDefinitelyNotDefaultImplsMethod()) {
                val inheritedFun =
                    if (classDescriptor !== descriptor) {
                        InterfaceLowering.createDefaultImplFunDescriptor(
                            descriptor as DefaultImplsClassDescriptorImpl,
                            interfaceFun,
                            classDescriptor,
                            state.typeMapper
                        )
                    } else {
                        value
                    }
                generateDelegationToDefaultImpl(irClass, interfaceFun, inheritedFun)
            }
        }
    }

    private fun generateDelegationToDefaultImpl(irClass: IrClass, interfaceFun: FunctionDescriptor, inheritedFun: FunctionDescriptor) {
        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val irFunction = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, inheritedFun, irBody)
        irFunction.createParameterDeclarations()
        irClass.declarations.add(irFunction)

        val interfaceDescriptor = interfaceFun.containingDeclaration as ClassDescriptor
        val defaultImpls = InterfaceLowering.createDefaultImplsClassDescriptor(interfaceDescriptor)
        val defaultImplFun =
            InterfaceLowering.createDefaultImplFunDescriptor(defaultImpls, interfaceFun.original, interfaceDescriptor, state.typeMapper)

        val irCallImpl =
            IrCallImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                defaultImplFun.returnType!!.toIrType()!!,
                defaultImplFun,
                defaultImplFun.typeParametersCount,
                JvmLoweredStatementOrigin.DEFAULT_IMPLS_DELEGATION
            )
        irBody.statements.add(
            IrReturnImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                irFunction.symbol.owner.returnType,
                irFunction.symbol,
                irCallImpl
            )
        )

        var offset = 0
        irFunction.dispatchReceiverParameter?.let {
            irCallImpl.putValueArgument(
                offset,
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
            )
            offset++
        }

        irFunction.extensionReceiverParameter?.let {
            irCallImpl.putValueArgument(
                offset,
                IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.symbol)
            )
            offset++
        }

        irFunction.valueParameters.mapIndexed { i, parameter ->
            irCallImpl.putValueArgument(i + offset, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol, null))
        }
    }

}
