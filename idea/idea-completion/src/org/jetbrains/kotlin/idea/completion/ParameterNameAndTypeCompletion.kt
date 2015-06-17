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

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.psi.PsiClass
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.core.refactoring.EmptyValidator
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class ParameterNameAndTypeCompletion(
        private val collector: LookupElementsCollector,
        private val lookupElementFactory: LookupElementFactory,
        private val prefixMatcher: PrefixMatcher
) {
    private val modifiedPrefixMatcher = prefixMatcher.cloneWithPrefix(prefixMatcher.getPrefix().capitalize())

    public fun addFromImports(nameExpression: JetSimpleNameExpression, bindingContext: BindingContext, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        if (prefixMatcher.getPrefix().isEmpty()) return

        val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, nameExpression] ?: return
        val classifiers = resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, modifiedPrefixMatcher.asNameFilter())

        for (classifier in classifiers) {
            if (visibilityFilter(classifier)) {
                addSuggestionsForClassifier(classifier)
            }
        }
    }

    public fun addAll(parameters: CompletionParameters, indicesHelper: KotlinIndicesHelper) {
        if (prefixMatcher.getPrefix().isEmpty()) return

        AllClassesCompletion(parameters, indicesHelper, modifiedPrefixMatcher, { !it.isSingleton() })
                .collect({ addSuggestionsForClassifier(it) }, { addSuggestionsForJavaClass(it) })
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
                val nameAndType = nameAndTypeFactory(parameterName)
                collector.addElement(MyLookupElement(nameAndType, lookupElementFactory))
            }
        }
    }

    private interface NameAndType {
        val parameterName: String

        fun createTypeLookupElement(lookupElementFactory: LookupElementFactory): LookupElement
    }

    private data class NameAndDescriptorType(override val parameterName: String, val type: ClassifierDescriptor) : NameAndType {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElement(type, false)
    }

    private data class NameAndJavaType(override val parameterName: String, val type: PsiClass) : NameAndType {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForJavaClass(type)
    }

    private class MyLookupElement(
            val nameAndType: NameAndType,
            factory: LookupElementFactory
    ) : LookupElementDecorator<LookupElement>(nameAndType.createTypeLookupElement(factory)) {
        override fun getObject() = nameAndType

        override fun equals(other: Any?)
                = other is MyLookupElement && nameAndType.parameterName == other.getObject().parameterName && getDelegate() == other.getDelegate()
        override fun hashCode() = nameAndType.parameterName.hashCode()

        override fun getLookupString() = nameAndType.parameterName
        override fun getAllLookupStrings() = setOf(nameAndType.parameterName)

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)
            presentation.setItemText(nameAndType.parameterName + ": " + presentation.getItemText())
        }

        override fun handleInsert(context: InsertionContext) {
            super.handleInsert(context)

            val settings = CodeStyleSettingsManager.getInstance(context.getProject()).getCurrentSettings().getCustomSettings(javaClass<JetCodeStyleSettings>())
            val spaceBefore = if (settings.SPACE_BEFORE_TYPE_COLON) " " else ""
            val spaceAfter = if (settings.SPACE_AFTER_TYPE_COLON) " " else ""
            val text = nameAndType.parameterName + spaceBefore + ":" + spaceAfter
            context.getDocument().insertString(context.getStartOffset(), text)
        }
    }
}