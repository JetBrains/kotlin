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

import com.google.common.base.Predicate
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.psi.psiUtil.*
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.calls.autocasts.AutoCastUtils
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.scopes.JetScopeUtils
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils

import java.util.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo

public object TipsManager{

    public fun getReferenceVariants(expression: JetSimpleNameExpression, context: BindingContext): Collection<DeclarationDescriptor> {
        val receiverExpression = expression.getReceiverExpression()
        val parent = expression.getParent()
        val inPositionForCompletionWithReceiver = parent is JetCallExpression || parent is JetQualifiedExpression
        if (receiverExpression != null && inPositionForCompletionWithReceiver) {
            // Process as call expression
            val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()

            val descriptors = HashSet<DeclarationDescriptor>()

            val qualifier = context[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                // It's impossible to add extension function for package or class (if it's class object, expression type is not null)
                descriptors.addAll(qualifier.scope.getAllDescriptors())
            }

            val expressionType = context[BindingContext.EXPRESSION_TYPE, receiverExpression]
            if (expressionType != null && !expressionType.isError()) {
                val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

                val info = context.getDataFlowInfo(expression)

                val variantsForExplicitReceiver = AutoCastUtils.getAutoCastVariants(receiverValue, context, info)

                for (variant in variantsForExplicitReceiver) {
                    descriptors.addAll(variant.getMemberScope().getAllDescriptors())
                }

                descriptors.addAll(externalCallableExtensions(resolutionScope, receiverValue, context, info))
            }

            return descriptors
        }
        else {
            return getVariantsNoReceiver(expression, context)
        }
    }

    public fun getVariantsNoReceiver(expression: JetExpression, context: BindingContext): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        val parent = expression.getParent()
        if (parent is JetImportDirective || parent is JetPackageDirective) {
            return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors())
        }
        else {
            val descriptorsSet = HashSet<DeclarationDescriptor>()

            for (receiverDescriptor in resolutionScope.getImplicitReceiversHierarchy()) {
                receiverDescriptor.getType().getMemberScope().getAllDescriptors().filterTo(descriptorsSet) {
                    it !is CallableDescriptor || it.getReceiverParameter() == null/*skip member extension functions and properties*/
                }
            }

            descriptorsSet.addAll(resolutionScope.getAllDescriptors())

            return excludeNotCallableExtensions(descriptorsSet, resolutionScope, context, context.getDataFlowInfo(expression))
        }
    }

    public fun getPackageReferenceVariants(expression: JetSimpleNameExpression, context: BindingContext): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return excludeNonPackageDescriptors(resolutionScope.getAllDescriptors())
    }

    public fun excludeNotCallableExtensions(descriptors: Collection<DeclarationDescriptor>, scope: JetScope, context: BindingContext, dataFlowInfo: DataFlowInfo): Collection<DeclarationDescriptor> {
        val implicitReceivers = scope.getImplicitReceiversHierarchy()

        val descriptorsSet = HashSet(descriptors)
        descriptorsSet.removeAll(JetScopeUtils.getAllExtensions(scope).filter { callable ->
            if (callable.getReceiverParameter() == null)
                false
            else
                implicitReceivers.none { ExpressionTypingUtils.checkIsExtensionCallable(it.getValue(), callable, context, dataFlowInfo) }
        })
        return descriptorsSet
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

    private fun externalCallableExtensions(externalScope: JetScope, receiverValue: ReceiverValue, context: BindingContext, dataFlowInfo: DataFlowInfo): Collection<CallableDescriptor> {
        return JetScopeUtils.getAllExtensions(externalScope).filter {
            ExpressionTypingUtils.checkIsExtensionCallable(receiverValue, it, context, dataFlowInfo)
        }
    }
}
