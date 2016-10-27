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

import com.intellij.codeInsight.navigation.MethodImplementationsSearch
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.*

import java.util.ArrayList

import org.jetbrains.kotlin.asJava.toLightClass

class KotlinDefinitionsSearcher : QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
    override fun execute(queryParameters: DefinitionsScopedSearch.SearchParameters, consumer: Processor<PsiElement>): Boolean {
        var consumer = consumer
        val element = queryParameters.element
        val scope = queryParameters.scope
        consumer = skipDelegatedMethodsConsumer(consumer)

        if (element is KtClass) {
            return processClassImplementations(element, consumer)
        }

        if (element is KtNamedFunction || element is KtSecondaryConstructor) {
            return processFunctionImplementations(element as KtFunction, scope, consumer)
        }

        if (element is KtProperty) {
            return processPropertyImplementations(element, scope, consumer)
        }

        if (element is KtParameter) {

            if (isFieldParameter(element)) {
                return processPropertyImplementations(element, scope, consumer)
            }
        }

        return true
    }

    companion object {

        private fun skipDelegatedMethodsConsumer(baseConsumer: Processor<PsiElement>): Processor<PsiElement> {
            return Processor { element ->
                if (isDelegated(element)) {
                    return@Processor true
                }
                baseConsumer.process(element)
            }
        }

        private fun isDelegated(element: PsiElement): Boolean {
            return element is KtLightMethod && element.isDelegated
        }

        private fun isFieldParameter(parameter: KtParameter): Boolean {
            return ApplicationManager.getApplication().runReadAction(
                    Computable { KtPsiUtil.getClassIfParameterIsProperty(parameter) != null })
        }

        private fun processClassImplementations(klass: KtClass, consumer: Processor<PsiElement>): Boolean {
            val psiClass = ApplicationManager.getApplication().runReadAction(Computable<com.intellij.psi.PsiClass> { klass.toLightClass() })
            if (psiClass != null) {
                return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, true), consumer)
            }
            return true
        }

        private fun processFunctionImplementations(function: KtFunction, scope: SearchScope, consumer: Processor<PsiElement>): Boolean {
            val psiMethod = ApplicationManager.getApplication().runReadAction(Computable<com.intellij.psi.PsiMethod> { LightClassUtil.getLightClassMethod(function) })

            if (psiMethod != null) {
                MethodImplementationsSearch.processImplementations(psiMethod, consumer, scope)
            }

            return true
        }

        private fun processPropertyImplementations(parameter: KtParameter, scope: SearchScope, consumer: Processor<PsiElement>): Boolean {
            val accessorsPsiMethods = ApplicationManager.getApplication().runReadAction<LightClassUtil.PropertyAccessorsPsiMethods> { LightClassUtil.getLightClassPropertyMethods(parameter) }

            return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer)
        }

        private fun processPropertyImplementations(property: KtProperty, scope: SearchScope, consumer: Processor<PsiElement>): Boolean {
            val accessorsPsiMethods = ApplicationManager.getApplication().runReadAction<LightClassUtil.PropertyAccessorsPsiMethods> { LightClassUtil.getLightClassPropertyMethods(property) }

            return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer)
        }

        fun processPropertyImplementationsMethods(accessors: LightClassUtil.PropertyAccessorsPsiMethods, scope: SearchScope, consumer: Processor<PsiElement>): Boolean {
            for (method in accessors) {
                val implementations = ArrayList<PsiMethod>()
                MethodImplementationsSearch.getOverridingMethods(method, implementations, scope)

                for (implementation in implementations) {
                    if (isDelegated(implementation)) continue

                    val mirrorElement = if (implementation is KtLightMethod)
                        implementation.kotlinOrigin
                    else
                        null
                    if (mirrorElement is KtProperty || mirrorElement is KtParameter) {
                        if (!consumer.process(mirrorElement)) {
                            return false
                        }
                    }
                    else if (mirrorElement is KtPropertyAccessor && mirrorElement.parent is KtProperty) {
                        if (!consumer.process(mirrorElement.parent)) {
                            return false
                        }
                    }
                    else {
                        if (!consumer.process(implementation)) {
                            return false
                        }
                    }
                }
            }
            return true
        }
    }
}
