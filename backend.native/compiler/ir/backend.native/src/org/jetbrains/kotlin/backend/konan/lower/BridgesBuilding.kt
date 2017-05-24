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
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.type
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature
import org.jetbrains.kotlin.types.KotlinType

internal class BridgesBuilding(val context: Context) : ClassLoweringPass {

    private fun IrBuilderWithScope.returnIfBadType(value: IrExpression,
                                                   type: KotlinType,
                                                   returnValueOnFail: IrExpression)
            = irIfThen(irNotIs(value, type), irReturn(returnValueOnFail))

    private fun IrBuilderWithScope.irConst(value: Any?) = when (value) {
        null -> irNull()
        is Int -> irInt(value)
        is Boolean -> if (value) irTrue() else irFalse()
        else -> TODO()
    }

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

                val descriptor = declaration.descriptor
                val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor)
                if (typeSafeBarrierDescription == null || builtBridges.contains(descriptor))
                    return declaration

                val body = declaration.body as? IrBlockBody
                        ?: return declaration
                val irBuilder = context.createIrBuilder(declaration.symbol, declaration.startOffset, declaration.endOffset)
                declaration.body = irBuilder.irBlockBody(declaration) {
                    val valueParameters = declaration.valueParameters
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
                        body.statements.forEach { +it }
                    }
                }
                return declaration
            }
        })
    }

    private object DECLARATION_ORIGIN_BRIDGE_METHOD :
            IrDeclarationOriginImpl("BRIDGE_METHOD")

    private fun buildBridge(descriptor: OverriddenFunctionDescriptor, irClass: IrClass) {
        val bridgeDescriptor = context.specialDeclarationsFactory.getBridgeDescriptor(descriptor)
        val bridge = IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, DECLARATION_ORIGIN_BRIDGE_METHOD,
                bridgeDescriptor).apply { createParameterDeclarations() }

        val target = descriptor.descriptor

        assert(target.containingDeclaration == irClass.descriptor)
        val superQualifierSymbol = irClass.symbol
        val targetSymbol = irClass.findMember(target) // TODO: optimize

        val statements = mutableListOf<IrExpression>()
        val irBuilder = context.createIrBuilder(bridge.symbol, irClass.startOffset, irClass.endOffset)
        irBuilder.run {
            val typeSafeBarrierDescription = BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor.overriddenDescriptor)
            if (typeSafeBarrierDescription != null) {
                val valueParameters = bridge.valueParameters
                for (i in valueParameters.indices) {
                    if (!typeSafeBarrierDescription.checkParameter(i))
                        continue
                    val type = target.valueParameters[i].type
                    if (type != context.builtIns.nullableAnyType) {
                        statements += returnIfBadType(irGet(valueParameters[i].symbol), type,
                                if (typeSafeBarrierDescription == BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)
                                    irGet(valueParameters[2].symbol)
                                else irConst(typeSafeBarrierDescription.defaultValue)
                        )
                    }
                }
            }
        }

        val delegatingCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, targetSymbol, target,
                superQualifierSymbol = superQualifierSymbol /* Call non-virtually */
        ).apply {
            val dispatchReceiverParameter = bridge.dispatchReceiverParameter
            if (dispatchReceiverParameter != null)
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter.symbol)
            val extensionReceiverParameter = bridge.extensionReceiverParameter
            if (extensionReceiverParameter != null)
                extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionReceiverParameter.symbol)
            bridge.valueParameters.forEachIndexed { index, parameter ->
                this.putValueArgument(index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, parameter.symbol))
            }
        }

        val bridgeBody = if (bridgeDescriptor.returnType.let { it != null && !KotlinBuiltIns.isUnitOrNullableUnit(it) })
            IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, bridge.symbol, delegatingCall)
        else
            delegatingCall
        statements += bridgeBody
        irClass.declarations.add(
                IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, DECLARATION_ORIGIN_BRIDGE_METHOD,
                        bridgeDescriptor, IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, statements)
                ).apply { createParameterDeclarations() }
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
