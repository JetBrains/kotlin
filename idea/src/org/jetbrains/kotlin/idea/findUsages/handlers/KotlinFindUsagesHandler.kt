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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.usageView.UsageInfo
import com.intellij.util.CommonProcessors
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.findUsages.KotlinFindUsagesHandlerFactory
import org.jetbrains.kotlin.idea.findUsages.KotlinReferenceUsageInfo
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.util.*

abstract class KotlinFindUsagesHandler<T : PsiElement>(psiElement: T,
                                                              private val elementsToSearch: Collection<PsiElement>,
                                                              val factory: KotlinFindUsagesHandlerFactory)
    : FindUsagesHandler(psiElement) {

    @Suppress("UNCHECKED_CAST") fun getElement(): T {
        return psiElement as T
    }

    constructor(psiElement: T, factory: KotlinFindUsagesHandlerFactory) : this(psiElement, emptyList(), factory) {
    }

    override fun getPrimaryElements(): Array<PsiElement> {
        return if (elementsToSearch.isEmpty())
            arrayOf(psiElement)
        else
            elementsToSearch.toTypedArray()
    }

    protected fun searchTextOccurrences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val scope = options.searchScope

        if (options.isSearchForTextOccurrences && scope is GlobalSearchScope) {
            if (options.fastTrack == null) {
                return processUsagesInText(element, processor, scope)
            }
            options.fastTrack.searchCustom {
                processUsagesInText(element, processor, scope)
            }
        }
        return true
    }

    override fun processElementUsages(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        return searchReferences(element, processor, options) && searchTextOccurrences(element, processor, options)
    }

    protected fun searchReferences(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Boolean {
        val searcher = createSearcher(element, processor, options)
        if (!runReadAction { searcher.buildTaskList() }) return false
        return searcher.executeTasks()
    }

    protected abstract fun createSearcher(element: PsiElement, processor: Processor<UsageInfo>, options: FindUsagesOptions): Searcher

    override fun findReferencesToHighlight(target: PsiElement, searchScope: SearchScope): Collection<PsiReference> {
        val results = Collections.synchronizedList(arrayListOf<PsiReference>())
        val options = findUsagesOptions.clone()
        options.searchScope = searchScope
        searchReferences(target, object : Processor<UsageInfo> {
            override fun process(info: UsageInfo): Boolean {
                val reference = info.reference
                if (reference != null) {
                    results.add(reference)
                }
                return true
            }
        }, options)
        return results
    }

    protected abstract class Searcher(val element: PsiElement, val processor: Processor<UsageInfo>, val options: FindUsagesOptions) {
        private val tasks = ArrayList<() -> Boolean>()

        /**
         * Adds a time-consuming operation to be executed outside read-action
         */
        protected fun addTask(task: () -> Boolean) {
            tasks.add(task)
        }

        /**
         * Invoked outside read-action
         */
        fun executeTasks(): Boolean {
            return tasks.all { it() }
        }

        /**
         * Invoked under read-action, should use [addTask] for all time-consuming operations
         */
        abstract fun buildTaskList(): Boolean
    }

    companion object {
        val LOG = Logger.getInstance(KotlinFindUsagesHandler::class.java)

        internal fun processUsage(processor: Processor<UsageInfo>, ref: PsiReference): Boolean =
            processor.processIfNotNull { if (ref.element.isValid) KotlinReferenceUsageInfo(ref) else null }

        internal fun processUsage(processor: Processor<UsageInfo>, element: PsiElement): Boolean =
            processor.processIfNotNull { if (element.isValid) UsageInfo(element) else null }

        private fun Processor<UsageInfo>.processIfNotNull(callback: () -> UsageInfo?): Boolean {
            val usageInfo = runReadAction(callback)
            return if (usageInfo != null) process(usageInfo) else true
        }

        internal fun createReferenceProcessor(usageInfoProcessor: Processor<UsageInfo>): Processor<PsiReference> {
            val uniqueProcessor = CommonProcessors.UniqueProcessor(usageInfoProcessor)

            return Processor { KotlinFindUsagesHandler.processUsage(uniqueProcessor, it) }
        }
    }
}
