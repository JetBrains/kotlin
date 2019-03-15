/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.filterDeclarations
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.isSubclassOf
import org.jetbrains.kotlin.name.Name

internal val functionNVarargInvokePhase = makeIrFilePhase(
    ::FunctionNVarargInvokeLowering,
    name = "FunctionNVarargInvoke",
    description = "Handle invoke functions with large number of arguments"
)

private class FunctionNVarargInvokeLowering(var context: JvmBackendContext) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val invokeFunctions = irClass.filterDeclarations<IrSimpleFunction> { it.name.toString() == "invoke" }
        if (invokeFunctions.isEmpty() ||
            invokeFunctions.any { it.valueParameters.size > 0 && it.valueParameters.last().varargElementType != null } ||
            invokeFunctions.all { it.valueParameters.size + (if (it.extensionReceiverParameter != null) 1 else 0) <= CallableReferenceLowering.MAX_ARGCOUNT_WITHOUT_VARARG }
        ) {
            // No need to add a new vararg invoke method
            return
        }
        val functionInvokes = invokeFunctions.filter { irClass.isSubclassOf(context.ir.symbols.getFunction(it.valueParameters.size).owner) }
        if (functionInvokes.isEmpty()) return

        irClass.declarations.add(generateVarargInvoke(irClass, functionInvokes))
    }

    private fun generateVarargInvoke(irClass: IrClass, invokesToDelegateTo: List<IrSimpleFunction>): IrFunction {
        val backendContext = context
        val descriptor = WrappedSimpleFunctionDescriptor()
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL,
            symbol = IrSimpleFunctionSymbolImpl(descriptor),
            name = Name.identifier("invoke"),
            visibility = Visibilities.PUBLIC,
            modality = Modality.OPEN,
            returnType = context.irBuiltIns.anyNType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false
        ).apply {
            descriptor.bind(this)
            dispatchReceiverParameter = irClass.thisReceiver
            val varargParameterDescriptor = WrappedValueParameterDescriptor()
            val varargParam = IrValueParameterImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                origin = JvmLoweredDeclarationOrigin.FUNCTION_REFERENCE_IMPL,
                symbol = IrValueParameterSymbolImpl(varargParameterDescriptor),
                name = Name.identifier("args"),
                index = 0,
                type = context.irBuiltIns.arrayClass.typeWith(),
                varargElementType = context.irBuiltIns.anyClass.typeWith(),
                isCrossinline = false,
                isNoinline = false
            ).apply {
                varargParameterDescriptor.bind(this)
            }
            valueParameters.add(varargParam)
            val irBuilder = context.createIrBuilder(symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
            body = irBuilder.irBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET) {
                val arrayGetFun =
                    backendContext.irBuiltIns.arrayClass.owner.findDeclaration<IrSimpleFunction> { it.name.toString() == "get" }!!
                val arraySizeProperty = context.irBuiltIns.arrayClass.owner.findDeclaration<IrProperty> { it.name.toString() == "size" }!!
                val numberOfArguments = irTemporary(irCall(arraySizeProperty.getter!!).apply {
                    dispatchReceiver = irGet(varargParam)
                })

                for (target in invokesToDelegateTo) {
                    +irIfThen(
                        backendContext.irBuiltIns.unitType,
                        irEquals(irGet(numberOfArguments), irInt(target.valueParameters.size)),
                        irReturn(
                            IrTypeOperatorCallImpl(
                                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                backendContext.irBuiltIns.anyNType,
                                IrTypeOperator.CAST,
                                target.returnType,
                                target.returnType.classifierOrFail,
                                irCall(target).apply {
                                    dispatchReceiver = irGet(irClass.thisReceiver!!)
                                    target.valueParameters.forEachIndexed { i, irValueParameter ->
                                        val type = irValueParameter.type
                                        putValueArgument(
                                            i,
                                            irBlock(resultType = type) {
                                                val argValue = irTemporary(
                                                    irCallOp(
                                                        arrayGetFun.symbol,
                                                        context.irBuiltIns.anyNType,
                                                        irGet(varargParam),
                                                        irInt(i)
                                                    )
                                                )
                                                +irIfThen(
                                                    irNotIs(irGet(argValue), type),
                                                    irCall(context.irBuiltIns.illegalArgumentExceptionFun).apply {
                                                        putValueArgument(0, irString("Wrong type, expected $type"))
                                                    }
                                                )
                                                +irGet(argValue)
                                            }
                                        )
                                    }
                                }
                            )
                        )
                    )
                }

                val throwMessage = invokesToDelegateTo.map { it.valueParameters.size.toString() }.joinToString(
                    prefix = "Expected ",
                    separator = " or ",
                    postfix = " arguments to invoke call"
                )
                +irCall(context.irBuiltIns.illegalArgumentExceptionFun.symbol).apply {
                    putValueArgument(0, irString(throwMessage))
                }
            }
        }
    }
}
