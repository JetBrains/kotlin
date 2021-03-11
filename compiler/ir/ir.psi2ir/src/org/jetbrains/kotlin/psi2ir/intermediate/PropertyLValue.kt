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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.Scope
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

abstract class PropertyLValueBase(
    protected val context: GeneratorContext,
    val scope: Scope,
    val startOffset: Int,
    val endOffset: Int,
    val origin: IrStatementOrigin?,
    override val type: IrType,
    val callReceiver: CallReceiver,
    val superQualifier: IrClassSymbol?
) : LValue, AssignmentReceiver {
    override fun assign(withLValue: (LValue) -> IrExpression) =
        callReceiver.call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            val dispatchReceiverVariable2 = dispatchReceiverValue?.let {
                scope.createTemporaryVariable(dispatchReceiverValue.load(), "this")
            }
            val dispatchReceiverValue2 = dispatchReceiverVariable2?.let { VariableLValue(context, it) }

            val extensionReceiverVariable2 = extensionReceiverValue?.let {
                scope.createTemporaryVariable(extensionReceiverValue.load(), "receiver")
            }
            val extensionReceiverValue2 = extensionReceiverVariable2?.let { VariableLValue(context, it) }

            val contextReceiverVariables2 = contextReceiverValues.mapIndexed { i, value ->
                scope.createTemporaryVariable(value.load(), "additionalReceiver$i")
            }
            val contextReceiversValues2 = contextReceiverVariables2.map { VariableLValue(context, it) }

            val irResultExpression = withLValue(withReceiver(dispatchReceiverValue2, extensionReceiverValue2, contextReceiversValues2))

            val irBlock = IrBlockImpl(startOffset, endOffset, irResultExpression.type, origin)
            irBlock.addIfNotNull(dispatchReceiverVariable2)
            irBlock.addIfNotNull(extensionReceiverVariable2)
            contextReceiverVariables2.forEach { irBlock.addIfNotNull(it) }
            irBlock.statements.add(irResultExpression)
            irBlock
        }

    override fun assign(value: IrExpression): IrExpression =
        store(value)

    protected abstract fun withReceiver(dispatchReceiver: VariableLValue?, extensionReceiver: VariableLValue?, contextReceivers: List<VariableLValue>): PropertyLValueBase
}

class FieldPropertyLValue(
    context: GeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    origin: IrStatementOrigin?,
    val field: IrFieldSymbol,
    val descriptor: PropertyDescriptor,
    type: IrType,
    callReceiver: CallReceiver,
    superQualifier: IrClassSymbol?
) :
    PropertyLValueBase(context, scope, startOffset, endOffset, origin, type, callReceiver, superQualifier) {

    override fun load(): IrExpression =
        callReceiver.call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            assert(extensionReceiverValue == null) { "Field can't have an extension receiver: ${field.descriptor}" }
            assert(contextReceiverValues.isEmpty()) { "Field can't have context receivers: ${field.descriptor}" }
            IrGetFieldImpl(
                startOffset, endOffset,
                field,
                type,
                dispatchReceiverValue?.load(),
                origin,
                superQualifier
            ).also { context.callToSubstitutedDescriptorMap[it] = descriptor }
        }

    override fun store(irExpression: IrExpression) =
        callReceiver.call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            assert(extensionReceiverValue == null) { "Field can't have an extension receiver: ${field.descriptor}" }
            assert(contextReceiverValues.isEmpty()) { "Field can't have context receivers: ${field.descriptor}" }
            IrSetFieldImpl(
                startOffset, endOffset,
                field,
                dispatchReceiverValue?.load(),
                irExpression,
                context.irBuiltIns.unitType,
                origin,
                superQualifier
            ).also { context.callToSubstitutedDescriptorMap[it] = descriptor }
        }

    override fun withReceiver(dispatchReceiver: VariableLValue?, extensionReceiver: VariableLValue?, contextReceivers: List<VariableLValue>): PropertyLValueBase =
        FieldPropertyLValue(
            context,
            scope, startOffset, endOffset, origin,
            field,
            descriptor,
            type,
            SimpleCallReceiver(dispatchReceiver, extensionReceiver, contextReceivers),
            superQualifier
        )
}

class AccessorPropertyLValue(
    context: GeneratorContext,
    scope: Scope,
    startOffset: Int,
    endOffset: Int,
    origin: IrStatementOrigin?,
    type: IrType,
    val getter: IrSimpleFunctionSymbol?,
    val getterDescriptor: FunctionDescriptor?,
    val setter: IrSimpleFunctionSymbol?,
    val setterDescriptor: FunctionDescriptor?,
    val typeArguments: List<IrType>?,
    callReceiver: CallReceiver,
    superQualifier: IrClassSymbol?
) : PropertyLValueBase(context, scope, startOffset, endOffset, origin, type, callReceiver, superQualifier) {

    private val typeArgumentsCount = typeArguments?.size ?: 0

    private fun IrMemberAccessExpression<*>.putTypeArguments() {
        typeArguments?.forEachIndexed { index, irType ->
            putTypeArgument(index, irType)
        }
    }

    override fun load(): IrExpression =
        callReceiver.adjustForCallee(getterDescriptor!!).call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            IrCallImpl(
                startOffset, endOffset,
                type,
                getter!!, typeArgumentsCount,
                contextReceiverValues.size,
                origin,
                superQualifier
            ).apply {
                context.callToSubstitutedDescriptorMap[this] = getterDescriptor
                putTypeArguments()
                dispatchReceiver = dispatchReceiverValue?.load()
                extensionReceiver = extensionReceiverValue?.load()
                for ((i, contextReceiverValue) in contextReceiverValues.withIndex()) {
                    putValueArgument(i, contextReceiverValue.load())
                }
            }
        }

    override fun store(irExpression: IrExpression) =
        callReceiver.adjustForCallee(setterDescriptor!!).call { dispatchReceiverValue, extensionReceiverValue, contextReceiverValues ->
            IrCallImpl(
                startOffset, endOffset,
                context.irBuiltIns.unitType,
                setter!!, typeArgumentsCount,
                1 + contextReceiverValues.size,
                origin,
                superQualifier
            ).apply {
                context.callToSubstitutedDescriptorMap[this] = setterDescriptor
                putTypeArguments()
                dispatchReceiver = dispatchReceiverValue?.load()
                extensionReceiver = extensionReceiverValue?.load()
                for ((i, contextReceiverValue) in contextReceiverValues.withIndex()) {
                    putValueArgument(i, contextReceiverValue.load())
                }
                putValueArgument(contextReceiverValues.size, irExpression)
            }
        }

    override fun withReceiver(dispatchReceiver: VariableLValue?, extensionReceiver: VariableLValue?, contextReceivers: List<VariableLValue>): PropertyLValueBase =
        AccessorPropertyLValue(
            context, scope,
            startOffset, endOffset, origin,
            type, getter, getterDescriptor, setter, setterDescriptor,
            typeArguments,
            SimpleCallReceiver(dispatchReceiver, extensionReceiver, contextReceivers),
            superQualifier
        )
}