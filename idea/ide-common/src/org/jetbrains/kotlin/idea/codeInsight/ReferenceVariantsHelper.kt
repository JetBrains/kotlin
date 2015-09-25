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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.idea.util.CallType
import org.jetbrains.kotlin.idea.util.ShadowedDeclarationsFilter
import org.jetbrains.kotlin.idea.util.getImplicitReceiversWithInstance
import org.jetbrains.kotlin.idea.util.substituteExtensionIfCallable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isImportDirectiveExpression
import org.jetbrains.kotlin.psi.psiUtil.isPackageDirectiveExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotatedAsHidden
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.JetTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

public class ReferenceVariantsHelper(
        private val context: BindingContext,
        private val resolutionFacade: ResolutionFacade,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public data class ExplicitReceiverData(
            val expression: JetExpression,
            val callType: CallType
    )

    public data class ReceiversData(
            public val receivers: Collection<ReceiverValue>,
            public val callType: CallType
    ) {
        companion object {
            val Empty = ReceiversData(listOf(), CallType.NORMAL)
        }
    }

    @JvmOverloads
    public fun getReferenceVariants(
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            explicitReceiverData: ExplicitReceiverData? = getExplicitReceiverData(expression),
            filterOutJavaGettersAndSetters: Boolean = false,
            useRuntimeReceiverType: Boolean = false
    ): Collection<DeclarationDescriptor> {
        var variants: Collection<DeclarationDescriptor>
                = getReferenceVariantsNoVisibilityFilter(expression, kindFilter, nameFilter, explicitReceiverData, useRuntimeReceiverType)
                .filter { !it.isAnnotatedAsHidden() && visibilityFilter(it) }

        variants = ShadowedDeclarationsFilter(context, resolutionFacade, expression, explicitReceiverData).filter(variants)

        if (filterOutJavaGettersAndSetters) {
            val accessorMethodsToRemove = HashSet<FunctionDescriptor>()
            for (variant in variants) {
                if (variant is SyntheticJavaPropertyDescriptor) {
                    accessorMethodsToRemove.add(variant.getMethod.original)
                    accessorMethodsToRemove.addIfNotNull(variant.setMethod?.original)
                }
            }

            variants = variants.filter { it.original !in accessorMethodsToRemove }
        }

        return variants
    }

    private fun getReferenceVariantsNoVisibilityFilter(
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            explicitReceiverData: ExplicitReceiverData?,
            useRuntimeReceiverType: Boolean
    ): Collection<DeclarationDescriptor> {
        if (expression.isImportDirectiveExpression()) {
            return getVariantsForImportOrPackageDirective(explicitReceiverData, kindFilter, nameFilter)
        }
        if (expression.isPackageDirectiveExpression()) {
            val packageKindFilter = kindFilter restrictedToKinds DescriptorKindFilter.PACKAGES_MASK
            return getVariantsForImportOrPackageDirective(explicitReceiverData, packageKindFilter, nameFilter)
        }
        if (expression.getParent() is JetUserType) {
            return getVariantsForUserType(explicitReceiverData, expression, kindFilter, nameFilter)
        }

        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        val containingDeclaration = resolutionScope.getContainingDeclaration()

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        val dataFlowInfo = context.getDataFlowInfo(expression)

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance().flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(it.value, context, containingDeclaration, dataFlowInfo)
        }.toSet()

        if (explicitReceiverData != null) {
            val (receiverExpression, callType) = explicitReceiverData

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
                val explicitReceiverTypes = smartCastManager
                        .getSmartCastVariantsWithLessSpecificExcluded(receiverValue, context, containingDeclaration, dataFlowInfo)

                descriptors.processAll(implicitReceiverTypes, explicitReceiverTypes, resolutionScope, callType, kindFilter, nameFilter)
            }
        }
        else {
            descriptors.processAll(implicitReceiverTypes, implicitReceiverTypes, resolutionScope, CallType.NORMAL, kindFilter, nameFilter)

            // process non-instance members.
            for (descriptor in resolutionScope.getDescriptorsFiltered(kindFilter, nameFilter)) {
                if (!descriptor.isExtension) {
                    descriptors.add(descriptor)
                }
            }
        }

        return descriptors
    }

    private fun getVariantsForUserType(
            explicitReceiverData: ExplicitReceiverData?,
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val accurateKindFilter = kindFilter.restrictedToKinds(DescriptorKindFilter.CLASSIFIERS_MASK or DescriptorKindFilter.PACKAGES_MASK)
        if (explicitReceiverData != null) {
            val qualifier = context[BindingContext.QUALIFIER, explicitReceiverData.expression] ?: return emptyList()
            return qualifier.scope.getDescriptorsFiltered(accurateKindFilter, nameFilter)
        }
        else {
            val lexicalScope = expression.getParentOfType<JetTypeReference>(strict = true)?.let {
                context[BindingContext.TYPE_RESOLUTION_SCOPE, it]
            } ?: return emptyList()
            return lexicalScope.getDescriptorsFiltered(accurateKindFilter, nameFilter)
        }
    }

    private fun getVariantsForImportOrPackageDirective(
            explicitReceiverData: ExplicitReceiverData?,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (explicitReceiverData != null) {
            val qualifier = context[BindingContext.QUALIFIER, explicitReceiverData.expression] ?: return emptyList()
            return qualifier.scope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
        else {
            val rootPackage = resolutionFacade.moduleDescriptor.getPackage(FqName.ROOT)
            return rootPackage.memberScope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
    }

    private fun MutableSet<DeclarationDescriptor>.processAll(
            implicitReceiverTypes: Collection<JetType>,
            receiverTypes: Collection<JetType>,
            resolutionScope: JetScope,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        addNonExtensionMembers(receiverTypes, callType, kindFilter, nameFilter)
        addMemberExtensions(implicitReceiverTypes, receiverTypes, callType, kindFilter, nameFilter)
        addScopeAndSyntheticExtensions(resolutionScope, receiverTypes, callType, kindFilter, nameFilter)
    }

    private fun MutableSet<DeclarationDescriptor>.addMemberExtensions(
            dispatchReceiverTypes: Collection<JetType>,
            extensionReceiverTypes: Collection<JetType>,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        val memberFilter = kindFilter exclude DescriptorKindExclude.NonExtensions
        for (dispatchReceiverType in dispatchReceiverTypes) {
            for (member in dispatchReceiverType.memberScope.getDescriptorsFiltered(memberFilter, nameFilter)) {
                addAll((member as CallableDescriptor).substituteExtensionIfCallable(extensionReceiverTypes, callType))
            }
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionMembers(
            receiverTypes: Collection<JetType>,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        val memberFilter = kindFilter exclude DescriptorKindExclude.Extensions
        for (receiverType in receiverTypes) {
            val members = receiverType.memberScope.getDescriptorsFiltered(DescriptorKindFilter.ALL, nameFilter) // filter by kind later because of constructors
            for (member in members) {
                if (member is ClassDescriptor) {
                    if (member.isInner) {
                        member.constructors.filterTo(this) { callType.canCall(it) && memberFilter.accepts(it) }
                    }
                }
                else if (callType.canCall(member) && memberFilter.accepts(member)) {
                    this.add(member)
                }
            }
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addScopeAndSyntheticExtensions(
            resolutionScope: JetScope,
            receiverTypes: Collection<JetType>,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        if (kindFilter.excludes.contains(DescriptorKindExclude.Extensions)) return


        fun process(extension: CallableDescriptor) {
            if (nameFilter(extension.name) && kindFilter.accepts(extension)) {
                addAll(extension.substituteExtensionIfCallable(receiverTypes, callType))
            }
        }

        for (descriptor in resolutionScope.getDescriptors(kindFilter exclude DescriptorKindExclude.NonExtensions, nameFilter)) {
            // todo: sometimes resolution scope here is LazyJavaClassMemberScope. see ea.jetbrains.com/browser/ea_problems/72572
            if (descriptor.isExtension) {
                process(descriptor as CallableDescriptor)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.VARIABLES_MASK)) {
            for (extension in resolutionScope.getSyntheticExtensionProperties(receiverTypes)) {
                process(extension)
            }
        }

        if (kindFilter.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            for (extension in resolutionScope.getSyntheticExtensionFunctions(receiverTypes)) {
                process(extension)
            }
        }
    }

    public fun getReferenceVariantsReceivers(expression: JetSimpleNameExpression): ReceiversData {
        val receiverData = getExplicitReceiverData(expression)
        if (receiverData != null) {
            val receiverExpression = receiverData.expression
            val expressionType = context.getType(receiverExpression) ?: return ReceiversData.Empty
            return ReceiversData(listOf(ExpressionReceiver(receiverExpression, expressionType)), receiverData.callType)
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

    public fun getPackageReferenceVariants(
            expression: JetSimpleNameExpression,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return listOf()
        return resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.PACKAGES, nameFilter).filter(visibilityFilter)
    }

    companion object {
        public fun getExplicitReceiverData(expression: JetSimpleNameExpression): ExplicitReceiverData? {
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

                is JetUserType -> CallType.NORMAL

                else -> return null
            }
            return ExplicitReceiverData(receiverExpression, callType)
        }
    }
}
