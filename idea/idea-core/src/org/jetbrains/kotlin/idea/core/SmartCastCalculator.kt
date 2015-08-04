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

package org.jetbrains.kotlin.idea.core

import com.google.common.collect.SetMultimap
import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.Nullability
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.singletonOrEmptyList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet

class SmartCastCalculator(
        val bindingContext: BindingContext,
        val containingDeclarationOrModule: DeclarationDescriptor,
        nameExpression: JetSimpleNameExpression
): (VariableDescriptor) -> Collection<JetType> {

    override fun invoke(descriptor: VariableDescriptor): Collection<JetType> {
        return function(descriptor)
    }

    private val function: (VariableDescriptor) -> Collection<JetType> = run {
        val receiver = nameExpression.getReceiverExpression()
        val dataFlowInfo = bindingContext.getDataFlowInfo(nameExpression)
        val (variableToTypes, notNullVariables) = processDataFlowInfo(dataFlowInfo, receiver)

        fun typesOf(descriptor: VariableDescriptor): Collection<JetType> {
            var type = descriptor.getReturnType() ?: return listOf()
            if (notNullVariables.contains(descriptor)) {
                type = type.makeNotNullable()
            }

            val smartCastTypes = variableToTypes[descriptor]
            if (smartCastTypes == null || smartCastTypes.isEmpty()) return type.singletonOrEmptyList()
            return smartCastTypes + type.singletonOrEmptyList()
        }

        ::typesOf
    }

    private data class ProcessDataFlowInfoResult(
            val variableToTypes: Map<VariableDescriptor, Collection<JetType>> = Collections.emptyMap(),
            val notNullVariables: Set<VariableDescriptor> = Collections.emptySet()
    )

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo, receiver: JetExpression?): ProcessDataFlowInfoResult {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return ProcessDataFlowInfoResult()

        val dataFlowValueToVariable: (DataFlowValue) -> VariableDescriptor?
        if (receiver != null) {
            val receiverType = bindingContext.getType(receiver) ?: return ProcessDataFlowInfoResult()
            val receiverId = DataFlowValueFactory.createDataFlowValue(receiver, receiverType, bindingContext, containingDeclarationOrModule).getId()
            dataFlowValueToVariable = { value ->
                val id = value.getId()
                if (id is Pair<*, *> && id.first == receiverId) id.second as? VariableDescriptor else null
            }
        }
        else {
            dataFlowValueToVariable = { value ->
                val id = value.getId()
                when {
                    id is VariableDescriptor -> id
                    id is Pair<*, *> && id.first is ThisReceiver -> id.second as? VariableDescriptor
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