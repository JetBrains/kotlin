/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredStatementOrigin
import org.jetbrains.kotlin.backend.jvm.codegen.isJvmInterface
import org.jetbrains.kotlin.backend.jvm.descriptors.DefaultImplsClassDescriptor
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.isDefinitelyNotDefaultImplsMethod
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid


class InterfaceDelegationLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {

    val state: GenerationState = context.state

    override fun lower(irClass: IrClass) {
        if (irClass.isJvmInterface) return

        irClass.transformChildrenVoid(this)
        generateInterfaceMethods(irClass)
    }


    private fun generateInterfaceMethods(irClass: IrClass) {
        val irClassDescriptor = irClass.descriptor
        val actualClassDescriptor = (irClassDescriptor as? DefaultImplsClassDescriptor)?.correspondingInterface ?: irClassDescriptor
        val isDefaultImplsGeneration = actualClassDescriptor !== irClassDescriptor
        for ((interfaceFun, value) in CodegenUtil.getNonPrivateTraitMethods(actualClassDescriptor, !isDefaultImplsGeneration)) {
            //skip java 8 default methods
            if (!interfaceFun.isDefinitelyNotDefaultImplsMethod() && !FunctionCodegen.isMethodOfAny(interfaceFun)) {
                generateDelegationToDefaultImpl(
                    irClass, context.ir.symbols.externalSymbolTable.referenceSimpleFunction(
                        interfaceFun.original
                    ).owner, value, isDefaultImplsGeneration
                )
            }
        }
    }

    private fun generateDelegationToDefaultImpl(
        irClass: IrClass,
        interfaceFun: IrFunction,
        inheritedFun: FunctionDescriptor,
        isDefaultImplsGeneration: Boolean
    ) {
        val defaultImplFun = context.declarationFactory.getDefaultImplsFunction(interfaceFun)

        val irFunction =
            if (!isDefaultImplsGeneration) IrFunctionImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                IrDeclarationOrigin.DEFINED,
                inheritedFun,
                null
            ).also {
                it.createParameterDeclarations()
                it.returnType = defaultImplFun.returnType
            }
            else context.declarationFactory.getDefaultImplsFunction(
                context.ir.symbols.externalSymbolTable.referenceSimpleFunction(
                    inheritedFun.original
                ).owner
            )
        val irBody = IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET)
        irFunction.body = irBody
        irClass.declarations.add(irFunction)

        val irCallImpl =
            IrCallImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                defaultImplFun.returnType,
                defaultImplFun.symbol,
                defaultImplFun.descriptor,
                origin = JvmLoweredStatementOrigin.DEFAULT_IMPLS_DELEGATION
            )
        irBody.statements.add(
            IrReturnImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                irFunction.returnType,
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
