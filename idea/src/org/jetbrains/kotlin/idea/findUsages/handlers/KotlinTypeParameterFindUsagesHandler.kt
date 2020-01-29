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
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.dialogs.KotlinTypeParameterFindUsagesDialog
import org.jetbrains.kotlin.psi.KtNamedDeclaration

class KotlinTypeParameterFindUsagesHandler(
    element: KtNamedDeclaration,
    factory: KotlinFindUsagesHandlerFactory
) : KotlinFindUsagesHandler<KtNamedDeclaration>(element, factory) {
    override fun getFindUsagesDialog(
        isSingleFile: Boolean, toShowInNewTab: Boolean, mustOpenInNewTab: Boolean
    ): AbstractFindUsagesDialog {
        return KotlinTypeParameterFindUsagesDialog<KtNamedDeclaration>(
            getElement(), project, findUsagesOptions, toShowInNewTab, mustOpenInNewTab, isSingleFile, this
        )
    }

    override fun createSearcher(element: PsiElement, processor: UsageInfoProcessor, options: FindUsagesOptions): Searcher {
        return object : Searcher(element, processor, options) {
            override fun buildTaskList(forHighlight: Boolean): Boolean {
                addTask {
                    ReferencesSearch.search(element, options.searchScope).all { processUsage(processor, it) }
                }
                return true
            }
        }
    }

    override fun getFindUsagesOptions(dataContext: DataContext?): FindUsagesOptions {
        return factory.defaultOptions
    }
}
