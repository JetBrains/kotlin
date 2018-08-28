/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering.Companion.clinitName
import org.jetbrains.kotlin.backend.common.lower.VariableRemapper
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptorImpl
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes

class InterfaceLowering(val state: GenerationState) : IrElementTransformerVoid(), ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val interfaceDescriptor = irClass.descriptor
        val defaultImplsDescriptor = createDefaultImplsClassDescriptor(interfaceDescriptor)
        val defaultImplsIrClass =
            IrClassImpl(irClass.startOffset, irClass.endOffset, JvmLoweredDeclarationOrigin.DEFAULT_IMPLS, defaultImplsDescriptor)
        irClass.declarations.add(defaultImplsIrClass)

        val members = defaultImplsIrClass.declarations

        irClass.declarations.filterIsInstance<IrFunction>().forEach {
            val descriptor = it.descriptor
            if (it.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER) {
                members.add(it) //just copy $default to DefaultImpls
            } else if (descriptor.modality != Modality.ABSTRACT) {
                val functionDescriptorImpl =
                    createDefaultImplFunDescriptor(defaultImplsDescriptor, descriptor, interfaceDescriptor, state.typeMapper)
                members.add(functionDescriptorImpl.createFunctionAndMapVariables(it, it.visibility))
                it.body = null
                //TODO reset modality to abstract
            }
        }


        irClass.transformChildrenVoid(this)

        //REMOVE private methods
        val privateToRemove = irClass.declarations.filterIsInstance<IrFunction>().mapNotNull {
            val visibility = AsmUtil.getVisibilityAccessFlag(it.descriptor)
            if (visibility == Opcodes.ACC_PRIVATE && it.descriptor.name != clinitName) {
                it
            } else null
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


internal fun createStaticFunctionWithReceivers(
    owner: ClassOrPackageFragmentDescriptor,
    name: Name,
    descriptor: FunctionDescriptor,
    dispatchReceiverType: KotlinType
): SimpleFunctionDescriptorImpl {
    val newFunction = SimpleFunctionDescriptorImpl.create(
        owner,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.DECLARATION, descriptor.source
    )
    var offset = 0
    val dispatchReceiver =
        ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
            newFunction, null, offset++, Annotations.EMPTY, Name.identifier("this"),
            dispatchReceiverType, false, false, false, null, descriptor.source, null
        )
    val extensionReceiver =
        descriptor.extensionReceiverParameter?.let { extensionReceiver ->
            ValueParameterDescriptorImpl.createWithDestructuringDeclarations(
                newFunction, null, offset++, Annotations.EMPTY, Name.identifier("receiver"),
                extensionReceiver.value.type, false, false, false, null, extensionReceiver.source, null
            )
        }

    val valueParameters = listOfNotNull(dispatchReceiver, extensionReceiver) +
            descriptor.valueParameters.map { it.copy(newFunction, it.name, it.index + offset) }

    newFunction.initialize(
        null, null, emptyList()/*TODO: type parameters*/,
        valueParameters, descriptor.returnType, Modality.FINAL, descriptor.visibility
    )
    return newFunction
}

internal fun FunctionDescriptor.createFunctionAndMapVariables(
    oldFunction: IrFunction,
    visibility: Visibility
) =
    IrFunctionImpl(
        oldFunction.startOffset, oldFunction.endOffset, oldFunction.origin, IrSimpleFunctionSymbolImpl(this),
        visibility = visibility
    ).apply {
        body = oldFunction.body
        createParameterDeclarations()
        val mapping: Map<ValueDescriptor, IrValueParameter> =
            (
                    listOfNotNull(oldFunction.descriptor.dispatchReceiverParameter!!, oldFunction.descriptor.extensionReceiverParameter) +
                            oldFunction.descriptor.valueParameters
                    ).zip(valueParameters).toMap()

        body?.transform(VariableRemapper(mapping), null)
    }
