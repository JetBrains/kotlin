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

package org.jetbrains.kotlin.idea.findUsages.handlers

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.JavaFindUsagesHelper
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.FilteredQuery
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindClassUsagesDialog
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.search.usagesSearch.isConstructorUsage
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.effectiveDeclarations
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.util.*

class KotlinFindClassUsagesHandler(
    ktClass: KtClassOrObject,
    factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<KtClassOrObject>(ktClass, factory) {
    override fun getFindUsagesDialog(
        isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        return KotlinFindClassUsagesDialog(
            getElement(),
            project,
            factory.findClassOptions,
            toShowInNewTab,
            mustOpenInNewTab,
            isSingleFile,
            this
        )
    }

    override fun createSearcher(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Searcher {
        return MySearcher(element, processor, options)
    }

    private class MySearcher(
        element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions
    ) : Searcher(element, processor, options) {

        private val kotlinOptions = options as KotlinClassFindUsagesOptions
        private val referenceProcessor = createReferenceProcessor(processor)

        override fun buildTaskList(forHighlight: Boolean): Boolean {
            val classOrObject = element as KtClassOrObject

            if (kotlinOptions.isUsages || kotlinOptions.searchConstructorUsages) {
                processClassReferencesLater(classOrObject)
            }

            if (kotlinOptions.isFieldsUsages || kotlinOptions.isMethodsUsages) {
                processMemberReferencesLater(classOrObject)
            }

            if (kotlinOptions.isUsages && classOrObject is KtObjectDeclaration && classOrObject.isCompanion() && classOrObject in options.searchScope) {
                if (!processCompanionObjectInternalReferences(classOrObject)) return false
            }

            if (kotlinOptions.searchConstructorUsages) {
                classOrObject.toLightClass()?.constructors?.filterIsInstance<KtLightMethod>()?.forEach { constructor ->
                    val scope = constructor.useScope.intersectWith(options.searchScope)
                    var query = MethodReferencesSearch.search(constructor, scope, true)
                    if (kotlinOptions.isSkipImportStatements) {
                        query = FilteredQuery(query) { !it.isImportUsage() }
                    }
                    addTask { query.forEach(Processor { referenceProcessor.process(it) }) }
                }
            }

            if (kotlinOptions.isDerivedClasses || kotlinOptions.isDerivedInterfaces) {
                processInheritorsLater()
            }

            return true
        }

        private fun processInheritorsLater() {
            val request = HierarchySearchRequest(element, options.searchScope, kotlinOptions.isCheckDeepInheritance)
            addTask {
                request.searchInheritors().forEach(
                    PsiElementProcessorAdapter(
                        PsiElementProcessor<PsiClass> { element ->
                            runReadAction {
                                if (!element.isValid) return@runReadAction false
                                val isInterface = element.isInterface
                                when {
                                    isInterface && kotlinOptions.isDerivedInterfaces || !isInterface && kotlinOptions.isDerivedClasses ->
                                        processUsage(processor, element.navigationElement)

                                    else -> true
                                }
                            }
                        }
                    )
                )
            }
        }

        private fun processClassReferencesLater(classOrObject: KtClassOrObject) {
            val searchParameters = KotlinReferencesSearchParameters(
                classOrObject,
                scope = options.searchScope,
                kotlinOptions = KotlinReferencesSearchOptions(
                    acceptCompanionObjectMembers = true,
                    searchForExpectedUsages = kotlinOptions.searchExpected
                )
            )
            var usagesQuery = ReferencesSearch.search(searchParameters)

            if (kotlinOptions.isSkipImportStatements) {
                usagesQuery = FilteredQuery(usagesQuery) { !it.isImportUsage() }
            }

            if (!kotlinOptions.searchConstructorUsages) {
                usagesQuery = FilteredQuery(usagesQuery) { !it.isConstructorUsage(classOrObject) }
            } else if (!options.isUsages) {
                usagesQuery = FilteredQuery(usagesQuery) { it.isConstructorUsage(classOrObject) }
            }
            addTask { usagesQuery.forEach(referenceProcessor) }
        }

        private fun processCompanionObjectInternalReferences(companionObject: KtObjectDeclaration): Boolean {
            val klass = companionObject.getStrictParentOfType<KtClass>() ?: return true
            val companionObjectDescriptor = companionObject.descriptor
            return !klass.anyDescendantOfType(fun(element: KtElement): Boolean {
                if (element == companionObject) return false // skip companion object itself

                val bindingContext = element.analyze()
                val resolvedCall = bindingContext[BindingContext.CALL, element]?.getResolvedCall(bindingContext) ?: return false
                if ((resolvedCall.dispatchReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor
                    || (resolvedCall.extensionReceiver as? ImplicitClassReceiver)?.declarationDescriptor == companionObjectDescriptor
                ) {
                    return element.references.any { !referenceProcessor.process(it) }
                }
                return false
            })
        }

        private fun processMemberReferencesLater(classOrObject: KtClassOrObject) {
            for (declaration in classOrObject.effectiveDeclarations()) {
                if ((declaration is KtNamedFunction && kotlinOptions.isMethodsUsages) ||
                    ((declaration is KtProperty || declaration is KtParameter) && kotlinOptions.isFieldsUsages)
                ) {
                    addTask { ReferencesSearch.search(declaration, options.searchScope).forEach(referenceProcessor) }
                }
            }
        }
    }

    override fun getStringsToSearch(element: PsiElement): Collection<String> {
        val psiClass = when (element) {
            is PsiClass -> element
            is KtClassOrObject -> getElement().toLightClass()
            else -> null
        } ?: return Collections.emptyList()

        return JavaFindUsagesHelper.getElementNames(psiClass)
    }

    override fun isSearchForTextOccurrencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean {
        return !isSingleFile
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return factory.findClassOptions
    }
}
