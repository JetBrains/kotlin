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
import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptorImpl
import org.jetbrains.kotlin.backend.jvm.lower.InitializersLowering.Companion.clinitName
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes

class InterfaceLowering(val state: GenerationState) : IrElementTransformerVoid(), ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (!DescriptorUtils.isInterface(irClass.descriptor)) {
            return
        }

        val interfaceDescriptor = irClass.descriptor
        val defaultImplsDescriptor = createDefaultImplsClassDescriptor(interfaceDescriptor)
        val defaultImplsIrClass = IrClassImpl(irClass.startOffset, irClass.endOffset, JvmLoweredDeclarationOrigin.DEFAULT_IMPLS, defaultImplsDescriptor)
        irClass.declarations.add(defaultImplsIrClass)

        val members = defaultImplsIrClass.declarations

        irClass.declarations.filterIsInstance<IrFunction>().forEach {
            val descriptor = it.descriptor
            if (descriptor.modality != Modality.ABSTRACT) {
                val functionDescriptorImpl = createDefaultImplFunDescriptor(defaultImplsDescriptor, descriptor, interfaceDescriptor, state.typeMapper)
                members.add(functionDescriptorImpl.createFunctionAndMapVariables(it))
                it.body = null
            }
        }


        irClass.transformChildrenVoid(this)

        //REMOVE private methods
        val privateToRemove = irClass.declarations.filterIsInstance<IrFunction>().mapNotNull {
            val visibility = AsmUtil.getVisibilityAccessFlag(it.descriptor)
            if (visibility == Opcodes.ACC_PRIVATE && it.descriptor.name != clinitName) {
                it
            }
            else null
        }

        val defaultBodies = irClass.declarations.filterIsInstance<IrFunction>().filter {
            it.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
        }
        irClass.declarations.removeAll(privateToRemove)
        irClass.declarations.removeAll(defaultBodies)
    }

    companion object {

        fun createDefaultImplsClassDescriptor(interfaceDescriptor: ClassDescriptor): DefaultImplsClassDescriptorImpl {
            return DefaultImplsClassDescriptorImpl(
                    Name.identifier(JvmAbi.DEFAULT_IMPLS_CLASS_NAME), interfaceDescriptor, interfaceDescriptor.source
            )
        }

        fun createDefaultImplFunDescriptor(
                defaultImplsDescriptor: DefaultImplsClassDescriptorImpl,
                descriptor: FunctionDescriptor,
                interfaceDescriptor: ClassDescriptor, typeMapper: KotlinTypeMapper
        ): SimpleFunctionDescriptorImpl {
            val name = Name.identifier(typeMapper.mapAsmMethod(descriptor).name)
            return createStaticFunctionWithReceivers(defaultImplsDescriptor, name, descriptor, interfaceDescriptor.defaultType)
        }
    }
}


internal fun createStaticFunctionWithReceivers(owner: ClassOrPackageFragmentDescriptor, name: Name, descriptor: FunctionDescriptor, dispatchReceiverType: KotlinType): SimpleFunctionDescriptorImpl {
    val newFunction = SimpleFunctionDescriptorImpl.create(
            owner,
            AnnotationsImpl(emptyList()),
            name,
            CallableMemberDescriptor.Kind.DECLARATION, descriptor.source
    )

    val dispatchReceiver =
            ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                    newFunction, null, 0, AnnotationsImpl(emptyList()), Name.identifier("this"),
                    dispatchReceiverType, false, false, false, null, descriptor.source, null)
    val extensionReceiver =
            descriptor.extensionReceiverParameter?.let { extensionReceiver ->
                ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                        newFunction, null, 1, AnnotationsImpl(emptyList()), Name.identifier("receiver"),
                        extensionReceiver.value.type, false, false, false, null, extensionReceiver.source, null)
            }

    val valueParameters = listOf(dispatchReceiver, extensionReceiver).filterNotNull() +
                          descriptor.valueParameters.map { it.copy(newFunction, it.name, it.index + 1) }

    newFunction.initialize(
            null, null, emptyList()/*TODO: type parameters*/,
            valueParameters, descriptor.returnType, Modality.FINAL, descriptor.visibility
    )
    return newFunction
}

internal fun FunctionDescriptor.createFunctionAndMapVariables(oldFunction: IrFunction) =
        IrFunctionImpl(oldFunction.startOffset, oldFunction.endOffset, oldFunction.origin, this, oldFunction.body).also {
            val mapping: Map<ValueDescriptor, ValueDescriptor> =
                    (
                            listOf(oldFunction.descriptor.dispatchReceiverParameter!!, oldFunction.descriptor.extensionReceiverParameter).filterNotNull() +
                            oldFunction.descriptor.valueParameters
                    ).zip(this.valueParameters).toMap()

            it.body?.transform(VariableRemapper(mapping), null)
        }
