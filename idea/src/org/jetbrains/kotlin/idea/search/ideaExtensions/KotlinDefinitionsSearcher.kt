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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.DefinitionsScopedSearch
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.compatibility.ExecutorProcessor
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachImplementation
import org.jetbrains.kotlin.idea.search.declarationsSearch.forEachOverridingMethod
import org.jetbrains.kotlin.idea.search.declarationsSearch.toPossiblyFakeLightMethods
import org.jetbrains.kotlin.idea.util.actualsForExpected
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import java.util.*

class KotlinDefinitionsSearcher : QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
    override fun execute(queryParameters: DefinitionsScopedSearch.SearchParameters, consumer: ExecutorProcessor<PsiElement>): Boolean {
        val consumer = skipDelegatedMethodsConsumer(consumer)
        val element = queryParameters.element
        val scope = queryParameters.scope

        return when (element) {
            is KtClass -> {
                processClassImplementations(element, consumer) && processActualDeclarations(element, consumer)
            }

            is KtLightClass -> {
                val useScope = runReadAction { element.useScope }
                if (useScope is LocalSearchScope)
                    processLightClassLocalImplementations(element, useScope, consumer)
                else
                    true
            }

            is KtNamedFunction, is KtSecondaryConstructor -> {
                processFunctionImplementations(element as KtFunction, scope, consumer) && processActualDeclarations(element, consumer)
            }

            is KtProperty -> {
                processPropertyImplementations(element, scope, consumer) && processActualDeclarations(element, consumer)
            }

            is KtParameter -> {
                if (isFieldParameter(element)) {
                    processPropertyImplementations(element, scope, consumer) && processActualDeclarations(element, consumer)
                } else {
                    true
                }
            }

            else -> true
        }
    }

    companion object {

        private fun skipDelegatedMethodsConsumer(baseConsumer: ExecutorProcessor<PsiElement>): Processor<PsiElement> {
            return Processor { element ->
                if (isDelegated(element)) {
                    return@Processor true
                }
                baseConsumer.process(element)
            }
        }

        private fun isDelegated(element: PsiElement): Boolean = element is KtLightMethod && element.isDelegated

        private fun isFieldParameter(parameter: KtParameter): Boolean {
            return runReadAction { KtPsiUtil.getClassIfParameterIsProperty(parameter) != null }
        }

        private fun processClassImplementations(klass: KtClass, consumer: Processor<PsiElement>): Boolean {
            val psiClass = runReadAction { klass.toLightClass() ?: KtFakeLightClass(klass) }

            val searchScope = runReadAction { psiClass.useScope }
            if (searchScope is LocalSearchScope) {
                return processLightClassLocalImplementations(psiClass, searchScope, consumer)
            }

            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, true), consumer)
        }

        private fun processLightClassLocalImplementations(
            psiClass: KtLightClass,
            searchScope: LocalSearchScope,
            consumer: Processor<PsiElement>
        ): Boolean {
            // workaround for IDEA optimization that uses Java PSI traversal to locate inheritors in local search scope
            val virtualFiles = searchScope.scope.mapTo(HashSet()) { it.containingFile.virtualFile }
            val globalScope = GlobalSearchScope.filesScope(psiClass.project, virtualFiles)
            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, globalScope, true)) { candidate ->
                val candidateOrigin = candidate.unwrapped ?: candidate
                if (candidateOrigin in searchScope) {
                    consumer.process(candidate)
                } else {
                    true
                }
            }
        }

        private fun processFunctionImplementations(function: KtFunction, scope: SearchScope, consumer: Processor<PsiElement>): Boolean {
            return runReadAction {
                function.toPossiblyFakeLightMethods().firstOrNull()?.forEachImplementation(scope, consumer::process) ?: true
            }
        }

        private fun processPropertyImplementations(
            declaration: KtNamedDeclaration,
            scope: SearchScope,
            consumer: Processor<PsiElement>
        ): Boolean {
            return runReadAction {
                processPropertyImplementationsMethods(declaration.toPossiblyFakeLightMethods(), scope, consumer)
            }
        }

        private fun processActualDeclarations(declaration: KtDeclaration, consumer: Processor<PsiElement>): Boolean {
            return runReadAction {
                if (!declaration.isExpectDeclaration()) true
                else declaration.actualsForExpected().all(consumer::process)
            }
        }

        fun processPropertyImplementationsMethods(
            accessors: Iterable<PsiMethod>,
            scope: SearchScope,
            consumer: Processor<PsiElement>
        ): Boolean {
            return accessors.all { method ->
                method.forEachOverridingMethod(scope) { implementation ->
                    if (isDelegated(implementation)) return@forEachOverridingMethod true

                    val elementToProcess = runReadAction {
                        val mirrorElement = (implementation as? KtLightMethod)?.kotlinOrigin
                        when (mirrorElement) {
                            is KtProperty, is KtParameter -> mirrorElement
                            is KtPropertyAccessor -> if (mirrorElement.parent is KtProperty) mirrorElement.parent else implementation
                            else -> implementation
                        }
                    }

                    consumer.process(elementToProcess)
                }
            }
        }
    }
}
