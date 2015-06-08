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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.idea.references.matchesTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*

public class KotlinReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val element = queryParameters.getElementToSearch()

        val unwrappedElement = element.namedUnwrappedElement
        if (unwrappedElement == null || !ProjectRootsUtil.isInProjectOrLibSource(unwrappedElement)) return

        val words = unwrappedElement.getSpecialNamesToSearch()

        val resultProcessor = object : RequestResultProcessor() {
            private val referenceService = PsiReferenceService.getService()

            override fun processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Processor<PsiReference>): Boolean {
                return referenceService.getReferences(element, PsiReferenceService.Hints.NO_HINTS).all { ref ->
                    ProgressManager.checkCanceled()

                    when {
                        !ReferenceRange.containsOffsetInElement(ref, offsetInElement) -> true
                        !ref.isReferenceTo(unwrappedElement) -> true
                        else -> consumer.process(ref)
                    }
                }
            }
        }

        words.forEach { word ->
            queryParameters.getOptimizer().searchWord(word, queryParameters.getEffectiveSearchScope(),
                                                      UsagesSearchLocation.EVERYWHERE.searchContext, true, unwrappedElement,
                                                      resultProcessor)
        }

        searchLightElements(queryParameters, element)
    }

    companion object {
        public fun processJetClassOrObject(element: JetClassOrObject, queryParameters: ReferencesSearch.SearchParameters) {
            val className = element.getName()
            if (className != null) {
                val lightClass = runReadAction { LightClassUtil.getPsiClass(element) }
                if (lightClass != null) {
                    searchNamedElement(queryParameters, lightClass, className)

                    if (element is JetObjectDeclaration && element.isCompanion()) {
                        val fieldForCompanionObject = runReadAction { LightClassUtil.getLightFieldForCompanionObject(element) }
                        if (fieldForCompanionObject != null) {
                            searchNamedElement(queryParameters, fieldForCompanionObject)
                        }
                    }
                }
            }
        }

        private fun searchLightElements(queryParameters: ReferencesSearch.SearchParameters, element: PsiElement) {
            when (element) {
                is JetClassOrObject -> processJetClassOrObject(element, queryParameters)
                is JetNamedFunction, is JetSecondaryConstructor -> {
                    val function = element as JetFunction
                    val name = function.getName()
                    if (name != null) {
                        val method = runReadAction { LightClassUtil.getLightClassMethod(function) }
                        searchNamedElement(queryParameters, method)
                    }
                }
                is JetProperty -> {
                    val propertyMethods = runReadAction { LightClassUtil.getLightClassPropertyMethods(element) }
                    searchNamedElement(queryParameters, propertyMethods.getGetter())
                    searchNamedElement(queryParameters, propertyMethods.getSetter())
                }
                is KotlinLightMethod -> {
                    val declaration = element.getOrigin()
                    if (declaration is JetProperty || (declaration is JetParameter && declaration.hasValOrVar())) {
                        searchNamedElement(queryParameters, declaration as PsiNamedElement)
                    }
                    else if (declaration is JetPropertyAccessor) {
                        searchNamedElement(queryParameters, PsiTreeUtil.getParentOfType<JetProperty>(declaration, javaClass<JetProperty>()))
                    }
                }
            }
        }

        private fun searchNamedElement(queryParameters: ReferencesSearch.SearchParameters, element: PsiNamedElement?,
                                       name: String? = element?.getName()) {
            if (name != null && element != null) {
                queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, element)
            }
        }
    }
}
