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
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.core.SmartCastCalculator
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.isAlmostEverything
import org.jetbrains.kotlin.idea.util.makeNullable
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable
import java.util.ArrayList
import java.util.HashSet

interface InheritanceItemsSearcher {
    fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit)
}

class SmartCompletion(
        val expression: JetExpression,
        val resolutionFacade: ResolutionFacade,
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext,
        val visibilityFilter: (DeclarationDescriptor) -> Boolean,
        val inDescriptor: DeclarationDescriptor,
        val prefixMatcher: PrefixMatcher,
        val inheritorSearchScope: GlobalSearchScope,
        val toFromOriginalFileMapper: ToFromOriginalFileMapper,
        val lookupElementFactory: LookupElementFactory
) {
    private val receiver = if (expression is JetSimpleNameExpression) expression.getReceiverExpression() else null

    public class Result(
            val declarationFilter: ((DeclarationDescriptor) -> Collection<LookupElement>)?,
            val additionalItems: Collection<LookupElement>,
            val inheritanceSearcher: InheritanceItemsSearcher?)

    public fun execute(): Result? {
        fun postProcess(item: LookupElement): LookupElement {
            return if (item.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) == null) {
                object : LookupElementDecorator<LookupElement>(item) {
                    override fun handleInsert(context: InsertionContext) {
                        if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                            val offset = context.getOffsetMap().getOffset(OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                            if (offset != -1) {
                                context.getDocument().deleteString(context.getTailOffset(), offset)
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

        val result = executeInternal() ?: return null
        // TODO: code could be more simple, see KT-5726
        val additionalItems = result.additionalItems.map(::postProcess)
        val inheritanceSearcher = result.inheritanceSearcher?.let {
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    it.search(nameFilter, { consumer(postProcess(it)) })
                }
            }
        }
        val filter = result.declarationFilter
        return if (filter != null)
            Result({ filter(it).map(::postProcess) }, additionalItems, inheritanceSearcher)
        else
            Result(null, additionalItems, inheritanceSearcher)
    }

    private fun executeInternal(): Result? {
        val asTypePositionResult = buildForAsTypePosition()
        if (asTypePositionResult != null) return asTypePositionResult

        val expressionWithType = expression.toExpressionWithType()

        var originalExpectedInfos = calcExpectedInfos(expressionWithType) ?: return null
        originalExpectedInfos = originalExpectedInfos.filterNot { it.fuzzyType?.type?.isError ?: false }

        // if we complete argument of == or !=, make types in expected info's nullable to allow nullable items too
        val expectedInfos = if ((expressionWithType.getParent() as? JetBinaryExpression)?.getOperationToken() in COMPARISON_TOKENS)
            originalExpectedInfos.map { if (it.fuzzyType != null) ExpectedInfo(it.fuzzyType!!.makeNullable(), it.expectedName, it.tail) else it }
        else
            originalExpectedInfos

        val smartCastCalculator = SmartCastCalculator(bindingContext, moduleDescriptor, expression)

        val itemsToSkip = calcItemsToSkip(expressionWithType)

        val functionExpectedInfos = expectedInfos.filter { it.fuzzyType != null && KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(it.fuzzyType!!.type) }

        fun filterDeclaration(descriptor: DeclarationDescriptor): Collection<LookupElement> {
            if (descriptor in itemsToSkip) return emptyList()

            val result = SmartList<LookupElement>()
            val types = descriptor.fuzzyTypesForSmartCompletion(smartCastCalculator)
            val infoClassifier = { expectedInfo: ExpectedInfo -> types.classifyExpectedInfo(expectedInfo) }

            result.addLookupElements(descriptor, expectedInfos, infoClassifier, noNameSimilarityForReturnItself = receiver == null) { descriptor ->
                lookupElementFactory.createLookupElement(descriptor, bindingContext, true)
            }

            if (descriptor is PropertyDescriptor) {
                result.addLookupElements(descriptor, expectedInfos, infoClassifier) { descriptor ->
                    lookupElementFactory.createBackingFieldLookupElement(descriptor, inDescriptor, resolutionFacade)
                }
            }

            if (receiver == null) {
                toFunctionReferenceLookupElement(descriptor, functionExpectedInfos)?.let { result.add(it) }
            }

            return result
        }

        val additionalItems = ArrayList<LookupElement>()
        val inheritanceSearchers = ArrayList<InheritanceItemsSearcher>()
        if (receiver == null) {
            TypeInstantiationItems(resolutionFacade, moduleDescriptor, bindingContext, visibilityFilter, toFromOriginalFileMapper, inheritorSearchScope, lookupElementFactory)
                    .addTo(additionalItems, inheritanceSearchers, expectedInfos)

            if (expression is JetSimpleNameExpression) {
                StaticMembers(bindingContext, lookupElementFactory).addToCollection(additionalItems, expectedInfos, expression, itemsToSkip)
            }

            additionalItems.addThisItems(expression, expectedInfos, smartCastCalculator)

            LambdaItems.addToCollection(additionalItems, functionExpectedInfos)

            KeywordValues.addToCollection(additionalItems, originalExpectedInfos/* use originalExpectedInfos to not include null after == */, expression)

            MultipleArgumentsItemProvider(bindingContext, smartCastCalculator).addToCollection(additionalItems, expectedInfos, expression)
        }

        val inheritanceSearcher = if (inheritanceSearchers.isNotEmpty())
            object : InheritanceItemsSearcher {
                override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
                    inheritanceSearchers.forEach { it.search(nameFilter, consumer) }
                }
            }
        else
            null
        return Result(::filterDeclaration, additionalItems, inheritanceSearcher)
    }

    private fun MutableCollection<LookupElement>.addThisItems(place: JetExpression, expectedInfos: Collection<ExpectedInfo>, smartCastCalculator: SmartCastCalculator) {
        if (shouldCompleteThisItems(prefixMatcher)) {
            val items = thisExpressionItems(bindingContext, place, prefixMatcher.getPrefix())
            for (item in items) {
                val types = smartCastCalculator.types(item.receiverParameter).map { FuzzyType(it, emptyList()) }
                val classifier = { expectedInfo: ExpectedInfo -> types.classifyExpectedInfo(expectedInfo) }
                addLookupElements(null, expectedInfos, classifier) {
                    item.createLookupElement().assignSmartCompletionPriority(SmartCompletionItemPriority.THIS)
                }
            }
        }
    }

    private fun calcExpectedInfos(expression: JetExpression): Collection<ExpectedInfo>? {
        // if our expression is initializer of implicitly typed variable - take type of variable from original file (+ the same for function)
        val declaration = implicitlyTypedDeclarationFromInitializer(expression)
        if (declaration != null) {
            val originalDeclaration = toFromOriginalFileMapper.toOriginalFile(declaration)
            if (originalDeclaration != null) {
                val originalDescriptor = originalDeclaration.resolveToDescriptor() as? CallableDescriptor
                val returnType = originalDescriptor?.getReturnType()
                if (returnType != null && !returnType.isError) {
                    return listOf(ExpectedInfo(returnType, declaration.getName(), null))
                }
            }
        }

        // if expected types are too general, try to use expected type from outer calls
        var count = 0
        while (true) {
            val infos = ExpectedInfos(bindingContext, resolutionFacade, moduleDescriptor, useOuterCallsExpectedTypeCount = count)
                    .calculate(expression) ?: return null
            if (count == 2 /* use two outer calls maximum */ || infos.none { it.fuzzyType?.isAlmostEverything() ?: false }) return infos
            count++
        }
        //TODO: we could always give higher priority to results with outer call expected type used
    }

    private fun implicitlyTypedDeclarationFromInitializer(expression: JetExpression): JetDeclaration? {
        val parent = expression.getParent()
        when (parent) {
            is JetVariableDeclaration -> if (expression == parent.getInitializer() && parent.getTypeReference() == null) return parent
            is JetNamedFunction -> if (expression == parent.getInitializer() && parent.getTypeReference() == null) return parent
        }
        return null
    }

    private fun calcItemsToSkip(expression: JetExpression): Set<DeclarationDescriptor> {
        val parent = expression.getParent()
        when(parent) {
            is JetBinaryExpression -> {
                if (parent.getRight() == expression) {
                    val operationToken = parent.getOperationToken()
                    if (operationToken == JetTokens.EQ || operationToken in COMPARISON_TOKENS) {
                        val left = parent.getLeft()
                        if (left is JetReferenceExpression) {
                            return bindingContext[BindingContext.REFERENCE_TARGET, left].toSet()
                        }
                    }
                }
            }

            is JetWhenConditionWithExpression -> {
                val entry = parent.getParent() as JetWhenEntry
                val whenExpression = entry.getParent() as JetWhenExpression
                val subject = whenExpression.getSubjectExpression() ?: return setOf()

                val itemsToSkip = HashSet<DeclarationDescriptor>()

                if (subject is JetSimpleNameExpression) {
                    val variable = bindingContext[BindingContext.REFERENCE_TARGET, subject] as? VariableDescriptor
                    if (variable != null) {
                        itemsToSkip.add(variable)
                    }
                }

                val subjectType = bindingContext.getType(subject) ?: return setOf()
                val classDescriptor = TypeUtils.getClassDescriptor(subjectType)
                if (classDescriptor != null && DescriptorUtils.isEnumClass(classDescriptor)) {
                    val conditions = whenExpression.getEntries()
                            .flatMap { it.getConditions().toList() }
                            .filterIsInstance<JetWhenConditionWithExpression>()
                    for (condition in conditions) {
                        val selectorExpr = (condition.getExpression() as? JetDotQualifiedExpression)
                                ?.getSelectorExpression() as? JetReferenceExpression ?: continue
                        val target = bindingContext[BindingContext.REFERENCE_TARGET, selectorExpr] as? ClassDescriptor ?: continue
                        if (DescriptorUtils.isEnumEntry(target)) {
                            itemsToSkip.add(target)
                        }
                    }
                }

                return itemsToSkip
            }
        }
        return setOf()
    }

    private fun toFunctionReferenceLookupElement(descriptor: DeclarationDescriptor,
                                                 functionExpectedInfos: Collection<ExpectedInfo>): LookupElement? {
        if (functionExpectedInfos.isEmpty()) return null

        fun toLookupElement(descriptor: FunctionDescriptor): LookupElement? {
            val functionType = functionType(descriptor) ?: return null

            val matchedExpectedInfos = functionExpectedInfos.filter { it.matchingSubstitutor(functionType) != null }
            if (matchedExpectedInfos.isEmpty()) return null

            var lookupElement = lookupElementFactory.createLookupElement(descriptor, bindingContext, true)
            val text = "::" + (if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration().getName() else descriptor.getName())
            lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = text
                override fun getAllLookupStrings() = setOf(text)

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.setItemText(text)
                    presentation.clearTail()
                    presentation.setTypeText(null)
                }

                override fun handleInsert(context: InsertionContext) {
                }
            }

            return lookupElement
                    .assignSmartCompletionPriority(SmartCompletionItemPriority.FUNCTION_REFERENCE)
                    .addTailAndNameSimilarity(matchedExpectedInfos)
        }

        if (descriptor is SimpleFunctionDescriptor) {
            return toLookupElement(descriptor)
        }
        else if (descriptor is ClassDescriptor && descriptor.getModality() != Modality.ABSTRACT) {
            val constructors = descriptor.getConstructors().filter(visibilityFilter)
            if (constructors.size() == 1) {
                //TODO: this code is to be changed if overloads to start work after ::
                return toLookupElement(constructors.single())
            }
        }

        return null
    }

    private fun buildForAsTypePosition(): Result? {
        val binaryExpression = ((expression.getParent() as? JetUserType)
                ?.getParent() as? JetTypeReference)
                    ?.getParent() as? JetBinaryExpressionWithTypeRHS
                        ?: return null
        val elementType = binaryExpression.getOperationReference().getReferencedNameElementType()
        if (elementType != JetTokens.AS_KEYWORD && elementType != JetTokens.AS_SAFE) return null
        val expectedInfos = calcExpectedInfos(binaryExpression) ?: return null

        val expectedInfosGrouped: Map<JetType?, List<ExpectedInfo>> = expectedInfos.groupBy { it.fuzzyType?.type?.makeNotNullable() }

        val items = ArrayList<LookupElement>()
        for ((type, infos) in expectedInfosGrouped) {
            if (type == null) continue
            val lookupElement = lookupElementFactory.createLookupElementForType(type) ?: continue
            items.add(lookupElement.addTailAndNameSimilarity(infos))
        }
        return Result(null, items, null)
    }

    companion object {
        public val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        public val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")

        private val COMPARISON_TOKENS = setOf(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ)
    }
}
