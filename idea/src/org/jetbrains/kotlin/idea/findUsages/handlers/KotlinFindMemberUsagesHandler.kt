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

package org.jetbrains.kotlin.idea.findUsages.handlers

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector
import com.intellij.find.FindManager
import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.impl.FindManagerImpl
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.*
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.findUsages.KotlinCallableFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindFunctionUsagesDialog
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindPropertyUsagesDialog
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReadWriteAccessDetector
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchOptions
import org.jetbrains.kotlin.idea.search.ideaExtensions.KotlinReferencesSearchParameters
import org.jetbrains.kotlin.idea.search.usagesSearch.dataClassComponentFunction
import org.jetbrains.kotlin.idea.search.usagesSearch.isImportUsage
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.findOriginalTopMostOverriddenDescriptors
import org.jetbrains.kotlin.resolve.source.getPsi

abstract class KotlinFindMemberUsagesHandler<T : KtNamedDeclaration>
    protected constructor(declaration: T, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory)
    : KotlinFindUsagesHandler<T>(declaration, elementsToSearch, factory) {

    private class Function(declaration: KtFunction,
                           elementsToSearch: Collection<PsiElement>,
                           factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<KtFunction>(declaration, elementsToSearch, factory) {

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findFunctionOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            val options = factory.findFunctionOptions
            val lightMethod = getElement().toLightMethods().firstOrNull()
            if (lightMethod != null) {
                return KotlinFindFunctionUsagesDialog(lightMethod, project, options, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
            }

            return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
        }

        override fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions {
            val kotlinOptions = options as KotlinFunctionFindUsagesOptions
            return KotlinReferencesSearchOptions(true,
                                                 kotlinOptions.isIncludeOverloadUsages,
                                                 kotlinOptions.isIncludeOverloadUsages)
        }

        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
            val kotlinOptions = options as KotlinFunctionFindUsagesOptions
            return query
                .applyFilter(kotlinOptions.isSkipImportStatements) { !it.isImportUsage() }
        }
    }

    private class Property(declaration: KtNamedDeclaration, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<KtNamedDeclaration>(declaration, elementsToSearch, factory) {

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findPropertyOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            return KotlinFindPropertyUsagesDialog(getElement(), project, factory.findPropertyOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
        }

        override fun applyQueryFilters(element: PsiElement, options: FindUsagesOptions, query: Query<PsiReference>): Query<PsiReference> {
            val kotlinOptions = options as KotlinPropertyFindUsagesOptions

            if (!kotlinOptions.isReadAccess && !kotlinOptions.isWriteAccess) {
                return EmptyQuery()
            }

            val result = query
                    .applyFilter(kotlinOptions.isSkipImportStatements) { !it.isImportUsage() }

            if (!kotlinOptions.isReadAccess || !kotlinOptions.isWriteAccess) {
                val detector = KotlinReadWriteAccessDetector()

                return FilteredQuery(result) {
                    val access = detector.getReferenceAccess(element, it)
                    when (access) {
                        ReadWriteAccessDetector.Access.Read -> kotlinOptions.isReadAccess
                        ReadWriteAccessDetector.Access.Write -> kotlinOptions.isWriteAccess
                        ReadWriteAccessDetector.Access.ReadWrite -> kotlinOptions.isReadWriteAccess
                    }
                }
            }
            return result
        }

        override fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions {
            return KotlinReferencesSearchOptions(true, false, false)
        }
    }

    override fun createSearcher(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Searcher {
        return MySearcher(element, processor, options)
    }

    private inner class MySearcher(
            element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions
    ) : Searcher(element, processor, options) {

        private val kotlinOptions = options as KotlinCallableFindUsagesOptions

        override fun buildTaskList(): Boolean {
            val referenceProcessor = KotlinFindUsagesHandler.createReferenceProcessor(processor)
            val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

            if (options.isUsages) {
                val kotlinSearchOptions = createKotlinReferencesSearchOptions(options)
                val searchParameters = KotlinReferencesSearchParameters(element, options.searchScope, kotlinOptions = kotlinSearchOptions)

                applyQueryFilters(element, options, ReferencesSearch.search(searchParameters)).let { query ->
                    addTask { query.forEach(referenceProcessor) }
                }


                for (psiMethod in element.toLightMethods()) {
                    var searchScope = options.searchScope
                    // TODO: very bad code!! ReferencesSearch does not work correctly for constructors and annotation parameters
                    if (element is KtNamedFunction || (element is KtParameter && element.dataClassComponentFunction() != null)) {
                        searchScope = searchScope.excludeKotlinSources()
                    }
                    applyQueryFilters(element, options, MethodReferencesSearch.search(psiMethod, searchScope, true)).let { query ->
                        addTask { query.forEach(referenceProcessor) }
                    }
                }
            }

            if (kotlinOptions.searchOverrides) {
                addTask {
                    val overriders = HierarchySearchRequest(element, options.searchScope, true).searchOverriders()
                    overriders.all {
                        val element = runReadAction { it.takeIf { it.isValid }?.navigationElement } ?: return@all true
                        KotlinFindUsagesHandler.processUsage(uniqueProcessor, element)
                    }
                }
            }

            return true
        }
    }

    protected abstract fun createKotlinReferencesSearchOptions(options: FindUsagesOptions): KotlinReferencesSearchOptions

    protected abstract fun applyQueryFilters(element: PsiElement,
                                             options: FindUsagesOptions,
                                             query: Query<PsiReference>): Query<PsiReference>

    override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = !isSingleFile && psiElement !is KtParameter

    override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {
        val callableDescriptor = (target as? KtCallableDeclaration)?.resolveToDescriptorIfAny() as? CallableDescriptor
        val descriptorsToHighlight = if (callableDescriptor is ParameterDescriptor)
            listOf(callableDescriptor)
        else
            callableDescriptor?.findOriginalTopMostOverriddenDescriptors() ?: emptyList<CallableDescriptor>()

        val baseDeclarations = descriptorsToHighlight.map { it.source.getPsi() }.filter { it != null && it != target }

        return if (baseDeclarations.isNotEmpty()) {
            baseDeclarations.flatMap {
                val handler = (FindManager.getInstance(project) as FindManagerImpl).findUsagesManager.getFindUsagesHandler(it!!, true)
                handler?.findReferencesToHighlight(it, searchScope) ?: emptyList()
            }
        }
        else {
            super.findReferencesToHighlight(target, searchScope)
        }
    }

    companion object {

        fun getInstance(declaration: KtNamedDeclaration,
                               elementsToSearch: Collection<PsiElement> = emptyList(),
                               factory: KotlinFindUsagesHandlerFactory): KotlinFindMemberUsagesHandler<out KtNamedDeclaration> {
            return if (declaration is KtFunction)
                Function(declaration, elementsToSearch, factory)
            else
                Property(declaration, elementsToSearch, factory)
        }
    }
}


fun Query<PsiReference>.applyFilter(flag: Boolean, condition: (PsiReference) -> Boolean): Query<PsiReference> {
    return if (flag) FilteredQuery(this, condition) else this
}
