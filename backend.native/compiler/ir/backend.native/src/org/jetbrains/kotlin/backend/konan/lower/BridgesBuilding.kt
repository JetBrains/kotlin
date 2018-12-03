/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature

internal class WorkersBridgesBuilding(val context: Context) : DeclarationContainerLoweringPass, IrElementTransformerVoid() {

    val interop = context.interopBuiltIns
    val symbols = context.ir.symbols
    val nullableAnyType = context.builtIns.nullableAnyType
    lateinit var runtimeJobFunction: IrSimpleFunction

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat {
            listOf(it) + buildWorkerBridges(it).also { bridges ->
                // `buildWorkerBridges` builds bridges for all declarations inside `it` and nested declarations,
                // so some bridges get incorrect parent. Fix it:
                bridges.forEach { bridge -> bridge.parent = irDeclarationContainer }
            }
        }
    }

    private fun buildWorkerBridges(declaration: IrDeclaration): List<IrFunction> {
        val bridges = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object: IrElementTransformerVoid() {

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val descriptor = expression.descriptor.original
                if (descriptor != interop.executeImplFunction)
                    return expression

                val job = expression.getValueArgument(3) as IrFunctionReference
                val jobFunction = (job.symbol as IrSimpleFunctionSymbol).owner
                val jobDescriptor = job.descriptor
                val arg = jobDescriptor.valueParameters[0]
                if (!::runtimeJobFunction.isInitialized) {
                    val runtimeJobDescriptor = SimpleFunctionDescriptorImpl.create(
                            jobDescriptor.containingDeclaration,
                            jobDescriptor.annotations,
                            jobDescriptor.name,
                            jobDescriptor.kind,
                            jobDescriptor.source
                    ).apply {
                        initialize(
                                null,
                                null,
                                emptyList(),
                                listOf(ValueParameterDescriptorImpl(
                                        containingDeclaration = this,
                                        original              = null,
                                        index                 = 0,
                                        annotations           = Annotations.EMPTY,
                                        name                  = arg.name,
                                        outType               = nullableAnyType,
                                        declaresDefaultValue  = arg.declaresDefaultValue(),
                                        isCrossinline         = arg.isCrossinline,
                                        isNoinline            = arg.isNoinline,
                                        varargElementType     = arg.varargElementType,
                                        source                = arg.source
                                )),
                                nullableAnyType,
                                jobDescriptor.modality,
                                jobDescriptor.visibility
                        )
                    }

                    runtimeJobFunction = IrFunctionImpl(
                            jobFunction.startOffset,
                            jobFunction.endOffset,
                            IrDeclarationOrigin.DEFINED,
                            runtimeJobDescriptor,
                            context.irBuiltIns.anyNType
                    ).also {
                        it.valueParameters += IrValueParameterImpl(
                                it.startOffset,
                                it.endOffset,
                                IrDeclarationOrigin.DEFINED,
                                it.descriptor.valueParameters.single(),
                                context.irBuiltIns.anyNType,
                                null
                        )
                    }
                }
                val overriddenJobDescriptor = OverriddenFunctionDescriptor(jobFunction, runtimeJobFunction)
                if (!overriddenJobDescriptor.needBridge) return expression

                val bridge = context.buildBridge(
                        startOffset  = job.startOffset,
                        endOffset    = job.endOffset,
                        descriptor   = overriddenJobDescriptor,
                        targetSymbol = job.symbol)
                bridges += bridge
                expression.putValueArgument(3, IrFunctionReferenceImpl(
                        startOffset   = job.startOffset,
                        endOffset     = job.endOffset,
                        type          = job.type,
                        symbol        = bridge.symbol,
                        descriptor    = bridge.descriptor,
                        typeArgumentsCount = 0)
                )
                return expression
            }
        })
        return bridges
    }
}

internal class BridgesBuilding(val context: Context) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val builtBridges = mutableSetOf<IrSimpleFunction>()

        irClass.simpleFunctions()
                .forEach { function ->
                    function.allOverriddenDescriptors
                            .map { OverriddenFunctionDescriptor(function, it) }
                            .filter { !it.bridgeDirections.allNotNeeded() }
                            .filter { it.canBeCalledVirtually }
                            .filter { !it.inheritsBridge }
                            .distinctBy { it.bridgeDirections }
                            .forEach {
                                buildBridge(it, irClass)
                                builtBridges += it.descriptor
                            }
                }
        irClass.transformChildrenVoid(object: IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                declaration.transformChildrenVoid(this)

                val body = declaration.body as? IrBlockBody
                        ?: return declaration
                val descriptor = declaration.descriptor
                val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor)
                if (typeSafeBarrierDescription == null || builtBridges.contains(declaration))
                    return declaration

                val irBuilder = context.createIrBuilder(declaration.symbol, declaration.startOffset, declaration.endOffset)
                declaration.body = irBuilder.irBlockBody(declaration) {
                    buildTypeSafeBarrier(declaration, declaration, typeSafeBarrierDescription)
                    body.statements.forEach { +it }
                }
                return declaration
            }
        })
    }

    private fun buildBridge(descriptor: OverriddenFunctionDescriptor, irClass: IrClass) {
        irClass.declarations.add(context.buildBridge(
                startOffset          = irClass.startOffset,
                endOffset            = irClass.endOffset,
                descriptor           = descriptor,
                targetSymbol         = descriptor.descriptor.symbol,
                superQualifierSymbol = irClass.symbol)
        )
    }
}

internal class DECLARATION_ORIGIN_BRIDGE_METHOD(val bridgeTarget: IrFunction) : IrDeclarationOrigin {
    override fun toString(): String {
        return "BRIDGE_METHOD(target=${bridgeTarget.descriptor})"
    }
}

internal val IrFunction.bridgeTarget: IrFunction?
        get() = (origin as? DECLARATION_ORIGIN_BRIDGE_METHOD)?.bridgeTarget

private fun IrBuilderWithScope.returnIfBadType(value: IrExpression,
                                               type: IrType,
                                               returnValueOnFail: IrExpression)
        = irIfThen(irNotIs(value, type), irReturn(returnValueOnFail))

private fun IrBuilderWithScope.irConst(value: Any?) = when (value) {
    null       -> irNull()
    is Int     -> irInt(value)
    is Boolean -> if (value) irTrue() else irFalse()
    else       -> TODO()
}

private fun IrBlockBodyBuilder.buildTypeSafeBarrier(function: IrFunction,
                                                    originalFunction: IrFunction,
                                                    typeSafeBarrierDescription: BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription) {
    val valueParameters = function.valueParameters
    val originalValueParameters = originalFunction.valueParameters
    for (i in valueParameters.indices) {
        if (!typeSafeBarrierDescription.checkParameter(i))
            continue
        val type = originalValueParameters[i].type
        if (!type.isNullableAny()) {
            +returnIfBadType(irGet(valueParameters[i]), type,
                    if (typeSafeBarrierDescription == BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)
                        irGet(valueParameters[2])
                    else irConst(typeSafeBarrierDescription.defaultValue)
            )
        }
    }
}

private fun Context.buildBridge(startOffset: Int, endOffset: Int,
                                descriptor: OverriddenFunctionDescriptor, targetSymbol: IrFunctionSymbol,
                                superQualifierSymbol: IrClassSymbol? = null): IrFunction {

    val bridge = specialDeclarationsFactory.getBridge(descriptor)

    if (bridge.modality == Modality.ABSTRACT) {
        return bridge
    }

    val irBuilder = createIrBuilder(bridge.symbol, startOffset, endOffset)
    bridge.body = irBuilder.irBlockBody(bridge) {
        val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor.overriddenDescriptor.descriptor)
        typeSafeBarrierDescription?.let { buildTypeSafeBarrier(bridge, descriptor.descriptor, it) }

        val delegatingCall = IrCallImpl(
                startOffset,
                endOffset,
                targetSymbol.owner.returnType,
                targetSymbol,
                targetSymbol.descriptor,
                superQualifierSymbol = superQualifierSymbol /* Call non-virtually */
        ).apply {
            bridge.dispatchReceiverParameter?.let {
                dispatchReceiver = irGet(it)
            }
            bridge.extensionReceiverParameter?.let {
                extensionReceiver = irGet(it)
            }
            bridge.valueParameters.forEachIndexed { index, parameter ->
                this.putValueArgument(index, irGet(parameter))
            }
        }

        +irReturn(delegatingCall)
    }
    return bridge
}