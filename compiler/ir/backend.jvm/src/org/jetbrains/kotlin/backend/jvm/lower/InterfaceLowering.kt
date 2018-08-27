/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.lower.DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER
import org.jetbrains.kotlin.backend.common.lower.InitializersLowering.Companion.clinitName
import org.jetbrains.kotlin.backend.common.lower.VariableRemapperDesc
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes

class InterfaceLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state = context.state

    override fun lower(irClass: IrClass) {
        if (!irClass.isInterface) return

        val defaultImplsIrClass = context.declarationFactory.getDefaultImplsClass(irClass)
        irClass.declarations.add(defaultImplsIrClass)
        val members = defaultImplsIrClass.declarations

        irClass.declarations.filterIsInstance<IrFunction>().forEach {
            val descriptor = it.descriptor
            if (it.origin == DECLARATION_ORIGIN_FUNCTION_FOR_DEFAULT_PARAMETER) {
                members.add(it) //just copy $default to DefaultImpls
            } else if (descriptor.modality != Modality.ABSTRACT && it.origin != IrDeclarationOrigin.FAKE_OVERRIDE) {
                val element = context.declarationFactory.getDefaultImplsFunction(it)
                members.add(element)
                element.body = it.body
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
    visibility: Visibility = oldFunction.visibility,
    origin: IrDeclarationOrigin = oldFunction.origin
) =
    IrFunctionImpl(
        oldFunction.startOffset, oldFunction.endOffset, origin, IrSimpleFunctionSymbolImpl(this),
        visibility = visibility
    ).apply {
        body = oldFunction.body
        returnType = oldFunction.returnType
        createParameterDeclarations()
        // TODO: do we really need descriptor here? This workaround is about coping `dispatchReceiver` descriptor
        val mapping: Map<ValueDescriptor, IrValueParameter> =
            (listOfNotNull(oldFunction.dispatchReceiverParameter!!.descriptor, oldFunction.extensionReceiverParameter?.descriptor) + oldFunction.valueParameters.map { it.descriptor })
                .zip(valueParameters).toMap()

        body?.transform(VariableRemapperDesc(mapping), null)
    }
