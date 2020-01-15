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
import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.find.findUsages.JavaFindUsagesHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.findUsages.KotlinCallableFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class DelegatingFindMemberUsagesHandler(
    val declaration: KtNamedDeclaration,
    private val elementsToSearch: Collection<PsiElement>,
    val factory: KotlinFindUsagesHandlerFactory
) : FindUsagesHandler(declaration) {
    private val kotlinHandler = KotlinFindMemberUsagesHandler.getInstance(declaration, elementsToSearch, factory)

    private data class HandlerAndOptions(
        val handler: FindUsagesHandler,
        val options: FindUsagesOptions?
    )

    private fun getHandlerAndOptions(element: PsiElement, options: FindUsagesOptions?): HandlerAndOptions? {
        return when (element) {
            is KtNamedDeclaration ->
                HandlerAndOptions(KotlinFindMemberUsagesHandler.getInstance(element, elementsToSearch, factory), options)

            is PsiMethod, is PsiParameter ->
                HandlerAndOptions(
                    JavaFindUsagesHandler(element, elementsToSearch.toTypedArray(), factory.javaHandlerFactory),
                    (options as KotlinCallableFindUsagesOptions?)?.toJavaOptions(project)
                )

            else -> null
        }
    }

    override fun getFindUsagesDialog(isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean): AbstractFindUsagesDialog {
        return getHandlerAndOptions(psiElement, null)?.handler?.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
            ?: super.getFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab)
    }

    override fun getPrimaryElements(): Array<PsiElement> {
        return kotlinHandler.primaryElements
    }

    override fun getSecondaryElements(): Array<out PsiElement> {
        return kotlinHandler.secondaryElements
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return kotlinHandler.getFindUsagesOptions(dataContext)
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val (handler, handlerOptions) = runReadAction { getHandlerAndOptions(element, options) } ?: return true
        return handler.processElementUsages(element, processor, handlerOptions!!)
    }

    override fun isSearchForTextOccurrencesAvailable(psiElement: PsiElement, isSingleFile: Boolean): Boolean = !isSingleFile
}
