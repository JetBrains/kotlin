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
        private val lookupElementFactory: LookupElementFactory,
        private val sorter: CompletionSorter
) {

    private val elements = ArrayList<LookupElement>()

    private val resultSet = resultSet
            .withPrefixMatcher(prefixMatcher)
            .withRelevanceSorter(sorter)

    private val postProcessors = ArrayList<(LookupElement) -> LookupElement>()

    public fun flushToResultSet() {
        if (!elements.isEmpty()) {
            resultSet.addAllElements(elements)
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
                                     notImported: Boolean = false,
                                     withReceiverCast: Boolean = false
    ) {
        for (descriptor in descriptors) {
            addDescriptorElements(descriptor, notImported, withReceiverCast)
        }
    }

    public fun addDescriptorElements(descriptor: DeclarationDescriptor, notImported: Boolean = false, withReceiverCast: Boolean = false) {
        run {
            var lookupElements = lookupElementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)

            if (withReceiverCast) {
                lookupElements = lookupElements.map { it.withReceiverCast() }
            }

            addElements(lookupElements, notImported)
        }
    }

    public fun addElement(element: LookupElement, notImported: Boolean = false) {
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

        val psiElement = (result.`object` as? DeclarationLookupObject)?.psiElement
        if (psiElement != null) {
            result = object : LookupElementDecorator<LookupElement>(result) {
                override fun getPsiElement() = psiElement
            }
        }

        elements.add(result)
    }

    // used to avoid insertion of spaces before/after ',', '=' on just typing
    private fun isJustTyping(context: InsertionContext, element: LookupElement): Boolean {
        if (!completionParameters.isAutoPopup()) return false
        val insertedText = context.getDocument().getText(TextRange(context.getStartOffset(), context.getTailOffset()))
        return insertedText == element.getUserDataDeep(KotlinCompletionCharFilter.JUST_TYPING_PREFIX)
    }

    public fun addElements(elements: Iterable<LookupElement>, notImported: Boolean = false) {
        elements.forEach { addElement(it, notImported) }
    }

    public fun advertiseSecondCompletion() {
        JavaCompletionContributor.advertiseSecondCompletion(completionParameters.getOriginalFile().getProject(), resultSet)
    }

    public fun restartCompletionOnPrefixChange(prefixCondition: ElementPattern<String>) {
        resultSet.restartCompletionOnPrefixChange(prefixCondition)
    }
}
