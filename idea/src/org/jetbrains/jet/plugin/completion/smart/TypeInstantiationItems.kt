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

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertHandler
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.descriptors.ClassKind
import org.jetbrains.jet.plugin.codeInsight.ImplementMethodsHandler
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies
import org.jetbrains.jet.plugin.completion.handlers.KotlinFunctionInsertHandler
import org.jetbrains.jet.plugin.completion.*
import org.jetbrains.jet.plugin.completion.handlers.CaretPosition
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.resolve.java.descriptor.SamConstructorDescriptor

class TypeInstantiationItems(val resolveSession: ResolveSessionForBodies, val bindingContext: BindingContext, val visibilityFilter: (DeclarationDescriptor) -> Boolean) {
    public fun addToCollection(collection: MutableCollection<LookupElement>, expectedInfos: Collection<ExpectedInfo>) {
        val expectedInfosGrouped: Map<JetType, List<ExpectedInfo>> = expectedInfos.groupBy { it.type.makeNotNullable() }
        for ((jetType, infos) in expectedInfosGrouped) {
            val tail = mergeTails(infos.map { it.tail })
            addToCollection(collection, jetType, tail)
        }
    }

    private fun addToCollection(collection: MutableCollection<LookupElement>, jetType: JetType, tail: Tail?) {
        if (KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(jetType)) return // do not show "object: ..." for function types

        val classifier = jetType.getConstructor().getDeclarationDescriptor()
        if (classifier !is ClassDescriptor) return

        addSamConstructorItem(collection, classifier, tail)

        val isAbstract = classifier.getModality() == Modality.ABSTRACT
        val allConstructors = classifier.getConstructors()
        val visibleConstructors = allConstructors.filter {
            if (isAbstract)
                visibilityFilter(it) || it.getVisibility() == Visibilities.PROTECTED
            else
                visibilityFilter(it)
        }
        if (allConstructors.isNotEmpty() && visibleConstructors.isEmpty()) return

        var lookupElement = createLookupElement(classifier, resolveSession, bindingContext)

        var lookupString = lookupElement.getLookupString()
        var allLookupStrings = setOf(lookupString)

        val typeArgs = jetType.getArguments()
        var itemText = lookupString + DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderTypeArguments(typeArgs)

        val insertHandler: InsertHandler<LookupElement>
        val typeText = qualifiedNameForSourceCode(classifier) + IdeDescriptorRenderers.SOURCE_CODE.renderTypeArguments(typeArgs)
        if (isAbstract) {
            val constructorParenthesis = if (classifier.getKind() != ClassKind.TRAIT) "()" else ""
            itemText += constructorParenthesis
            itemText = "object: " + itemText + "{...}"
            lookupString = "object"
            allLookupStrings = setOf(lookupString, lookupElement.getLookupString())
            insertHandler = InsertHandler<LookupElement> {(context, item) ->
                val editor = context.getEditor()
                val startOffset = context.getStartOffset()
                val text = "object: $typeText$constructorParenthesis {}"
                editor.getDocument().replaceString(startOffset, context.getTailOffset(), text)
                editor.getCaretModel().moveToOffset(startOffset + text.length - 1)

                shortenReferences(context, startOffset, startOffset + text.length)

                ImplementMethodsHandler().invoke(context.getProject(), editor, context.getFile(), true)
            }
            lookupElement = lookupElement.suppressAutoInsertion()
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.ANONYMOUS_OBJECT)
        }
        else {
            //TODO: when constructor has one parameter of lambda type with more than one parameter, generate special additional item
            itemText += "()"
            val baseInsertHandler =
                    (if (visibleConstructors.size == 0)
                        KotlinFunctionInsertHandler.NO_PARAMETERS_HANDLER
                    else if (visibleConstructors.size == 1)
                        KotlinLookupElementFactory.getDefaultInsertHandler(visibleConstructors.single())
                    else
                        KotlinFunctionInsertHandler.WITH_PARAMETERS_HANDLER) as KotlinFunctionInsertHandler
            insertHandler = object : InsertHandler<LookupElement> {
                override fun handleInsert(context: InsertionContext, item: LookupElement) {
                    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), typeText)
                    context.setTailOffset(context.getStartOffset() + typeText.length)

                    baseInsertHandler.handleInsert(context, item)

                    shortenReferences(context, context.getStartOffset(), context.getTailOffset())
                }
            }
            if (baseInsertHandler.caretPosition == CaretPosition.IN_BRACKETS) {
                lookupElement = lookupElement.keepOldArgumentListOnTab()
            }
            if (baseInsertHandler.lambdaInfo != null) {
                lookupElement.putUserData(KotlinCompletionCharFilter.ACCEPT_OPENING_BRACE, true)
            }
            lookupElement = lookupElement.assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION)
        }

        lookupElement = object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getLookupString() = lookupString

            override fun getAllLookupStrings() = allLookupStrings

            override fun renderElement(presentation: LookupElementPresentation) {
                getDelegate().renderElement(presentation)
                presentation.setItemText(itemText)
            }

            override fun handleInsert(context: InsertionContext) {
                insertHandler.handleInsert(context, getDelegate())
            }
        }

        collection.add(lookupElement.addTail(tail))
    }

    private fun addSamConstructorItem(collection: MutableCollection<LookupElement>, `class`: ClassDescriptor, tail: Tail?) {
        if (`class`.getKind() == ClassKind.TRAIT) {
            val container = `class`.getContainingDeclaration()
            val scope = when (container) {
                is PackageFragmentDescriptor -> container.getMemberScope()
                is ClassDescriptor -> container.getStaticScope()
                else -> return
            }
            val samConstructor = scope.getFunctions(`class`.getName())
                                         .filterIsInstance(javaClass<SamConstructorDescriptor>())
                                         .singleOrNull() ?: return
            val lookupElement = createLookupElement(samConstructor, resolveSession, bindingContext)
                    .assignSmartCompletionPriority(SmartCompletionItemPriority.INSTANTIATION)
                    .addTail(tail)
            collection.add(lookupElement)
        }
    }
}
