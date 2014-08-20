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

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.lang.types.*
import com.intellij.codeInsight.lookup.*
import com.intellij.codeInsight.completion.*
import java.util.*
import org.jetbrains.jet.plugin.completion.*
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.plugin.util.makeNullable
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.plugin.caches.resolve.getLazyResolveSession
import org.jetbrains.jet.renderer.DescriptorRenderer

class SmartCompletion(val expression: JetSimpleNameExpression,
                      val resolveSession: ResolveSessionForBodies,
                      val visibilityFilter: (DeclarationDescriptor) -> Boolean,
                      val originalFile: JetFile) {

    private val bindingContext = resolveSession.resolveToElement(expression)
    private val moduleDescriptor = resolveSession.getModuleDescriptor()
    private val project = expression.getProject()

    public fun buildLookupElements(referenceVariants: Iterable<DeclarationDescriptor>): Collection<LookupElement>? {
        return buildLookupElementsInternal(referenceVariants)?.map {
            if (it.getUserData(KEEP_OLD_ARGUMENT_LIST_ON_TAB_KEY) == null) {
                object : LookupElementDecorator<LookupElement>(it) {
                    override fun handleInsert(context: InsertionContext) {
                        if (context.getCompletionChar() == Lookup.REPLACE_SELECT_CHAR) {
                            val offset = context.getOffsetMap().getOffset(SmartCompletion.OLD_ARGUMENTS_REPLACEMENT_OFFSET)
                            if (offset != -1) {
                                context.getDocument().deleteString(context.getTailOffset(), offset)
                            }
                        }

                        super.handleInsert(context)
                    }
                }
            }
            else {
                it
            }
        }
    }

    private fun buildLookupElementsInternal(referenceVariants: Iterable<DeclarationDescriptor>): Collection<LookupElement>? {
        val elements = buildForAsTypePosition()
        if (elements != null) return elements

        val parent = expression.getParent()
        val expressionWithType: JetExpression
        val receiver: JetExpression?
        if (parent is JetQualifiedExpression) {
            expressionWithType = parent
            receiver = parent.getReceiverExpression()
        }
        else {
            expressionWithType = expression
            receiver = null
        }

        val allExpectedInfos = calcExpectedInfos(expressionWithType) ?: return null
        val filteredExpectedInfos = allExpectedInfos.filter { !it.`type`.isError() }
        if (filteredExpectedInfos.isEmpty()) return null

        // if we complete argument of == or !=, make types in expected info's nullable to allow nullable items too
        val expectedInfos = if ((expressionWithType.getParent() as? JetBinaryExpression)?.getOperationToken() in setOf(JetTokens.EQEQ, JetTokens.EXCLEQ))
            filteredExpectedInfos.map { ExpectedInfo(it.`type`.makeNullable(), it.tail) }
        else
            filteredExpectedInfos

        val result = ArrayList<LookupElement>()

        val typesWithAutoCasts: (DeclarationDescriptor) -> Iterable<JetType> = TypesWithAutoCasts(bindingContext).calculate(expressionWithType, receiver)

        val itemsToSkip = calcItemsToSkip(expressionWithType)

        val functionExpectedInfos = expectedInfos.filter { KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(it.`type`) }

        for (descriptor in referenceVariants) {
            if (itemsToSkip.contains(descriptor)) continue

            val types = typesWithAutoCasts(descriptor)
            val nonNullTypes = types.map { it.makeNotNullable() }
            val classifier = { (expectedInfo: ExpectedInfo) ->
                when {
                    types.any { it.isSubtypeOf(expectedInfo.`type`) } -> ExpectedInfoClassification.MATCHES
                    nonNullTypes.any { it.isSubtypeOf(expectedInfo.`type`) } -> ExpectedInfoClassification.MAKE_NOT_NULLABLE
                    else -> ExpectedInfoClassification.NOT_MATCHES
                }
            }
            result.addLookupElements(expectedInfos, classifier, { createLookupElement(descriptor, resolveSession) })

            if (receiver == null) {
                toFunctionReferenceLookupElement(descriptor, functionExpectedInfos)?.let { result.add(it) }
            }
        }

        if (receiver == null) {
            TypeInstantiationItems(resolveSession, visibilityFilter).addToCollection(result, expectedInfos)

            StaticMembers(bindingContext, resolveSession).addToCollection(result, expectedInfos, expression, itemsToSkip)

            ThisItems(bindingContext).addToCollection(result, expressionWithType, expectedInfos)

            LambdaItems.addToCollection(result, functionExpectedInfos)

            KeywordValues.addToCollection(result, filteredExpectedInfos/* use filteredExpectedInfos to not include null after == */, expressionWithType)
        }

        return result
    }

    private fun calcExpectedInfos(expression: JetExpression): Collection<ExpectedInfo>? {
        // if our expression is initializer of implicitly typed variable - take type of variable from original file (+ the same for function)
        val declaration = implicitlyTypedDeclarationFromInitializer(expression)
        if (declaration != null) {
            val offset = declaration.getTextRange()!!.getStartOffset()
            val originalDeclaration = PsiTreeUtil.findElementOfClassAtOffset(originalFile, offset, javaClass<JetDeclaration>(), true)
            if (originalDeclaration != null) {
                val originalDescriptor = originalDeclaration.getLazyResolveSession().resolveToDescriptor(originalDeclaration) as? CallableDescriptor
                val returnType = originalDescriptor?.getReturnType()
                return if (returnType != null) listOf(ExpectedInfo(returnType, null)) else null
            }
        }

        return ExpectedInfos(bindingContext, moduleDescriptor).calculate(expression)
    }

    private fun implicitlyTypedDeclarationFromInitializer(expression: JetExpression): JetDeclaration? {
        val parent = expression.getParent()
        when (parent) {
            is JetVariableDeclaration -> if (expression == parent.getInitializer() && parent.getTypeRef() == null) return parent
            is JetNamedFunction -> if (expression == parent.getInitializer() && parent.getReturnTypeRef() == null) return parent
        }
        return null
    }

    private fun calcItemsToSkip(expression: JetExpression): Set<DeclarationDescriptor> {
        val parent = expression.getParent()
        when(parent) {
            is JetProperty -> {
                //TODO: this can be filtered out by ordinary completion
                if (expression == parent.getInitializer()) {
                    return resolveSession.resolveToElement(parent)[BindingContext.DECLARATION_TO_DESCRIPTOR, parent].toSet()
                }
            }

            is JetBinaryExpression -> {
                if (parent.getRight() == expression) {
                    val operationToken = parent.getOperationToken()
                    if (operationToken == JetTokens.EQ || operationToken == JetTokens.EQEQ || operationToken == JetTokens.EXCLEQ) {
                        val left = parent.getLeft()
                        if (left is JetReferenceExpression) {
                            return resolveSession.resolveToElement(left)[BindingContext.REFERENCE_TARGET, left].toSet()
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
                            .filterIsInstance(javaClass<JetWhenConditionWithExpression>())
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
            val functionType = functionType(descriptor)
            if (functionType == null) return null

            val matchedExpectedInfos = functionExpectedInfos.filter { functionType.isSubtypeOf(it.`type`) }
            if (matchedExpectedInfos.isEmpty()) return null

            var lookupElement = createLookupElement(descriptor, resolveSession)
            val text = "::" + (if (descriptor is ConstructorDescriptor) descriptor.getContainingDeclaration().getName() else descriptor.getName())
            lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
                override fun getLookupString() = text

                override fun renderElement(presentation: LookupElementPresentation) {
                    super.renderElement(presentation)
                    presentation.setItemText(text)
                    presentation.setTypeText("")
                }

                override fun handleInsert(context: InsertionContext) {
                }
            }

            return lookupElement.addTail(matchedExpectedInfos)
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

    private fun buildForAsTypePosition(): Collection<LookupElement>? {
        val binaryExpression = ((expression.getParent() as? JetUserType)
                ?.getParent() as? JetTypeReference)
                    ?.getParent() as? JetBinaryExpressionWithTypeRHS
        if (binaryExpression != null) {
            val elementType = binaryExpression.getOperationReference().getReferencedNameElementType()
            if (elementType == JetTokens.AS_KEYWORD || elementType == JetTokens.AS_SAFE) {
                val expectedInfos = calcExpectedInfos(binaryExpression) ?: return null

                val expectedInfosGrouped: Map<JetType, List<ExpectedInfo>> = expectedInfos.groupBy { it.`type`.makeNotNullable() }

                val result = ArrayList<LookupElement>()
                for ((jetType, infos) in expectedInfosGrouped) {
                    val lookupElement = lookupElementForType(jetType) ?: continue
                    result.add(lookupElement.addTail(infos))
                }
                return result
            }
        }
        return null
    }

    private fun lookupElementForType(jetType: JetType): LookupElement? {
        if (jetType.isError()) return null
        val classifier = jetType.getConstructor().getDeclarationDescriptor() ?: return null

        val lookupElement = createLookupElement(classifier, resolveSession)
        val lookupString = lookupElement.getLookupString()

        val typeArgs = jetType.getArguments()
        var itemText = lookupString + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderTypeArguments(typeArgs)
        val typeText = DescriptorUtils.getFqName(classifier).toString() + DescriptorRenderer.SOURCE_CODE.renderTypeArguments(typeArgs)

        val insertHandler: InsertHandler<LookupElement> = object : InsertHandler<LookupElement> {
            override fun handleInsert(context: InsertionContext, item: LookupElement) {
                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), typeText)
                context.setTailOffset(context.getStartOffset() + typeText.length)
                shortenReferences(context, context.getStartOffset(), context.getTailOffset())
            }
        }

        return object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

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
    }
}
