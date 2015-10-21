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
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.handlers.WithExpressionPrefixInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.callableReferences.getReflectionTypeForCandidateDescriptor
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList
import java.util.*

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
    return tails.singleOrNull() ?: tails.toSet().singleOrNull()
}

fun LookupElement.addTail(tail: Tail?): LookupElement {
    return when (tail) {
        null -> this

        Tail.COMMA -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.COMMA.handleInsert(context, getDelegate())
            }
        }

        Tail.RPARENTH -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RPARENTH.handleInsert(context, getDelegate())
            }
        }

        Tail.RBRACKET -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RBRACKET.handleInsert(context, getDelegate())
            }
        }

        Tail.ELSE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.ELSE.handleInsert(context, getDelegate())
            }
        }

        Tail.RBRACE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RBRACE.handleInsert(context, getDelegate())
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

class ExpectedInfoMatch
private constructor(
        val substitutor: TypeSubstitutor?,
        val makeNotNullable: Boolean
) {
    fun isMatch() = substitutor != null && !makeNotNullable

    companion object {
        val noMatch = ExpectedInfoMatch(null, false)
        fun match(substitutor: TypeSubstitutor) = ExpectedInfoMatch(substitutor, false)
        fun ifNotNullMatch(substitutor: TypeSubstitutor) = ExpectedInfoMatch(substitutor, true)
    }
}

fun Collection<FuzzyType>.matchExpectedInfo(expectedInfo: ExpectedInfo): ExpectedInfoMatch {
    val sequence = asSequence()
    val substitutor = sequence.map { expectedInfo.matchingSubstitutor(it) }.firstOrNull()
    if (substitutor != null) {
        return ExpectedInfoMatch.match(substitutor)
    }

    if (sequence.any { it.nullability() == TypeNullability.NULLABLE }) {
        val substitutor2 = sequence.map { expectedInfo.matchingSubstitutor(it.makeNotNullable()) }.firstOrNull()
        if (substitutor2 != null) {
            return ExpectedInfoMatch.ifNotNullMatch(substitutor2)
        }
    }

    return ExpectedInfoMatch.noMatch
}

fun FuzzyType.classifyExpectedInfo(expectedInfo: ExpectedInfo) = listOf(this).matchExpectedInfo(expectedInfo)

fun<TDescriptor: DeclarationDescriptor?> MutableCollection<LookupElement>.addLookupElements(
        descriptor: TDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        infoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
        noNameSimilarityForReturnItself: Boolean = false,
        lookupElementFactory: (TDescriptor) -> Collection<LookupElement>
) {
    class ItemData(val descriptor: TDescriptor, val itemOptions: ItemOptions) {
        override fun equals(other: Any?)
                = other is ItemData && descriptorsEqualWithSubstitution(this.descriptor, other.descriptor) && itemOptions == other.itemOptions
        override fun hashCode() = if (this.descriptor != null) this.descriptor.getOriginal().hashCode() else 0
    }

    fun ItemData.createLookupElements() = lookupElementFactory(this.descriptor).map { it.withOptions(this.itemOptions) }

    val matchedInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    val makeNullableInfos = HashMap<ItemData, MutableList<ExpectedInfo>>()
    for (info in expectedInfos) {
        val classification = infoMatcher(info)
        if (classification.substitutor != null) {
            @Suppress("UNCHECKED_CAST")
            val substitutedDescriptor = descriptor.substituteFixed(classification.substitutor)
            val map = if (classification.makeNotNullable) makeNullableInfos else matchedInfos
            map.getOrPut(ItemData(substitutedDescriptor, info.itemOptions)) { ArrayList() }.add(info)
        }
    }

    if (!matchedInfos.isEmpty()) {
        for ((itemData, infos) in matchedInfos) {
            val lookupElements = itemData.createLookupElements()
            val nameSimilarityInfos = if (noNameSimilarityForReturnItself && descriptor is CallableDescriptor) {
                infos.filter { (it.additionalData as? ReturnValueAdditionalData)?.callable != descriptor } // do not calculate name similarity with function itself in its return
            }
            else
                infos
            lookupElements.mapTo(this) { it.addTailAndNameSimilarity(infos, nameSimilarityInfos) }
        }
    }
    else {
        for ((itemData, infos) in makeNullableInfos) {
            addLookupElementsForNullable({ itemData.createLookupElements() }, infos)
        }
    }
}

private fun <T : DeclarationDescriptor?> T.substituteFixed(substitutor: TypeSubstitutor): T {
    if (this is LocalVariableDescriptor || this is ValueParameterDescriptor || this is TypeParameterDescriptor) { // TODO: it's not implemented for them
        return this
    }
    return this?.substitute(substitutor) as T
}

private fun MutableCollection<LookupElement>.addLookupElementsForNullable(factory: () -> Collection<LookupElement>, matchedInfos: Collection<ExpectedInfo>) {
    fun LookupElement.postProcess(): LookupElement {
        var element = this
        element = element.suppressAutoInsertion()
        element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.NULLABLE)
        element = element.addTailAndNameSimilarity(matchedInfos)
        return element
    }

    factory().mapTo(this) {
        object: LookupElementDecorator<LookupElement>(it) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("!! " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("!!", spaceBefore = false, spaceAfter = false).handleInsert(context, getDelegate())
            }
        }.postProcess()
    }

    factory().mapTo(this) {
        object: LookupElementDecorator<LookupElement>(it) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.setItemText("?: " + presentation.getItemText())
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("?:", spaceBefore = true, spaceAfter = true).handleInsert(context, getDelegate()) //TODO: code style
            }
        }.postProcess()
    }
}

fun CallableDescriptor.callableReferenceType(resolutionFacade: ResolutionFacade): FuzzyType? {
    if (!CallType.CALLABLE_REFERENCE.descriptorKindFilter.accepts(this)) return null // not supported by callable references
    val type = getReflectionTypeForCandidateDescriptor(this, resolutionFacade.getFrontendService(ReflectionTypes::class.java)) ?: return null
    return FuzzyType(type, emptyList())
}

fun LookupElementFactory.createLookupElementsInSmartCompletion(
        descriptor: DeclarationDescriptor,
        bindingContext: BindingContext,
        useReceiverTypes: Boolean
): Collection<LookupElement> {
    return createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes).map {
        var element = it

        if (descriptor is FunctionDescriptor && descriptor.valueParameters.isNotEmpty()) {
            element = element.keepOldArgumentListOnTab()
        }

        if (descriptor is ValueParameterDescriptor && bindingContext[BindingContext.AUTO_CREATED_IT, descriptor]!!) {
            element = element.assignSmartCompletionPriority(SmartCompletionItemPriority.IT)
        }

        element
    }
}

enum class SmartCompletionItemPriority {
    MULTIPLE_ARGUMENTS_ITEM,
    IT,
    TRUE,
    FALSE,
    CLASS_LITERAL,
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

fun DeclarationDescriptor.fuzzyTypesForSmartCompletion(
        smartCastCalculator: SmartCastCalculator,
        callType: CallType<*>,
        resolutionFacade: ResolutionFacade
): Collection<FuzzyType> {
    if (callType == CallType.CALLABLE_REFERENCE) {
        return (this as? CallableDescriptor)?.callableReferenceType(resolutionFacade).singletonOrEmptyList()
    }

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

fun Collection<ExpectedInfo>.filterFunctionExpected()
        = filter { it.fuzzyType != null && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(it.fuzzyType!!.type) }

fun Collection<ExpectedInfo>.filterCallableExpected()
        = filter { it.fuzzyType != null && ReflectionTypes.isCallableType(it.fuzzyType!!.type) }

