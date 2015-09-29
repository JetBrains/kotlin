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

import org.jetbrains.kotlin.descriptors.*
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
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotatedAsHidden
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindExclude
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getDescriptorsFiltered
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
            val element: JetElement?,
            val callType: CallType
    )

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

        if (expression.parent is JetUserType) {
            return getVariantsForUserType(explicitReceiverData, expression, kindFilter, nameFilter)
        }

        val resolutionScope = resolutionScope(expression) ?: return emptyList()
        val dataFlowInfo = dataFlowInfo(expression)
        val containingDeclaration = resolutionScope.getContainingDeclaration()

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance().flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(it.value, context, containingDeclaration, dataFlowInfo)
        }.toSet()

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        if (explicitReceiverData != null) {
            val (receiverElement, callType) = explicitReceiverData

            if (callType == CallType.CALLABLE_REFERENCE) {
                return getVariantsForCallableReference(receiverElement as JetTypeReference?, resolutionScope, implicitReceiverTypes, kindFilter, nameFilter)
            }

            receiverElement as JetExpression

            val qualifier = context[BindingContext.QUALIFIER, receiverElement]
            if (qualifier != null) {
                // It's impossible to add extension function for package or class (if it's companion object, expression type is not null)
                qualifier.scope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter).filterTo(descriptors)  { callType.canCall(it) }
            }

            val expressionType = if (useRuntimeReceiverType)
                                        getQualifierRuntimeType(receiverElement)
                                    else
                                        context.getType(receiverElement)
            if (expressionType != null && !expressionType.isError()) {
                val receiverValue = ExpressionReceiver(receiverElement, expressionType)
                val explicitReceiverTypes = smartCastManager
                        .getSmartCastVariantsWithLessSpecificExcluded(receiverValue, context, containingDeclaration, dataFlowInfo)

                descriptors.processAll(implicitReceiverTypes, explicitReceiverTypes, resolutionScope, callType, kindFilter, nameFilter)
            }
        }
        else {
            descriptors.processAll(implicitReceiverTypes, implicitReceiverTypes, resolutionScope, CallType.NORMAL, kindFilter, nameFilter)

            // add non-instance members
            descriptors.addAll(resolutionScope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter))
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
            val receiverExpression = explicitReceiverData.element as? JetExpression ?: return emptyList()
            val qualifier = context[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            return qualifier.scope.getDescriptorsFiltered(accurateKindFilter, nameFilter)
        }
        else {
            val lexicalScope = expression.getParentOfType<JetTypeReference>(strict = true)?.let {
                context[BindingContext.LEXICAL_SCOPE, it]
            } ?: return emptyList()
            return lexicalScope.getDescriptorsFiltered(accurateKindFilter, nameFilter)
        }
    }

    private fun getVariantsForCallableReference(
            qualifierTypeRef: JetTypeReference?,
            resolutionScope: JetScope,
            implicitReceiverTypes: Collection<JetType>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val accurateKindFilter = kindFilter.restrictedToKinds(DescriptorKindFilter.CALLABLES.kindMask)
        val descriptors = LinkedHashSet<DeclarationDescriptor>()
        if (qualifierTypeRef != null) {
            val type = context[BindingContext.TYPE, qualifierTypeRef] ?: return emptyList()

            descriptors.addNonExtensionMembers(listOf(type), CallType.CALLABLE_REFERENCE, accurateKindFilter, nameFilter, constructorsForInnerClassesOnly = false)

            descriptors.addScopeAndSyntheticExtensions(resolutionScope, listOf(type), CallType.CALLABLE_REFERENCE, accurateKindFilter, nameFilter)
        }
        else {
            // process non-instance members and class constructors
            descriptors.addNonExtensionCallablesAndConstructors(resolutionScope, CallType.CALLABLE_REFERENCE, kindFilter, nameFilter, constructorsForInnerClassesOnly = false)

            descriptors.addNonExtensionMembers(implicitReceiverTypes, CallType.CALLABLE_REFERENCE, accurateKindFilter, nameFilter, constructorsForInnerClassesOnly = true)

            //TODO: should we show synthetic extensions? get/set's?
            descriptors.addScopeAndSyntheticExtensions(resolutionScope, implicitReceiverTypes, CallType.CALLABLE_REFERENCE, accurateKindFilter, nameFilter)
        }
        return descriptors
    }

    private fun getVariantsForImportOrPackageDirective(
            explicitReceiverData: ExplicitReceiverData?,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (explicitReceiverData != null) {
            val receiverExpression = explicitReceiverData.element as? JetExpression ?: return emptyList()
            val qualifier = context[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
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
        addNonExtensionMembers(receiverTypes, callType, kindFilter, nameFilter, constructorsForInnerClassesOnly = true)
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
            nameFilter: (Name) -> Boolean,
            constructorsForInnerClassesOnly: Boolean
    ) {
        for (receiverType in receiverTypes) {
            addNonExtensionCallablesAndConstructors(receiverType.memberScope, callType, kindFilter, nameFilter, constructorsForInnerClassesOnly)
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionCallablesAndConstructors(
            scope: JetScope,
            callType: CallType,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            constructorsForInnerClassesOnly: Boolean
    ) {
        var filterToUse = kindFilter.restrictedToKinds(DescriptorKindFilter.CALLABLES.kindMask).exclude(DescriptorKindExclude.Extensions)

        // should process classes if we need constructors
        if (filterToUse.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            filterToUse = filterToUse.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        }

        for (descriptor in scope.getDescriptorsFiltered(filterToUse, nameFilter)) {
            if (descriptor is ClassDescriptor) {
                if (constructorsForInnerClassesOnly && !descriptor.isInner) continue
                if (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.SEALED) continue
                descriptor.constructors.filterTo(this) { callType.canCall(it) && kindFilter.accepts(it) }
            }
            else if (callType.canCall(descriptor) && kindFilter.accepts(descriptor)) {
                this.add(descriptor)
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

    //TODO: drop these methods
    public fun resolutionScope(expression: JetSimpleNameExpression): JetScope? {
        val parent = expression.parent
        return context[BindingContext.RESOLUTION_SCOPE, if (parent is JetCallableReferenceExpression) parent else expression]
    }

    public fun dataFlowInfo(expression: JetSimpleNameExpression): DataFlowInfo {
        val parent = expression.parent
        return context.getDataFlowInfo(if (parent is JetCallableReferenceExpression) parent else expression)
    }

    companion object {
        public fun getExplicitReceiverData(expression: JetSimpleNameExpression): ExplicitReceiverData? {
            val parent = expression.parent
            if (parent is JetCallableReferenceExpression) {
                return ExplicitReceiverData(parent.typeReference, CallType.CALLABLE_REFERENCE)
            }

            val receiverExpression = expression.getReceiverExpression() ?: return null
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
