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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.MutableResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.types.BoundsSubstitutor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance


class CandidateCallWithArgumentMapping<D : CallableDescriptor, K> private constructor(
        val resolvedCall: MutableResolvedCall<D>,
        private val argumentsToParameters: Map<K, ValueParameterDescriptor>,
        val parametersWithDefaultValuesCount: Int
) {
    override fun toString(): String =
            "${resolvedCall.call}: $parametersWithDefaultValuesCount defaults in ${resolvedCall.candidateDescriptor}"

    val resultingDescriptor: D
        get() = resolvedCall.resultingDescriptor

    val argumentsCount: Int
        get() = argumentsToParameters.size

    val argumentKeys: Collection<K>
        get() = argumentsToParameters.keys

    val callElement: KtElement
        get() = resolvedCall.call.callElement

    val isGeneric: Boolean = resolvedCall.resultingDescriptor.original.typeParameters.isNotEmpty()

    private val upperBoundsSubstitutor =
            BoundsSubstitutor.createUpperBoundsSubstitutor(resolvedCall.resultingDescriptor)

    fun getExtensionReceiverType(substituteUpperBounds: Boolean): KotlinType? =
            resultingDescriptor.extensionReceiverParameter?.type?.let {
                extensionReceiverType ->
                if (substituteUpperBounds)
                    upperBoundsSubstitutor.substitute(extensionReceiverType, Variance.INVARIANT)
                else
                    extensionReceiverType
            }

    /**
     * Returns the type of a value that can be used in place of the corresponding parameter.
     */
    fun getValueParameterType(argumentKey: K, substituteUpperBounds: Boolean): KotlinType? =
            argumentsToParameters[argumentKey]?.let {
                valueParameterDescriptor ->
                val valueParameterType = valueParameterDescriptor.varargElementType ?: valueParameterDescriptor.type
                if (substituteUpperBounds)
                    upperBoundsSubstitutor.substitute(valueParameterType, Variance.INVARIANT)
                else
                    valueParameterType
            }

    companion object {
        fun <D : CallableDescriptor, K> create(
                call: MutableResolvedCall<D>,
                resolvedArgumentToKeys: (ResolvedValueArgument) -> Collection<K>
        ): CandidateCallWithArgumentMapping<D, K> {
            val argumentsToParameters = hashMapOf<K, ValueParameterDescriptor>()
            var parametersWithDefaultValuesCount = 0

            for ((valueParameterDescriptor, resolvedValueArgument) in call.valueArguments.entries) {
                if (resolvedValueArgument is DefaultValueArgument) {
                    parametersWithDefaultValuesCount++
                }
                else {
                    val keys = resolvedArgumentToKeys(resolvedValueArgument)
                    for (argumentKey in keys) {
                        argumentsToParameters[argumentKey] = valueParameterDescriptor
                    }
                }
            }

            return CandidateCallWithArgumentMapping(call, argumentsToParameters, parametersWithDefaultValuesCount)
        }
    }
}

