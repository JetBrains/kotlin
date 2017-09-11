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

import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.OffsetKey
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.handlers.WithTailInsertHandler
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.isAlmostEverything
import org.jetbrains.kotlin.idea.util.toFuzzyType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

interface InheritanceItemsSearcher {
    fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit)
}

class SmartCompletion(
        private val expression: KtExpression,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean,
        private val indicesHelper: KotlinIndicesHelper,
        private val prefixMatcher: PrefixMatcher,
        private val inheritorSearchScope: GlobalSearchScope,
        private val toFromOriginalFileMapper: ToFromOriginalFileMapper,
        private val callTypeAndReceiver: CallTypeAndReceiver<*, *>,
        private val isJvmModule: Boolean,
        private val forBasicCompletion: Boolean = false
) {
    private val expressionWithType = when (callTypeAndReceiver) {
        is CallTypeAndReceiver.DEFAULT ->
            expression

        is CallTypeAndReceiver.DOT,
        is CallTypeAndReceiver.SAFE,
        is CallTypeAndReceiver.SUPER_MEMBERS,
        is CallTypeAndReceiver.INFIX,
        is CallTypeAndReceiver.CALLABLE_REFERENCE ->
            expression.parent as KtExpression

        else -> // actually no smart completion for such places
            expression
    }

    val expectedInfos: Collection<ExpectedInfo> = calcExpectedInfos(expressionWithType)

    private val callableTypeExpectedInfo = expectedInfos.filterCallableExpected()

    val smartCastCalculator: SmartCastCalculator by lazy(LazyThreadSafetyMode.NONE) {
        SmartCastCalculator(bindingContext, resolutionFacade.moduleDescriptor, expression, callTypeAndReceiver.receiver as? KtExpression, resolutionFacade)
    }

    val descriptorFilter: ((DeclarationDescriptor, AbstractLookupElementFactory) -> Collection<LookupElement>)? =
            { descriptor: DeclarationDescriptor, factory: AbstractLookupElementFactory ->
                filterDescriptor(descriptor, factory).map { postProcess(it) }
            }.takeIf { expectedInfos.isNotEmpty() }

    fun additionalItems(lookupElementFactory: LookupElementFactory): Pair<Collection<LookupElement>, InheritanceItemsSearcher?> {
        val (items, inheritanceSearcher) = additionalItemsNoPostProcess(lookupElementFactory)
        val postProcessedItems = items.map { postProcess(it) }
        //TODO: could not use "let" because of KT-8754
        val postProcessedSearcher = if (inheritanceSearcher != null)
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    inheritanceSearcher.search(nameFilter, { consumer(postProcess(it)) })
                }
            }
        else
            null
        return postProcessedItems to postProcessedSearcher
    }

    val descriptorsToSkip: Set<DeclarationDescriptor> by lazy<Set<DeclarationDescriptor>> {
        val parent = expressionWithType.parent
        when (parent) {
            is KtBinaryExpression -> {
                if (parent.right == expressionWithType) {
                    val operationToken = parent.operationToken
                    if (operationToken == KtTokens.EQ || operationToken in COMPARISON_TOKENS) {
                        val left = parent.left
                        if (left is KtReferenceExpression) {
                            return@lazy bindingContext[BindingContext.REFERENCE_TARGET, left]?.let(::setOf).orEmpty()
                        }
                    }
                }
            }

            is KtWhenConditionWithExpression -> {
                val entry = parent.parent as KtWhenEntry
                val whenExpression = entry.parent as KtWhenExpression
                val subject = whenExpression.subjectExpression ?: return@lazy emptySet()

                val descriptorsToSkip = HashSet<DeclarationDescriptor>()

                if (subject is KtSimpleNameExpression) {
                    val variable = bindingContext[BindingContext.REFERENCE_TARGET, subject] as? VariableDescriptor
                    if (variable != null) {
                        descriptorsToSkip.add(variable)
                    }
                }

                val subjectType = bindingContext.getType(subject) ?: return@lazy emptySet()
                val classDescriptor = TypeUtils.getClassDescriptor(subjectType)
                if (classDescriptor != null && DescriptorUtils.isEnumClass(classDescriptor)) {
                    val conditions = whenExpression.entries
                            .flatMap { it.conditions.toList() }
                            .filterIsInstance<KtWhenConditionWithExpression>()
                    for (condition in conditions) {
                        val selectorExpr = (condition.expression as? KtDotQualifiedExpression)
                                                   ?.selectorExpression as? KtReferenceExpression ?: continue
                        val target = bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr] as? ClassDescriptor ?: continue
                        if (DescriptorUtils.isEnumEntry(target)) {
                            descriptorsToSkip.add(target)
                        }
                    }
                }

                return@lazy descriptorsToSkip
            }
        }
        return@lazy emptySet()
    }

    private fun filterDescriptor(descriptor: DeclarationDescriptor, lookupElementFactory: AbstractLookupElementFactory): Collection<LookupElement> {
        ProgressManager.checkCanceled()
        if (descriptor in descriptorsToSkip) return emptyList()

        val result = SmartList<LookupElement>()
        val types = descriptor.fuzzyTypesForSmartCompletion(smartCastCalculator, callTypeAndReceiver, resolutionFacade, bindingContext)
        val infoMatcher = { expectedInfo: ExpectedInfo -> types.matchExpectedInfo(expectedInfo) }

        result.addLookupElements(descriptor, expectedInfos, infoMatcher, noNameSimilarityForReturnItself = callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) { descriptor ->
            lookupElementFactory.createStandardLookupElementsForDescriptor(descriptor, useReceiverTypes = true)
        }

        if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
            result.addCallableReferenceLookupElements(descriptor, lookupElementFactory)
        }

        return result
    }

    private fun additionalItemsNoPostProcess(lookupElementFactory: LookupElementFactory): Pair<Collection<LookupElement>, InheritanceItemsSearcher?> {
        val asTypePositionItems = buildForAsTypePosition(lookupElementFactory.basicFactory)
        if (asTypePositionItems != null) {
            assert(expectedInfos.isEmpty())
            return Pair(asTypePositionItems, null)
        }

        val items = ArrayList<LookupElement>()
        val inheritanceSearchers = ArrayList<InheritanceItemsSearcher>()

        if (!forBasicCompletion) { // basic completion adds keyword values on its own
            val keywordValueConsumer = object : KeywordValues.Consumer {
                override fun consume(lookupString: String, expectedInfoMatcher: (ExpectedInfo) -> ExpectedInfoMatch, priority: SmartCompletionItemPriority, factory: () -> LookupElement) {
                    items.addLookupElements(null, expectedInfos, expectedInfoMatcher) {
                        val lookupElement = factory()
                        lookupElement.putUserData(SMART_COMPLETION_ITEM_PRIORITY_KEY, priority)
                        listOf(lookupElement)
                    }
                }
            }
            KeywordValues.process(keywordValueConsumer, callTypeAndReceiver, bindingContext, resolutionFacade, moduleDescriptor, isJvmModule)
        }

        if (expectedInfos.isNotEmpty()) {
            if (!forBasicCompletion && (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT || callTypeAndReceiver is CallTypeAndReceiver.UNKNOWN /* after this@ */)) {
                items.addThisItems(expression, expectedInfos, smartCastCalculator)
            }

            if (callTypeAndReceiver is CallTypeAndReceiver.DEFAULT) {
                TypeInstantiationItems(resolutionFacade, bindingContext, visibilityFilter, toFromOriginalFileMapper, inheritorSearchScope, lookupElementFactory, forBasicCompletion, indicesHelper)
                        .addTo(items, inheritanceSearchers, expectedInfos)

                if (expression is KtSimpleNameExpression) {
                    StaticMembers(bindingContext, lookupElementFactory, resolutionFacade, moduleDescriptor)
                            .addToCollection(items, expectedInfos, expression, descriptorsToSkip)
                }

                ClassLiteralItems.addToCollection(items, expectedInfos, lookupElementFactory.basicFactory, isJvmModule)

                items.addNamedArgumentsWithLiteralValueItems(expectedInfos)

                LambdaSignatureItems.addToCollection(items, expressionWithType, bindingContext, resolutionFacade)

                if (!forBasicCompletion) {
                    LambdaItems.addToCollection(items, expectedInfos)

                    val whenCondition = expressionWithType.parent as? KtWhenConditionWithExpression
                    if (whenCondition != null) {
                        val entry = whenCondition.parent as KtWhenEntry
                        val whenExpression = entry.parent as KtWhenExpression
                        val entries = whenExpression.entries
                        if (whenExpression.elseExpression == null && entry == entries.last() && entries.size != 1) {
                            val lookupElement = LookupElementBuilder.create("else").bold().withTailText(" ->")
                            items.add(object: LookupElementDecorator<LookupElement>(lookupElement) {
                                override fun handleInsert(context: InsertionContext) {
                                    WithTailInsertHandler("->", spaceBefore = true, spaceAfter = true).handleInsert(context, delegate)
                                }
                            })
                        }
                    }
                }

                MultipleArgumentsItemProvider(bindingContext, smartCastCalculator, resolutionFacade)
                        .addToCollection(items, expectedInfos, expression)
            }
        }

        val inheritanceSearcher = if (inheritanceSearchers.isNotEmpty())
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    inheritanceSearchers.forEach { it.search(nameFilter, consumer) }
                }
            }
        else
            null

        return Pair(items, inheritanceSearcher)
    }

    private fun postProcess(item: LookupElement): LookupElement {
        if (forBasicCompletion) return item

        return if (item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) == null) {
            object : LookupElementDecorator<LookupElement>(item) {
                override fun handleInsert(context: InsertionContext) {
                    if (context.completionChar == Lookup.REPLACE_SELECT_CHAR) {
                        val offset = context.offsetMap.tryGetOffset(OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                        if (offset != null) {
                            context.document.deleteString(context.tailOffset, offset)
                        }
                    }

                    super.handleInsert(context)
                }
            }
        }
        else {
            item
        }
    }

    private fun MutableCollection<LookupElement>.addThisItems(place: KtExpression, expectedInfos: Collection<ExpectedInfo>, smartCastCalculator: SmartCastCalculator) {
        if (shouldCompleteThisItems(prefixMatcher)) {
            val items = thisExpressionItems(bindingContext, place, prefixMatcher.prefix, resolutionFacade)
            for (item in items) {
                val types = smartCastCalculator.types(item.receiverParameter).map { it.toFuzzyType(emptyList()) }
                val matcher = { expectedInfo: ExpectedInfo -> types.matchExpectedInfo(expectedInfo) }
                addLookupElements(null, expectedInfos, matcher) {
                    listOf(item.createLookupElement().assignSmartCompletionPriority(SmartCompletionItemPriority.THIS))
                }
            }
        }
    }

    private fun MutableCollection<LookupElement>.addNamedArgumentsWithLiteralValueItems(expectedInfos: Collection<ExpectedInfo>) {
        data class NameAndValue(val name: Name, val value: String, val priority: SmartCompletionItemPriority)

        val nameAndValues = HashMap<NameAndValue, MutableList<ExpectedInfo>>()

        fun addNameAndValue(name: Name, value: String, priority: SmartCompletionItemPriority, expectedInfo: ExpectedInfo) {
            nameAndValues.getOrPut(NameAndValue(name, value, priority)) { ArrayList() }.add(expectedInfo)
        }

        for (expectedInfo in expectedInfos) {
            val argumentData = expectedInfo.additionalData as? ArgumentPositionData.Positional ?: continue
            if (argumentData.namedArgumentCandidates.isEmpty()) continue
            val parameters = argumentData.function.valueParameters
            if (argumentData.argumentIndex >= parameters.size) continue
            val parameterName = parameters[argumentData.argumentIndex].name

            if (expectedInfo.fuzzyType?.type?.isBooleanOrNullableBoolean() == true) {
                addNameAndValue(parameterName, "true", SmartCompletionItemPriority.NAMED_ARGUMENT_TRUE, expectedInfo)
                addNameAndValue(parameterName, "false", SmartCompletionItemPriority.NAMED_ARGUMENT_FALSE, expectedInfo)
            }
            if (expectedInfo.fuzzyType?.type?.isMarkedNullable == true) {
                addNameAndValue(parameterName, "null", SmartCompletionItemPriority.NAMED_ARGUMENT_NULL, expectedInfo)
            }
        }

        for ((nameAndValue, infos) in nameAndValues) {
            var lookupElement = createNamedArgumentWithValueLookupElement(nameAndValue.name, nameAndValue.value, nameAndValue.priority)
            lookupElement = lookupElement.addTail(mergeTails(infos.map { it.tail }))
            add(lookupElement)
        }
    }

    private fun createNamedArgumentWithValueLookupElement(name: Name, value: String, priority: SmartCompletionItemPriority): LookupElement {
        val lookupElement = LookupElementBuilder.create("${name.asString()} = $value")
                .withIcon(KotlinIcons.PARAMETER)
                .withInsertHandler({ context, _ -> context.document.replaceString(context.startOffset, context.tailOffset, "${name.render()} = $value") })
        lookupElement.putUserData(SmartCompletionInBasicWeigher.NAMED_ARGUMENT_KEY, Unit)
        lookupElement.assignSmartCompletionPriority(priority)
        return lookupElement
    }

    private fun calcExpectedInfos(expression: KtExpression): Collection<ExpectedInfo> {
        // if our expression is initializer of implicitly typed variable - take type of variable from original file (+ the same for function)
        val declaration = implicitlyTypedDeclarationFromInitializer(expression)
        if (declaration != null) {
            val originalDeclaration = toFromOriginalFileMapper.toOriginalFile(declaration)
            if (originalDeclaration != null) {
                val originalDescriptor = originalDeclaration.resolveToDescriptorIfAny() as? CallableDescriptor
                val returnType = originalDescriptor?.returnType
                if (returnType != null && !returnType.isError) {
                    return listOf(ExpectedInfo(returnType, declaration.name, null))
                }
            }
        }

        // if expected types are too general, try to use expected type from outer calls
        var count = 0
        while (true) {
            val infos = ExpectedInfos(bindingContext, resolutionFacade, indicesHelper, useOuterCallsExpectedTypeCount = count)
                    .calculate(expression)
            if (count == 2 /* use two outer calls maximum */ || infos.none { it.fuzzyType?.isAlmostEverything() ?: false }) {
                return if (forBasicCompletion)
                    infos.map { it.copy(tail = null) }
                else
                    infos
            }
            count++
        }
        //TODO: we could always give higher priority to results with outer call expected type used
    }

    private fun implicitlyTypedDeclarationFromInitializer(expression: KtExpression): KtDeclaration? {
        val parent = expression.parent
        when (parent) {
            is KtVariableDeclaration -> if (expression == parent.initializer && parent.typeReference == null) return parent
            is KtNamedFunction -> if (expression == parent.initializer && parent.typeReference == null) return parent
        }
        return null
    }

    private fun MutableCollection<LookupElement>.addCallableReferenceLookupElements(descriptor: DeclarationDescriptor, lookupElementFactory: AbstractLookupElementFactory) {
        if (callableTypeExpectedInfo.isEmpty()) return

        fun toLookupElement(descriptor: CallableDescriptor): LookupElement? {
            val callableReferenceType = descriptor.callableReferenceType(resolutionFacade, null) ?: return null

            val matchedExpectedInfos = callableTypeExpectedInfo.filter { it.matchingSubstitutor(callableReferenceType) != null }
            if (matchedExpectedInfos.isEmpty()) return null

            var lookupElement = lookupElementFactory.createLookupElement(descriptor, useReceiverTypes = false, parametersAndTypeGrayed = true)
                                ?: return null
            lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = "::" + delegate.lookupString
                override fun getAllLookupStrings() = setOf(lookupString)

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.itemText = "::" + presentation.itemText
                }

                override fun handleInsert(context: InsertionContext) {
                }
            }

            return lookupElement
                    .assignSmartCompletionPriority(SmartCompletionItemPriority.CALLABLE_REFERENCE)
                    .addTailAndNameSimilarity(matchedExpectedInfos)
        }

        when (descriptor) {
            is CallableDescriptor -> {
                // members and extensions are not supported after "::" currently
                if (descriptor.dispatchReceiverParameter == null && descriptor.extensionReceiverParameter == null) {
                    addIfNotNull(toLookupElement(descriptor))
                }
            }

            is ClassDescriptor -> {
                if (descriptor.modality != Modality.ABSTRACT && !descriptor.isInner) {
                    descriptor.constructors
                            .filter(visibilityFilter)
                            .mapNotNullTo(this, ::toLookupElement)
                }
            }
        }
    }

    private fun buildForAsTypePosition(lookupElementFactory: BasicLookupElementFactory): Collection<LookupElement>? {
        val binaryExpression = ((expression.parent as? KtUserType)
                ?.parent as? KtTypeReference)
                    ?.parent as? KtBinaryExpressionWithTypeRHS
                        ?: return null
        val elementType = binaryExpression.operationReference.getReferencedNameElementType()
        if (elementType != KtTokens.AS_KEYWORD && elementType != KtTokens.AS_SAFE) return null
        val expectedInfos = calcExpectedInfos(binaryExpression)

        val expectedInfosGrouped: Map<KotlinType?, List<ExpectedInfo>> = expectedInfos.groupBy { it.fuzzyType?.type?.makeNotNullable() }

        val items = ArrayList<LookupElement>()
        for ((type, infos) in expectedInfosGrouped) {
            if (type == null) continue
            val lookupElement = lookupElementFactory.createLookupElementForType(type) ?: continue
            items.add(lookupElement.addTailAndNameSimilarity(infos))
        }
        return items
    }

    companion object {
        val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")
    }
}
