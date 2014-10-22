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
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils

import java.util.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.descriptorUtil.isExtension

public object TipsManager{

    public fun getReferenceVariants(expression: JetSimpleNameExpression, context: BindingContext, visibilityFilter: (DeclarationDescriptor) -> Boolean): Collection<DeclarationDescriptor> {
        return getReferenceVariants(expression, context).filter(visibilityFilter)
    }

    private fun getReferenceVariants(expression: JetSimpleNameExpression, context: BindingContext): Collection<DeclarationDescriptor> {
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
                    // It's impossible to add extension function for package or class (if it's class object, expression type is not null)
                    qualifier.scope.getAllDescriptors().filterTo(descriptors, ::filterIfInfix)
                }

                val expressionType = context[BindingContext.EXPRESSION_TYPE, receiverExpression]
                if (expressionType != null && !expressionType.isError()) {
                    val receiverValue = ExpressionReceiver(receiverExpression, expressionType)
                    val dataFlowInfo = context.getDataFlowInfo(expression)

                    for (variant in SmartCastUtils.getSmartCastVariants(receiverValue, context, dataFlowInfo)) {
                        variant.getMemberScope().getAllDescriptors().filterTo(descriptors) { filterIfInfix(it) && !it.isExtension }
                    }

                    JetScopeUtils.getAllExtensions(resolutionScope).filterTo(descriptors) {
                        ExpressionTypingUtils.checkIsExtensionCallable(receiverValue, it, isInfixCall, context, dataFlowInfo)
                    }
                }

                return descriptors
            }
        }

        if (parent is JetImportDirective || parent is JetPackageDirective) {
            return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors())
        }
        else {
            val descriptorsSet = HashSet<DeclarationDescriptor>()

            for (receiverDescriptor in resolutionScope.getImplicitReceiversHierarchy()) {
                receiverDescriptor.getType().getMemberScope().getAllDescriptors().filterTo(descriptorsSet) { !it.isExtension }
            }

            descriptorsSet.addAll(resolutionScope.getAllDescriptors())

            descriptorsSet.excludeNotCallableExtensions(resolutionScope, context, context.getDataFlowInfo(expression))

            return descriptorsSet
        }
    }

    public fun getPackageReferenceVariants(expression: JetSimpleNameExpression, context: BindingContext): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors())
    }

    public fun excludeNotCallableExtensions(descriptors: Collection<DeclarationDescriptor>,
                                            scope: JetScope,
                                            context: BindingContext,
                                            dataFlowInfo: DataFlowInfo): Collection<DeclarationDescriptor> {
        val set = HashSet(descriptors)
        set.excludeNotCallableExtensions(scope, context, dataFlowInfo)
        return set
    }

    private fun MutableSet<DeclarationDescriptor>.excludeNotCallableExtensions(scope: JetScope,
                                                                               context: BindingContext,
                                                                               dataFlowInfo: DataFlowInfo) {
        val implicitReceivers = scope.getImplicitReceiversHierarchy()
        removeAll(JetScopeUtils.getAllExtensions(scope).filter { callable ->
            implicitReceivers.none { ExpressionTypingUtils.checkIsExtensionCallable(it.getValue(), callable, false, context, dataFlowInfo) }
        })
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
