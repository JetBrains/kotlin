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
import org.jetbrains.kotlin.builtins.ReflectionTypes
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.idea.completion.handlers.WithExpressionPrefixInsertHandler
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.completion.shortenReferences
import org.jetbrains.kotlin.idea.completion.suppressAutoInsertion
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing
import org.jetbrains.kotlin.util.descriptorsEqualWithSubstitution
import java.util.*

class ArtificialElementInsertHandler(
        private val textBeforeCaret: String,
        private val textAfterCaret: String,
        private val shortenRefs: Boolean
) : InsertHandler<LookupElement>{
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val offset = context.editor.caretModel.offset
        val startOffset = offset - item.lookupString.length
        context.document.deleteString(startOffset, offset) // delete inserted lookup string
        context.document.insertString(startOffset, textBeforeCaret + textAfterCaret)
        context.editor.caretModel.moveToOffset(startOffset + textBeforeCaret.length)

        if (shortenRefs) {
            shortenReferences(context, startOffset, startOffset + textBeforeCaret.length + textAfterCaret.length)
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
                WithTailInsertHandler.COMMA.handleInsert(context, delegate)
            }
        }

        Tail.RPARENTH -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RPARENTH.handleInsert(context, delegate)
            }
        }

        Tail.RBRACKET -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RBRACKET.handleInsert(context, delegate)
            }
        }

        Tail.ELSE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.ELSE.handleInsert(context, delegate)
            }
        }

        Tail.RBRACE -> object: LookupElementDecorator<LookupElement>(this) {
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler.RBRACE.handleInsert(context, delegate)
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
                presentation.itemText = "*" + presentation.itemText
            }

            override fun handleInsert(context: InsertionContext) {
                WithExpressionPrefixInsertHandler("*").handleInsert(context, delegate)
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
    val similarity = calcNameSimilarity(lookupElement.lookupString, nameSimilarityExpectedInfos)
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

fun FuzzyType.matchExpectedInfo(expectedInfo: ExpectedInfo) = listOf(this).matchExpectedInfo(expectedInfo)

fun<TDescriptor: DeclarationDescriptor?> MutableCollection<LookupElement>.addLookupElements(
        descriptor: TDescriptor,
        expectedInfos: Collection<ExpectedInfo>,
        infoMatcher: (ExpectedInfo) -> ExpectedInfoMatch,
        noNameSimilarityForReturnItself: Boolean = false,
        lookupElementFactory: (TDescriptor) -> Collection<LookupElement>
) {
    class ItemData(val descriptor: TDescriptor, val itemOptions: ItemOptions) {
        @Suppress("UNCHECKED_CAST")
        override fun equals(other: Any?)
                = descriptorsEqualWithSubstitution(this.descriptor, (other as? ItemData)?.descriptor) && itemOptions ==
                (other as? ItemData)?.itemOptions
        override fun hashCode() = if (this.descriptor != null) this.descriptor.original.hashCode() else 0
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

@Suppress("UNCHECKED_CAST")
private fun <T : DeclarationDescriptor?> T.substituteFixed(substitutor: TypeSubstitutor): T {
    if (this is LocalVariableDescriptor || this is ValueParameterDescriptor || this !is Substitutable<*>) {
        return this
    }
    return this.substitute(substitutor) as T
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
                presentation.itemText = "!! " + presentation.itemText
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("!!", spaceBefore = false, spaceAfter = false).handleInsert(context, delegate)
            }
        }.postProcess()
    }

    factory().mapTo(this) {
        object: LookupElementDecorator<LookupElement>(it) {
            override fun renderElement(presentation: LookupElementPresentation) {
                super.renderElement(presentation)
                presentation.itemText = "?: " + presentation.itemText
            }
            override fun handleInsert(context: InsertionContext) {
                WithTailInsertHandler("?:", spaceBefore = true, spaceAfter = true).handleInsert(context, delegate) //TODO: code style
            }
        }.postProcess()
    }
}

fun CallableDescriptor.callableReferenceType(resolutionFacade: ResolutionFacade, lhs: DoubleColonLHS?): FuzzyType? {
    if (!CallType.CALLABLE_REFERENCE.descriptorKindFilter.accepts(this)) return null // not supported by callable references

    return DoubleColonExpressionResolver.createKCallableTypeForReference(
            this,
            lhs,
            resolutionFacade.getFrontendService(ReflectionTypes::class.java),
            resolutionFacade.moduleDescriptor
    )?.toFuzzyType(emptyList())
}

enum class SmartCompletionItemPriority {
    MULTIPLE_ARGUMENTS_ITEM,
    LAMBDA_SIGNATURE,
    LAMBDA_SIGNATURE_EXPLICIT_PARAMETER_TYPES,
    IT,
    TRUE,
    FALSE,
    NAMED_ARGUMENT_TRUE,
    NAMED_ARGUMENT_FALSE,
    CLASS_LITERAL,
    THIS,
    DELEGATES_STATIC_MEMBER,
    DEFAULT,
    NULLABLE,
    INSTANTIATION,
    STATIC_MEMBER,
    ANONYMOUS_OBJECT,
    LAMBDA_NO_PARAMS,
    LAMBDA,
    CALLABLE_REFERENCE,
    NULL,
    NAMED_ARGUMENT_NULL,
    INHERITOR_INSTANTIATION
}

val SMART_COMPLETION_ITEM_PRIORITY_KEY = Key<SmartCompletionItemPriority>("SMART_COMPLETION_ITEM_PRIORITY_KEY")

fun LookupElement.assignSmartCompletionPriority(priority: SmartCompletionItemPriority): LookupElement {
    putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
    return this
}

fun DeclarationDescriptor.fuzzyTypesForSmartCompletion(
        smartCastCalculator: SmartCastCalculator,
        callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        resolutionFacade: ResolutionFacade,
        bindingContext: BindingContext
): Collection<FuzzyType> {
    if (callTypeAndReceiver is CallTypeAndReceiver.CALLABLE_REFERENCE) {
        val lhs = callTypeAndReceiver.receiver?.let { bindingContext[BindingContext.DOUBLE_COLON_LHS, it] }
        return listOfNotNull((this as? CallableDescriptor)?.callableReferenceType(resolutionFacade, lhs))
    }

    if (this is CallableDescriptor) {
        val returnType = fuzzyReturnType() ?: return emptyList()

        // skip declarations of types Nothing, Nothing?, dynamic or of generic parameter type which has no real bounds
        if (returnType.type.isNothing() ||
            returnType.type.isNullableNothing() ||
            returnType.type.isDynamic() ||
            returnType.isAlmostEverything()) {
            return emptyList()
        }

        return if (this is VariableDescriptor) { //TODO: generic properties!
            smartCastCalculator.types(this).map { it.toFuzzyType(emptyList()) }
        }
        else {
            listOf(returnType)
        }
    }
    else if (this is ClassDescriptor && kind.isSingleton) {
        return listOf(defaultType.toFuzzyType(emptyList()))
    }
    else {
        return emptyList()
    }
}

fun Collection<ExpectedInfo>.filterFunctionExpected()
        = filter { it.fuzzyType != null && it.fuzzyType!!.type.isFunctionType }

fun Collection<ExpectedInfo>.filterCallableExpected()
        = filter { it.fuzzyType != null && ReflectionTypes.isCallableType(it.fuzzyType!!.type) }
