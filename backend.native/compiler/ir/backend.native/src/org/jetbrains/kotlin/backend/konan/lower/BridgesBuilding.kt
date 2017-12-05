/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.DeclarationContainerLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.util.type
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.types.KotlinType

internal class WorkersBridgesBuilding(val context: Context) : DeclarationContainerLoweringPass, IrElementTransformerVoid() {

    val interop = context.interopBuiltIns
    val symbols = context.ir.symbols
    val nullableAnyType = context.builtIns.nullableAnyType
    var runtimeJobDescriptor: FunctionDescriptor? = null

    override fun lower(irDeclarationContainer: IrDeclarationContainer) {
        irDeclarationContainer.declarations.transformFlat {
            listOf(it) + buildWorkerBridges(it)
        }
    }

    private fun buildWorkerBridges(declaration: IrDeclaration): List<IrFunction> {
        val bridges = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object: IrElementTransformerVoid() {

            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val descriptor = expression.descriptor.original
                if (descriptor != interop.scheduleImplFunction)
                    return expression

                val job = expression.getValueArgument(3) as IrFunctionReference
                val jobDescriptor = job.descriptor
                val arg = jobDescriptor.valueParameters[0]
                if (runtimeJobDescriptor == null) {
                    runtimeJobDescriptor = jobDescriptor.newCopyBuilder()
                            .setReturnType(nullableAnyType)
                            .setValueParameters(listOf(ValueParameterDescriptorImpl(
                                    containingDeclaration = jobDescriptor,
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
                            )))
                            .build()!!
                }
                val overriddenJobDescriptor = OverriddenFunctionDescriptor(jobDescriptor, runtimeJobDescriptor!!)
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
                        typeArguments = null)
                )
                return expression
            }
        })
        return bridges
    }
}

internal class BridgesBuilding(val context: Context) : ClassLoweringPass {

    override fun lower(irClass: IrClass) {
        val functions = mutableSetOf<FunctionDescriptor?>()
        irClass.declarations.forEach {
            when (it) {
                is IrFunction -> functions.add(it.descriptor)
                is IrProperty -> {
                    functions.add(it.getter?.descriptor)
                    functions.add(it.setter?.descriptor)
                }
            }
        }

        irClass.descriptor.contributedMethods.forEach { functions.add(it) }

        val builtBridges = mutableSetOf<FunctionDescriptor>()
        functions.filterNotNull()
                .filterNot { it.modality == Modality.ABSTRACT }
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
                val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(declaration.descriptor)
                if (typeSafeBarrierDescription == null || builtBridges.contains(declaration.descriptor))
                    return declaration

                val irBuilder = context.createIrBuilder(declaration.symbol, declaration.startOffset, declaration.endOffset)
                declaration.body = irBuilder.irBlockBody(declaration) {
                    buildTypeSafeBarrier(declaration, typeSafeBarrierDescription)
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
                targetSymbol         = irClass.findMember(descriptor.descriptor), // TODO: optimize.
                superQualifierSymbol = irClass.symbol)
        )
    }
}

private fun IrClass.findMember(descriptor: FunctionDescriptor): IrFunctionSymbol {
    val functions = this.declarations.filterIsInstance<IrFunction>().map { it.symbol }
    val propertyAccessors = this.declarations
            .filterIsInstance<IrProperty>()
            .flatMap { listOfNotNull(it.getter?.symbol, it.setter?.symbol) }

    return (functions + propertyAccessors).single { it.descriptor == descriptor }
}

private object DECLARATION_ORIGIN_BRIDGE_METHOD :
        IrDeclarationOriginImpl("BRIDGE_METHOD")

private fun IrBuilderWithScope.returnIfBadType(value: IrExpression,
                                               type: KotlinType,
                                               returnValueOnFail: IrExpression)
        = irIfThen(irNotIs(value, type), irReturn(returnValueOnFail))

private fun IrBuilderWithScope.irConst(value: Any?) = when (value) {
    null       -> irNull()
    is Int     -> irInt(value)
    is Boolean -> if (value) irTrue() else irFalse()
    else       -> TODO()
}

private fun IrBlockBodyBuilder.buildTypeSafeBarrier(function: IrFunction,
                                                    typeSafeBarrierDescription: BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription) {
    val valueParameters = function.valueParameters
    for (i in valueParameters.indices) {
        if (!typeSafeBarrierDescription.checkParameter(i))
            continue
        val type = valueParameters[i].type
        if (type != context.builtIns.nullableAnyType) {
            +returnIfBadType(irGet(valueParameters[i].symbol), type,
                    if (typeSafeBarrierDescription == BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)
                        irGet(valueParameters[2].symbol)
                    else irConst(typeSafeBarrierDescription.defaultValue)
            )
        }
    }
}

private fun Context.buildBridge(startOffset: Int, endOffset: Int,
                                descriptor: OverriddenFunctionDescriptor, targetSymbol: IrFunctionSymbol,
                                superQualifierSymbol: IrClassSymbol? = null): IrFunctionImpl {
    val bridgeDescriptor = specialDeclarationsFactory.getBridgeDescriptor(descriptor)
    val bridge = IrFunctionImpl(startOffset, endOffset, DECLARATION_ORIGIN_BRIDGE_METHOD,
            bridgeDescriptor).apply { createParameterDeclarations() }

    val irBuilder = createIrBuilder(bridge.symbol, startOffset, endOffset)
    bridge.body = irBuilder.irBlockBody(bridge) {
        val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor.overriddenDescriptor)
        typeSafeBarrierDescription?.let { buildTypeSafeBarrier(bridge, it) }

        val delegatingCall = IrCallImpl(startOffset, endOffset, targetSymbol, descriptor.descriptor,
                superQualifierSymbol = superQualifierSymbol /* Call non-virtually */
        ).apply {
            bridge.dispatchReceiverParameter?.let {
                dispatchReceiver = irGet(it.symbol)
            }
            bridge.extensionReceiverParameter?.let {
                extensionReceiver = irGet(it.symbol)
            }
            bridge.valueParameters.forEachIndexed { index, parameter ->
                this.putValueArgument(index, irGet(parameter.symbol))
            }
        }

        if (KotlinBuiltIns.isUnitOrNullableUnit(bridgeDescriptor.returnType!!))
            +delegatingCall
        else
            +irReturn(delegatingCall)
    }
    return bridge
}