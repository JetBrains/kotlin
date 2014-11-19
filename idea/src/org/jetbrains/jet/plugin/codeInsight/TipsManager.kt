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

import java.util.*
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindFilter
import org.jetbrains.jet.lang.resolve.scopes.DescriptorKindExclude
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl
import com.google.dart.compiler.util.Maps
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintsUtil

public object TipsManager{

    public fun getReferenceVariants(
            expression: JetSimpleNameExpression,
            context: BindingContext,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            visibilityFilter: (DeclarationDescriptor) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return getReferenceVariants(expression, context, kindFilter, nameFilter).filter(visibilityFilter)
    }

    private fun getReferenceVariants(
            expression: JetSimpleNameExpression,
            context: BindingContext,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val parent = expression.getParent()
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()

        if (parent is JetImportDirective || parent is JetPackageDirective) {
            val restrictedFilter = kindFilter.restrictedToKinds(DescriptorKindFilter.PACKAGES_MASK) ?: return listOf()
            return resolutionScope.getDescriptorsFiltered(restrictedFilter, nameFilter)
        }

        val receiverExpression = getReferenceVariantsReceiver(expression)
        if (receiverExpression != null) {
            val isInfixCall = parent is JetBinaryExpression
            fun filterIfInfix(descriptor: DeclarationDescriptor)
                    = if (isInfixCall) descriptor is SimpleFunctionDescriptor && descriptor.getValueParameters().size == 1 else true

            // Process as call expression
            val descriptors = HashSet<DeclarationDescriptor>()

            val qualifier = context[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                // It's impossible to add extension function for package or class (if it's class object, expression type is not null)
                qualifier.scope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter).filterTo(descriptors, ::filterIfInfix)
            }

            val expressionType = context[BindingContext.EXPRESSION_TYPE, receiverExpression]
            if (expressionType != null && !expressionType.isError()) {
                val receiverValue = ExpressionReceiver(receiverExpression, expressionType)
                val dataFlowInfo = context.getDataFlowInfo(expression)

                val mask = kindFilter.withoutKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK).exclude(DescriptorKindExclude.Extensions)
                for (variant in SmartCastUtils.getSmartCastVariantsWithLessSpecificExcluded(receiverValue, context, dataFlowInfo)) {
                    variant.getMemberScope().getDescriptorsFiltered(mask, nameFilter).filterTo(descriptors, ::filterIfInfix)
                }

                descriptors.addCallableExtensions(resolutionScope, receiverValue, context, dataFlowInfo, isInfixCall, kindFilter, nameFilter)
            }

            return descriptors
        }
        else {
            val descriptorsSet = HashSet<DeclarationDescriptor>()

            val receivers = resolutionScope.getImplicitReceiversHierarchy()
            receivers.flatMapTo(descriptorsSet) {
                it.getType().getMemberScope().getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter)
            }

            val dataFlowInfo = context.getDataFlowInfo(expression)
            val receiverValues = receivers.map { it.getValue() }

            resolutionScope.getDescriptorsFiltered(kindFilter, nameFilter).filterTo(descriptorsSet) {
                if (it is CallableDescriptor && it.getExtensionReceiverParameter() != null)
                    it.isExtensionCallable(receiverValues, context, dataFlowInfo, false)
                else
                    true
            }

            return descriptorsSet
        }
    }

    public fun getReferenceVariantsReceivers(expression: JetSimpleNameExpression, context: BindingContext): Collection<ReceiverValue> {
        val receiverExpression = getReferenceVariantsReceiver(expression)
        if (receiverExpression != null) {
            val expressionType = context[BindingContext.EXPRESSION_TYPE, receiverExpression] ?: return listOf()
            return listOf(ExpressionReceiver(receiverExpression, expressionType))
        }
        else {
            val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
            return resolutionScope.getImplicitReceiversHierarchy().map { it.getValue() }
        }
    }

    private fun getReferenceVariantsReceiver(expression: JetSimpleNameExpression): JetExpression? {
        val parent = expression.getParent()
        val inPositionForCompletionWithReceiver = parent is JetCallExpression
                                                  || parent is JetQualifiedExpression
                                                  || parent is JetBinaryExpression
        return if (inPositionForCompletionWithReceiver)
            expression.getReceiverExpression()
        else
            null
    }

    private fun MutableCollection<DeclarationDescriptor>.addCallableExtensions(
            resolutionScope: JetScope,
            receiver: ReceiverValue,
            context: BindingContext,
            dataFlowInfo: DataFlowInfo,
            isInfixCall: Boolean,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        if (!kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) {
            resolutionScope.getDescriptorsFiltered(kindFilter, nameFilter)
                    .stream()
                    .filterIsInstance(javaClass<CallableDescriptor>())
                    .filterTo(this) { checkIsExtensionCallable(receiver, it, isInfixCall, context, dataFlowInfo) }
        }
    }

    public fun CallableDescriptor.isExtensionCallable(receivers: Collection<ReceiverValue>,
                                                      context: BindingContext,
                                                      dataFlowInfo: DataFlowInfo,
                                                      isInfixCall: Boolean): Boolean
            = receivers.any { checkIsExtensionCallable(it, this, isInfixCall, context, dataFlowInfo) }

    public fun CallableDescriptor.isExtensionCallableWithImplicitReceiver(scope: JetScope, context: BindingContext, dataFlowInfo: DataFlowInfo): Boolean
            = isExtensionCallable(scope.getImplicitReceiversHierarchy().map { it.getValue() }, context, dataFlowInfo, false)

    public fun getPackageReferenceVariants(expression: JetSimpleNameExpression,
                                           context: BindingContext,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.PACKAGES, nameFilter)
    }

    /*
    * Checks if receiver declaration could be resolved to call expected receiver.
    */
    public fun checkIsExtensionCallable(
            receiverArgument: ReceiverValue,
            callableDescriptor: CallableDescriptor,
            isInfixCall: Boolean,
            bindingContext: BindingContext,
            dataFlowInfo: DataFlowInfo
    ): Boolean {
        if (isInfixCall && (callableDescriptor !is SimpleFunctionDescriptor || callableDescriptor.getValueParameters().size() != 1)) {
            return false
        }

        val types = SmartCastUtils.getSmartCastVariants(receiverArgument, bindingContext, dataFlowInfo)

        for (type in types) {
            if (checkReceiverResolution(receiverArgument, type, callableDescriptor)) return true
        }

        return false
    }

    private fun checkReceiverResolution(receiverArgument: ReceiverValue, receiverType: JetType, callableDescriptor: CallableDescriptor): Boolean {
        val receiverParameter = callableDescriptor.getExtensionReceiverParameter()

        if (!receiverArgument.exists() && receiverParameter == null) {
            // Both receivers do not exist
            return true
        }

        if (!(receiverArgument.exists() && receiverParameter != null)) {
            return false
        }

        val typeNamesInReceiver = collectUsedTypeNames(receiverParameter.getType())

        val constraintSystem = ConstraintSystemImpl()
        val typeVariables = LinkedHashMap<TypeParameterDescriptor, Variance>()
        for (typeParameterDescriptor in callableDescriptor.getTypeParameters()) {
            if (typeNamesInReceiver.contains(typeParameterDescriptor.getName())) {
                typeVariables.put(typeParameterDescriptor, Variance.INVARIANT)
            }
        }
        constraintSystem.registerTypeVariables(typeVariables)

        constraintSystem.addSubtypeConstraint(receiverType, receiverParameter.getType(), ConstraintPosition.RECEIVER_POSITION)
        return constraintSystem.getStatus().isSuccessful() && ConstraintsUtil.checkBoundsAreSatisfied(constraintSystem, true)
    }

    private fun collectUsedTypeNames(jetType: JetType): Set<Name> {
        val typeNames = HashSet<Name>()

        val descriptor = jetType.getConstructor().getDeclarationDescriptor()
        if (descriptor != null) {
            typeNames.add(descriptor.getName())
        }

        for (argument in jetType.getArguments()) {
            typeNames.addAll(collectUsedTypeNames(argument.getType()))
        }

        return typeNames
    }
}
