/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion.smart

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import java.util.Collections
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowValueFactory
import java.util.HashMap
import com.google.common.collect.SetMultimap
import org.jetbrains.jet.lang.resolve.calls.smartcasts.Nullability
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo

class TypesWithSmartCasts(val bindingContext: BindingContext) {
    public fun calculate(expression: JetExpression, receiver: JetExpression?): (DeclarationDescriptor) -> Iterable<JetType> {
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)
        val (variableToTypes: Map<VariableDescriptor, Collection<JetType>>, notNullVariables: Set<VariableDescriptor>)
            = processDataFlowInfo(dataFlowInfo, receiver)

        fun typesOf(descriptor: DeclarationDescriptor): Iterable<JetType> {
            if (descriptor is CallableDescriptor) {
                var returnType = descriptor.getReturnType()
                if (returnType != null && KotlinBuiltIns.getInstance().isNothing(returnType!!)) {
                    //TODO: maybe we should include them on the second press?
                    return listOf()
                }
                if (descriptor is VariableDescriptor) {
                    if (notNullVariables.contains(descriptor)) {
                        returnType = returnType?.makeNotNullable()
                    }

                    val smartCastTypes = variableToTypes[descriptor]
                    if (smartCastTypes != null && !smartCastTypes.isEmpty()) {
                        return smartCastTypes + returnType.toList()
                    }
                }
                return returnType.toList()
            }
            else if (descriptor is ClassDescriptor && descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                return listOf(descriptor.getDefaultType())
            }
            else {
                return listOf()
            }
        }

        return ::typesOf
    }

    private data class ProcessDataFlowInfoResult(
            val variableToTypes: Map<VariableDescriptor, Collection<JetType>> = Collections.emptyMap(),
            val notNullVariables: Set<VariableDescriptor> = Collections.emptySet()
    )

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo, receiver: JetExpression?): ProcessDataFlowInfoResult {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return ProcessDataFlowInfoResult()

        val dataFlowValueToVariable: (DataFlowValue) -> VariableDescriptor?
        if (receiver != null) {
            val receiverType = bindingContext[BindingContext.EXPRESSION_TYPE, receiver] ?: return ProcessDataFlowInfoResult()
            val receiverId = DataFlowValueFactory.createDataFlowValue(receiver, receiverType, bindingContext).getId()
            dataFlowValueToVariable = {(value) ->
                val id = value.getId()
                if (id is com.intellij.openapi.util.Pair<*, *> && id.first == receiverId) id.second as? VariableDescriptor else null
            }
        }
        else {
            dataFlowValueToVariable = {(value) ->
                val id = value.getId()
                when {
                    id is VariableDescriptor -> id
                    id is com.intellij.openapi.util.Pair<*, *> && id.first is ThisReceiver -> id.second as? VariableDescriptor
                    else -> null
                }
            }
        }

        val variableToType = HashMap<VariableDescriptor, Collection<JetType>>()
        val typeInfo: SetMultimap<DataFlowValue, JetType> = dataFlowInfo.getCompleteTypeInfo()
        for ((dataFlowValue, types) in typeInfo.asMap().entrySet()) {
            val variable = dataFlowValueToVariable.invoke(dataFlowValue)
            if (variable != null) {
                variableToType[variable] = types
            }
        }

        val nullabilityInfo: Map<DataFlowValue, Nullability> = dataFlowInfo.getCompleteNullabilityInfo()
        val notNullVariables = nullabilityInfo
                .filter { it.getValue() == Nullability.NOT_NULL }
                .map { dataFlowValueToVariable(it.getKey()) }
                .filterNotNullTo(HashSet<VariableDescriptor>())

        return ProcessDataFlowInfoResult(variableToType, notNullVariables)
    }
}