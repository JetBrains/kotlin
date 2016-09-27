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
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightParameter
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.search.KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.effectiveSearchScope
import org.jetbrains.kotlin.idea.search.unionSafe
import org.jetbrains.kotlin.idea.search.usagesSearch.OperatorReferenceSearcher
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.getClassNameForCompanionObject
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import java.util.*

data class KotlinReferencesSearchOptions(val acceptCallableOverrides: Boolean = false,
                                         val acceptOverloads: Boolean = false,
                                         val acceptExtensionsOfDeclarationClass: Boolean = false,
                                         val acceptCompanionObjectMembers: Boolean = false,
                                         val searchForComponentConventions: Boolean = true,
                                         val searchForOperatorConventions: Boolean = true,
                                         val searchNamedArguments: Boolean = true) {
    fun anyEnabled(): Boolean = acceptCallableOverrides || acceptOverloads || acceptExtensionsOfDeclarationClass

    companion object {
        val Empty = KotlinReferencesSearchOptions()
    }
}

class KotlinReferencesSearchParameters(elementToSearch: PsiElement,
                                              scope: SearchScope = runReadAction { elementToSearch.project.allScope() },
                                              ignoreAccessScope: Boolean = false,
                                              optimizer: SearchRequestCollector? = null,
                                              val kotlinOptions: KotlinReferencesSearchOptions = KotlinReferencesSearchOptions.Empty)
        : ReferencesSearch.SearchParameters(elementToSearch, scope, ignoreAccessScope, optimizer) {
}

class KotlinReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val processor = QueryProcessor(queryParameters, consumer)
        runReadAction { processor.processInReadAction() }
        processor.executeLongRunningTasks()
    }

    private class QueryProcessor(val queryParameters: ReferencesSearch.SearchParameters, val consumer: Processor<PsiReference>) {

        private val kotlinOptions = (queryParameters as? KotlinReferencesSearchParameters)?.kotlinOptions
                                    ?: KotlinReferencesSearchOptions.Empty

        private val longTasks = ArrayList<() -> Unit>()

        fun executeLongRunningTasks() {
            longTasks.forEach { it() }
        }

        fun processInReadAction() {
            val element = queryParameters.elementToSearch
            if (!element.isValid) return

            val unwrappedElement = element.namedUnwrappedElement ?: return

            val elements = if (unwrappedElement is KtDeclaration) unwrappedElement.toLightElements() else listOf(unwrappedElement)
            val effectiveSearchScope = elements.fold(queryParameters.effectiveSearchScope) { scope, e ->
                scope.unionSafe(queryParameters.effectiveSearchScope(e))
            }

            val refFilter: (PsiReference) -> Boolean = when {
                unwrappedElement is KtParameter -> ({ ref: PsiReference -> !ref.isNamedArgumentReference()/* they are processed later*/ })
                else -> ({true})
            }

            val resultProcessor = KotlinRequestResultProcessor(unwrappedElement, filter = refFilter, options = kotlinOptions)

            val name = unwrappedElement.name
            if (kotlinOptions.anyEnabled()) {
                if (name != null) {
                    queryParameters.optimizer.searchWord(
                            name, effectiveSearchScope, UsageSearchContext.IN_CODE, true, unwrappedElement, resultProcessor)
                }
            }


            val classNameForCompanionObject = unwrappedElement.getClassNameForCompanionObject()
            if (classNameForCompanionObject != null) {
                queryParameters.optimizer.searchWord(
                        classNameForCompanionObject, effectiveSearchScope, UsageSearchContext.ANY, true, unwrappedElement, resultProcessor)
            }

            if (unwrappedElement is KtParameter && kotlinOptions.searchNamedArguments) {
                searchNamedArguments(unwrappedElement)
            }

            if (!(unwrappedElement is KtElement && isOnlyKotlinSearch(effectiveSearchScope))) {
                searchLightElements(element)
            }

            if (element is KtFunction || element is PsiMethod) {
                val referenceSearcher = OperatorReferenceSearcher.create(
                        element, effectiveSearchScope, consumer, queryParameters.optimizer, kotlinOptions)
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
            val function = parameter.ownerFunction ?: return
            if (function.nameAsName?.isSpecial ?: true) return
            val project = function.project
            var namedArgsScope = function.useScope.intersectWith(queryParameters.scopeDeterminedByUser)

            if (namedArgsScope is GlobalSearchScope) {
                namedArgsScope = KotlinSourceFilterScope.sourcesAndLibraries(namedArgsScope, project)

                val filesWithFunctionName = CacheManager.SERVICE.getInstance(project).getVirtualFilesWithWord(
                        function.name!!, UsageSearchContext.IN_CODE, namedArgsScope, true)
                namedArgsScope = GlobalSearchScope.filesScope(project, filesWithFunctionName.asList())
            }

            val processor = KotlinRequestResultProcessor(parameter, filter = { it.isNamedArgumentReference() })
            queryParameters.optimizer.searchWord(parameterName,
                                                 namedArgsScope,
                                                 KOTLIN_NAMED_ARGUMENT_SEARCH_CONTEXT,
                                                 true,
                                                 parameter,
                                                 processor)
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
                    }
                    else if (declaration is KtPropertyAccessor) {
                        val property = declaration.getStrictParentOfType<KtProperty>()
                        searchNamedElement(property)
                    }
                    else if (declaration is KtFunction) {
                        processStaticsFromCompanionObject(declaration)
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
                        val lightDeclarations: List<KtLightElement<*, *>?> =
                                originLightClass.methods.map { it as? KtLightMethod } +
                                originLightClass.fields.map { it as? KtLightField }

                        for (declaration in element.declarations) {
                            lightDeclarations
                                    .firstOrNull { it?.kotlinOrigin == declaration }
                                    ?.let { searchNamedElement(it) }
                        }
                    }
                }
            }
        }

        private fun searchDataClassComponentUsages(containingClass: PsiClass?,
                                                   componentFunctionDescriptor: FunctionDescriptor,
                                                   kotlinOptions: KotlinReferencesSearchOptions
        ) {
            val componentFunction = containingClass?.methods?.firstOrNull {
                it.name == componentFunctionDescriptor.name.asString() && it.parameterList.parametersCount == 0
            }
            if (componentFunction != null) {
                searchNamedElement(componentFunction)

                val searcher = OperatorReferenceSearcher.create(
                        componentFunction, queryParameters.effectiveSearchScope, consumer, queryParameters.optimizer, kotlinOptions)
                longTasks.add { searcher!!.run() }
            }
        }

        private fun isOnlyKotlinSearch(searchScope: SearchScope): Boolean {
            return searchScope is LocalSearchScope && searchScope.scope.all { it.containingFile is KtFile }
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

        private fun searchNamedElement(element: PsiNamedElement?, name: String? = element?.name) {
            if (name != null && element != null) {
                val scope = queryParameters.effectiveSearchScope(element)
                val context = UsageSearchContext.IN_CODE + UsageSearchContext.IN_FOREIGN_LANGUAGES + UsageSearchContext.IN_COMMENTS
                val resultProcessor = KotlinRequestResultProcessor(element,
                                                                   queryParameters.elementToSearch.namedUnwrappedElement ?: element,
                                                                   options = kotlinOptions)
                queryParameters.optimizer.searchWord(name, scope, context.toShort(), true, element, resultProcessor)
            }
        }

        private fun PsiReference.isNamedArgumentReference(): Boolean {
            return this is KtSimpleNameReference && expression.parent is KtValueArgumentName
        }
    }
}
