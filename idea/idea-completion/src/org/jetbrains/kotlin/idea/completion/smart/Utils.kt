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
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.handlers.WithExpressionPrefixInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.SmartCastCalculator
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

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

        Tail.RBRACE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.rbraceTail().handleInsert(context, getDelegate())
            }
        }
    }
}

fun LookupElement.withOptions(options: ItemOptions): LookupElement {
    var lookupElement = this
    if (options.starPrefix) {
        lookupElement = object : LookupElementDecorator<LookupElement>(this) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("*" + presentation.getItemText())
            }

            override fun handleInsert(context: InsertionContext) {
                WithExpressionPrefixInsertHandler("*").handleInsert(context, getDelegate())
            }
        }
    }
    return lookupElement
}

fun LookupElement.addTailAndNameSimilarity(
        matchedExpectedInfos: Collection<ExpectedInfo>,
        nameSimilarityExpectedInfos: Collection<ExpectedInfo> = matchedExpectedInfos
): LookupElement {
    val lookupElement = addTail(mergeTails(matchedExpectedInfos.map { it.tail }))
    val similarity = calcNameSimilarity(lookupElement.getLookupString(), nameSimilarityExpectedInfos)
    if (similarity != 0) {
        lookupElement.putUserData(NAME_SIMILARITY_KEY, similarity)
    }
    return lookupElement
}

class ExpectedInfoClassification
private constructor(
        val substitutor: TypeSubstitutor?,
        val makeNotNullable: Boolean
) {
    fun isMatch() = substitutor != null && !makeNotNullable

    companion object {
        val noMatch = ExpectedInfoClassification(null, false)
        fun match(substitutor: TypeSubstitutor) = ExpectedInfoClassification(substitutor, false)
        fun ifNotNullMatch(substitutor: TypeSubstitutor) = ExpectedInfoClassification(substitutor, true)
    }
}

fun Collection<FuzzyType>.classifyExpectedInfo(expectedInfo: ExpectedInfo): ExpectedInfoClassification {
    val sequence = asSequence()
    val substitutor = sequence.map { expectedInfo.matchingSubstitutor(it) }.firstOrNull()
    if (substitutor != null) {
        return ExpectedInfoClassification.match(substitutor)
    }

    if (sequence.any { it.nullability() == TypeNullability.NULLABLE }) {
        val substitutor2 = sequence.map { expectedInfo.matchingSubstitutor(it.makeNotNullable()) }.firstOrNull()
        if (substitutor2 != null) {
            return ExpectedInfoClassification.ifNotNullMatch(substitutor2)
        }
    }

    return ExpectedInfoClassification.noMatch
}

fun FuzzyType.classifyExpectedInfo(expectedInfo: ExpectedInfo) = listOf(this).classifyExpectedInfo(expectedInfo)

fun<TDescriptor: DeclarationDescriptor?> MutableCollection<LookupElement>.addLookupElements(
        descriptor: TDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        infoClassifier: (ExpectedInfo) -> ExpectedInfoClassification,
        noNameSimilarityForReturnItself: Boolean = false,
        lookupElementFactory: (TDescriptor) -> LookupElement?
) {
    class ItemData(val descriptor: TDescriptor, val itemOptions: ItemOptions) {
        override fun equals(other: Any?)
                = other is ItemData && descriptorsEqualWithSubstitution(this.descriptor, other.descriptor) && itemOptions == other.itemOptions
        override fun hashCode() = if (this.descriptor != null) this.descriptor.getOriginal().hashCode() else 0
    }

    fun ItemData.createLookupElement() = lookupElementFactory(this.descriptor)?.withOptions(this.itemOptions)

    val matchedInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    val makeNullableInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    for (info in expectedInfos) {
        val classification = infoClassifier(info)
        if (classification.substitutor != null) {
            @suppress("UNCHECKED_CAST")
            val substitutedDescriptor = descriptor.substituteFixed(classification.substitutor)
            val map = if (classification.makeNotNullable) makeNullableInfos else matchedInfos
            map.getOrPut(ItemData(substitutedDescriptor, info.itemOptions)) { ArrayList() }.add(info)
        }
    }

    if (!matchedInfos.isEmpty()) {
        for ((itemData, infos) in matchedInfos) {
            val lookupElement = itemData.createLookupElement()
            if (lookupElement != null) {
                val nameSimilarityInfos = if (noNameSimilarityForReturnItself && descriptor is CallableDescriptor) {
                    infos.filter { (it as? ReturnValueExpectedInfo)?.callable != descriptor } // do not calculate name similarity with function itself in its return
                }
                else
                    infos
                add(lookupElement.addTailAndNameSimilarity(infos, nameSimilarityInfos))
            }
        }
    }
    else {
        for ((itemData, infos) in makeNullableInfos) {
            addLookupElementsForNullable({ itemData.createLookupElement() }, infos)
        }
    }
}

private fun <T : DeclarationDescriptor?> T.substituteFixed(substitutor: TypeSubstitutor): T {
    if (this is LocalVariableDescriptor || this is ValueParameterDescriptor || this is TypeParameterDescriptor) { // TODO: it's not implemented for them
        return this
    }
    return this?.substitute(substitutor) as T
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
        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
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
        result.add(lookupElement!!)
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
        result.add(lookupElement!!)
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
    return function.builtIns.getFunctionType(
            function.getAnnotations(), receiverType,
            function.getValueParameters().map { it.getType() },
            function.getReturnType() ?: return null
    )
}

fun LookupElementFactory.createLookupElement(
        descriptor: DeclarationDescriptor,
        bindingContext: BindingContext,
        boldImmediateMembers: Boolean
): LookupElement {
    var element = createLookupElement(descriptor, boldImmediateMembers)

    if (descriptor is FunctionDescriptor && descriptor.getValueParameters().isNotEmpty()) {
        element = element.keepOldArgumentListOnTab()
    }

    if (descriptor is ValueParameterDescriptor && bindingContext[BindingContext.AUTO_CREATED_IT, descriptor]!!) {
        element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.IT)
    }

    return element
}

fun <T : Any> T?.toList(): List<T> = if (this != null) listOf(this) else listOf()
fun <T : Any> T?.toSet(): Set<T> = if (this != null) setOf(this) else setOf()

enum class SmartCompletionItemPriority {
    IT,
    TRUE,
    FALSE,
    THIS,
    DEFAULT,
    NULLABLE,
    INSTANTIATION,
    STATIC_MEMBER,
    ANONYMOUS_OBJECT,
    LAMBDA_NO_PARAMS,
    LAMBDA,
    FUNCTION_REFERENCE,
    NULL,
    INHERITOR_INSTANTIATION
}

val SMART_COMPLETION_ITEM_PRIORITY_KEY = Key<SmartCompletionItemPriority>("SMART_COMPLETION_ITEM_PRIORITY_KEY")

fun LookupElement.assignSmartCompletionPriority(priority: SmartCompletionItemPriority): LookupElement {
    putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
    return this
}

fun DeclarationDescriptor.fuzzyTypesForSmartCompletion(smartCastCalculator: SmartCastCalculator): Collection<FuzzyType> {
    if (this is CallableDescriptor) {
        var returnType = fuzzyReturnType() ?: return emptyList()
        // skip declarations of type Nothing or of generic parameter type which has no real bounds
        if (returnType.type.isNothing() || returnType.isAlmostEverything()) return emptyList()

        if (this is VariableDescriptor) { //TODO: generic properties!
            return smartCastCalculator.types(this).map { FuzzyType(it, emptyList()) }
        }
        else {
            return listOf(returnType)
        }
    }
    else if (this is ClassDescriptor && kind.isSingleton) {
        return listOf(FuzzyType(defaultType, emptyList()))
    }
    else {
        return emptyList()
    }
}
