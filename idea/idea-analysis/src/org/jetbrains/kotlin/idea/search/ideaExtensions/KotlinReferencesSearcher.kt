/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.*
import com.intellij.psi.impl.cache.CacheManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.containers.nullize
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMember
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.*
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions.Companion.Empty
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.getClassNameForCompanionObject
import org.jetbrains.kotlin.idea.search.usagesSearch.operators.OperatorReferenceSearcher
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.expectedDeclarationIfAny
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.util.*

data class KotlinReferencesSearchOptions(
    val acceptCallableOverrides: Boolean = false,
    val acceptOverloads: Boolean = false,
    val acceptExtensionsOfDeclarationClass: Boolean = false,
    val acceptCompanionObjectMembers: Boolean = false,
    val searchForComponentConventions: Boolean = true,
    val searchForOperatorConventions: Boolean = true,
    val searchNamedArguments: Boolean = true,
    val searchForExpectedUsages: Boolean = true
) {
    fun anyEnabled(): Boolean = acceptCallableOverrides || acceptOverloads || acceptExtensionsOfDeclarationClass

    companion object {
        val Empty = KotlinReferencesSearchOptions()
    }
}

class KotlinReferencesSearchParameters(
    elementToSearch: PsiElement,
    scope: SearchScope = runReadAction { elementToSearch.project.allScope() },
    ignoreAccessScope: Boolean = false,
    optimizer: SearchRequestCollector? = null,
    val kotlinOptions: KotlinReferencesSearchOptions = Empty
) : ReferencesSearch.SearchParameters(elementToSearch, scope, ignoreAccessScope, optimizer)

class KotlinReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: ExecutorProcessor<PsiReference>) {
        val processor = QueryProcessor(queryParameters, consumer)
        runReadAction { processor.processInReadAction() }
        processor.executeLongRunningTasks()
    }

    private class QueryProcessor(val queryParameters: ReferencesSearch.SearchParameters, val consumer: ExecutorProcessor<PsiReference>) {

        private val kotlinOptions = (queryParameters as? KotlinReferencesSearchParameters)?.kotlinOptions ?: Empty

        private val longTasks = ArrayList<() -> Unit>()

        fun executeLongRunningTasks() {
            longTasks.forEach { it() }
        }

        fun processInReadAction() {
            val element = queryParameters.elementToSearch
            if (!element.isValid) return

            val unwrappedElement = element.namedUnwrappedElement ?: return

            val elementToSearch =
                if (kotlinOptions.searchForExpectedUsages && unwrappedElement is KtDeclaration && unwrappedElement.hasActualModifier()) {
                    unwrappedElement.expectedDeclarationIfAny() as? PsiNamedElement
                } else {
                    null
                } ?: unwrappedElement

            val effectiveSearchScope = run {
                val elements = if (elementToSearch is KtDeclaration && !isOnlyKotlinSearch(queryParameters.scopeDeterminedByUser)) {
                    elementToSearch.toLightElements().nullize()
                } else {
                    null
                } ?: listOf(elementToSearch)

                elements.fold(queryParameters.effectiveSearchScope) { scope, e ->
                    scope.unionSafe(queryParameters.effectiveSearchScope(e))
                }
            }

            val refFilter: (PsiReference) -> Boolean = when (elementToSearch) {
                is KtParameter -> ({ ref: PsiReference -> !ref.isNamedArgumentReference()/* they are processed later*/ })
                else -> ({ true })
            }

            val resultProcessor = KotlinRequestResultProcessor(elementToSearch, filter = refFilter, options = kotlinOptions)

            val name = elementToSearch.name
            if (kotlinOptions.anyEnabled() || elementToSearch is KtNamedDeclaration && elementToSearch.isExpectDeclaration()) {
                if (name != null) {
                    // Check difference with default scope
                    queryParameters.optimizer.searchWord(
                        name, effectiveSearchScope, UsageSearchContext.IN_CODE, true, elementToSearch, resultProcessor
                    )
                }
            }


            val classNameForCompanionObject = elementToSearch.getClassNameForCompanionObject()
            if (classNameForCompanionObject != null) {
                queryParameters.optimizer.searchWord(
                    classNameForCompanionObject, effectiveSearchScope, UsageSearchContext.ANY, true, elementToSearch, resultProcessor
                )
            }

            if (elementToSearch is KtParameter && kotlinOptions.searchNamedArguments) {
                searchNamedArguments(elementToSearch)
            }

            if (!(elementToSearch is KtElement && isOnlyKotlinSearch(effectiveSearchScope))) {
                searchLightElements(element)
            }

            if (element is KtFunction || element is PsiMethod) {
                val referenceSearcher = OperatorReferenceSearcher.create(
                    element, effectiveSearchScope, consumer, queryParameters.optimizer, kotlinOptions
                )
                if (referenceSearcher != null) {
                    longTasks.add { referenceSearcher.run() }
                }
            }

            if (kotlinOptions.searchForComponentConventions) {
                when (element) {
                    is KtParameter -> {
                        val componentFunctionDescriptor = element.dataClassComponentFunction()
                        if (componentFunctionDescriptor != null) {
                            val containingClass = element.getStrictParentOfType<KtClassOrObject>()?.toLightClass()
                            searchDataClassComponentUsages(containingClass, componentFunctionDescriptor, kotlinOptions)
                        }
                    }

                    is KtLightParameter -> {
                        val componentFunctionDescriptor = element.kotlinOrigin?.dataClassComponentFunction()
                        if (componentFunctionDescriptor != null) {
                            searchDataClassComponentUsages(element.method.containingClass, componentFunctionDescriptor, kotlinOptions)
                        }
                    }
                }
            }
        }

        private fun searchNamedArguments(parameter: KtParameter) {
            val parameterName = parameter.name ?: return
            val function = parameter.ownerFunction as? KtFunction ?: return
            if (function.nameAsName?.isSpecial != false) return
            val project = function.project
            var namedArgsScope = function.useScope.intersectWith(queryParameters.scopeDeterminedByUser)

            if (namedArgsScope is GlobalSearchScope) {
                namedArgsScope = KotlinSourceFilterScope.sourcesAndLibraries(namedArgsScope, project)

                val filesWithFunctionName = CacheManager.SERVICE.getInstance(project).getVirtualFilesWithWord(
                    function.name!!, UsageSearchContext.IN_CODE, namedArgsScope, true
                )
                namedArgsScope = GlobalSearchScope.filesScope(project, filesWithFunctionName.asList())
            }

            val processor = KotlinRequestResultProcessor(parameter, filter = { it.isNamedArgumentReference() })
            queryParameters.optimizer.searchWord(
                parameterName,
                namedArgsScope,
                KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT,
                true,
                parameter,
                processor
            )
        }

        private fun searchLightElements(element: PsiElement) {
            when (element) {
                is KtClassOrObject -> {
                    processKtClassOrObject(element)
                }

                is KtNamedFunction, is KtSecondaryConstructor -> {
                    val name = (element as KtFunction).name
                    if (name != null) {
                        val methods = LightClassUtil.getLightClassMethods(element)
                        for (method in methods) {
                            searchNamedElement(method)
                        }
                    }

                    processStaticsFromCompanionObject(element)
                }

                is KtProperty -> {
                    val propertyMethods = LightClassUtil.getLightClassPropertyMethods(element)
                    propertyMethods.allDeclarations.forEach { searchNamedElement(it) }
                    processStaticsFromCompanionObject(element)
                }

                is KtParameter -> {
                    searchPropertyAccessorMethods(element)
                }

                is KtLightMethod -> {
                    val declaration = element.kotlinOrigin
                    if (declaration is KtProperty || (declaration is KtParameter && declaration.hasValOrVar())) {
                        searchNamedElement(declaration as PsiNamedElement)
                        processStaticsFromCompanionObject(declaration)
                    } else if (declaration is KtPropertyAccessor) {
                        val property = declaration.getStrictParentOfType<KtProperty>()
                        searchNamedElement(property)
                    } else if (declaration is KtFunction) {
                        processStaticsFromCompanionObject(declaration)
                        if (element.isMangled) {
                            searchNamedElement(declaration) { it.restrictToKotlinSources() }
                        }
                    }
                }

                is KtLightParameter -> {
                    val origin = element.kotlinOrigin ?: return
                    searchPropertyAccessorMethods(origin)
                }
            }
        }

        private fun searchPropertyAccessorMethods(origin: KtParameter) {
            origin.toLightElements().forEach { searchNamedElement(it) }
        }

        private fun processKtClassOrObject(element: KtClassOrObject) {
            val className = element.name ?: return
            val lightClass = element.toLightClass() ?: return
            searchNamedElement(lightClass, className)

            if (element is KtObjectDeclaration && element.isCompanion()) {
                LightClassUtil.getLightFieldForCompanionObject(element)?.let { searchNamedElement(it) }

                if (kotlinOptions.acceptCompanionObjectMembers) {
                    val originLightClass = element.getStrictParentOfType<KtClass>()?.toLightClass()
                    if (originLightClass != null) {
                        val lightDeclarations: List<KtLightMember<*>?> =
                            originLightClass.methods.map { it as? KtLightMethod } + originLightClass.fields.map { it as? KtLightField }

                        for (declaration in element.declarations) {
                            lightDeclarations
                                .firstOrNull { it?.kotlinOrigin == declaration }
                                ?.let { searchNamedElement(it) }
                        }
                    }
                }
            }
        }

        private fun searchDataClassComponentUsages(
            containingClass: PsiClass?,
            componentFunctionDescriptor: FunctionDescriptor,
            kotlinOptions: KotlinReferencesSearchOptions
        ) {
            val componentFunction = containingClass?.methods?.firstOrNull {
                it.name == componentFunctionDescriptor.name.asString() && it.parameterList.parametersCount == 0
            }
            if (componentFunction != null) {
                searchNamedElement(componentFunction)

                val searcher = OperatorReferenceSearcher.create(
                    componentFunction, queryParameters.effectiveSearchScope, consumer, queryParameters.optimizer, kotlinOptions
                )
                longTasks.add { searcher!!.run() }
            }
        }

        private fun processStaticsFromCompanionObject(element: KtDeclaration) {
            findStaticMethodsFromCompanionObject(element).forEach { searchNamedElement(it) }
        }

        private fun findStaticMethodsFromCompanionObject(declaration: KtDeclaration): List<PsiMethod> {
            val originObject = declaration.parents
                .dropWhile { it is KtClassBody }
                .firstOrNull() as? KtObjectDeclaration ?: return emptyList()
            if (!originObject.isCompanion()) return emptyList()
            val originClass = originObject.getStrictParentOfType<KtClass>()
            val originLightClass = originClass?.toLightClass() ?: return emptyList()
            val allMethods = originLightClass.allMethods
            return allMethods.filter { it is KtLightMethod && it.kotlinOrigin == declaration }
        }

        private fun searchNamedElement(
            element: PsiNamedElement?,
            name: String? = element?.name,
            modifyScope: ((SearchScope) -> SearchScope)? = null
        ) {
            if (name != null && element != null) {
                val baseScope = queryParameters.effectiveSearchScope(element)
                val scope = if (modifyScope != null) modifyScope(baseScope) else baseScope
                val context = UsageSearchContext.IN_CODE + UsageSearchContext.IN_FOREIGN_LANGUAGES + UsageSearchContext.IN_COMMENTS
                val resultProcessor = KotlinRequestResultProcessor(
                    element,
                    queryParameters.elementToSearch.namedUnwrappedElement ?: element,
                    options = kotlinOptions
                )
                queryParameters.optimizer.searchWord(name, scope, context.toShort(), true, element, resultProcessor)
            }
        }

        private fun PsiReference.isNamedArgumentReference(): Boolean {
            return this is KtSimpleNameReference && expression.parent is KtValueArgumentName
        }
    }
}
