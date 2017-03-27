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
import com.intellij.codeInsight.completion.impl.RealPrefixMatchingWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.ElementPattern
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.completion.handlers.WithExpressionPrefixInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import java.util.*

class LookupElementsCollector(
        private val prefixMatcher: PrefixMatcher,
        private val completionParameters: CompletionParameters,
        resultSet: CompletionResultSet,
        sorter: CompletionSorter,
        private val filter: ((LookupElement) -> Boolean)?
) {

    var bestMatchingDegree = Int.MIN_VALUE
        private set

    private val elements = ArrayList<LookupElement>()

    private val resultSet = resultSet
            .withPrefixMatcher(prefixMatcher)
            .withRelevanceSorter(sorter)

    private val postProcessors = ArrayList<(LookupElement) -> LookupElement>()

    var isResultEmpty: Boolean = true
        private set

    fun flushToResultSet() {
        if (!elements.isEmpty()) {
            resultSet.addAllElements(elements)
            elements.clear()
            isResultEmpty = false
        }
    }

    fun addLookupElementPostProcessor(processor: (LookupElement) -> LookupElement) {
        postProcessors.add(processor)
    }

    fun addDescriptorElements(descriptors: Iterable<DeclarationDescriptor>,
                                     lookupElementFactory: LookupElementFactory,
                                     notImported: Boolean = false,
                                     withReceiverCast: Boolean = false
    ) {
        for (descriptor in descriptors) {
            addDescriptorElements(descriptor, lookupElementFactory, notImported, withReceiverCast)
        }
    }

    fun addDescriptorElements(
            descriptor: DeclarationDescriptor,
            lookupElementFactory: LookupElementFactory,
            notImported: Boolean = false,
            withReceiverCast: Boolean = false
    ) {
        var lookupElements = lookupElementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)

        if (withReceiverCast) {
            lookupElements = lookupElements.map { it.withReceiverCast() }
        }

        addElements(lookupElements, notImported)
    }

    fun addElement(element: LookupElement, notImported: Boolean = false) {
        if (!prefixMatcher.prefixMatches(element)) return

        if (notImported) {
            element.putUserData(NOT_IMPORTED_KEY, Unit)
            if (isResultEmpty && elements.isEmpty()) { /* without these checks we may get duplicated items */
                addElement(element.suppressAutoInsertion())
            }
            else {
                addElement(element)
            }
            return
        }

        val decorated = object : LookupElementDecorator<LookupElement>(element) {
            override fun handleInsert(context: InsertionContext) {
                delegate.handleInsert(context)

                if (context.shouldAddCompletionChar() && !isJustTyping(context, this)) {
                    when (context.completionChar) {
                        ',' -> WithTailInsertHandler.COMMA.postHandleInsert(context, delegate)

                        '=' -> WithTailInsertHandler.EQ.postHandleInsert(context, delegate)

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

        val declarationLookupObject = result.`object` as? DeclarationLookupObject
        if (declarationLookupObject != null) {
            result = object : LookupElementDecorator<LookupElement>(result) {
                override fun getPsiElement() = declarationLookupObject.psiElement
            }
        }

        if (filter?.invoke(result) ?: true) {
            elements.add(result)
        }

        val matchingDegree = RealPrefixMatchingWeigher.getBestMatchingDegree(result, prefixMatcher)
        bestMatchingDegree = Math.max(bestMatchingDegree, matchingDegree)
    }

    // used to avoid insertion of spaces before/after ',', '=' on just typing
    private fun isJustTyping(context: InsertionContext, element: LookupElement): Boolean {
        if (!completionParameters.isAutoPopup) return false
        val insertedText = context.document.getText(TextRange(context.startOffset, context.tailOffset))
        return insertedText == element.getUserDataDeep(KotlinCompletionCharFilter.JUST_TYPING_PREFIX)
    }

    fun addElements(elements: Iterable<LookupElement>, notImported: Boolean = false) {
        elements.forEach { addElement(it, notImported) }
    }

    fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>) {
        resultSet.restartCompletionOnPrefixChange(prefixCondition)
    }
}
