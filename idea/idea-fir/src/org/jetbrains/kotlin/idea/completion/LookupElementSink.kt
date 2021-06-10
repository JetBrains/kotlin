/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.openapi.editor.Document
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.idea.completion.stringTemplates.InsertStringTemplateBracesLookupElementDecorator
import org.jetbrains.kotlin.idea.completion.weighers.CompletionContributorGroupWeigher.groupPriority
import org.jetbrains.kotlin.idea.core.canDropBraces
import org.jetbrains.kotlin.idea.core.dropBraces
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression

internal class LookupElementSink(
    private val resultSet: CompletionResultSet,
    private val parameters: KotlinFirCompletionParameters,
    private val groupPriority: Int = 0,
) {

    fun withPriority(priority: Int): LookupElementSink =
        LookupElementSink(resultSet, parameters, priority)

    fun addElement(element: LookupElement) {
        element.groupPriority = groupPriority
        resultSet.addElement(applyWrappersToLookupElement(element))
    }

    fun addAllElements(elements: Iterable<LookupElement>) {
        elements.forEach {
            it.groupPriority = groupPriority
        }
        resultSet.addAllElements(elements.map(::applyWrappersToLookupElement))
    }

    private fun applyWrappersToLookupElement(lookupElement: LookupElement): LookupElement {
        val wrappers = WrappersProvider.getWrapperForLookupElement(parameters)
        return wrappers.wrap(lookupElement)
    }
}


private object WrappersProvider {
    fun getWrapperForLookupElement(parameters: KotlinFirCompletionParameters): List<LookupElementWrapper> {
        return when (parameters.type) {
            KotlinFirCompletionParameters.CorrectionType.BRACES_FOR_STRING_TEMPLATE -> {
                listOf(LookupElementWrapper(::InsertStringTemplateBracesLookupElementDecorator))
            }
            else -> listOf(LookupElementWrapper(::WrapSingleStringTemplateEntryWithBraces))
        }
    }
}

private class WrapSingleStringTemplateEntryWithBraces(lookupElement: LookupElement) : LookupElementDecorator<LookupElement>(lookupElement) {
    override fun handleInsert(context: InsertionContext) {
        val document = context.document
        context.commitDocument()

        if (needInsertBraces(context)) {
            insertBraces(context, document)
            context.commitDocument()

            super.handleInsert(context)

            removeUnneededBraces(context)
            context.commitDocument()
        } else {
            super.handleInsert(context)
        }
    }

    private fun insertBraces(context: InsertionContext, document: Document) {
        val startOffset = context.startOffset
        document.insertString(context.startOffset, "{")
        context.offsetMap.addOffset(CompletionInitializationContext.START_OFFSET, startOffset + 1)

        val tailOffset = context.tailOffset
        document.insertString(tailOffset, "}")
        context.tailOffset = tailOffset
    }

    private fun removeUnneededBraces(context: InsertionContext) {
        val templateEntry = getContainingTemplateEntry(context) as? KtBlockStringTemplateEntry ?: return
        if (templateEntry.canDropBraces()) {
            templateEntry.dropBraces()
        }
    }

    private fun needInsertBraces(context: InsertionContext): Boolean =
        getContainingTemplateEntry(context) is KtSimpleNameStringTemplateEntry

    private fun getContainingTemplateEntry(context: InsertionContext): KtStringTemplateEntryWithExpression? {
        val file = context.file
        val element = file.findElementAt(context.startOffset) ?: return null
        if (element.elementType != KtTokens.IDENTIFIER) return null
        val identifier = element.parent as? KtNameReferenceExpression ?: return null
        return identifier.parent as? KtStringTemplateEntryWithExpression
    }
}