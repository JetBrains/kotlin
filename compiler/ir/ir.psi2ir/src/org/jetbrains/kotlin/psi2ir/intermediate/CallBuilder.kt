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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.psi2ir.generators.hasNoSideEffects
import org.jetbrains.kotlin.psi2ir.isValueArgumentReorderingRequired
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType

class CallBuilder(
    val original: ResolvedCall<*>, // TODO get rid of "original", sometimes we want to generate a call without ResolvedCall
    val descriptor: CallableDescriptor,
    val typeArguments: Map<TypeParameterDescriptor, KotlinType>?,
    val isExtensionInvokeCall: Boolean = false
) {
    var superQualifier: ClassDescriptor? = null

    lateinit var callReceiver: CallReceiver

    private val parametersOffset = if (isExtensionInvokeCall) 1 else 0

    val irValueArgumentsByIndex = arrayOfNulls<IrExpression>(descriptor.valueParameters.size)

    fun getValueArgument(valueParameterDescriptor: ValueParameterDescriptor) =
        irValueArgumentsByIndex[valueParameterDescriptor.index + parametersOffset]
}

val CallBuilder.argumentsCount: Int
    get() =
        irValueArgumentsByIndex.size

var CallBuilder.lastArgument: IrExpression?
    get() = irValueArgumentsByIndex.last()
    set(value) {
        irValueArgumentsByIndex[argumentsCount - 1] = value
    }

fun CallBuilder.getValueArgumentsInParameterOrder(): List<IrExpression?> =
    descriptor.valueParameters.map { irValueArgumentsByIndex[it.index] }

fun CallBuilder.isValueArgumentReorderingRequired() =
    original.isValueArgumentReorderingRequired() && irValueArgumentsByIndex.any { it != null && !it.hasNoSideEffects() }

val CallBuilder.hasExtensionReceiver: Boolean
    get() =
        descriptor.extensionReceiverParameter != null

val CallBuilder.hasDispatchReceiver: Boolean
    get() =
        descriptor.dispatchReceiverParameter != null

val CallBuilder.extensionReceiverType: KotlinType?
    get() =
        descriptor.extensionReceiverParameter?.type

val CallBuilder.dispatchReceiverType: KotlinType?
    get() =
        descriptor.dispatchReceiverParameter?.type

val CallBuilder.explicitReceiverParameter: ReceiverParameterDescriptor?
    get() =
        descriptor.extensionReceiverParameter ?: descriptor.dispatchReceiverParameter

val CallBuilder.explicitReceiverType: KotlinType?
    get() =
        explicitReceiverParameter?.type

fun CallBuilder.setExplicitReceiverValue(explicitReceiverValue: IntermediateValue) {
    val previousCallReceiver = callReceiver
    callReceiver = object : CallReceiver {
        override fun call(withDispatchAndExtensionAndContextReceivers: (IntermediateValue?, IntermediateValue?, List<IntermediateValue>) -> IrExpression): IrExpression {
            return previousCallReceiver.call { dispatchReceiverValue, _, contextReceiverValues ->
                val newDispatchReceiverValue = if (hasExtensionReceiver) dispatchReceiverValue else explicitReceiverValue
                val newExtensionReceiverValue = if (hasExtensionReceiver) explicitReceiverValue else null
                withDispatchAndExtensionAndContextReceivers(newDispatchReceiverValue, newExtensionReceiverValue, contextReceiverValues)
            }
        }
    }
}
