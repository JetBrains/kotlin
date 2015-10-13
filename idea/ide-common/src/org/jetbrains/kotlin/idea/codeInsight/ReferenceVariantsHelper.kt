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
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.JetCodeFragment
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.SmartCastManager
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.isAnnotatedAsHidden
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
    @JvmOverloads
    public fun getReferenceVariants(
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            callTypeAndReceiver: CallTypeAndReceiver<*, *> = CallTypeAndReceiver.detect(expression),
            filterOutJavaGettersAndSetters: Boolean = false,
            useRuntimeReceiverType: Boolean = false
    ): Collection<DeclarationDescriptor> {
        var variants: Collection<DeclarationDescriptor>
                = getReferenceVariantsNoVisibilityFilter(expression, kindFilter, nameFilter, callTypeAndReceiver, useRuntimeReceiverType)
                .filter { !it.isAnnotatedAsHidden() && visibilityFilter(it) }

        ShadowedDeclarationsFilter.create(context, resolutionFacade, expression, callTypeAndReceiver)?.let {
            variants = it.filter(variants)
        }


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
            callTypeAndReceiver: CallTypeAndReceiver<*, *>,
            useRuntimeReceiverType: Boolean
    ): Collection<DeclarationDescriptor> {
        val callType = callTypeAndReceiver.callType

        @Suppress("NAME_SHADOWING")
        val kindFilter = kindFilter.intersect(callType.descriptorKindFilter)

        val receiverExpression: JetExpression?
        when (callTypeAndReceiver) {
            is CallTypeAndReceiver.IMPORT_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.PACKAGE_DIRECTIVE -> {
                return getVariantsForImportOrPackageDirective(callTypeAndReceiver.receiver, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.TYPE -> {
                return getVariantsForUserType(callTypeAndReceiver.receiver, expression, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.CALLABLE_REFERENCE -> {
                val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression.parent as JetExpression] ?: return emptyList()
                return getVariantsForCallableReference(callTypeAndReceiver.receiver, resolutionScope, kindFilter, nameFilter)
            }

            is CallTypeAndReceiver.DEFAULT -> receiverExpression = null
            is CallTypeAndReceiver.DOT -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.SAFE -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.INFIX -> receiverExpression = callTypeAndReceiver.receiver
            is CallTypeAndReceiver.OPERATOR -> return emptyList()
            is CallTypeAndReceiver.UNKNOWN -> return emptyList()
            else -> throw RuntimeException() //TODO: see KT-9394
        }

        val resolutionScope = context[BindingContext.RESOLUTION_SCOPE, expression] ?: return emptyList()
        val dataFlowInfo = context.getDataFlowInfo(expression)
        val containingDeclaration = resolutionScope.getContainingDeclaration()

        val smartCastManager = resolutionFacade.frontendService<SmartCastManager>()
        val implicitReceiverTypes = resolutionScope.getImplicitReceiversWithInstance().flatMap {
            smartCastManager.getSmartCastVariantsWithLessSpecificExcluded(it.value, context, containingDeclaration, dataFlowInfo)
        }.toSet()

        val descriptors = LinkedHashSet<DeclarationDescriptor>()

        if (receiverExpression != null) {
            val qualifier = context[BindingContext.QUALIFIER, receiverExpression]
            if (qualifier != null) {
                descriptors.addAll(qualifier.scope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter))
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
            descriptors.processAll(implicitReceiverTypes, implicitReceiverTypes, resolutionScope, callType, kindFilter, nameFilter)

            // add non-instance members
            descriptors.addAll(resolutionScope.getDescriptorsFiltered(kindFilter exclude DescriptorKindExclude.Extensions, nameFilter))
        }

        return descriptors
    }

    private fun getVariantsForUserType(
            receiverExpression: JetExpression?,
            expression: JetSimpleNameExpression,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
            val qualifier = context[BindingContext.QUALIFIER, receiverExpression] ?: return emptyList()
            return qualifier.scope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
        else {
            val lexicalScope = expression.getParentOfType<JetTypeReference>(strict = true)?.let {
                context[BindingContext.LEXICAL_SCOPE, it]
            } ?: return emptyList()
            return lexicalScope.getDescriptorsFiltered(kindFilter, nameFilter)
        }
    }

    private fun getVariantsForCallableReference(
            qualifierTypeRef: JetTypeReference?,
            resolutionScope: JetScope,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        val descriptors = LinkedHashSet<DeclarationDescriptor>()
        if (qualifierTypeRef != null) {
            val type = context[BindingContext.TYPE, qualifierTypeRef] ?: return emptyList()

            descriptors.addNonExtensionMembers(listOf(type), kindFilter, nameFilter, constructorFilter = { true })

            descriptors.addScopeAndSyntheticExtensions(resolutionScope, listOf(type), CallType.CALLABLE_REFERENCE, kindFilter, nameFilter)
        }
        else {
            // process non-instance members and class constructors
            descriptors.addNonExtensionCallablesAndConstructors(resolutionScope, kindFilter, nameFilter, constructorFilter = { !it.isInner })
        }
        return descriptors
    }

    private fun getVariantsForImportOrPackageDirective(
            receiverExpression: JetExpression?,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (receiverExpression != null) {
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
            callType: CallType<*>,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
    ) {
        addNonExtensionMembers(receiverTypes, kindFilter, nameFilter, constructorFilter = { it.isInner })
        addMemberExtensions(implicitReceiverTypes, receiverTypes, callType, kindFilter, nameFilter)
        addScopeAndSyntheticExtensions(resolutionScope, receiverTypes, callType, kindFilter, nameFilter)
    }

    private fun MutableSet<DeclarationDescriptor>.addMemberExtensions(
            dispatchReceiverTypes: Collection<JetType>,
            extensionReceiverTypes: Collection<JetType>,
            callType: CallType<*>,
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
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        for (receiverType in receiverTypes) {
            addNonExtensionCallablesAndConstructors(receiverType.memberScope, kindFilter, nameFilter, constructorFilter)
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addNonExtensionCallablesAndConstructors(
            scope: JetScope,
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean,
            constructorFilter: (ClassDescriptor) -> Boolean
    ) {
        var filterToUse = DescriptorKindFilter(kindFilter.kindMask and DescriptorKindFilter.CALLABLES.kindMask).exclude(DescriptorKindExclude.Extensions)

        // should process classes if we need constructors
        if (filterToUse.acceptsKinds(DescriptorKindFilter.FUNCTIONS_MASK)) {
            filterToUse = filterToUse.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        }

        for (descriptor in scope.getDescriptorsFiltered(filterToUse, nameFilter)) {
            if (descriptor is ClassDescriptor) {
                if (descriptor.modality == Modality.ABSTRACT || descriptor.modality == Modality.SEALED) continue
                if (!constructorFilter(descriptor)) continue
                descriptor.constructors.filterTo(this) { kindFilter.accepts(it) }
            }
            else if (kindFilter.accepts(descriptor)) {
                this.add(descriptor)
            }
        }
    }

    private fun MutableSet<DeclarationDescriptor>.addScopeAndSyntheticExtensions(
            resolutionScope: JetScope,
            receiverTypes: Collection<JetType>,
            callType: CallType<*>,
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
            val runtimeType = evaluator?.invoke(receiver)
            if (runtimeType == type) return null
            return runtimeType
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
}
