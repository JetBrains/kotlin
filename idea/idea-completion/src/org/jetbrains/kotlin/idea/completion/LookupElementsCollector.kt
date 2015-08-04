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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.completion.handlers.*
import org.jetbrains.kotlin.types.JetType
import java.util.ArrayList
import java.util.LinkedHashMap

class LookupElementsCollector(
        private val defaultPrefixMatcher: PrefixMatcher,
        private val completionParameters: CompletionParameters,
        resultSet: CompletionResultSet,
        private val resolutionFacade: ResolutionFacade,
        private val lookupElementFactory: LookupElementFactory,
        private val sorter: CompletionSorter,
        private val inDescriptor: DeclarationDescriptor,
        private val context: LookupElementsCollector.Context
) {
    public enum class Context {
        NORMAL,
        STRING_TEMPLATE_AFTER_DOLLAR,
        INFIX_CALL
    }

    private val elements = LinkedHashMap<PrefixMatcher, ArrayList<LookupElement>>()

    private val defaultResultSet = resultSet
            .withPrefixMatcher(defaultPrefixMatcher)
            .withRelevanceSorter(sorter)

    private val postProcessors = ArrayList<(LookupElement) -> LookupElement>()

    public fun flushToResultSet() {
        if (!elements.isEmpty()) {
            for ((prefixMatcher, elements) in elements) {
                val resultSet = if (prefixMatcher == defaultPrefixMatcher)
                    defaultResultSet
                else
                    defaultResultSet.withPrefixMatcher(prefixMatcher)
                resultSet.addAllElements(elements)
            }
            elements.clear()
            isResultEmpty = false
        }
    }

    public var isResultEmpty: Boolean = true
        private set

    public fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        postProcessors.add(processor)
    }

    public fun addDescriptorElements(descriptors: Iterable<DeclarationDescriptor>,
                                     suppressAutoInsertion: Boolean, // auto-insertion suppression is used for elements that require adding an import
                                     withReceiverCast: Boolean = false
    ) {
        for (descriptor in descriptors) {
            addDescriptorElements(descriptor, suppressAutoInsertion, withReceiverCast)
        }
    }

    public fun addDescriptorElements(descriptor: DeclarationDescriptor, suppressAutoInsertion: Boolean, withReceiverCast: Boolean = false) {
        run {
            var lookupElement = lookupElementFactory.createLookupElement(descriptor, true)

            if (withReceiverCast) {
                lookupElement = lookupElement.withReceiverCast()
            }

            if (context == Context.STRING_TEMPLATE_AFTER_DOLLAR && (descriptor is FunctionDescriptor || descriptor is ClassifierDescriptor)) {
                lookupElement = lookupElement.withBracesSurrounding()
            }

            if (suppressAutoInsertion) {
                addElementWithAutoInsertionSuppressed(lookupElement)
            }
            else {
                addElement(lookupElement)
            }
        }

        // add special item for function with one argument of function type with more than one parameter
        if (context != Context.INFIX_CALL && descriptor is FunctionDescriptor) {
            addSpecialFunctionDescriptorElementIfNeeded(descriptor)
        }

        if (descriptor is PropertyDescriptor) {
            var lookupElement = lookupElementFactory.createBackingFieldLookupElement(descriptor, inDescriptor, resolutionFacade)
            if (lookupElement != null) {
                if (context == Context.STRING_TEMPLATE_AFTER_DOLLAR) {
                    lookupElement = lookupElement.withBracesSurrounding()
                }
                addElement(lookupElement)
            }
        }
    }

    private fun addSpecialFunctionDescriptorElementIfNeeded(descriptor: FunctionDescriptor) {
        val parameters = descriptor.getValueParameters()
        if (parameters.size() == 1) {
            val parameterType = parameters.get(0).getType()
            if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(parameterType)) {
                val parameterCount = KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(parameterType).size()
                if (parameterCount > 1) {
                    addSpecialFunctionDescriptorElement(descriptor, parameterType)
                }
            }
        }
    }

    private fun addSpecialFunctionDescriptorElement(descriptor: FunctionDescriptor, parameterType: JetType) {
        var lookupElement = lookupElementFactory.createLookupElement(descriptor, true)

        lookupElement = object : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)

                val tails = presentation.getTailFragments()
                presentation.clearTail()
                presentation.appendTailText(" " + buildLambdaPresentation(parameterType) + " ", false)
                tails.forEach { presentation.appendTailText(it.text, true) }
            }

            override fun handleInsert(context: InsertionContext) {
                KotlinFunctionInsertHandler(CaretPosition.IN_BRACKETS, GenerateLambdaInfo(parameterType, true)).handleInsert(context, this)
            }
        }

        if (context == Context.STRING_TEMPLATE_AFTER_DOLLAR) {
            lookupElement = lookupElement.withBracesSurrounding()
        }

        addElement(lookupElement)
    }

    public fun addElement(element: LookupElement, prefixMatcher: PrefixMatcher = defaultPrefixMatcher) {
        if (prefixMatcher.prefixMatches(element)) {
            val decorated = object : LookupElementDecorator<LookupElement>(element) {
                override fun handleInsert(context: InsertionContext) {
                    getDelegate().handleInsert(context)

                    if (context.shouldAddCompletionChar() && !isJustTyping(context, this)) {
                        when (context.getCompletionChar()) {
                            ',' -> WithTailInsertHandler.commaTail().postHandleInsert(context, getDelegate())

                            '=' -> WithTailInsertHandler.eqTail().postHandleInsert(context, getDelegate())

                            '!' -> {
                                WithExpressionPrefixInsertHandler("!").postHandleInsert(context)
                                context.setAddCompletionChar(false)
                            }
                        }
                    }

                }
            }

            var result: LookupElement = decorated
            for (postProcessor in postProcessors) {
                result = postProcessor(result)
            }

            elements.getOrPut(prefixMatcher) { ArrayList() }.add(result)
        }
    }

    // used to avoid insertion of spaces before/after ',', '=' on just typing
    private fun isJustTyping(context: InsertionContext, element: LookupElement): Boolean {
        if (!completionParameters.isAutoPopup()) return false
        val insertedText = context.getDocument().getText(TextRange(context.getStartOffset(), context.getTailOffset()))
        return insertedText == element.getUserData(KotlinCompletionCharFilter.JUST_TYPING_PREFIX)
    }

    public fun addElementWithAutoInsertionSuppressed(element: LookupElement) {
        if (isResultEmpty && elements.isEmpty()) { /* without these checks we may get duplicated items */
            addElement(element.suppressAutoInsertion())
        }
        else {
            addElement(element)
        }
    }

    public fun addElements(elements: Iterable<LookupElement>) {
        elements.forEach { addElement(it) }
    }

    public fun advertiseSecondCompletion() {
        JavaCompletionContributor.advertiseSecondCompletion(completionParameters.getOriginalFile().getProject(), defaultResultSet)
    }

    public fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>) {
        defaultResultSet.restartCompletionOnPrefixChange(prefixCondition)
    }
}
