/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptor
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptorImpl
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.isDefinitelyNotDefaultImplsMethod
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils


class InterfaceDelegationLowering(val state: GenerationState) : IrElementTransformerVoid(), ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val descriptor = irClass.descriptor
        if (DescriptorUtils.isInterface(descriptor)) {
            return
        }

        irClass.transformChildrenVoid(this)
        generateInterfaceMethods(irClass, descriptor)
    }


    private fun generateInterfaceMethods(irClass: IrClass, descriptor: ClassDescriptor) {
        val classDescriptor = if (descriptor is DefaultImplsClassDescriptor) descriptor.correspondingInterface else descriptor
        for ((interfaceFun, value) in CodegenUtil.getNonPrivateTraitMethods(classDescriptor)) {
            //skip java 8 default methods
            if (!interfaceFun.isDefinitelyNotDefaultImplsMethod()) {
                val inheritedFun =
                        if (classDescriptor !== descriptor) {
                            InterfaceLowering.createDefaultImplFunDescriptor(descriptor as DefaultImplsClassDescriptorImpl, interfaceFun, classDescriptor, state.typeMapper)
                        }
                        else {
                            value
                        }
                generateDelegationToDefaultImpl(irClass, interfaceFun, inheritedFun)
            }
        }
    }

    private fun generateDelegationToDefaultImpl(irClass: IrClass, interfaceFun: FunctionDescriptor, inheritedFun: FunctionDescriptor) {
        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        val irFunction = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrDeclarationOrigin.DEFINED, inheritedFun, irBody)
        irClass.declarations.add(irFunction)

        val interfaceDescriptor = interfaceFun.containingDeclaration as ClassDescriptor
        val defaultImpls = InterfaceLowering.createDefaultImplsClassDescriptor(interfaceDescriptor)
        val defaultImplFun = InterfaceLowering.createDefaultImplFunDescriptor(defaultImpls, interfaceFun.original, interfaceDescriptor, state.typeMapper)
        val returnType = inheritedFun.returnType!!
        val irCallImpl = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, returnType, defaultImplFun, null, JvmLoweredStatementOrigin.DEFAULT_IMPLS_DELEGATION)
        irBody.statements.add(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, returnType, inheritedFun, irCallImpl))

        var shift = 0
        if (inheritedFun.dispatchReceiverParameter != null) {
            irCallImpl.putValueArgument(0, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, interfaceFun.dispatchReceiverParameter!!))
            shift = 1
        }
        inheritedFun.valueParameters.mapIndexed { i, valueParameterDescriptor ->
            irCallImpl.putValueArgument(i + shift, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, valueParameterDescriptor, null))
        }
    }

}
