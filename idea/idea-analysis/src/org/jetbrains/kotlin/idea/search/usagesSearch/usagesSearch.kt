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

package org.jetbrains.kotlin.idea.search.usagesSearch

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.cache.impl.id.IdIndex
import com.intellij.psi.impl.cache.impl.id.IdIndexEntry
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import com.intellij.psi.search.*
import com.intellij.psi.search.searches.ReferenceDescriptor
import com.intellij.util.*
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.kotlin.idea.search.and
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchFilter.False
import org.jetbrains.kotlin.idea.util.application.runReadAction
import java.util.Collections

public data class UsagesSearchLocation(
        val inCode: Boolean = true,
        val inComments: Boolean = false,
        val inStrings: Boolean = false,
        val inPlainText: Boolean = true
) {
    companion object {
        public val DEFAULT: UsagesSearchLocation = UsagesSearchLocation()
        public val EVERYWHERE: UsagesSearchLocation = UsagesSearchLocation(true, true, true, true)
    }

    val searchContext: Short
        get() {
            var res = 0

            if (inCode) res += UsageSearchContext.IN_CODE + UsageSearchContext.IN_FOREIGN_LANGUAGES
            if (inComments) res += UsageSearchContext.IN_COMMENTS
            if (inStrings) res += UsageSearchContext.IN_STRINGS
            if (inPlainText) res += UsageSearchContext.IN_PLAIN_TEXT

            return res.toShort()
        }
}

public data class UsagesSearchTarget<out T : PsiElement>(
        val element: T,
        private val scope: SearchScope = element.getUseScope(),
        val location: UsagesSearchLocation = UsagesSearchLocation.DEFAULT,
        val restrictByTargetScope: Boolean = true
) {
    fun <U: PsiElement> withTarget(element: U) =
            UsagesSearchTarget(element, scope, location, restrictByTargetScope)

    fun withScope(scope: SearchScope) =
            UsagesSearchTarget(element, scope, location, restrictByTargetScope)

    val effectiveScope: SearchScope
        get() = if (restrictByTargetScope) scope and element.effectiveScope else scope
}

public interface UsagesSearchFilter {
    object False: UsagesSearchFilter {
        override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = false
    }

    object True: UsagesSearchFilter {
        override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = true
    }

    fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean
}

public class UsagesSearchRequestItem(
        val target: UsagesSearchTarget<PsiElement>,
        val words: List<String>,
        val filter: UsagesSearchFilter,
        val additionalSearchDelegate: ((Processor<PsiReference>) -> Boolean)? = null,
        val additionalFileFilters: Collection<AdditionalFileFilter> = emptyList()
)

public class AdditionalFileFilter(
        val word: String,
        val searchContext: Short,
        val caseSensitively: Boolean
)

public data class UsagesSearchRequest(val project: Project, val items: List<UsagesSearchRequestItem>) {
    val collector: SearchRequestCollector = SearchRequestCollector(SearchSession())
}

public class KotlinPsiSearchHelper(private val project: Project): PsiSearchHelperImpl(PsiManager.getInstance(project) as PsiManagerEx) {
    class ResultTextProcessorImpl(
            private val node: UsagesSearchRequestItem,
            private val consumer: Processor<PsiReference>
    ): TextOccurenceProcessor {
        private val referenceService = PsiReferenceService.getService()!!

        override fun execute(element: PsiElement, offsetInElement: Int): Boolean {
            return referenceService.getReferences(element, PsiReferenceService.Hints.NO_HINTS).all { ref ->
                ProgressManager.checkCanceled()

                when {
                    !ReferenceRange.containsOffsetInElement(ref, offsetInElement) -> true
                    !node.filter.accepts(ref, node) -> true
                    else -> consumer.process(ref)
                }
            }
        }
    }

    override fun processFilesWithText(
            scope: GlobalSearchScope,
            searchContext: Short,
            caseSensitively: Boolean,
            text: String,
            processor: Processor<VirtualFile>
    ): Boolean {
        if (text !in ALL_SEARCHABLE_OPERATION_PATTERNS) {
            return super.processFilesWithText(scope, searchContext, caseSensitively, text, processor)
        }

        val entries = Collections.singletonList(IdIndexEntry(text, caseSensitively))
        val index = FileIndexFacade.getInstance(project)
        val checker: (Int?) -> Boolean = { (it!! and searchContext.toInt()) != 0 }
        return runReadAction {
            FileBasedIndex.getInstance().processFilesContainingAllKeys(IdIndex.NAME, entries, scope, checker) { file ->
                !index.shouldBeFound(scope, file) || processor.process(file)
            }
        }
    }

    public fun processFilesWithText(item: UsagesSearchRequestItem, consumer: Processor<PsiReference>): Boolean {
        var scope = runReadAction { item.target.effectiveScope }

        for (filter in item.additionalFileFilters) {
            if (scope !is GlobalSearchScope) break
            val processor = CommonProcessors.CollectProcessor<VirtualFile>()
            processFilesWithText(scope, filter.searchContext, filter.caseSensitively, filter.word, processor)
            if (processor.results.isEmpty()) return true
            scope = GlobalSearchScope.filesScope(project, processor.results)
        }

        return item.words.all { word ->
            val textProcessor = ResultTextProcessorImpl(item, consumer)
            processElementsWithWord(textProcessor, scope, word, item.target.location.searchContext, true)
        }
    }
}

public object UsagesSearch: QueryFactory<PsiReference, UsagesSearchRequest>() {
    init {
        val executorImpl = object : QueryExecutorBase<PsiReference, UsagesSearchRequest>() {
            override fun processQuery(request: UsagesSearchRequest, consumer: Processor<PsiReference>) {
                val searchHelper = KotlinPsiSearchHelper(request.project)
                request.items.filter { it.filter != False }.all {
                    searchHelper.processFilesWithText(it, consumer) && (it.additionalSearchDelegate?.invoke(consumer) ?: true) }
            }
        }

        registerExecutor(executorImpl)
    }

    fun search(request: UsagesSearchRequest): Query<PsiReference> = with(request) {
        createQuery(this) merge SearchRequestQuery(project, collector).unique
    }
}

fun UsagesSearchRequest.search(): Query<PsiReference> = UsagesSearch.search(this)

val PsiElement.effectiveScope: SearchScope
    get() = PsiSearchHelper.SERVICE.getInstance(getProject())!!.getUseScope(this)

fun <T> Query<T>.merge(that: Query<T>): Query<T> = MergeQuery(this, that)

val Query<PsiReference>.unique: Query<PsiReference>
    get() = UniqueResultsQuery(this, ContainerUtil.canonicalStrategy(), ReferenceDescriptor.MAPPER)
