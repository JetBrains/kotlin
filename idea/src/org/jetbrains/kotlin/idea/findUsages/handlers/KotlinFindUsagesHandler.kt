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

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory

import java.util.ArrayList
import java.util.Collections

public abstract class KotlinFindUsagesHandler<T : PsiElement>(psiElement: T, private val elementsToSearch: Collection<PsiElement>, public val factory: KotlinFindUsagesHandlerFactory) : FindUsagesHandler(psiElement) {

    @suppress("UNCHECKED_CAST")
    public fun getElement(): T {
        return getPsiElement() as T
    }

    public constructor(psiElement: T, factory: KotlinFindUsagesHandlerFactory) : this(psiElement, emptyList<PsiElement>(), factory) {
    }

    override fun getPrimaryElements(): Array<PsiElement> {
        return if (elementsToSearch.isEmpty())
            arrayOf(getPsiElement())
        else
            elementsToSearch.toTypedArray()
    }

    protected fun searchTextOccurrences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val scope = options.searchScope

        val searchText = options.isSearchForTextOccurrences && scope is GlobalSearchScope

        if (searchText) {
            if (options.fastTrack != null) {
                options.fastTrack.searchCustom(object : Processor<Processor<PsiReference>> {
                    override fun process(consumer: Processor<PsiReference>): Boolean {
                        return processUsagesInText(element, processor, scope as GlobalSearchScope)
                    }
                })
            }
            else {
                return processUsagesInText(element, processor, scope as GlobalSearchScope)
            }
        }
        return true
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        return searchReferences(element, processor, options) && searchTextOccurrences(element, processor, options)
    }

    protected abstract fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean

    override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {
        val results = ArrayList<PsiReference>()
        val options = getFindUsagesOptions()
        options.searchScope = searchScope
        searchReferences(target, object : Processor<UsageInfo> {
            override fun process(info: UsageInfo): Boolean {
                val reference = info.getReference()
                if (reference != null) {
                    results.add(reference)
                }
                return true
            }
        }, options)
        return results
    }

    companion object {

        protected fun processUsage(processor: Processor<UsageInfo>, ref: PsiReference?): Boolean {
            if (ref == null) return true
            val rangeInElement = ref.getRangeInElement()
            return processor.process(UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false))
        }

        protected fun processUsage(processor: Processor<UsageInfo>, element: PsiElement): Boolean {
            return processor.process(UsageInfo(element))
        }
    }
}
