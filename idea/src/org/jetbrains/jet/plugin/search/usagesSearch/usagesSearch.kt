/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.search.usagesSearch

import com.intellij.psi.PsiReference
import com.intellij.util.QueryFactory
import com.intellij.psi.search.SearchScope
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchRequestCollector
import com.intellij.psi.search.SearchSession
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.util.Query
import com.intellij.util.MergeQuery
import com.intellij.psi.search.SearchRequestQuery
import com.intellij.util.UniqueResultsQuery
import com.intellij.psi.search.searches.ReferenceDescriptor
import com.intellij.util.containers.ContainerUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.PsiElement
import com.intellij.psi.search.RequestResultProcessor
import com.intellij.util.Processor
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReferenceService
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.ReferenceRange
import java.util.Collections
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import java.util.ArrayList
import com.intellij.util.EmptyQuery
import com.intellij.openapi.project.Project
import java.util.ArrayDeque
import org.jetbrains.jet.plugin.search.usagesSearch.UsagesSearchFilter.*
import org.jetbrains.jet.plugin.search.and

public data class UsagesSearchLocation(
        val inCode: Boolean = true,
        val inComments: Boolean = false,
        val inStrings: Boolean = false,
        val inPlainText: Boolean = false
) {
    class object {
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
        val scope: SearchScope,
        val location: UsagesSearchLocation = UsagesSearchLocation.DEFAULT,
        val restrictByTargetScope: Boolean = true
)

public trait UsagesSearchFilter {
    class object {
        object False: UsagesSearchFilter {
            override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = false
        }

        object True: UsagesSearchFilter {
            override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = true
        }
    }

    fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean
}

public data class UsagesSearchRequestItem(
        val target: UsagesSearchTarget<PsiElement>,
        val words: List<String>,
        val filter: UsagesSearchFilter
)

public data class UsagesSearchRequest(val project: Project, val items: List<UsagesSearchRequestItem>) {
    val collector: SearchRequestCollector = SearchRequestCollector(SearchSession())
}

public object UsagesSearch: QueryFactory<PsiReference, UsagesSearchRequest>() {
    {
        class ResultProcessorImpl(private val node: UsagesSearchRequestItem) : RequestResultProcessor() {
            private val referenceService = PsiReferenceService.getService()!!

            override fun processTextOccurrence(element: PsiElement, offsetInElement: Int, consumer: Processor<PsiReference>): Boolean {
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

        object ExecutorImpl: QueryExecutorBase<PsiReference, UsagesSearchRequest>() {
            override fun processQuery(request: UsagesSearchRequest, consumer: Processor<PsiReference>) {
                for (item in request.items) {
                    with (item) {
                        if (filter != False) {
                            ApplicationManager.getApplication()?.runReadAction {
                                for (word in words) {
                                    request.collector.searchWord(
                                            word, target.scope, target.location.searchContext, true, ResultProcessorImpl(item)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        registerExecutor(ExecutorImpl)
    }

    fun search(request: UsagesSearchRequest): Query<PsiReference> = with(request) {
        createQuery(this) merge SearchRequestQuery(project, collector).unique
    }
}

fun UsagesSearchRequest.search(): Query<PsiReference> = UsagesSearch.search(this)

fun <A: PsiElement, B: PsiElement> UsagesSearchTarget<A>.retarget(element: B) =
        UsagesSearchTarget(element, scope, location, restrictByTargetScope)

val <T: PsiElement> UsagesSearchTarget<T>.effectiveScope: SearchScope
    get() = if (restrictByTargetScope) scope and element.effectiveScope else scope

val PsiElement.effectiveScope: SearchScope
    get() = PsiSearchHelper.SERVICE.getInstance(getProject())!!.getUseScope(this)

fun <T> Query<T>.merge(that: Query<T>): Query<T> = MergeQuery(this, that)

val Query<PsiReference>.unique: Query<PsiReference>
    get() = UniqueResultsQuery(this, ContainerUtil.canonicalStrategy(), ReferenceDescriptor.MAPPER)