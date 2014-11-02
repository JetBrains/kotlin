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

package org.jetbrains.jet.plugin.codeInsight

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils

import java.util.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.descriptorUtil.isExtension
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue

public object TipsManager{

    public fun getReferenceVariants(expression: JetSimpleNameExpression,
                                    context: BindingContext,
                                    kindFilterMask: Int,
                                    nameFilter: (Name) -> Boolean,
                                    visibilityFilter: (DeclarationDescriptor) -> Boolean): Collection<DeclarationDescriptor> {
        return getReferenceVariants(expression, context, kindFilterMask, nameFilter).filter(visibilityFilter)
    }

    private fun getReferenceVariants(expression: JetSimpleNameExpression,
                                     context: BindingContext,
                                     kindFilterMask: Int,
                                     nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val receiverExpression = expression.getReceiverExpression()
        val parent = expression.getParent()
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()

        if (receiverExpression != null) {
            val inPositionForCompletionWithReceiver = parent is JetCallExpression
                                                      || parent is JetQualifiedExpression
                                                      || parent is JetBinaryExpression
            if (inPositionForCompletionWithReceiver) {
                val isInfixCall = parent is JetBinaryExpression
                fun filterIfInfix(descriptor: DeclarationDescriptor)
                        = if (isInfixCall) descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size == 1 else true

                // Process as call expression
                val descriptors = HashSet<DeclarationDescriptor>()

                val qualifier = context[BindingContext.QUALIFIER, receiverExpression]
                if (qualifier != null) {
                    //TODO: filter out extensions!
                    // It's impossible to add extension function for package or class (if it's class object, expression type is not null)
                    qualifier.scope.getDescriptors(kindFilterMask and JetScope.NON_EXTENSIONS_MASK, nameFilter).filterTo(descriptors, ::filterIfInfix)
                }

                val expressionType = context[BindingContext.EXPRESSION_TYPE, receiverExpression]
                if (expressionType != null && !expressionType.isError()) {
                    val receiverValue = ExpressionReceiver(receiverExpression, expressionType)
                    val dataFlowInfo = context.getDataFlowInfo(expression)

                    for (variant in SmartCastUtils.getSmartCastVariants(receiverValue, context, dataFlowInfo)) {
                        //TODO: filter out nested classes!
                        variant.getMemberScope().getDescriptors(kindFilterMask, nameFilter).filterTo(descriptors) { filterIfInfix(it) && !it.isExtension }
                    }

                    descriptors.addAll(getCallableExtensions(resolutionScope, listOf(receiverValue), context, dataFlowInfo, isInfixCall, kindFilterMask, nameFilter))
                }

                return descriptors
            }
        }

        if (parent is JetImportDirective || parent is JetPackageDirective) {
            return excludeNonPackageDescriptors(resolutionScope.getDescriptors(kindFilterMask and JetScope.PACKAGE, nameFilter))
        }
        else {
            val descriptorsSet = HashSet<DeclarationDescriptor>()

            val receivers = resolutionScope.getImplicitReceiversHierarchy()
            for (receiver in receivers) {
                receiver.getType().getMemberScope().getDescriptors(kindFilterMask and JetScope.NON_EXTENSIONS_MASK, nameFilter)
                        .filterTo(descriptorsSet) { !it.isExtension }
            }

            val dataFlowInfo = context.getDataFlowInfo(expression)
            val receiverValues = receivers.map { it.getValue() }

            resolutionScope.getDescriptors(kindFilterMask, nameFilter).filterTo(descriptorsSet) {
                if (it is CallableDescriptor && it.getExtensionReceiverParameter() != null) {
                    it.isExtensionCallable(receiverValues, context, dataFlowInfo, false)
                }
                else
                    true
            }

            return descriptorsSet
        }
    }

    public fun getCallableExtensions(
            resolutionScope: JetScope,
            receivers: Collection<ReceiverValue>,
            context: BindingContext,
            dataFlowInfo: DataFlowInfo,
            isInfixCall: Boolean,
            kindFilterMask: Int,
            nameFilter: (Name) -> Boolean): Collection<CallableDescriptor> {
        val mask = kindFilterMask and JetScope.EXTENSIONS_MASK
        if (mask == 0) return listOf()
        return resolutionScope.getDescriptors(mask, nameFilter)
                .stream()
                .filterIsInstance(javaClass<CallableDescriptor>())
                .filter {
                    it.getExtensionReceiverParameter() != null && it.isExtensionCallable(receivers, context, dataFlowInfo, isInfixCall)
                }
                .toList()
    }

    public fun CallableDescriptor.isExtensionCallable(receivers: Collection<ReceiverValue>,
                                                      context: BindingContext,
                                                      dataFlowInfo: DataFlowInfo,
                                                      isInfixCall: Boolean): Boolean
            = receivers.any { ExpressionTypingUtils.checkIsExtensionCallable(it, this, isInfixCall, context, dataFlowInfo) }

    public fun CallableDescriptor.isExtensionCallableWithImplicitReceiver(scope: JetScope, context: BindingContext, dataFlowInfo: DataFlowInfo): Boolean
            = isExtensionCallable(scope.getImplicitReceiversHierarchy().map { it.getValue() }, context, dataFlowInfo, false)

    public fun getPackageReferenceVariants(expression: JetSimpleNameExpression,
                                           context: BindingContext,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return excludeNonPackageDescriptors(resolutionScope.getDescriptors(JetScope.PACKAGE, nameFilter))
    }

    private fun excludeNonPackageDescriptors(descriptors: Collection<DeclarationDescriptor>): Collection<DeclarationDescriptor> {
        return descriptors.filter{
                if (it is PackageViewDescriptor) {
                    // Heuristic: we don't want to complete "System" in "package java.lang.Sys",
                    // so we find class of the same name as package, we exclude this package
                    val parent = it.getContainingDeclaration()
                    parent == null || parent.getMemberScope().getClassifier(it.getName()) == null
                }
                else {
                    false
                }
            }
    }
}
