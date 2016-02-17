/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.results

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.*

class FlatSignature<out T>(
        val origin: T,
        val typeParameters: Collection<TypeParameterDescriptor>,
        val valueParameterTypes: List<KotlinType?>,
        val hasExtensionReceiver: Boolean,
        val hasVarargs: Boolean,
        val numDefaults: Int
) {
    val isGeneric = typeParameters.isNotEmpty()

    companion object {
        fun <D : CallableDescriptor, RC : ResolvedCall<D>> createFromResolvedCall(resolvedCall: RC): FlatSignature<RC> {
            val originalDescriptor = resolvedCall.candidateDescriptor.original
            val originalValueParameters = originalDescriptor.valueParameters

            var numDefaults = 0
            val valueArgumentToParameterType = HashMap<ValueArgument, KotlinType>()
            for ((valueParameter, resolvedValueArgument) in resolvedCall.valueArguments.entries) {
                if (resolvedValueArgument is DefaultValueArgument) {
                    numDefaults++
                }
                else {
                    val originalValueParameter = originalValueParameters[valueParameter.index]
                    val parameterType = originalValueParameter.argumentValueType
                    for (valueArgument in resolvedValueArgument.arguments) {
                        valueArgumentToParameterType[valueArgument] = parameterType
                    }
                }
            }

            return FlatSignature(resolvedCall,
                                 originalDescriptor.typeParameters,
                                 valueParameterTypes = originalDescriptor.extensionReceiverTypeOrEmpty() +
                                                       resolvedCall.call.valueArguments.map { valueArgumentToParameterType[it] },
                                 hasExtensionReceiver = originalDescriptor.extensionReceiverParameter != null,
                                 hasVarargs = originalDescriptor.valueParameters.any { it.varargElementType != null },
                                 numDefaults = numDefaults)
        }

        fun <D : CallableDescriptor> createFromCallableDescriptor(descriptor: D): FlatSignature<D> =
                FlatSignature(descriptor,
                              descriptor.typeParameters,
                              valueParameterTypes = descriptor.extensionReceiverTypeOrEmpty() + descriptor.valueParameters.map { it.argumentValueType },
                              hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
                              hasVarargs = descriptor.valueParameters.any { it.varargElementType != null },
                              numDefaults = 0)

        val ValueParameterDescriptor.argumentValueType: KotlinType
            get() = varargElementType ?: type

        fun CallableDescriptor.extensionReceiverTypeOrEmpty() =
                extensionReceiverParameter?.type.singletonOrEmptyList()
    }
}
