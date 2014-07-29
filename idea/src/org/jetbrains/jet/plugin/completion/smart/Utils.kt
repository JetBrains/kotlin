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

package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.codeInsight.ShortenReferences
import java.util.HashSet
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import com.intellij.codeInsight.lookup.LookupElementPresentation
import java.util.ArrayList
import org.jetbrains.jet.plugin.completion.*
import com.intellij.openapi.util.Key
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.plugin.completion.handlers.WithTailInsertHandler

class ArtificialElementInsertHandler(
        val textBeforeCaret: String, val textAfterCaret: String, val shortenRefs: Boolean) : InsertHandler<LookupElement>{
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val offset = context.getEditor().getCaretModel().getOffset()
        val startOffset = offset - item.getLookupString().length
        context.getDocument().deleteString(startOffset, offset) // delete inserted lookup string
        context.getDocument().insertString(startOffset, textBeforeCaret + textAfterCaret)
        context.getEditor().getCaretModel().moveToOffset(startOffset + textBeforeCaret.length)

        if (shortenRefs) {
            shortenReferences(context, startOffset, startOffset + textBeforeCaret.length + textAfterCaret.length)
        }
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
    ShortenReferences.process(context.getFile() as JetFile, startOffset, endOffset)
}

fun mergeTails(tails: Collection<Tail?>): Tail? {
    if (tails.size == 1) return tails.single()
    return if (HashSet(tails).size == 1) tails.first() else null
}

fun LookupElement.addTail(tail: Tail?): LookupElement {
    return when (tail) {
        null -> this

        Tail.COMMA -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler(",", spaceBefore = false, spaceAfter = true /*TODO: use code style option*/).handleInsert(context, getDelegate())
            }
        }

        Tail.RPARENTH -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                handlers.WithTailInsertHandler(")", spaceBefore = false, spaceAfter = false).handleInsert(context, getDelegate())
            }
        }

        Tail.ELSE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                handlers.WithTailInsertHandler("else", spaceBefore = true, spaceAfter = true).handleInsert(context, getDelegate())
            }
        }
    }
}

fun LookupElement.addTail(matchedExpectedInfos: Collection<ExpectedInfo>): LookupElement
    = addTail(mergeTails(matchedExpectedInfos.map { it.tail }))

enum class ExpectedInfoClassification {
    MATCHES
    MAKE_NOT_NULLABLE
    NOT_MATCHES
}

fun MutableCollection<LookupElement>.addLookupElements(expectedInfos: Collection<ExpectedInfo>,
                                                       infoClassifier: (ExpectedInfo) -> ExpectedInfoClassification,
                                                       lookupElementFactory: () -> LookupElement?) {
    val matchedInfos = ArrayList<ExpectedInfo>()
    val matchedInfosNotNullable = ArrayList<ExpectedInfo>()
    for (info in expectedInfos) {
        when (infoClassifier(info)) {
            ExpectedInfoClassification.MATCHES -> matchedInfos.add(info)
            ExpectedInfoClassification.MAKE_NOT_NULLABLE -> matchedInfosNotNullable.add(info)
        }
    }

    if (matchedInfos.isNotEmpty()) {
        val lookupElement = lookupElementFactory()
        if (lookupElement != null) {
            add(lookupElement.addTail(matchedInfos))
        }
    }
    else if (matchedInfosNotNullable.isNotEmpty()) {
        addLookupElementsForNullable(lookupElementFactory, matchedInfosNotNullable)
    }
}

fun MutableCollection<LookupElement>.addLookupElementsForNullable(factory: () -> LookupElement?, matchedInfos: Collection<ExpectedInfo>) {
    var lookupElement = factory()
    if (lookupElement != null) {
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement!!) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("!! " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("!!", spaceBefore = false, spaceAfter = false).handleInsert(context, getDelegate())
            }
        }
        lookupElement = lookupElement!!.suppressAutoInsertion()
        add(lookupElement!!.addTail(matchedInfos))
    }

    lookupElement = factory()
    if (lookupElement != null) {
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement!!) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("?: " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                handlers.WithTailInsertHandler("?:", spaceBefore = true, spaceAfter = true).handleInsert(context, getDelegate()) //TODO: code style
            }
        }
        lookupElement = lookupElement!!.suppressAutoInsertion()
        add(lookupElement!!.addTail(matchedInfos))
    }
}

fun functionType(function: FunctionDescriptor): JetType? {
    return KotlinBuiltIns.getInstance().getFunctionType(function.getAnnotations(),
                                                        null,
                                                        function.getValueParameters().map { it.getType() },
                                                        function.getReturnType() ?: return null)
}

fun createLookupElement(descriptor: DeclarationDescriptor, resolveSession: ResolveSessionForBodies): LookupElement {
    val element = DescriptorLookupConverter.createLookupElement(resolveSession, descriptor)
    return if (descriptor is FunctionDescriptor && descriptor.getValueParameters().isNotEmpty()) element.keepOldArgumentListOnTab() else element
}

fun JetType.isSubtypeOf(expectedType: JetType) = !isError() && JetTypeChecker.DEFAULT.isSubtypeOf(this, expectedType)

fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()
fun <T : Any> T?.toSet(): Set<T> = if (this != null) setOf(this) else setOf()

fun String?.isNullOrEmpty() = this == null || this.isEmpty()
