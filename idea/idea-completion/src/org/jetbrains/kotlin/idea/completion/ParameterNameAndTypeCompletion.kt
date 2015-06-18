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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionInitializationContext
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.ClassDescriptorWithResolutionScopes
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.core.refactoring.EmptyValidator
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.JetType

class ParameterNameAndTypeCompletion(
        private val collector: LookupElementsCollector,
        private val lookupElementFactory: LookupElementFactory,
        private val prefixMatcher: PrefixMatcher,
        private val resolutionFacade: ResolutionFacade
) {
    private val modifiedPrefixMatcher = prefixMatcher.cloneWithPrefix(prefixMatcher.getPrefix().capitalize())

    public fun addFromImportedClasses(position: PsiElement, bindingContext: BindingContext, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        if (prefixMatcher.getPrefix().isEmpty()) return

        val resolutionScope = position.getResolutionScope(bindingContext)
        val classifiers = resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, modifiedPrefixMatcher.asNameFilter())

        for (classifier in classifiers) {
            if (visibilityFilter(classifier)) {
                addSuggestionsForClassifier(classifier)
            }
        }
    }

    private fun PsiElement.getResolutionScope(bindingContext: BindingContext): JetScope {
        for (parent in parentsWithSelf) {
            if (parent is JetExpression) {
                val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, parent]
                if (scope != null) return scope
            }

            if (parent is JetClassOrObject) {
                val classDescriptor = bindingContext[BindingContext.CLASS, parent] as? ClassDescriptorWithResolutionScopes
                if (classDescriptor != null) {
                    return classDescriptor.getScopeForMemberDeclarationResolution()
                }
            }

            if (parent is JetFile) {
                return resolutionFacade.getFileTopLevelScope(parent)
            }
        }
        error("Not in JetFile")
    }

    public fun addFromAllClasses(parameters: CompletionParameters, indicesHelper: KotlinIndicesHelper) {
        if (prefixMatcher.getPrefix().isEmpty()) return

        AllClassesCompletion(parameters, indicesHelper, modifiedPrefixMatcher, { !it.isSingleton() })
                .collect({ addSuggestionsForClassifier(it) }, { addSuggestionsForJavaClass(it) })
    }

    public fun addFromParametersInFile(position: PsiElement, resolutionFacade: ResolutionFacade, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        position.getContainingFile().forEachDescendantOfType<JetParameter>(
                canGoInside = { it !is JetExpression || it is JetDeclaration } // we analyze parameters inside bodies to not resolve too much
        ) { declaration ->
            val name = declaration.getName()
            if (name != null && prefixMatcher.prefixMatches(name)) {
                val parameter = resolutionFacade.analyze(declaration)[BindingContext.VALUE_PARAMETER, declaration]
                if (parameter != null) {
                    val parameterType = parameter.getType()
                    if (parameterType.isVisible(visibilityFilter)) {
                        addLookupElement(NameAndArbitraryType(name, parameterType))
                    }
                }
            }
        }
    }

    private fun addSuggestionsForClassifier(classifier: DeclarationDescriptor) {
        addSuggestions(classifier.getName().asString()) { name -> NameAndDescriptorType(name, classifier as ClassifierDescriptor) }
    }

    private fun addSuggestionsForJavaClass(psiClass: PsiClass) {
        addSuggestions(psiClass.getName()) { name -> NameAndJavaType(name, psiClass) }
    }

    private inline fun addSuggestions(className: String, nameAndTypeFactory: (String) -> NameAndType) {
        val parameterNames = JetNameSuggester.getCamelNames(className, EmptyValidator)
        for (parameterName in parameterNames) {
            if (prefixMatcher.prefixMatches(parameterName)) {
                addLookupElement(nameAndTypeFactory(parameterName))
            }
        }
    }

    private fun addLookupElement(nameAndType: NameAndType) {
        val lookupElement = MyLookupElement.create(nameAndType, lookupElementFactory)
        if (lookupElement != null) {
            collector.addElement(lookupElement)
        }
    }

    private fun JetType.isVisible(visibilityFilter: (DeclarationDescriptor) -> Boolean): Boolean {
        if (isError()) return false
        val classifier = getConstructor().getDeclarationDescriptor() ?: return false
        return visibilityFilter(classifier) && getArguments().all { it.getType().isVisible(visibilityFilter) }
    }

    private abstract class NameAndType(val parameterName: String) {
        abstract fun createTypeLookupElement(lookupElementFactory: LookupElementFactory): LookupElement?
    }

    private class NameAndDescriptorType(parameterName: String, private val type: ClassifierDescriptor) : NameAndType(parameterName) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElement(type, false)
    }

    private class NameAndJavaType(parameterName: String, private val type: PsiClass) : NameAndType(parameterName) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForJavaClass(type)
    }

    private class NameAndArbitraryType(parameterName: String, private val type: JetType) : NameAndType(parameterName) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForType(type)
    }

    private class MyLookupElement private constructor(
            private val parameterName: String,
            typeLookupElement: LookupElement
    ) : LookupElementDecorator<LookupElement>(typeLookupElement) {

        companion object {
            fun create(nameAndType: NameAndType, factory: LookupElementFactory): LookupElement? {
                val lookupElement = nameAndType.createTypeLookupElement(factory) ?: return null
                return MyLookupElement(nameAndType.parameterName, lookupElement).suppressAutoInsertion()
            }
        }

        override fun equals(other: Any?)
                = other is MyLookupElement && parameterName == other.parameterName && getDelegate() == other.getDelegate()
        override fun hashCode() = parameterName.hashCode()

        override fun getLookupString() = parameterName
        override fun getAllLookupStrings() = setOf(parameterName)

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)
            presentation.setItemText(parameterName + ": " + presentation.getItemText())
        }

        override fun handleInsert(context: InsertionContext) {
            val settings = CodeStyleSettingsManager.getInstance(context.getProject()).getCurrentSettings().getCustomSettings(javaClass<JetCodeStyleSettings>())
            val spaceBefore = if (settings.SPACE_BEFORE_TYPE_COLON) " " else ""
            val spaceAfter = if (settings.SPACE_AFTER_TYPE_COLON) " " else ""
            val text = parameterName + spaceBefore + ":" + spaceAfter
            val startOffset = context.getStartOffset()
            context.getDocument().insertString(startOffset, text)

            // update start offset so that it does not include the text we inserted
            context.getOffsetMap().addOffset(CompletionInitializationContext.START_OFFSET, startOffset + text.length())

            super.handleInsert(context)
        }
    }
}