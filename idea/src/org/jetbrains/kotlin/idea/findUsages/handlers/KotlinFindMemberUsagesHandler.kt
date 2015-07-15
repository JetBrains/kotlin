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

import com.intellij.find.findUsages.AbstractFindUsagesDialog
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.search.PsiElementProcessor
import com.intellij.psi.search.PsiElementProcessorAdapter
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.findUsages.*
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindFunctionUsagesDialog
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinFindPropertyUsagesDialog
import org.jetbrains.kotlin.idea.search.declarationsSearch.*
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearch
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchRequest
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import java.util.Collections

public abstract class KotlinFindMemberUsagesHandler<T : JetNamedDeclaration>
    protected constructor(declaration: T, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory)
    : KotlinFindUsagesHandler<T>(declaration, elementsToSearch, factory) {

    private class Function(declaration: JetFunction,
                           elementsToSearch: Collection<PsiElement>,
                           factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<JetFunction>(declaration, elementsToSearch, factory) {

        override fun getSearchHelper(options: KotlinCallableFindUsagesOptions): UsagesSearchHelper<JetFunction> {
            return (options as KotlinFunctionFindUsagesOptions).toHelper()
        }

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findFunctionOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            val options = factory.findFunctionOptions
            val lightMethod = getElement().toLightMethods().firstOrNull()
            if (lightMethod != null) {
                return KotlinFindFunctionUsagesDialog(lightMethod, getProject(), options, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
            }

            return super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
        }
    }

    private class Property(declaration: JetNamedDeclaration, elementsToSearch: Collection<PsiElement>, factory: KotlinFindUsagesHandlerFactory) : KotlinFindMemberUsagesHandler<JetNamedDeclaration>(declaration, elementsToSearch, factory) {

        override fun getSearchHelper(options: KotlinCallableFindUsagesOptions): UsagesSearchHelper<JetNamedDeclaration> {
            return (options as KotlinPropertyFindUsagesOptions).toHelper()
        }

        override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions = factory.findPropertyOptions

        override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
            return KotlinFindPropertyUsagesDialog(getElement(), getProject(), factory.findPropertyOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this)
        }
    }

    protected abstract fun getSearchHelper(options: KotlinCallableFindUsagesOptions): UsagesSearchHelper<T>

    override fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val kotlinOptions = options as KotlinCallableFindUsagesOptions

        val request = runReadAction {
            @suppress("UNCHECKED_CAST")
            getSearchHelper(kotlinOptions).newRequest(options.toSearchTarget<T>(element as T, true))
        }

        val uniqueProcessor = CommonProcessors.UniqueProcessor(processor)

        for (ref in UsagesSearch.search(request)) {
            KotlinFindUsagesHandler.processUsage(uniqueProcessor, ref)
        }

        val psiMethod: PsiMethod? = when (element) {
            is PsiMethod -> element
            is JetConstructor<*> -> LightClassUtil.getLightClassMethod(element as JetFunction)
            else -> null
        }
        if (psiMethod != null) {
            for (ref in MethodReferencesSearch.search(psiMethod, options.searchScope, true)) {
                KotlinFindUsagesHandler.processUsage(uniqueProcessor, ref.getElement())
            }
        }

        if (kotlinOptions.searchOverrides) {
            for (method in HierarchySearchRequest(element, options.searchScope, true).searchOverriders()) {
                if (!KotlinFindUsagesHandler.processUsage(uniqueProcessor, method.getNavigationElement())) break
            }
        }

        return true
    }

    override fun isSearchForTextOccurencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = !isSingleFile

    companion object {

        public fun getInstance(declaration: JetNamedDeclaration,
                               elementsToSearch: Collection<PsiElement> = emptyList(),
                               factory: KotlinFindUsagesHandlerFactory): KotlinFindMemberUsagesHandler<out JetNamedDeclaration> {
            return if (declaration is JetFunction)
                Function(declaration, elementsToSearch, factory)
            else
                Property(declaration, elementsToSearch, factory)
        }
    }
}
