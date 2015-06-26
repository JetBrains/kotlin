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

import com.intellij.codeInsight.completion.*
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
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.core.KotlinIndicesHelper
import org.jetbrains.kotlin.idea.core.formatter.JetCodeStyleSettings
import org.jetbrains.kotlin.idea.core.getResolutionScope
import org.jetbrains.kotlin.idea.core.EmptyValidator
import org.jetbrains.kotlin.idea.core.JetNameSuggester
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.JetType
import java.util.HashSet
import java.util.LinkedHashMap

class ParameterNameAndTypeCompletion(
        private val collector: LookupElementsCollector,
        private val lookupElementFactory: LookupElementFactory,
        defaultPrefixMatcher: PrefixMatcher,
        private val resolutionFacade: ResolutionFacade
) {
    private val parametersInCurrentFilePrefixMatcher = MyPrefixMatcher(defaultPrefixMatcher.getPrefix())

    private val prefixWords: Array<String>
    private val nameSuggestionPrefixes: List<String> // prefixes to use to generate parameter names from class names

    init {
        val prefix = defaultPrefixMatcher.getPrefix()
        prefixWords = NameUtil.splitNameIntoWords(prefix)

        nameSuggestionPrefixes = if (prefix.isEmpty() || prefix[0].isUpperCase())
            emptyList()
        else
            prefixWords.indices.map { index -> if (index == 0) prefix else prefixWords.drop(index).join("") }
    }

    private val nameSuggestionPrefixMatchers = nameSuggestionPrefixes.map { MyPrefixMatcher(it) }

    private val userPrefixes = nameSuggestionPrefixes.indices.map { prefixWords.take(it).join("") }

    private val suggestionsByTypesAdded = HashSet<Type>()

    public fun addFromImportedClasses(position: PsiElement, bindingContext: BindingContext, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        for ((i, prefixMatcher) in nameSuggestionPrefixMatchers.withIndex()) {
            val resolutionScope = position.getResolutionScope(bindingContext, resolutionFacade)
            val classifiers = resolutionScope.getDescriptorsFiltered(DescriptorKindFilter.NON_SINGLETON_CLASSIFIERS, prefixMatcher.toClassifierNamePrefixMatcher().asNameFilter())

            for (classifier in classifiers) {
                if (visibilityFilter(classifier)) {
                    addSuggestionsForClassifier(classifier, userPrefixes[i], prefixMatcher)
                }
            }

            collector.flushToResultSet()
        }
    }

    public fun addFromAllClasses(parameters: CompletionParameters, indicesHelper: KotlinIndicesHelper) {
        for ((i, prefixMatcher) in nameSuggestionPrefixMatchers.withIndex()) {
            AllClassesCompletion(parameters, indicesHelper, prefixMatcher.toClassifierNamePrefixMatcher(), { !it.isSingleton() })
                    .collect(
                            { addSuggestionsForClassifier(it, userPrefixes[i], prefixMatcher) },
                            { addSuggestionsForJavaClass(it, userPrefixes[i], prefixMatcher) }
                    )

            collector.flushToResultSet()
        }
    }

    private fun PrefixMatcher.toClassifierNamePrefixMatcher() = cloneWithPrefix(getPrefix().capitalize())

    public fun addFromParametersInFile(position: PsiElement, resolutionFacade: ResolutionFacade, visibilityFilter: (DeclarationDescriptor) -> Boolean) {
        val lookupElementToCount = LinkedHashMap<LookupElement, Int>()
        position.getContainingFile().forEachDescendantOfType<JetParameter>(
                canGoInside = { it !is JetExpression || it is JetDeclaration } // we analyze parameters inside bodies to not resolve too much
        ) { parameter ->
            ProgressManager.checkCanceled()

            val name = parameter.getName()
            if (name != null && parametersInCurrentFilePrefixMatcher.prefixMatches(name)) {
                val descriptor = resolutionFacade.analyze(parameter)[BindingContext.VALUE_PARAMETER, parameter]
                if (descriptor != null) {
                    val parameterType = descriptor.getType()
                    if (parameterType.isVisible(visibilityFilter)) {
                        val lookupElement = MyLookupElement.create("", name, ArbitraryType(parameterType), lookupElementFactory)
                        val count = lookupElementToCount[lookupElement] ?: 0
                        lookupElementToCount[lookupElement] = count + 1
                    }
                }
            }
        }

        for ((lookupElement, count) in lookupElementToCount) {
            lookupElement.putUserData(PRIORITY_KEY, -count)
            collector.addElement(lookupElement, parametersInCurrentFilePrefixMatcher)
        }
    }

    private fun addSuggestionsForClassifier(classifier: DeclarationDescriptor, userPrefix: String, prefixMatcher: PrefixMatcher) {
        addSuggestions(classifier.getName().asString(), userPrefix, prefixMatcher, DescriptorType(classifier as ClassifierDescriptor))
    }

    private fun addSuggestionsForJavaClass(psiClass: PsiClass, userPrefix: String, prefixMatcher: PrefixMatcher) {
        addSuggestions(psiClass.getName(), userPrefix, prefixMatcher, JavaClassType(psiClass))
    }

    private fun addSuggestions(className: String, userPrefix: String, prefixMatcher: PrefixMatcher, type: Type) {
        ProgressManager.checkCanceled()
        if (suggestionsByTypesAdded.contains(type)) return // don't add suggestions for the same with longer user prefix

        val nameSuggestions = JetNameSuggester.getCamelNames(className, EmptyValidator, userPrefix.isEmpty())
        for (name in nameSuggestions) {
            if (prefixMatcher.prefixMatches(name)) {
                val lookupElement = MyLookupElement.create(userPrefix, name, type, lookupElementFactory)
                if (lookupElement != null) {
                    lookupElement.putUserData(PRIORITY_KEY, userPrefix.length()) // suggestions with longer user prefix get smaller priority
                    collector.addElement(lookupElement, prefixMatcher)
                    suggestionsByTypesAdded.add(type)
                }
            }
        }
    }

    private fun JetType.isVisible(visibilityFilter: (DeclarationDescriptor) -> Boolean): Boolean {
        if (isError()) return false
        val classifier = getConstructor().getDeclarationDescriptor() ?: return false
        return visibilityFilter(classifier) && getArguments().all { it.getType().isVisible(visibilityFilter) }
    }

    private abstract class Type(private val idString: String) {
        abstract fun createTypeLookupElement(lookupElementFactory: LookupElementFactory): LookupElement?

        override fun equals(other: Any?) = other is Type && other.idString == idString
        override fun hashCode() = idString.hashCode()
    }

    private class DescriptorType(private val classifier: ClassifierDescriptor) : Type(IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(classifier)) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElement(classifier, false, qualifyNestedClasses = true)
    }

    private class JavaClassType(private val psiClass: PsiClass) : Type(psiClass.getQualifiedName()!!) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForJavaClass(psiClass, qualifyNestedClasses = true)
    }

    private class ArbitraryType(private val type: JetType) : Type(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)) {
        override fun createTypeLookupElement(lookupElementFactory: LookupElementFactory)
                = lookupElementFactory.createLookupElementForType(type)
    }

    private data class MyLookupElement private constructor(
            private val userPrefix: String,
            private val nameSuggestion: String,
            private val type: Type,
            typeLookupElement: LookupElement
    ) : LookupElementDecorator<LookupElement>(typeLookupElement) {

        companion object {
            fun create(userPrefix: String, nameSuggestion: String, type: Type, factory: LookupElementFactory): LookupElement? {
                val typeLookupElement = type.createTypeLookupElement(factory) ?: return null
                val lookupElement = MyLookupElement(userPrefix, nameSuggestion, type, typeLookupElement)
                return lookupElement.suppressAutoInsertion()
            }
        }

        private val parameterName = userPrefix + nameSuggestion

        override fun equals(other: Any?)
                = other is MyLookupElement && nameSuggestion == other.nameSuggestion && userPrefix == other.userPrefix && type == other.type
        override fun hashCode() = parameterName.hashCode()

        override fun getLookupString() = nameSuggestion
        override fun getAllLookupStrings() = setOf(nameSuggestion)

        override fun renderElement(presentation: LookupElementPresentation) {
            super.renderElement(presentation)
            presentation.setItemText(parameterName + ": " + presentation.getItemText())
        }

        override fun handleInsert(context: InsertionContext) {
            val settings = CodeStyleSettingsManager.getInstance(context.getProject()).getCurrentSettings().getCustomSettings(javaClass<JetCodeStyleSettings>())
            val spaceBefore = if (settings.SPACE_BEFORE_TYPE_COLON) " " else ""
            val spaceAfter = if (settings.SPACE_AFTER_TYPE_COLON) " " else ""
            val text = nameSuggestion + spaceBefore + ":" + spaceAfter
            val startOffset = context.getStartOffset()
            context.getDocument().insertString(startOffset, text)

            // update start offset so that it does not include the text we inserted
            context.getOffsetMap().addOffset(CompletionInitializationContext.START_OFFSET, startOffset + text.length())

            super.handleInsert(context)
        }
    }

    private companion object {
        val PRIORITY_KEY = Key<Int>("ParameterNameAndTypeCompletion.PRIORITY_KEY")
    }

    object Weigher : LookupElementWeigher("kotlin.parameterNameAndTypePriority") {
        override fun weigh(element: LookupElement, context: WeighingContext): Int = element.getUserData(PRIORITY_KEY) ?: 0
    }

    private class MyPrefixMatcher(prefix: String) : CamelHumpMatcher(prefix, false) {
        override fun prefixMatches(element: LookupElement) = isStartMatch(element)

        override fun cloneWithPrefix(prefix: String) = MyPrefixMatcher(prefix)
    }
}