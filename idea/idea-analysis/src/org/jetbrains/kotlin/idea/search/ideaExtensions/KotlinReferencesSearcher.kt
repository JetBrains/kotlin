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

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.*
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

data class KotlinReferencesSearchOptions(val acceptCallableOverrides: Boolean = false,
                                         val acceptOverloads: Boolean = false,
                                         val acceptExtensionsOfDeclarationClass: Boolean = false,
                                         val acceptCompanionObjectMembers: Boolean = false) {
    fun anyEnabled(): Boolean = acceptCallableOverrides || acceptOverloads || acceptExtensionsOfDeclarationClass

    companion object {
        val Empty = KotlinReferencesSearchOptions(false, false, false)
    }
}

public class KotlinReferencesSearchParameters(elementToSearch: PsiElement,
                                              scope: SearchScope = runReadAction { elementToSearch.project.allScope() },
                                              ignoreAccessScope: Boolean = false,
                                              optimizer: SearchRequestCollector? = null,
                                              val kotlinOptions: KotlinReferencesSearchOptions = KotlinReferencesSearchOptions.Empty)
        : ReferencesSearch.SearchParameters(elementToSearch, scope, ignoreAccessScope, optimizer) {
}

public class KotlinReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val element = queryParameters.getElementToSearch()

        val unwrappedElement = element.namedUnwrappedElement ?: return

        val words = runReadAction {
            val classNameForCompanionObject = unwrappedElement.getClassNameForCompanionObject()
            unwrappedElement.getSpecialNamesToSearch() +
                (if (classNameForCompanionObject != null) listOf(classNameForCompanionObject) else emptyList())
        }

        val effectiveSearchScope = runReadAction { queryParameters.effectiveSearchScope }

        val refFilter: (PsiReference) -> Boolean = if (unwrappedElement is KtParameter)
            ({ ref: PsiReference -> !ref.isNamedArgumentReference()/* they are processed later*/ })
        else
            ({true})


        val kotlinOptions = (queryParameters as? KotlinReferencesSearchParameters)?.kotlinOptions
                            ?: KotlinReferencesSearchOptions.Empty
        val resultProcessor = MyRequestResultProcessor(unwrappedElement, filter = refFilter, options = kotlinOptions)

        if (kotlinOptions.anyEnabled()) {
            val name = runReadAction { unwrappedElement.name }
            if (name != null) {
                queryParameters.optimizer.searchWord(name, effectiveSearchScope, UsageSearchContext.IN_CODE, true, unwrappedElement,
                                                     resultProcessor)
            }
        }
        words.forEach { word ->
            queryParameters.optimizer.searchWord(word, effectiveSearchScope,
                                                 UsageSearchContext.ANY, true, unwrappedElement,
                                                 resultProcessor)
        }

        if (unwrappedElement is KtParameter) {
            runReadAction { searchNamedArguments(unwrappedElement, queryParameters) }
        }

        if (!(unwrappedElement is KtElement && isOnlyKotlinSearch(effectiveSearchScope))) {
            searchLightElements(queryParameters, element)
        }
    }

    private fun searchNamedArguments(parameter: KtParameter, queryParameters: ReferencesSearch.SearchParameters) {
        val parameterName = parameter.name ?: return
        val function = parameter.ownerFunction ?: return
        if (function.nameAsName?.isSpecial ?: true) return
        val project = function.project
        var namedArgsScope = function.useScope.intersectWith(queryParameters.scopeDeterminedByUser)

        if (namedArgsScope is GlobalSearchScope) {
            namedArgsScope = KotlinSourceFilterScope.kotlinSourcesAndLibraries(namedArgsScope, project)

            val filesWithFunctionName = CacheManager.SERVICE.getInstance(project).getVirtualFilesWithWord(
                    function.name!!, UsageSearchContext.IN_CODE, namedArgsScope, true)
            namedArgsScope = GlobalSearchScope.filesScope(project, filesWithFunctionName.asList())
        }

        val processor = MyRequestResultProcessor(parameter, filter = { it.isNamedArgumentReference() })
        queryParameters.optimizer.searchWord(parameterName,
                                             namedArgsScope,
                                             KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT,
                                             true,
                                             parameter,
                                             processor)
    }

    private fun PsiReference.isNamedArgumentReference(): Boolean {
        return this is KtSimpleNameReference && expression.parent is KtValueArgumentName
    }

    private class MyRequestResultProcessor(
            private val unwrappedElement: PsiElement,
            private val originalElement: PsiElement = unwrappedElement,
            private val filter: (PsiReference) -> Boolean = { true },
            private val options: KotlinReferencesSearchOptions = KotlinReferencesSearchOptions.Empty
    ) : RequestResultProcessor(unwrappedElement, originalElement, filter, options) {
        private val referenceService = PsiReferenceService.getService()

        override fun processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Processor<PsiReference>): Boolean {
            return referenceService.getReferences(element, PsiReferenceService.Hints.NO_HINTS).all { ref ->
                ProgressManager.checkCanceled()

                when {
                    !filter(ref) -> true
                    !ReferenceRange.containsOffsetInElement(ref, offsetInElement) -> true
                    !ref.isReferenceToTarget(unwrappedElement) -> true
                    else -> consumer.process(ref)
                }
            }
        }

        private fun PsiReference.isReferenceToTarget(element: PsiElement): Boolean {
            if (isReferenceTo(element)) {
                return true
            }
            if (originalElement is KtNamedDeclaration) {
                if (options.acceptCallableOverrides && isCallableOverrideUsage(originalElement)) {
                    return true
                }
                if (options.acceptOverloads && isUsageInContainingDeclaration(originalElement)) {
                    return true
                }
                if (options.acceptExtensionsOfDeclarationClass && isExtensionOfDeclarationClassUsage(originalElement)) {
                    return true
                }
            }
            return false
        }
    }

    companion object {
        public fun processJetClassOrObject(element: KtClassOrObject, queryParameters: ReferencesSearch.SearchParameters) {
            val className = element.getName()
            if (className != null) {
                val lightClass = runReadAction { LightClassUtil.getPsiClass(element) }
                if (lightClass != null) {
                    searchNamedElement(queryParameters, lightClass, className)

                    if (element is KtObjectDeclaration && element.isCompanion()) {
                        val fieldForCompanionObject = runReadAction { LightClassUtil.getLightFieldForCompanionObject(element) }
                        if (fieldForCompanionObject != null) {
                            searchNamedElement(queryParameters, fieldForCompanionObject)
                        }

                        val kotlinReferencesSearchOptions = (queryParameters as? KotlinReferencesSearchParameters)?.kotlinOptions
                        if (kotlinReferencesSearchOptions?.acceptCompanionObjectMembers == true) {
                            runReadAction {
                                val originClass = element.getStrictParentOfType<KtClass>()
                                val originLightClass = LightClassUtil.getPsiClass(originClass)
                                if (originLightClass != null) {
                                    val lightDeclarations: List<KtLightElement<*, *>?> =
                                            originLightClass.methods.map { it as? KtLightMethod } +
                                            originLightClass.fields.map { it as? KtLightField }

                                    for (declaration in element.declarations) {
                                        val lightDeclaration = lightDeclarations.find { it?.getOrigin() == declaration }
                                        if (lightDeclaration != null) {
                                            searchNamedElement(queryParameters, lightDeclaration)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun findStaticMethodFromCompanionObject(function: KtFunction): PsiMethod? {
            val originObject = function.parents
                .dropWhile { it is KtClassBody }
                .firstOrNull() as? KtObjectDeclaration ?: return null
            if (originObject.isCompanion()) {
                val originClass = originObject.getStrictParentOfType<KtClass>()
                val originLightClass = LightClassUtil.getPsiClass(originClass)
                val allMethods = originLightClass?.allMethods
                return allMethods?.find { it is KtLightMethod && it.getOrigin() == function }
            }
            return null
        }

        private fun searchPropertyMethods(queryParameters: ReferencesSearch.SearchParameters, parameter: KtParameter) {
            val propertyMethods = runReadAction { LightClassUtil.getLightClassPropertyMethods(parameter) }
            propertyMethods.allDeclarations.forEach { searchNamedElement(queryParameters, it) }
        }

        private fun searchDataClassComponentUsages(queryParameters: ReferencesSearch.SearchParameters,
                                                   containingClass: PsiClass?,
                                                   componentFunctionDescriptor: FunctionDescriptor) {
            val componentFunction = containingClass?.methods?.find {
                it.name == componentFunctionDescriptor.name.asString() && it.parameterList.parametersCount == 0
            }
            if (componentFunction != null) {
                searchNamedElement(queryParameters, componentFunction)
            }
        }

        private fun searchLightElements(queryParameters: ReferencesSearch.SearchParameters, element: PsiElement) {
            when (element) {
                is KtClassOrObject -> processJetClassOrObject(element, queryParameters)
                is KtNamedFunction, is KtSecondaryConstructor -> {
                    val function = element as KtFunction
                    val name = runReadAction { function.getName() }
                    if (name != null) {
                        val methods = runReadAction { LightClassUtil.getLightClassMethods(function) }
                        for (method in methods) {
                            searchNamedElement(queryParameters, method)
                        }
                    }

                    val staticFromCompanionObject = runReadAction { findStaticMethodFromCompanionObject(element) }
                    if (staticFromCompanionObject != null) {
                        searchNamedElement(queryParameters, staticFromCompanionObject)
                    }
                }

                is KtProperty -> {
                    val propertyMethods = runReadAction { LightClassUtil.getLightClassPropertyMethods(element) }
                    propertyMethods.allDeclarations.forEach { searchNamedElement(queryParameters, it) }
                }

                is KtParameter -> {
                    searchPropertyMethods(queryParameters, element)
                    runReadAction {
                        val componentFunctionDescriptor = element.dataClassComponentFunction()
                        if (componentFunctionDescriptor != null) {
                            val containingClass = LightClassUtil.getPsiClass(element.getStrictParentOfType<KtClassOrObject>())
                            searchDataClassComponentUsages(queryParameters, containingClass, componentFunctionDescriptor)
                        }
                    }
                }

                is KtLightMethod -> {
                    val declaration = element.getOrigin()
                    if (declaration is KtProperty || (declaration is KtParameter && declaration.hasValOrVar())) {
                        searchNamedElement(queryParameters, declaration as PsiNamedElement)
                    }
                    else if (declaration is KtPropertyAccessor) {
                        val property = declaration.getStrictParentOfType<KtProperty>()
                        searchNamedElement(queryParameters, property)
                    }
                    else if (declaration is KtFunction) {
                        val staticFromCompanionObject = runReadAction { findStaticMethodFromCompanionObject(declaration) }
                        if (staticFromCompanionObject != null) {
                            searchNamedElement(queryParameters, staticFromCompanionObject)
                        }
                    }
                }

                is KtLightParameter -> {
                    val origin = element.getOrigin() ?: return
                    runReadAction {
                        val componentFunctionDescriptor = origin.dataClassComponentFunction()
                        if (componentFunctionDescriptor != null) {
                            searchDataClassComponentUsages(queryParameters, element.method.containingClass, componentFunctionDescriptor)
                        }
                    }
                    searchPropertyMethods(queryParameters, origin)
                }
            }
        }

        private fun isOnlyKotlinSearch(searchScope: SearchScope) =
                searchScope is LocalSearchScope && runReadAction {
                    searchScope.getScope().all { it.getContainingFile().getFileType() == KotlinFileType.INSTANCE }
                }

        private fun searchNamedElement(queryParameters: ReferencesSearch.SearchParameters,
                                       element: PsiNamedElement?,
                                       name: String? = element?.getName()) {
            if (name != null && element != null) {
                val scope = runReadAction { queryParameters.getEffectiveSearchScope() }
                val context = UsageSearchContext.IN_CODE + UsageSearchContext.IN_FOREIGN_LANGUAGES + UsageSearchContext.IN_COMMENTS
                val kotlinOptions = (queryParameters as? KotlinReferencesSearchParameters)?.kotlinOptions
                                    ?: KotlinReferencesSearchOptions.Empty
                val resultProcessor = MyRequestResultProcessor(element,
                                                               queryParameters.getElementToSearch().namedUnwrappedElement ?: element,
                                                               options = kotlinOptions)
                queryParameters.getOptimizer().searchWord(name, scope, context.toShort(), true, element,
                                                          resultProcessor)
            }
        }
    }
}
