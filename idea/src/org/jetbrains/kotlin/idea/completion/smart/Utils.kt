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

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.util.ShortenReferences
import java.util.HashSet
import com.intellij.codeInsight.lookup.LookupElementDecorator
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.types.TypeSubstitutor
import java.util.ArrayList
import java.util.HashMap
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.idea.util.TypeNullability

class ArtificialElementInsertHandler(
        val textBeforeCaret: String, val textAfterCaret: String, val shortenRefs: Boolean) : InsertHandler<LookupElement>{
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val offset = context.getEditor().getCaretModel().getOffset()
        val startOffset = offset - item.getLookupString().length()
        context.getDocument().deleteString(startOffset, offset) // delete inserted lookup string
        context.getDocument().insertString(startOffset, textBeforeCaret + textAfterCaret)
        context.getEditor().getCaretModel().moveToOffset(startOffset + textBeforeCaret.length())

        if (shortenRefs) {
            shortenReferences(context, startOffset, startOffset + textBeforeCaret.length() + textAfterCaret.length())
        }
    }
}

fun shortenReferences(context: InsertionContext, startOffset: Int, endOffset: Int) {
    PsiDocumentManager.getInstance(context.getProject()).commitAllDocuments();
    ShortenReferences.DEFAULT.process(context.getFile() as JetFile, startOffset, endOffset)
}

fun mergeTails(tails: Collection<Tail?>): Tail? {
    if (tails.size() == 1) return tails.single()
    return if (HashSet(tails).size() == 1) tails.first() else null
}

fun LookupElement.addTail(tail: Tail?): LookupElement {
    return when (tail) {
        null -> this

        Tail.COMMA -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.commaTail().handleInsert(context, getDelegate())
            }
        }

        Tail.RPARENTH -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.rparenthTail().handleInsert(context, getDelegate())
            }
        }

        Tail.ELSE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.elseTail().handleInsert(context, getDelegate())
            }
        }
    }
}

fun LookupElement.addTailAndNameSimilarity(matchedExpectedInfos: Collection<ExpectedInfo>): LookupElement {
    val lookupElement = addTail(mergeTails(matchedExpectedInfos.map { it.tail }))
    val similarity = calcNameSimilarity(lookupElement.getLookupString(), matchedExpectedInfos)
    if (similarity != 0) {
        lookupElement.putUserData(NAME_SIMILARITY_KEY, similarity)
    }
    return lookupElement
}

class ExpectedInfoClassification private(val substitutor: TypeSubstitutor?, val makeNotNullable: Boolean) {
    default object {
        val notMatches = ExpectedInfoClassification(null, false)
        fun matches(substitutor: TypeSubstitutor) = ExpectedInfoClassification(substitutor, false)
        fun matchesIfNotNullable(substitutor: TypeSubstitutor) = ExpectedInfoClassification(substitutor, true)
    }
}

fun Collection<FuzzyType>.classifyExpectedInfo(expectedInfo: ExpectedInfo): ExpectedInfoClassification {
    val stream = stream()
    val substitutor = stream.map { it.checkIsSubtypeOf(expectedInfo.type) }.firstOrNull()
    if (substitutor != null) {
        return ExpectedInfoClassification.matches(substitutor)
    }

    if (stream.any { it.nullability() == TypeNullability.NULLABLE }) {
        val substitutor2 = stream.map { it.makeNotNullable().checkIsSubtypeOf(expectedInfo.type) }.firstOrNull()
        if (substitutor2 != null) {
            return ExpectedInfoClassification.matchesIfNotNullable(substitutor2)
        }
    }

    return ExpectedInfoClassification.notMatches
}

fun FuzzyType.classifyExpectedInfo(expectedInfo: ExpectedInfo) = listOf(this).classifyExpectedInfo(expectedInfo)

fun<TDescriptor: DeclarationDescriptor?> MutableCollection<LookupElement>.addLookupElements(
        descriptor: TDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        infoClassifier: (ExpectedInfo) -> ExpectedInfoClassification,
        lookupElementFactory: (TDescriptor) -> LookupElement?
) {
    class DescriptorWrapper(val descriptor: TDescriptor) {
        override fun equals(other: Any?) = other is DescriptorWrapper && descriptorsEqualWithSubstitution(this.descriptor, other.descriptor)
        override fun hashCode() = if (this.descriptor != null) this.descriptor.getOriginal().hashCode() else 0
    }
    fun TDescriptor.wrap() = DescriptorWrapper(this)
    fun DescriptorWrapper.unwrap() = this.descriptor

    val matchedInfos = HashMap<DescriptorWrapper, MutableList<ExpectedInfo>>()
    val makeNullableInfos = HashMap<DescriptorWrapper, MutableList<ExpectedInfo>>()
    for (info in expectedInfos) {
        val classification = infoClassifier(info)
        if (classification.substitutor != null) {
            [suppress("UNCHECKED_CAST")]
            val substitutedDescriptor = descriptor?.substitute(classification.substitutor) as TDescriptor
            val map = if (classification.makeNotNullable) makeNullableInfos else matchedInfos
            map.getOrPut(substitutedDescriptor.wrap()) { ArrayList() }.add(info)
        }
    }

    if (!matchedInfos.isEmpty()) {
        for ((substitutedDescriptor, infos) in matchedInfos) {
            val lookupElement = lookupElementFactory(substitutedDescriptor.unwrap())
            if (lookupElement != null) {
                add(lookupElement.addTailAndNameSimilarity(infos))
            }
        }
    }
    else {
        for ((substitutedDescriptor, infos) in makeNullableInfos) {
            addLookupElementsForNullable({ lookupElementFactory(substitutedDescriptor.unwrap()) }, infos)
        }
    }
}

private fun MutableCollection<LookupElement>.addLookupElementsForNullable(factory: () -> LookupElement?, matchedInfos: Collection<ExpectedInfo>) {
    for (element in lookupElementsForNullable(factory)) {
        add(element.addTailAndNameSimilarity(matchedInfos))
    }
}

private fun lookupElementsForNullable(factory: () -> LookupElement?): Collection<LookupElement> {
    val result = ArrayList<LookupElement>(2)

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
        lookupElement = lookupElement!!.assignSmartCompletionPriority(SmartCompletionItemPriority.NULLABLE)
        result.add(lookupElement)
    }

    lookupElement = factory()
    if (lookupElement != null) {
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement!!) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("?: " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("?:", spaceBefore = true, spaceAfter = true).handleInsert(context, getDelegate()) //TODO: code style
            }
        }
        lookupElement = lookupElement!!.suppressAutoInsertion()
        lookupElement = lookupElement!!.assignSmartCompletionPriority(SmartCompletionItemPriority.NULLABLE)
        result.add(lookupElement)
    }

    return result
}

fun functionType(function: FunctionDescriptor): JetType? {
    val extensionReceiverType = function.getExtensionReceiverParameter()?.getType()
    val memberReceiverType = if (function is ConstructorDescriptor) {
        val classDescriptor = function.getContainingDeclaration()
        if (classDescriptor.isInner()) {
            (classDescriptor.getContainingDeclaration() as? ClassifierDescriptor)?.getDefaultType()
        }
        else {
            null
        }
    }
    else {
        (function.getContainingDeclaration() as? ClassifierDescriptor)?.getDefaultType()
    }
    //TODO: this is to be changed when references to member extensions supported
    val receiverType = if (extensionReceiverType != null && memberReceiverType != null)
        null
    else
        extensionReceiverType ?: memberReceiverType
    return KotlinBuiltIns.getInstance().getFunctionType(function.getAnnotations(),
                                                        receiverType,
                                                        function.getValueParameters().map { it.getType() },
                                                        function.getReturnType() ?: return null)
}

fun LookupElementFactory.createLookupElement(
        descriptor: DeclarationDescriptor,
        resolutionFacade: ResolutionFacade,
        bindingContext: BindingContext,
        boldImmediateMembers: Boolean
): LookupElement {
    var element = createLookupElement(resolutionFacade, descriptor, boldImmediateMembers)

    if (descriptor is FunctionDescriptor && descriptor.getValueParameters().isNotEmpty()) {
        element = element.keepOldArgumentListOnTab()
    }

    if (descriptor is ValueParameterDescriptor && bindingContext[BindingContext.AUTO_CREATED_IT, descriptor]) {
        element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.IT)
    }

    return element
}

fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()
fun <T : Any> T?.toSet(): Set<T> = if (this != null) setOf(this) else setOf()

fun String?.isNullOrEmpty() = this == null || this.isEmpty()

enum class SmartCompletionItemPriority {
    IT
    TRUE
    FALSE
    THIS
    DEFAULT
    NULLABLE
    STATIC_MEMBER
    INSTANTIATION
    ANONYMOUS_OBJECT
    LAMBDA_NO_PARAMS
    LAMBDA
    FUNCTION_REFERENCE
    NULL
    INHERITOR_INSTANTIATION
}

val SMART_COMPLETION_ITEM_PRIORITY_KEY = Key<SmartCompletionItemPriority>("SMART_COMPLETION_ITEM_PRIORITY_KEY")

fun LookupElement.assignSmartCompletionPriority(priority: SmartCompletionItemPriority): LookupElement {
    putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
    return this
}
