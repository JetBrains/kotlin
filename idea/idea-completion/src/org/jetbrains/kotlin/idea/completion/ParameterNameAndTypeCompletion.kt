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
import com.intellij.codeInsight.completion.impl.CamelHumpMatcher
import com.intellij.codeInsight.lookup.*
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.NameUtil
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.utils.getDescriptorsFiltered
import org.jetbrains.kotlin.types.JetType
import java.util.*

class ParameterNameAndTypeCompletion(
        private val collector: LookupElementsCollector,
        private val lookupElementFactory: LookupElementFactory,
        private val prefixMatcher: PrefixMatcher,
        private val resolutionFacade: ResolutionFacade
) {
    private val userPrefixes: List<String>
    private val classNamePrefixMatchers: List<PrefixMatcher>

    init {
        val prefix = prefixMatcher.prefix
        val prefixWords = NameUtil.splitNameIntoWords(prefix)

        // prefixes to use to generate parameter names from class names
        val nameSuggestionPrefixes = if (prefix.isEmpty() || prefix[0].isUpperCase())
            emptyList()
        else
            prefixWords.indices.map { index -> if (index == 0) prefix else prefixWords.drop(index).join("") }

        userPrefixes = nameSuggestionPrefixes.indices.map { prefixWords.take(it).join("") }
        classNamePrefixMatchers = nameSuggestionPrefixes.map { CamelHumpMatcher(it.capitalize(), false) }
    }

    private val suggestionsByTypesAdded = HashSet<Type>()

    public fun addFromImportedClasses(position: PsiElement, bindingContext: BindingContext, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        for ((classNameMatcher, userPrefix) in classNamePrefixMatchers.zip(userPrefixes)) {
            val resolutionScope = position.getResolutionScope(bindingContext, resolutionFacade)
            val classifiers = resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, classNameMatcher.asNameFilter())

            for (classifier in classifiers) {
                if (visibilityFilter(classifier)) {
                    addSuggestionsForClassifier(classifier, userPrefix, notImported = false)
                }
            }

            collector.flushToResultSet()
        }
    }

    public fun addFromAllClasses(parameters: CompletionParameters, indicesHelper: KotlinIndicesHelper) {
        for ((classNameMatcher, userPrefix) in classNamePrefixMatchers.zip(userPrefixes)) {
            AllClassesCompletion(
                    parameters, indicesHelper, classNameMatcher, resolutionFacade, { !it.isSingleton() }
            ).collect(
                    { addSuggestionsForClassifier(it, userPrefix, notImported = true) },
                    { addSuggestionsForJavaClass(it, userPrefix, notImported = true) }
            )

            collector.flushToResultSet()
        }
    }

    public fun addFromParametersInFile(position: PsiElement, resolutionFacade: ResolutionFacade, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        val lookupElementToCount = LinkedHashMap<LookupElement, Int>()
        position.getContainingFile().forEachDescendantOfType<JetParameter>(
                canGoInside = { it !is JetExpression || it is JetDeclaration } // we analyze parameters inside bodies to not resolve too much
        ) { parameter ->
            ProgressManager.checkCanceled()

            val name = parameter.getName()
            if (name != null && prefixMatcher.isStartMatch(name)) {
                val descriptor = resolutionFacade.analyze(parameter)[BindingContext.VALUE_PARAMETER, parameter]
                if (descriptor != null) {
                    val parameterType = descriptor.getType()
                    if (parameterType.isVisible(visibilityFilter)) {
                        val lookupElement = MyLookupElement.create(name, ArbitraryType(parameterType), lookupElementFactory)
                        val count = lookupElementToCount[lookupElement] ?: 0
                        lookupElementToCount[lookupElement!!] = count + 1
                    }
                }
            }
        }

        for ((lookupElement, count) in lookupElementToCount) {
            lookupElement.putUserData(PRIORITY_KEY, -count)
            collector.addElement(lookupElement)
        }
    }

    private fun addSuggestionsForClassifier(classifier: DeclarationDescriptor, userPrefix: String, notImported: Boolean) {
        addSuggestions(classifier.getName().asString(), userPrefix, DescriptorType(classifier as ClassifierDescriptor), notImported)
    }

    private fun addSuggestionsForJavaClass(psiClass: PsiClass, userPrefix: String, notImported: Boolean) {
        addSuggestions(psiClass.getName()!!, userPrefix, JavaClassType(psiClass), notImported)
    }

    private fun addSuggestions(className: String, userPrefix: String, type: Type, notImported: Boolean) {
        ProgressManager.checkCanceled()
        if (suggestionsByTypesAdded.contains(type)) return // don't add suggestions for the same with longer user prefix

        val nameSuggestions = KotlinNameSuggester.getCamelNames(className, { true }, userPrefix.isEmpty())
        for (name in nameSuggestions) {
            val parameterName = userPrefix + name
            if (prefixMatcher.isStartMatch(parameterName)) {
                val lookupElement = MyLookupElement.create(parameterName, type, lookupElementFactory)
                if (lookupElement != null) {
                    lookupElement.putUserData(PRIORITY_KEY, userPrefix.length()) // suggestions with longer user prefix get lower priority
                    collector.addElement(lookupElement, notImported)
                    suggestionsByTypesAdded.add(type)
                }
            }
        }
    }

    private fun JetType.isVisible(visibilityFilter: (DeclarationDescriptor) -> Boolean): Boolean {
        if (isError()) return false
        val classifier = getConstructor().getDeclarationDescriptor() ?: return false
        return visibilityFilter(classifier) && getArguments().all { it.isStarProjection || it.getType().isVisible(visibilityFilter) }
    }

    private abstract class Type(private val idString: String) {
        abstract fun createTypeLookupElement(lookupElementFactory: LookupElementFactory): LookupElement?

        override fun equals(other: Any?) = other is Type && other.idString == idString
        override fun hashCode() = idString.hashCode()
    }

    private class DescriptorType(private val classifier: ClassifierDescriptor) : Type(IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier)) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElement(classifier, useReceiverTypes = false, qualifyNestedClasses = true)
    }

    private class JavaClassType(private val psiClass: PsiClass) : Type(psiClass.getQualifiedName()!!) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForJavaClass(psiClass, qualifyNestedClasses = true)
    }

    private class ArbitraryType(private val type: JetType) : Type(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForType(type)
    }

    private class MyLookupElement private constructor(
            private val parameterName: String,
            private val type: Type,
            typeLookupElement: LookupElement
    ) : LookupElementDecorator<LookupElement>(typeLookupElement) {

        companion object {
            fun create(parameterName: String, type: Type, factory: LookupElementFactory): LookupElement? {
                val typeLookupElement = type.createTypeLookupElement(factory) ?: return null
                val lookupElement = MyLookupElement(parameterName, type, typeLookupElement)
                return lookupElement.suppressAutoInsertion()
            }
        }

        private val lookupString = parameterName + ": " + delegate.lookupString

        override fun getLookupString() = lookupString
        override fun getAllLookupStrings() = setOf(lookupString)

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)
            presentation.setItemText(parameterName + ": " + presentation.getItemText())
        }

        override fun handleInsert(context: InsertionContext) {
            val settings = CodeStyleSettingsManager.getInstance(context.getProject()).getCurrentSettings().getCustomSettings(JetCodeStyleSettings::class.java)
            val spaceBefore = if (settings.SPACE_BEFORE_TYPE_COLON) " " else ""
            val spaceAfter = if (settings.SPACE_AFTER_TYPE_COLON) " " else ""
            val text = parameterName + spaceBefore + ":" + spaceAfter
            val startOffset = context.getStartOffset()
            context.getDocument().insertString(startOffset, text)

            // update start offset so that it does not include the text we inserted
            context.getOffsetMap().addOffset(CompletionInitializationContext.START_OFFSET, startOffset + text.length())

            super.handleInsert(context)
        }

        override fun equals(other: Any?)
                = other is MyLookupElement && parameterName == other.parameterName && type == other.type
        override fun hashCode() = parameterName.hashCode()
    }

    private companion object {
        val PRIORITY_KEY = Key<Int>("ParameterNameAndTypeCompletion.PRIORITY_KEY")
    }

    object Weigher : LookupElementWeigher("kotlin.parameterNameAndTypePriority") {
        override fun weigh(element: LookupElement, context: WeighingContext): Int = element.getUserData(PRIORITY_KEY) ?: 0
    }
}