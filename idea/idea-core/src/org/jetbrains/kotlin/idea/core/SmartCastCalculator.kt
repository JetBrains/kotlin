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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfoBefore
import org.jetbrains.kotlin.resolve.calls.smartcasts.*
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.util.javaslang.component1
import org.jetbrains.kotlin.util.javaslang.component2
import java.util.*

class SmartCastCalculator(
        val bindingContext: BindingContext,
        val containingDeclarationOrModule: DeclarationDescriptor,
        contextElement: PsiElement,
        receiver: KtExpression?,
        resolutionFacade: ResolutionFacade
) {
    private val dataFlowValueFactory = resolutionFacade.frontendService<DataFlowValueFactory>()

    // keys are VariableDescriptor's and ThisReceiver's
    private val entityToSmartCastInfo: Map<Any, SmartCastInfo> = processDataFlowInfo(
            bindingContext.getDataFlowInfoBefore(contextElement),
            contextElement.getResolutionScope(bindingContext, resolutionFacade),
            receiver)

    fun types(descriptor: VariableDescriptor): Collection<KotlinType> {
        val type = descriptor.returnType ?: return emptyList()
        return entityType(descriptor, type)
    }

    fun types(thisReceiverParameter: ReceiverParameterDescriptor): Collection<KotlinType> {
        val type = thisReceiverParameter.type
        val thisReceiver = thisReceiverParameter.value as? ImplicitReceiver ?: return listOf(type)
        return entityType(thisReceiver, type)
    }

    private fun entityType(entity: Any, ownType: KotlinType): Collection<KotlinType> {
        val smartCastInfo = entityToSmartCastInfo[entity] ?: return listOf(ownType)

        var types = smartCastInfo.types + ownType

        if (smartCastInfo.notNull) {
            types = types.map { it.makeNotNullable() }
        }

        return types
    }

    private data class SmartCastInfo(var types: Collection<KotlinType>, var notNull: Boolean) {
        constructor() : this(emptyList(), false)
    }

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo, resolutionScope: LexicalScope?, receiver: KtExpression?): Map<Any, SmartCastInfo> {
        if (dataFlowInfo == DataFlowInfo.EMPTY) return emptyMap()

        val dataFlowValueToEntity: (DataFlowValue) -> Any?
        if (receiver != null) {
            val receiverType = bindingContext.getType(receiver) ?: return emptyMap()
            val receiverIdentifierInfo = dataFlowValueFactory.createDataFlowValue(
                    receiver, receiverType, bindingContext, containingDeclarationOrModule
            ).identifierInfo
            dataFlowValueToEntity = { value ->
                val identifierInfo = value.identifierInfo
                if (identifierInfo is IdentifierInfo.Qualified && identifierInfo.receiverInfo == receiverIdentifierInfo) {
                    (identifierInfo.selectorInfo as? IdentifierInfo.Variable)?.variable
                }
                else null
            }
        }
        else {
            dataFlowValueToEntity = fun (value: DataFlowValue): Any? {
                val identifierInfo = value.identifierInfo
                when(identifierInfo) {
                    is IdentifierInfo.Variable -> return identifierInfo.variable
                    is IdentifierInfo.Receiver -> return identifierInfo.value as? ImplicitReceiver

                    is IdentifierInfo.Qualified -> {
                        val receiverInfo = identifierInfo.receiverInfo
                        val selectorInfo = identifierInfo.selectorInfo
                        if (receiverInfo !is IdentifierInfo.Receiver || selectorInfo !is IdentifierInfo.Variable) return null
                        val receiverValue = receiverInfo.value as? ImplicitReceiver ?: return null
                        if (resolutionScope?.findNearestReceiverForVariable(selectorInfo.variable)?.value != receiverValue) return null
                        return selectorInfo.variable
                    }

                    else -> return null
                }
            }
        }

        val entityToInfo = HashMap<Any, SmartCastInfo>()

        for ((dataFlowValue, types) in dataFlowInfo.completeTypeInfo) {
            val entity = dataFlowValueToEntity.invoke(dataFlowValue)
            if (entity != null) {
                entityToInfo[entity] = SmartCastInfo(types.toJavaList(), false)
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

    private fun LexicalScope.findNearestReceiverForVariable(variableDescriptor: VariableDescriptor): ReceiverParameterDescriptor? {
        val classifier = variableDescriptor.containingDeclaration as? ClassifierDescriptor ?: return null
        val type = classifier.defaultType
        return getImplicitReceiversWithInstance().firstOrNull { it.type.isSubtypeOf(type) }
    }
}
