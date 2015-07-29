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

package org.jetbrains.kotlin.idea.codeInsight

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.HashSet
import java.util.LinkedHashSet

public class ReferenceVariantsHelper(
        private val context: BindingContext,
        private val moduleDescriptor: ModuleDescriptor,
        private val project: Project,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public data class ReceiversData(
            public val receivers: Collection<ReceiverValue>,
            public val callType: CallType
    ) {
        companion object {
            val Empty = ReceiversData(listOf(), CallType.NORMAL)
        }
    }

    public fun getReferenceVariants(
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            filterOutJavaGettersAndSetters: Boolean = false,
            useRuntimeReceiverType: Boolean = false
    ): Collection<DeclarationDescriptor> {
        var variants: Collection<DeclarationDescriptor>
                = getReferenceVariantsNoVisibilityFilter(expression, kindFilter, useRuntimeReceiverType, nameFilter)
                .filter(visibilityFilter)

        variants = ShadowedDeclarationsFilter(context, moduleDescriptor, project).filter(variants, expression)

        if (filterOutJavaGettersAndSetters) {
            val accessorMethodsToRemove = HashSet<FunctionDescriptor>()
            for (variant in variants) {
                val original = variant.original
                if (original is SyntheticJavaPropertyDescriptor) {
                    accessorMethodsToRemove.add(original.getMethod)
                    accessorMethodsToRemove.addIfNotNull(original.setMethod)
                }
            }

            variants = variants.filter { it.original !in accessorMethodsToRemove }
        }

        return variants
    }

    private fun getReferenceVariantsNoVisibilityFilter(
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            useRuntimeReceiverType: Boolean,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val parent = expression.getParent()
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        val containingDeclaration = resolutionScope.getContainingDeclaration()

        if (parent is JetImportDirective || parent is JetPackageDirective) {
            return resolutionScope.getDescriptorsFiltered(kindFilter.restrictedToKinds(DescriptorKindFilter.PACKAGES_MASK), nameFilter)
        }

        if (parent is JetUserType) {
            return resolutionScope.getDescriptorsFiltered(kindFilter.restrictedToKinds(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK), nameFilter)
        }

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        val dataFlowInfo = context.getDataFlowInfo(expression)

        val pair = getExplicitReceiverData(expression)
        if (pair != null) {
            val (receiverExpression, callType) = pair

            val qualifier = context[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                // It's impossible to add extension function for package or class (if it's companion object, expression type is not null)
                qualifier.scope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter).filterTo(descriptors)  { callType.canCall(it) }
            }

            val expressionType = if (useRuntimeReceiverType)
                                        getQualifierRuntimeType(receiverExpression)
                                    else
                                        context.getType(receiverExpression)
            if (expressionType != null && !expressionType.isError()) {
                val receiverValue = ExpressionReceiver(receiverExpression, expressionType)
                descriptors.addMembersFromReceiverAndSyntheticExtensions(receiverValue, callType, kindFilter, nameFilter, resolutionScope, dataFlowInfo)

                descriptors.addCallableExtensions(resolutionScope, receiverValue, dataFlowInfo, callType, kindFilter, nameFilter)
            }
        }
        else {
            // process instance members that can be called via implicit receiver's instances
            val receivers = resolutionScope.getImplicitReceiversWithInstance()
            val receiverValues = receivers.map { it.getValue() }
            for (receiverValue in receiverValues) {
                descriptors.addMembersFromReceiverAndSyntheticExtensions(receiverValue, CallType.NORMAL, kindFilter, nameFilter, resolutionScope, dataFlowInfo)
            }

            // process extensions and non-instance members
            for (descriptor in resolutionScope.getDescriptorsFiltered(kindFilter, nameFilter)) {
                if (descriptor is CallableDescriptor && descriptor.getExtensionReceiverParameter() != null) {
                    val dispatchReceiver = descriptor.getDispatchReceiverParameter()
                    if (dispatchReceiver == null || dispatchReceiver in receivers) {
                        descriptors.addAll(descriptor.substituteExtensionIfCallable(receiverValues, context, dataFlowInfo, CallType.NORMAL, containingDeclaration))
                    }
                }
                else {
                    if (descriptor is CallableDescriptor && descriptor.getDispatchReceiverParameter() != null) continue // should already be processed via implicit receivers
                    descriptors.add(descriptor)
                }
            }
        }

        return descriptors
    }

    private fun MutableSet<DeclarationDescriptor>.addMembersFromReceiverAndSyntheticExtensions(
            receiverValue: ReceiverValue,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            resolutionScope: JetScope,
            dataFlowInfo: DataFlowInfo
    ) {
        val memberFilter = kindFilter exclude DescriptorKindExclude.Extensions
        val containingDeclaration = resolutionScope.getContainingDeclaration()

        val receiverTypes = SmartCastUtils.getSmartCastVariantsWithLessSpecificExcluded(receiverValue, context, containingDeclaration, dataFlowInfo)

        for (receiverType in receiverTypes) {
            val members = receiverType.getMemberScope().getDescriptorsFiltered(DescriptorKindFilter.ALL, nameFilter) // filter by kind later because of constructors
            for (member in members) {
                if (member is ClassDescriptor) {
                    if (member.isInner()) {
                        member.getConstructors().filterTo(this) { callType.canCall(it) && memberFilter.accepts(it) }
                    }
                }
                else if (callType.canCall(member) && memberFilter.accepts(member)) {
                    this.add(member)
                }
            }
        }

        if (!kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) {
            fun processExtension(extension: CallableDescriptor) {
                if (nameFilter(extension.name) && kindFilter.accepts(extension)) {
                    addAll(extension.substituteExtensionIfCallable(receiverValue, callType, context, dataFlowInfo, containingDeclaration))
                }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
                for (extension in resolutionScope.getSyntheticExtensionProperties(receiverTypes)) {
                    processExtension(extension)
                }
            }

            if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
                for (extension in resolutionScope.getSyntheticExtensionFunctions(receiverTypes)) {
                    processExtension(extension)
                }
            }
        }
    }

    public fun getReferenceVariantsReceivers(expression: JetSimpleNameExpression): ReceiversData {
        val receiverData = getExplicitReceiverData(expression)
        if (receiverData != null) {
            val receiverExpression = receiverData.first
            val expressionType = context.getType(receiverExpression) ?: return ReceiversData.Empty
            return ReceiversData(listOf(ExpressionReceiver(receiverExpression, expressionType)), receiverData.second)
        }
        else {
            val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return ReceiversData.Empty
            return ReceiversData(resolutionScope.getImplicitReceiversWithInstance().map { it.getValue() }, CallType.NORMAL)
        }
    }

    private fun getQualifierRuntimeType(receiver: JetExpression): JetType? {
        val type = context.getType(receiver)
        if (type != null && TypeUtils.canHaveSubtypes(JetTypeChecker.DEFAULT, type)) {
            val evaluator = receiver.getContainingFile().getCopyableUserData(JetCodeFragment.RUNTIME_TYPE_EVALUATOR)
            return evaluator?.invoke(receiver)
        }
        return type
    }

    private fun MutableCollection<DeclarationDescriptor>.addCallableExtensions(
            resolutionScope: JetScope,
            receiver: ReceiverValue,
            dataFlowInfo: DataFlowInfo,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) return
        val extensionsFilter = kindFilter.exclude(DescriptorKindExclude.NonExtensions)

        fun processExtension(descriptor: DeclarationDescriptor) {
            addAll((descriptor as CallableDescriptor).substituteExtensionIfCallable(receiver, callType, context, dataFlowInfo, resolutionScope.getContainingDeclaration()))
        }

        // process member extensions from implicit receivers separately to filter out ones from implicit receivers with no instance
        for (implicitReceiver in resolutionScope.getImplicitReceiversWithInstance()) {
            for (extension in implicitReceiver.getType().getMemberScope().getDescriptorsFiltered(extensionsFilter, nameFilter)) {
                processExtension(extension)
            }
        }

        for (extension in resolutionScope.getDescriptorsFiltered(extensionsFilter, nameFilter)) {
            if ((extension as CallableDescriptor).getDispatchReceiverParameter() == null) { // otherwise it should already be processed via implicit receivers
                processExtension(extension)
            }
        }
    }

    public fun getPackageReferenceVariants(
            expression: JetSimpleNameExpression,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.PACKAGES, nameFilter).filter(visibilityFilter)
    }

    companion object {
        public fun getExplicitReceiverData(expression: JetSimpleNameExpression): Pair<JetExpression, CallType>? {
            val receiverExpression = expression.getReceiverExpression() ?: return null
            val parent = expression.getParent()
            val callType = when (parent) {
                is JetBinaryExpression -> CallType.INFIX

                is JetCallExpression -> {
                    if ((parent.getParent() as JetQualifiedExpression).getOperationSign() == JetTokens.SAFE_ACCESS)
                        CallType.SAFE
                    else
                        CallType.NORMAL
                }

                is JetQualifiedExpression -> {
                    if (parent.getOperationSign() == JetTokens.SAFE_ACCESS)
                        CallType.SAFE
                    else
                        CallType.NORMAL
                }

                is JetUnaryExpression -> CallType.UNARY

                else -> return null
            }
            return receiverExpression to callType
        }
    }
}
