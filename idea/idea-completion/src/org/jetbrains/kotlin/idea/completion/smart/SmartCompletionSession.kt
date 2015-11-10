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

package org.jetbrains.kotlin.idea.completion.smart

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptorKindExclude
import org.jetbrains.kotlin.psi.FunctionLiteralArgument
import org.jetbrains.kotlin.psi.KtCodeFragment
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
) : CompletionSession(configuration, parameters, resultSet) {

    override val descriptorKindFilter: DescriptorKindFilter by lazy {
        // we do not include SAM-constructors because they are handled separately and adding them requires iterating of java classes
        var filter = DescriptorKindFilter.VALUES exclude SamConstructorDescriptorKindExclude

        filter = filter exclude topLevelExtensionsExclude // handled via indices

        if (smartCompletion?.expectedInfos?.filterFunctionExpected()?.isNotEmpty() ?: false) {
            // if function type is expected we need classes to obtain their constructors
            filter = filter.withKinds(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS_MASK)
        }

        filter
    }

    private val smartCompletion by lazy(LazyThreadSafetyMode.NONE) {
        expression?.let {
            SmartCompletion(it, resolutionFacade, bindingContext, moduleDescriptor, isVisibleFilter,
                            prefixMatcher, originalSearchScope, toFromOriginalFileMapper,
                            callTypeAndReceiver, isJvmModule)
        }
    }

    override val expectedInfos: Collection<ExpectedInfo>
        get() = smartCompletion?.expectedInfos ?: emptyList()

    override fun doComplete() {
        if (nameExpression != null && NamedArgumentCompletion.isOnlyNamedArgumentExpected(nameExpression)) {
            NamedArgumentCompletion.complete(collector, expectedInfos)
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
        var contextVariableTypesForReferenceVariants = filter?.let {
            withCollectRequiredContextVariableTypes { lookupElementFactory ->
                val (imported, notImported) = referenceVariantsWithNonInitializedVarExcluded ?: return@withCollectRequiredContextVariableTypes
                imported.forEach { collector.addElements(filter(it, lookupElementFactory)) }
                notImported.forEach { collector.addElements(filter(it, lookupElementFactory), notImported = true) }
            }
        }

        flushToResultSet()

        val contextVariablesProvider = RealContextVariablesProvider(referenceVariantsHelper, position)
        withContextVariablesProvider(contextVariablesProvider) { lookupElementFactory ->
            if (contextVariableTypesForAdditionalItems.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                val additionalItems = smartCompletion!!.additionalItems(lookupElementFactory).first
                collector.addElements(additionalItems)
            }

            if (filter != null && contextVariableTypesForReferenceVariants!!.any { contextVariablesProvider.functionTypeVariables(it).isNotEmpty() }) {
                val (imported, notImported) = referenceVariantsWithSingleFunctionTypeParameter()!!
                imported.forEach { collector.addElements(filter(it, lookupElementFactory)) }
                notImported.forEach { collector.addElements(filter(it, lookupElementFactory), notImported = true) }
            }

            flushToResultSet()

            if (filter != null) {
                val staticMembersCompletion: StaticMembersCompletion?
                if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                    staticMembersCompletion = StaticMembersCompletion(prefixMatcher, resolutionFacade, lookupElementFactory, referenceVariants!!.imported, isJvmModule)
                    val decoratedFactory = staticMembersCompletion.decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER_FROM_IMPORTS)
                    staticMembersCompletion.membersFromImports(file)
                            .flatMap { filter(it, decoratedFactory) }
                            .forEach { collector.addElement(it) }
                }
                else {
                    staticMembersCompletion = null
                }

                if (shouldCompleteTopLevelCallablesFromIndex()) {
                    processTopLevelCallables {
                        collector.addElements(filter(it, lookupElementFactory), notImported = true)
                        flushToResultSet()
                    }
                }

                if (position.getContainingFile() is KtCodeFragment) {
                    val variantsAndFactory = getRuntimeReceiverTypeReferenceVariants(lookupElementFactory)
                    if (variantsAndFactory != null) {
                        val variants = variantsAndFactory.first
                        @Suppress("NAME_SHADOWING") val lookupElementFactory = variantsAndFactory.second
                        variants.imported.forEach { collector.addElements(filter(it, lookupElementFactory).map { it.withReceiverCast() }) }
                        variants.notImportedExtensions.forEach { collector.addElements(filter(it, lookupElementFactory).map { it.withReceiverCast() }, notImported = true) }
                        flushToResultSet()
                    }
                }

                if (staticMembersCompletion != null && configuration.completeStaticMembers) {
                    val decoratedFactory = staticMembersCompletion.decoratedLookupElementFactory(ItemPriority.STATIC_MEMBER)
                    staticMembersCompletion.processMembersFromIndices(indicesHelper(false)) {
                        filter(it, decoratedFactory).forEach {
                            collector.addElement(it)
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
            if (call != null && call.getFunctionLiteralArguments().isEmpty()) {
                val dummyArgument = object : FunctionLiteralArgument {
                    override fun getFunctionLiteral() = throw UnsupportedOperationException()
                    override fun getArgumentExpression() = throw UnsupportedOperationException()
                    override fun getArgumentName(): ValueArgumentName? = null
                    override fun isNamed() = false
                    override fun asElement() = throw UnsupportedOperationException()
                    override fun getSpreadElement(): LeafPsiElement? = null
                    override fun isExternal() = false
                }
                val dummyArguments = call.getValueArguments() + listOf(dummyArgument)
                val dummyCall = object : DelegatingCall(call) {
                    override fun getValueArguments() = dummyArguments
                    override fun getFunctionLiteralArguments() = listOf(dummyArgument)
                    override fun getValueArgumentList() = throw UnsupportedOperationException()
                }

                val expectedInfos = ExpectedInfos(bindingContext, resolutionFacade)
                        .calculateForArgument(dummyCall, dummyArgument)
                collector.addElements(LambdaItems.collect(expectedInfos))
            }
        }
    }

    override fun createSorter(): CompletionSorter {
        return super.createSorter()
                .weighBefore(KindWeigher.toString(), NameSimilarityWeigher, SmartCompletionPriorityWeigher)
    }

    override fun createLookupElementFactory(contextVariablesProvider: ContextVariablesProvider): LookupElementFactory {
        return super.createLookupElementFactory(contextVariablesProvider).copy(
                standardLookupElementsPostProcessor = { wrapStandardLookupElement(it) }
        )
    }

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