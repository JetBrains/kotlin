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
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.completion.*
import org.jetbrains.kotlin.idea.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.kotlin.idea.core.ExpectedInfo
import org.jetbrains.kotlin.idea.core.Tail
import org.jetbrains.kotlin.idea.core.multipleFuzzyTypes
import org.jetbrains.kotlin.idea.core.overrideImplement.ImplementMembersHandler
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.SamConstructorDescriptor
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

class TypeInstantiationItems(
        val resolutionFacade: ResolutionFacade,
        val bindingContext: BindingContext,
        val visibilityFilter: (DeclarationDescriptor) -> Boolean,
        val toFromOriginalFileMapper: ToFromOriginalFileMapper,
        val inheritorSearchScope: GlobalSearchScope,
        val lookupElementFactory: LookupElementFactory,
        val forOrdinaryCompletion: Boolean
) {
    fun addTo(
            items: MutableCollection<LookupElement>,
            inheritanceSearchers: MutableCollection<InheritanceItemsSearcher>,
            expectedInfos: Collection<ExpectedInfo>
    ) {
        val expectedInfosGrouped = LinkedHashMap<FuzzyType, MutableList<ExpectedInfo>>()
        for (expectedInfo in expectedInfos) {
            for (fuzzyType in expectedInfo.multipleFuzzyTypes) {
                expectedInfosGrouped.getOrPut(fuzzyType.makeNotNullable()) { ArrayList() }.add(expectedInfo)
            }
        }

        for ((type, infos) in expectedInfosGrouped) {
            val tail = mergeTails(infos.map { it.tail })
            addTo(items, inheritanceSearchers, type, tail)
        }
    }

    private fun addTo(
            items: MutableCollection<LookupElement>,
            inheritanceSearchers: MutableCollection<InheritanceItemsSearcher>,
            fuzzyType: FuzzyType,
            tail: Tail?
    ) {
        if (fuzzyType.type.isFunctionType) return // do not show "object: ..." for function types

        val classifier = fuzzyType.type.constructor.declarationDescriptor
        if (classifier !is ClassDescriptor) return

        addSamConstructorItem(items, classifier, tail)

        items.addIfNotNull(createTypeInstantiationItem(fuzzyType, tail))

        if (!forOrdinaryCompletion && !KotlinBuiltIns.isAny(classifier)) { // do not search inheritors of Any
            val typeArgs = fuzzyType.type.arguments
            inheritanceSearchers.addInheritorSearcher(classifier, classifier, typeArgs, fuzzyType.freeParameters, tail)

            val javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(DescriptorUtils.getFqName(classifier))
            if (javaClassId != null) {
                val javaAnalog = resolutionFacade.moduleDescriptor.resolveTopLevelClass(javaClassId.asSingleFqName(), NoLookupLocation.FROM_IDE)
                if (javaAnalog != null) {
                    inheritanceSearchers.addInheritorSearcher(javaAnalog, classifier, typeArgs, fuzzyType.freeParameters, tail)
                }
            }
        }
    }

    private fun MutableCollection<InheritanceItemsSearcher>.addInheritorSearcher(
            descriptor: ClassDescriptor, kotlinClassDescriptor: ClassDescriptor, typeArgs: List<TypeProjection>, freeParameters: Collection<TypeParameterDescriptor>, tail: Tail?
    ) {
        val _declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(resolutionFacade.project, descriptor) ?: return
        val declaration = if (_declaration is KtDeclaration)
            toFromOriginalFileMapper.toOriginalFile(_declaration) ?: return
        else
            _declaration

        val psiClass: PsiClass = when (declaration) {
            is PsiClass -> declaration
            is KtClassOrObject -> declaration.toLightClass() ?: return
            else -> return
        }
        add(InheritanceSearcher(psiClass, kotlinClassDescriptor, typeArgs, freeParameters, tail))
    }

    private fun createTypeInstantiationItem(fuzzyType: FuzzyType, tail: Tail?): LookupElement? {
        val classifier = fuzzyType.type.constructor.declarationDescriptor as? ClassDescriptor ?: return null

        var lookupElement = lookupElementFactory.createLookupElement(classifier, useReceiverTypes = false)

        if (DescriptorUtils.isNonCompanionObject(classifier)) {
            return lookupElement.addTail(tail)
        }

        // not all inner classes can be instantiated and we handle them via constructors returned by ReferenceVariantsHelper
        if (classifier.isInner) return null

        val isAbstract = classifier.modality == Modality.ABSTRACT
        if (forOrdinaryCompletion && isAbstract) return null

        val allConstructors = classifier.constructors
        val visibleConstructors = allConstructors.filter {
            if (isAbstract)
                visibilityFilter(it) || it.visibility == Visibilities.PROTECTED
            else
                visibilityFilter(it)
        }
        if (allConstructors.isNotEmpty() && visibleConstructors.isEmpty()) return null

        var lookupString = lookupElement.lookupString
        var allLookupStrings = setOf(lookupString)
        var itemText = lookupString
        var signatureText: String? = null
        var typeText = IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier)

        val insertHandler: InsertHandler<LookupElement>
        if (isAbstract) {
            val typeArgs = fuzzyType.type.arguments
            // drop "in" and "out" from type arguments - they cannot be used in constructor call
            val typeArgsToUse = typeArgs.map { TypeProjectionImpl(Variance.INVARIANT, it.type) }

            val allTypeArgsKnown = fuzzyType.freeParameters.isEmpty() || typeArgs.none { it.type.areTypeParametersUsedInside(fuzzyType.freeParameters) }
            if (allTypeArgsKnown) {
                itemText += IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderTypeArguments(typeArgsToUse)
            }
            else {
                itemText += "<...>"
            }

            val constructorParenthesis = if (classifier.kind != ClassKind.INTERFACE) "()" else ""
            itemText += constructorParenthesis
            itemText = "object: $itemText{...}"
            lookupString = "object"
            allLookupStrings = setOf(lookupString, lookupElement.lookupString)
            insertHandler = InsertHandler<LookupElement> { context, item ->
                val startOffset = context.startOffset

                val text1 = "object: $typeText"
                val text2 = "$constructorParenthesis {}"
                val text = if (allTypeArgsKnown)
                    text1 + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(typeArgsToUse) + text2
                else
                    text1 + "<>" + text2

                context.document.replaceString(startOffset, context.tailOffset, text)

                if (allTypeArgsKnown) {
                    context.editor.caretModel.moveToOffset(startOffset + text.length - 1)

                    shortenReferences(context, startOffset, startOffset + text.length)

                    ImplementMembersHandler().invoke(context.project, context.editor, context.file, true)
                }
                else {
                    context.editor.caretModel.moveToOffset(startOffset + text1.length + 1) // put caret into "<>"

                    shortenReferences(context, startOffset, startOffset + text.length)
                }
            }
            lookupElement = lookupElement.suppressAutoInsertion()
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.ANONYMOUS_OBJECT)
        }
        else {
            //TODO: when constructor has one parameter of lambda type with more than one parameter, generate special additional item
            signatureText = when (visibleConstructors.size) {
                0 -> "()"

                1 -> {
                    val constructor = visibleConstructors.single()
                    val substitutor = TypeSubstitutor.create(fuzzyType.presentationType())
                    val substitutedConstructor = constructor.substitute(substitutor)
                    DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderFunctionParameters(substitutedConstructor)
                }

                else -> "(...)"
            }

            val baseInsertHandler = when (visibleConstructors.size) {
                0 -> KotlinFunctionInsertHandler.Normal(inputTypeArguments = false, inputValueArguments = false, argumentsOnly = true)

                1 -> (lookupElementFactory.insertHandlerProvider.insertHandler(visibleConstructors.single()) as KotlinFunctionInsertHandler.Normal)
                        .copy(argumentsOnly = true)

                else -> KotlinFunctionInsertHandler.Normal(inputTypeArguments = false, inputValueArguments = true, argumentsOnly = true)
            }

            insertHandler = object : InsertHandler<LookupElement> {
                override fun handleInsert(context: InsertionContext, item: LookupElement) {
                    context.document.replaceString(context.startOffset, context.tailOffset, typeText)
                    context.tailOffset = context.startOffset + typeText.length

                    baseInsertHandler.handleInsert(context, item)

                    shortenReferences(context, context.startOffset, context.tailOffset)
                }
            }
            if (baseInsertHandler.inputValueArguments) {
                lookupElement = lookupElement.keepOldArgumentListOnTab()
            }
            if (baseInsertHandler.lambdaInfo != null) {
                lookupElement.putUserData(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, Unit)
            }
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION)
        }

        class InstantiationLookupElement : LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

            override fun getAllLookupStrings() = allLookupStrings

            override fun renderElement(presentation: LookupElementPresentation) {
                delegate.renderElement(presentation)
                presentation.itemText = itemText

                presentation.clearTail()
                if (signatureText != null) {
                    presentation.appendTailText(signatureText!!, false)
                }
                presentation.appendTailText(" (" + DescriptorUtils.getFqName(classifier.containingDeclaration) + ")", true)
            }

            override fun handleInsert(context: InsertionContext) {
                insertHandler.handleInsert(context, delegate)
            }

            override fun equals(other: Any?): Boolean {
                if (other === this) return true
                if (other !is InstantiationLookupElement) return false
                if (getLookupString() != other.lookupString) return false
                val presentation1 = LookupElementPresentation()
                val presentation2 = LookupElementPresentation()
                renderElement(presentation1)
                other.renderElement(presentation2)
                return presentation1.itemText == presentation2.itemText && presentation1.tailText == presentation2.tailText
            }

            override fun hashCode() = lookupString.hashCode()
        }

        return InstantiationLookupElement().addTail(tail)
    }

    private fun KotlinType.areTypeParametersUsedInside(freeParameters: Collection<TypeParameterDescriptor>): Boolean {
        return FuzzyType(this, freeParameters).freeParameters.isNotEmpty()
    }

    private fun addSamConstructorItem(collection: MutableCollection<LookupElement>, `class`: ClassDescriptor, tail: Tail?) {
        if (`class`.kind == ClassKind.INTERFACE) {
            val container = `class`.containingDeclaration
            val scope = when (container) {
                is PackageFragmentDescriptor -> container.getMemberScope()
                is ClassDescriptor -> container.staticScope
                else -> return
            }
            val samConstructor = scope.getContributedFunctions(`class`.name, NoLookupLocation.FROM_IDE)
                                         .filterIsInstance<SamConstructorDescriptor>()
                                         .singleOrNull() ?: return
            lookupElementFactory.createStandardLookupElementsForDescriptor(samConstructor, useReceiverTypes = false)
                    .mapTo(collection) {
                        it.assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION).addTail(tail)
                    }
        }
    }

    private inner class InheritanceSearcher(
            private val psiClass: PsiClass,
            classDescriptor: ClassDescriptor,
            typeArgs: List<TypeProjection>,
            private val freeParameters: Collection<TypeParameterDescriptor>,
            private val tail: Tail?) : InheritanceItemsSearcher {

        private val baseHasTypeArgs = classDescriptor.declaredTypeParameters.isNotEmpty()
        private val expectedType = KotlinTypeImpl.create(Annotations.EMPTY, classDescriptor, false, typeArgs)
        private val expectedFuzzyType = expectedType.toFuzzyType(freeParameters)

        override fun search(nameFilter: (String) -> Boolean, consumer: (LookupElement) -> Unit) {
            val parameters = ClassInheritorsSearch.SearchParameters(psiClass, inheritorSearchScope, true, true, false, nameFilter)
            for (inheritor in ClassInheritorsSearch.search(parameters)) {
                val descriptor = inheritor.resolveToDescriptor(
                        resolutionFacade,
                        { toFromOriginalFileMapper.toSyntheticFile(it) }
                ) ?: continue
                if (!visibilityFilter(descriptor)) continue

                var inheritorFuzzyType = descriptor.defaultType.toFuzzyType(descriptor.typeConstructor.parameters)
                val hasTypeArgs = descriptor.declaredTypeParameters.isNotEmpty()
                if (hasTypeArgs || baseHasTypeArgs) {
                    val substitutor = inheritorFuzzyType.checkIsSubtypeOf(expectedFuzzyType) ?: continue
                    if (!substitutor.isEmpty) {
                        val inheritorTypeSubstituted = substitutor.substitute(inheritorFuzzyType.type, Variance.INVARIANT)!!
                        inheritorFuzzyType = inheritorTypeSubstituted.toFuzzyType(freeParameters + inheritorFuzzyType.freeParameters)
                    }
                }

                val lookupElement = createTypeInstantiationItem(inheritorFuzzyType, tail) ?: continue
                consumer(lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.INHERITOR_INSTANTIATION))
            }
        }
    }
}
