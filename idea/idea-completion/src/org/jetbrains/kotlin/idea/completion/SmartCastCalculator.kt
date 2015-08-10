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

package org.jetbrains.kotlin.idea.completion

import com.intellij.openapi.util.Pair
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
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
import java.util.HashMap

class SmartCastCalculator(
        val bindingContext: BindingContext,
        val containingDeclarationOrModule: DeclarationDescriptor,
        expression: JetExpression
) {
    // keys are VariableDescriptor's and ThisReceiver's
    private val entityToSmartCastInfo: Map<Any, SmartCastInfo>
            = processDataFlowInfo(bindingContext.getDataFlowInfo(expression), if (expression is JetSimpleNameExpression) expression.getReceiverExpression() else null)

    fun types(descriptor: VariableDescriptor): Collection<JetType> {
        val type = descriptor.returnType ?: return emptyList()
        return entityType(descriptor, type)
    }

    fun types(thisReceiverParameter: ReceiverParameterDescriptor): Collection<JetType> {
        val type = thisReceiverParameter.type
        val thisReceiver = thisReceiverParameter.value as? ThisReceiver ?: return listOf(type)
        return entityType(thisReceiver, type)
    }

    private fun entityType(entity: Any, ownType: JetType): Collection<JetType> {
        val smartCastInfo = entityToSmartCastInfo[entity] ?: return listOf(ownType)

        var types = smartCastInfo.types + ownType

        if (smartCastInfo.notNull) {
            types = types.map { it.makeNotNullable() }
        }

        return types
    }

    private data class SmartCastInfo(var types: Collection<JetType>, var notNull: Boolean) {
        constructor() : this(emptyList(), false)
    }

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo, receiver: JetExpression?): Map<Any, SmartCastInfo> {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return emptyMap()

        val dataFlowValueToEntity: (DataFlowValue) -> Any?
        if (receiver != null) {
            val receiverType = bindingContext.getType(receiver) ?: return emptyMap()
            val receiverId = DataFlowValueFactory.createDataFlowValue(receiver, receiverType, bindingContext, containingDeclarationOrModule).id
            dataFlowValueToEntity = { value ->
                val id = value.id
                if (id is Pair<*, *> && id.first == receiverId) id.second as? VariableDescriptor else null
            }
        }
        else {
            dataFlowValueToEntity = { value ->
                val id = value.id
                when(id) {
                    is VariableDescriptor, is ThisReceiver -> id
                    is Pair<*, *> -> if (id.first is ThisReceiver) id.second as? VariableDescriptor else null
                    else -> null
                }
            }
        }

        val entityToInfo = HashMap<Any, SmartCastInfo>()

        for ((dataFlowValue, types) in dataFlowInfo.completeTypeInfo.asMap().entrySet()) {
            val entity = dataFlowValueToEntity.invoke(dataFlowValue)
            if (entity != null) {
                entityToInfo[entity] = SmartCastInfo(types, false)
            }
        }

        for ((dataFlowValue, nullability) in dataFlowInfo.completeNullabilityInfo) {
            if (nullability == Nullability.NOT_NULL) {
                val entity = dataFlowValueToEntity(dataFlowValue) ?: continue
                entityToInfo.getOrPut(entity, { SmartCastInfo() }).notNull = true
            }
        }

        return entityToInfo
    }
}