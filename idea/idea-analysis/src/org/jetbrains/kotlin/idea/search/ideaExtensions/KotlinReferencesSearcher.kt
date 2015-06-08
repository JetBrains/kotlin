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
import com.intellij.openapi.util.Computable
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.idea.search.usagesSearch.*
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.*

public class KotlinReferencesSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {

    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        val element = queryParameters.getElementToSearch()

        val unwrappedElement = element.namedUnwrappedElement
        if (unwrappedElement == null || !ProjectRootsUtil.isInProjectOrLibSource(unwrappedElement)) return

        ApplicationManager.getApplication().runReadAction(object : Runnable {
            override fun run() {
                val searchHelper = KotlinPsiSearchHelper(queryParameters.getElementToSearch().getProject())
                val searchTarget = UsagesSearchTarget(unwrappedElement, queryParameters.getEffectiveSearchScope(), UsagesSearchLocation.EVERYWHERE, false)
                val requestItem = UsagesSearchRequestItem(searchTarget, unwrappedElement.getSpecialNamesToSearch(), isTargetUsage, null)
                searchHelper.processFilesWithText(requestItem, consumer)
            }
        })

        searchLightElements(queryParameters, element)
    }

    companion object {
        public fun processJetClassOrObject(element: JetClassOrObject, queryParameters: ReferencesSearch.SearchParameters) {
            val className = element.getName()
            if (className != null) {
                val lightClass = ApplicationManager.getApplication().runReadAction<PsiClass>(object : Computable<PsiClass?> {
                    override fun compute(): PsiClass? {
                        return LightClassUtil.getPsiClass(element)
                    }
                })
                if (lightClass != null) {
                    searchNamedElement(queryParameters, lightClass, className)

                    if (element is JetObjectDeclaration && element.isCompanion()) {
                        val fieldForCompanionObject = ApplicationManager.getApplication().runReadAction<PsiField>(object : Computable<PsiField?> {
                            override fun compute(): PsiField? {
                                return LightClassUtil.getLightFieldForCompanionObject(element)
                            }
                        })
                        if (fieldForCompanionObject != null) {
                            searchNamedElement(queryParameters, fieldForCompanionObject)
                        }
                    }
                }
            }
        }

        private fun searchLightElements(queryParameters: ReferencesSearch.SearchParameters, element: PsiElement) {
            if (element is JetClassOrObject) {
                processJetClassOrObject(element, queryParameters)
            }
            else if (element is JetNamedFunction || element is JetSecondaryConstructor) {
                val function = element as JetFunction
                val name = function.getName()
                if (name != null) {
                    val method = ApplicationManager.getApplication().runReadAction<PsiMethod>(object : Computable<PsiMethod?> {
                        override fun compute(): PsiMethod? {
                            return LightClassUtil.getLightClassMethod(function)
                        }
                    })
                    searchNamedElement(queryParameters, method)
                }
            }
            else if (element is JetProperty) {
                val propertyMethods = ApplicationManager.getApplication().runReadAction<LightClassUtil.PropertyAccessorsPsiMethods>(object : Computable<LightClassUtil.PropertyAccessorsPsiMethods> {
                    override fun compute(): LightClassUtil.PropertyAccessorsPsiMethods {
                        return LightClassUtil.getLightClassPropertyMethods(element)
                    }
                })

                searchNamedElement(queryParameters, propertyMethods.getGetter())
                searchNamedElement(queryParameters, propertyMethods.getSetter())
            }
            else if (element is KotlinLightMethod) {
                val declaration = element.getOrigin()
                if (declaration is JetProperty || (declaration is JetParameter && declaration.hasValOrVar())) {
                    searchNamedElement(queryParameters, declaration as PsiNamedElement)
                }
                else if (declaration is JetPropertyAccessor) {
                    searchNamedElement(queryParameters, PsiTreeUtil.getParentOfType<JetProperty>(declaration, javaClass<JetProperty>()))
                }
            }
        }

        private fun searchNamedElement(queryParameters: ReferencesSearch.SearchParameters, element: PsiNamedElement,
                                       name: String? = element.getName()) {
            if (name != null) {
                queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, element)
            }
        }
    }
}
