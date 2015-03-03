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

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.types.*
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.completion.*
import java.util.*
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.util.makeNotNullable
import org.jetbrains.kotlin.idea.util.makeNullable
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf
import org.jetbrains.kotlin.idea.util.FuzzyType
import org.jetbrains.kotlin.idea.util.fuzzyReturnType
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.idea.util.IterableTypesDetector
import org.jetbrains.kotlin.idea.util.nullability
import org.jetbrains.kotlin.idea.util.TypeNullability
import org.jetbrains.kotlin.idea.util.SmartCastCalculator

trait InheritanceItemsSearcher {
    fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit)
}

class SmartCompletion(
        val expression: JetExpression,
        val resolutionFacade: ResolutionFacade,
        val moduleDescriptor: ModuleDescriptor,
        val bindingContext: BindingContext,
        val visibilityFilter: (DeclarationDescriptor) -> Boolean,
        val prefixMatcher: PrefixMatcher,
        val inheritorSearchScope: GlobalSearchScope,
        val toFromOriginalFileMapper: ToFromOriginalFileMapper,
        val lookupElementFactory: LookupElementFactory
) {
    private val project = expression.getProject()

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

        val receiver = if (expression is JetSimpleNameExpression) expression.getReceiverExpression() else null
        val expressionWithType = if (receiver != null) {
            expression.getParent() as? JetExpression ?: return null
        }
        else {
            expression
        }

        val loopRangePositionResult = buildForLoopRangePosition(expressionWithType, receiver)
        if (loopRangePositionResult != null) return loopRangePositionResult

        val inOperatorArgumentResult = buildForInOperatorArgument(expressionWithType, receiver)
        if (inOperatorArgumentResult != null) return inOperatorArgumentResult

        val allExpectedInfos = calcExpectedInfos(expressionWithType) ?: return null
        val filteredExpectedInfos = allExpectedInfos.filter { !it.type.isError() }
        if (filteredExpectedInfos.isEmpty()) return null

        // if we complete argument of == or !=, make types in expected info's nullable to allow nullable items too
        val expectedInfos = if ((expressionWithType.getParent() as? JetBinaryExpression)?.getOperationToken() in COMPARISON_TOKENS)
            filteredExpectedInfos.map { ExpectedInfo(it.type.makeNullable(), it.name, it.tail) }
        else
            filteredExpectedInfos

        val smartCastTypes: (VariableDescriptor) -> Collection<JetType> = SmartCastCalculator(bindingContext).calculate(expressionWithType, receiver)

        val itemsToSkip = calcItemsToSkip(expressionWithType)

        val functionExpectedInfos = expectedInfos.filter { KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(it.type) }

        fun filterDeclaration(descriptor: DeclarationDescriptor): Collection<LookupElement> {
            if (descriptor in itemsToSkip) return listOf()

            val result = ArrayList<LookupElement>()
            val types = descriptor.fuzzyTypes(smartCastTypes)
            val classifier = { (expectedInfo: ExpectedInfo) -> types.classifyExpectedInfo(expectedInfo) }
            result.addLookupElements(descriptor, expectedInfos, classifier) { descriptor ->
                lookupElementFactory.createLookupElement(descriptor, resolutionFacade, bindingContext, true)
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
                StaticMembers(bindingContext, resolutionFacade, lookupElementFactory).addToCollection(additionalItems, expectedInfos, expression, itemsToSkip)
            }

            additionalItems.addThisItems(expression, expectedInfos)

            LambdaItems.addToCollection(additionalItems, functionExpectedInfos)

            KeywordValues.addToCollection(additionalItems, filteredExpectedInfos/* use filteredExpectedInfos to not include null after == */, expression)

            MultipleArgumentsItemProvider(bindingContext, smartCastTypes).addToCollection(additionalItems, expectedInfos, expression)
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

    private fun MutableCollection<LookupElement>.addThisItems(place: JetExpression, expectedInfos: Collection<ExpectedInfo>) {
        if (shouldCompleteThisItems(prefixMatcher)) {
            val items = thisExpressionItems(bindingContext, place)
            for ((factory, type) in items) {
                val classifier = { (expectedInfo: ExpectedInfo) -> type.classifyExpectedInfo(expectedInfo) }
                addLookupElements(null, expectedInfos, classifier) {
                    factory().assignSmartCompletionPriority(SmartCompletionItemPriority.THIS)
                }
            }
        }
    }

    private fun DeclarationDescriptor.fuzzyTypes(smartCastTypes: (VariableDescriptor) -> Collection<JetType>): Collection<FuzzyType> {
        if (this is CallableDescriptor) {
            var returnType = fuzzyReturnType() ?: return listOf()
            //TODO: maybe we should include them on the second press?
            if (shouldSkipDeclarationsOfType(returnType)) return listOf()

            if (this is VariableDescriptor) {
                return smartCastTypes(this).map { FuzzyType(it, listOf()) }
            }
            else {
                return listOf(fuzzyReturnType()!!)
            }
        }
        else if (this is ClassDescriptor && getKind().isSingleton()) {
            return listOf(FuzzyType(getDefaultType(), listOf()))
        }
        else {
            return listOf()
        }
    }

    // skip declarations of type Nothing or of generic parameter type which has no real bounds
    private fun shouldSkipDeclarationsOfType(type: FuzzyType): Boolean {
        if (KotlinBuiltIns.isNothing(type.type)) return true
        if (type.freeParameters.isEmpty()) return false
        val typeParameter = type.type.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor ?: return false
        if (!type.freeParameters.contains(typeParameter)) return false
        return KotlinBuiltIns.isAnyOrNullableAny(typeParameter.getUpperBoundsAsType())
        //TODO: check for default object constraint when they are supported
    }

    private fun calcExpectedInfos(expression: JetExpression): Collection<ExpectedInfo>? {
        // if our expression is initializer of implicitly typed variable - take type of variable from original file (+ the same for function)
        val declaration = implicitlyTypedDeclarationFromInitializer(expression)
        if (declaration != null) {
            val originalDeclaration = toFromOriginalFileMapper.toOriginalFile(declaration)
            if (originalDeclaration != null) {
                val originalDescriptor = originalDeclaration.resolveToDescriptor() as? CallableDescriptor
                val returnType = originalDescriptor?.getReturnType()
                return if (returnType != null) listOf(ExpectedInfo(returnType, declaration.getName(), null)) else null
            }
        }

        return ExpectedInfos(bindingContext, resolutionFacade, moduleDescriptor, true).calculate(expression)
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
            is JetProperty -> {
                //TODO: this can be filtered out by ordinary completion
                if (expression == parent.getInitializer()) {
                    return bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, parent].toSet()
                }
            }

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

                val subjectType = bindingContext[BindingContext.EXPRESSION_TYPE, subject] ?: return setOf()
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

            val matchedExpectedInfos = functionExpectedInfos.filter { functionType.isSubtypeOf(it.type) }
            if (matchedExpectedInfos.isEmpty()) return null

            var lookupElement = lookupElementFactory.createLookupElement(descriptor, resolutionFacade, bindingContext, true)
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
            if (constructors.size == 1) {
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

        val expectedInfosGrouped: Map<JetType, List<ExpectedInfo>> = expectedInfos.groupBy { it.type.makeNotNullable() }

        val items = ArrayList<LookupElement>()
        for ((jetType, infos) in expectedInfosGrouped) {
            val lookupElement = lookupElementForType(jetType) ?: continue
            items.add(lookupElement.addTailAndNameSimilarity(infos))
        }
        return Result(null, items, null)
    }

    private fun buildForLoopRangePosition(expressionWithType: JetExpression, receiver: JetExpression?): Result? {
        val forExpression = (expressionWithType.getParent() as? JetContainerNode)
                                    ?.getParent() as? JetForExpression ?: return null
        if (expressionWithType != forExpression.getLoopRange()) return null

        val loopVar = forExpression.getLoopParameter()
        val loopVarType = if (loopVar != null && loopVar.getTypeReference() != null) {
            val type = (resolutionFacade.resolveToDescriptor(loopVar) as VariableDescriptor).getType()
            if (type.isError()) null else type
        }
        else {
            null
        }

        val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expressionWithType)
        val iterableDetector = IterableTypesDetector(project, moduleDescriptor, scope, loopVarType)

        return buildResultByTypeFilter(expressionWithType, receiver, Tail.RPARENTH) { iterableDetector.isIterable(it) }
    }

    private fun buildForInOperatorArgument(expressionWithType: JetExpression, receiver: JetExpression?): Result? {
        val binaryExpression = expressionWithType.getParent() as? JetBinaryExpression ?: return null
        val operationToken = binaryExpression.getOperationToken()
        if (operationToken != JetTokens.IN_KEYWORD && operationToken != JetTokens.NOT_IN || expressionWithType != binaryExpression.getRight()) return null

        val leftOperandType = bindingContext.get(BindingContext.EXPRESSION_TYPE, binaryExpression.getLeft()) ?: return null
        val scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, expressionWithType)
        val detector = TypesWithContainsDetector(scope, leftOperandType, project, moduleDescriptor)

        return buildResultByTypeFilter(expressionWithType, receiver, null) { detector.hasContains(it) }
    }

    private fun buildResultByTypeFilter(
            position: JetExpression,
            receiver: JetExpression?,
            tail: Tail?,
            typeFilter: (FuzzyType) -> Boolean
    ): Result {
        val smartCastTypes: (VariableDescriptor) -> Collection<JetType> = SmartCastCalculator(bindingContext).calculate(position, receiver)

        fun filterDeclaration(descriptor: DeclarationDescriptor): Collection<LookupElement> {
            val types = descriptor.fuzzyTypes(smartCastTypes)

            fun createLookupElement(): LookupElement {
                return lookupElementFactory.createLookupElement(descriptor, resolutionFacade, bindingContext, true)
            }

            if (types.any { typeFilter(it) }) {
                return listOf(createLookupElement().addTail(tail))
            }

            if (types.any { it.nullability() == TypeNullability.NULLABLE && typeFilter(it.makeNotNullable()) }) {
                return lookupElementsForNullable(::createLookupElement).map { it.addTail(tail) }
            }

            return listOf()
        }

        return Result(::filterDeclaration, listOf(), null)
    }

    private fun lookupElementForType(jetType: JetType): LookupElement? {
        if (jetType.isError()) return null
        val classifier = jetType.getConstructor().getDeclarationDescriptor() ?: return null

        val lookupElement = lookupElementFactory.createLookupElement(classifier, resolutionFacade, bindingContext, false)
        val lookupString = lookupElement.getLookupString()

        val typeArgs = jetType.getArguments()
        var itemText = lookupString + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderTypeArguments(typeArgs)
        val typeText = DescriptorUtils.getFqName(classifier).toString() + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(typeArgs)

        val insertHandler: InsertHandler<LookupElement> = object : InsertHandler<LookupElement> {
            override fun handleInsert(context: InsertionContext, item: LookupElement) {
                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), typeText)
                context.setTailOffset(context.getStartOffset() + typeText.length)
                shortenReferences(context, context.getStartOffset(), context.getTailOffset())
            }
        }

        return object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun renderElement(presentation: LookupElementPresentation) {
                getDelegate().renderElement(presentation)
                presentation.setItemText(itemText)
            }

            override fun handleInsert(context: InsertionContext) {
                insertHandler.handleInsert(context, getDelegate())
            }
        }
    }

    class object {
        public val OLD_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("nonFunctionReplacementOffset")
        public val MULTIPLE_ARGUMENTS_REPLACEMENT_OFFSET: OffsetKey = OffsetKey.create("multipleArgumentsReplacementOffset")

        private val COMPARISON_TOKENS = setOf(JetTokens.EQEQ, JetTokens.EXCLEQ, JetTokens.EQEQEQ, JetTokens.EXCLEQEQEQ)
    }
}
