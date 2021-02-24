/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.ExpectedInfos
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.core.fuzzyType
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.resolve.sam.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.psi.LambdaArgument
import org.jetbrains.kotlin.psi.ValueArgumentName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.util.DelegatingCall
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class SmartCompletionSession(
    configuration: CompletionSessionConfiguration,
    parameters: CompletionParameters,
    toFromOriginalFileMapper: ToFromOriginalFileMapper,
    resultSet: CompletionResultSet
) : CompletionSession(configuration, parameters, toFromOriginalFileMapper, resultSet) {

    override val descriptorKindFilter: DescriptorKindFilter by lazy {
        // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
        val filter = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

        val referenceToConstructorIsApplicable = smartCompletion?.expectedInfos.orEmpty().any {
            it.fuzzyType?.type?.isFunctionType == true
        }

        if (referenceToConstructorIsApplicable) {
            filter.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        } else {
            filter
        }
    }

    private val smartCompletion by lazy(LazyThreadSafetyMode.NONE) {
        expression?.let {
            SmartCompletion(
                it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter, indicesHelper(false),
                prefixMatcher, searchScope, toFromOriginalFileMapper,
                callTypeAndReceiver, isJvmModule
            )
        }
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    override fun doComplete() {
        if (nameExpression != null && NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression, resolutionFacade)) {
            NamedArgumentCompletion.complete(collector, expectedInfos, callTypeAndReceiver.callType)
            return
        }

        if (expression == null) return

        addFunctionLiteralArgumentCompletions()

        var inheritanceSearcher: InheritanceItemsSearcher? = null
        val contextVariableTypesForAdditionalItems = withCollectRequiredContextVariableTypes { lookupElementFactory ->
            val pair = smartCompletion!!.additionalItems(lookupElementFactory)
            collector.addElements(pair.first)
            inheritanceSearcher = pair.second
        }

        val filter = smartCompletion!!.descriptorFilter
        val contextVariableTypesForReferenceVariants = filter?.let {
            withCollectRequiredContextVariableTypes { lookupElementFactory ->
                if (referenceVariantsCollector != null) {
                    val (imported, notImported) = referenceVariantsCollector.collectReferenceVariants(descriptorKindFilter)
                        .excludeNonInitializedVariable()
                    imported.forEach { collector.addElements(filter(it, lookupElementFactory)) }
                    notImported.forEach { collector.addElements(filter(it, lookupElementFactory), notImported = true) }
                    referenceVariantsCollector.collectingFinished()
                }
            }
        }

        flushToResultSet()

        val contextVariablesProvider = RealContextVariablesProvider(referenceVariantsHelper, position)
        withContextVariablesProvider(contextVariablesProvider) { lookupElementFactory ->
            if (filter != null && receiverTypes != null) {
                val results = ExtensionFunctionTypeValueCompletion(
                    receiverTypes,
                    callTypeAndReceiver.callType,
                    lookupElementFactory
                ).processVariables(contextVariablesProvider)
                for ((invokeDescriptor, factory) in results) {
                    collector.addElements(filter(invokeDescriptor, factory))
                }
            }

            if (contextVariableTypesForAdditionalItems.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                val additionalItems = smartCompletion!!.additionalItems(lookupElementFactory).first
                collector.addElements(additionalItems)
            }

            if (filter != null &&
                contextVariableTypesForReferenceVariants!!.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }
            ) {
                val (imported, notImported) = referenceVariantsWithSingleFunctionTypeParameter()!!
                imported.forEach { collector.addElements(filter(it, lookupElementFactory)) }
                notImported.forEach { collector.addElements(filter(it, lookupElementFactory), notImported = true) }
            }

            flushToResultSet()

            if (filter != null) {
                val staticMembersCompletion: StaticMembersCompletion?
                if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                    val alreadyCollected = referenceVariantsCollector!!.allCollected.imported
                    staticMembersCompletion =
                        StaticMembersCompletion(prefixMatcher, resolutionFacade, lookupElementFactory, alreadyCollected, isJvmModule)
                    val decoratedFactory = staticMembersCompletion.decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER_FROM_IMPORTS)
                    staticMembersCompletion.membersFromImports(file).flatMap { filter(it, decoratedFactory) }
                        .forEach { collector.addElement(it) }
                } else {
                    staticMembersCompletion = null
                }

                if (shouldCompleteTopLevelCallablesFromIndex()) {
                    processTopLevelCallables {
                        collector.addElements(filter(it, lookupElementFactory), notImported = true)
                        flushToResultSet()
                    }
                }

                if (isDebuggerContext) {
                    val variantsAndFactory = getRuntimeReceiverTypeReferenceVariants(lookupElementFactory)
                    if (variantsAndFactory != null) {
                        val variants = variantsAndFactory.first
                        @Suppress("NAME_SHADOWING") val lookupElementFactory = variantsAndFactory.second
                        variants.imported.forEach { collector.addElements(filter(it, lookupElementFactory).map { it.withReceiverCast() }) }
                        variants.notImportedExtensions.forEach {
                            collector.addElements(
                                filter(it, lookupElementFactory).map { element -> element.withReceiverCast() },
                                notImported = true
                            )
                        }
                        flushToResultSet()
                    }
                }

                if (staticMembersCompletion != null && configuration.staticMembers) {
                    val decoratedFactory = staticMembersCompletion.decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)
                    staticMembersCompletion.processMembersFromIndices(indicesHelper(false)) {
                        filter(it, decoratedFactory).forEach { element ->
                            collector.addElement(element)
                            flushToResultSet()
                        }
                    }
                }
            }
        }


        // it makes no sense to search inheritors if there is no reference because it means that we have prefix like "this@"
        inheritanceSearcher?.search({ prefixMatcher.prefixMatches(it) }) {
            collector.addElement(it)
            flushToResultSet()
        }
    }

    // special completion for outside parenthesis lambda argument
    private fun addFunctionLiteralArgumentCompletions() {
        if (nameExpression != null) {
            val callTypeAndReceiver = CallTypeAndReceiver.detect(nameExpression) as? CallTypeAndReceiver.INFIX ?: return
            val call = callTypeAndReceiver.receiver.getCall(bindingContext)
            if (call != null && call.functionLiteralArguments.isEmpty()) {
                val dummyArgument = object : LambdaArgument {
                    override fun getLambdaExpression() = throw UnsupportedOperationException()
                    override fun getArgumentExpression() = throw UnsupportedOperationException()
                    override fun getArgumentName(): ValueArgumentName? = null
                    override fun isNamed() = false
                    override fun asElement() = throw UnsupportedOperationException()
                    override fun getSpreadElement(): LeafPsiElement? = null
                    override fun isExternal() = false
                }
                val dummyArguments = call.valueArguments + listOf(dummyArgument)
                val dummyCall = object : DelegatingCall(call) {
                    override fun getValueArguments() = dummyArguments
                    override fun getFunctionLiteralArguments() = listOf(dummyArgument)
                    override fun getValueArgumentList() = throw UnsupportedOperationException()
                }

                val expectedInfos =
                    ExpectedInfos(bindingContext, resolutionFacade, indicesHelper(false)).calculateForArgument(dummyCall, dummyArgument)
                collector.addElements(LambdaItems.collect(expectedInfos))
            }
        }
    }

    override fun createSorter(): CompletionSorter = super.createSorter().weighBefore(
        KindWeigher.toString(),
        NameSimilarityWeigher,
        SmartCompletionPriorityWeigher,
        CallableReferenceWeigher(callTypeAndReceiver.callType)
    )

    override fun createLookupElementFactory(contextVariablesProvider: ContextVariablesProvider): LookupElementFactory =
        super.createLookupElementFactory(contextVariablesProvider).copy(
            standardLookupElementsPostProcessor = { wrapStandardLookupElement(it) }
        )

    private fun wrapStandardLookupElement(lookupElement: LookupElement): LookupElement {
        val descriptor = (lookupElement.`object` as DeclarationLookupObject).descriptor
        var element = lookupElement

        if (descriptor is FunctionDescriptor && descriptor.valueParameters.isNotEmpty()) {
            element = element.keepOldArgumentListOnTab()
        }

        if (descriptor is ValueParameterDescriptor && bindingContext[BindingContext.AUTO_CREATED_IT, descriptor]!!) {
            element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.IT)
        }

        return element
    }
}