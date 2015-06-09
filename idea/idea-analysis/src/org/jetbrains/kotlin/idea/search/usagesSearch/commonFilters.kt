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

import com.intellij.psi.PsiReference
import com.intellij.psi.PsiNamedElement
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchFilter.*

class OrFilter(val filter1: UsagesSearchFilter, val filter2: UsagesSearchFilter): UsagesSearchFilter {
    override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean =
            filter1.accepts(ref, item) || filter2.accepts(ref, item)
}

class AndFilter(val filter1: UsagesSearchFilter, val filter2: UsagesSearchFilter): UsagesSearchFilter {
    override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean =
            filter1.accepts(ref, item) && filter2.accepts(ref, item)
}

class NotFilter(val filter: UsagesSearchFilter): UsagesSearchFilter {
    override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean =
            !filter.accepts(ref, item)
}

fun UsagesSearchFilter.or(that: UsagesSearchFilter): UsagesSearchFilter = when {
    this == False -> that
    that == False -> this
    this == True, that == True -> True
    else -> OrFilter(this, that)
}

fun UsagesSearchFilter.and(that: UsagesSearchFilter): UsagesSearchFilter = when {
    this == True -> that
    that == True -> this
    this == False, that == False -> False
    else -> AndFilter(this, that)
}

fun UsagesSearchFilter.not(): UsagesSearchFilter = when {
    this == False -> True
    this == True -> False
    else -> NotFilter(this)
}

fun UsagesSearchFilter.ifElse(condition: Boolean, elseFilter: UsagesSearchFilter): UsagesSearchFilter =
        if (condition) this else elseFilter

fun UsagesSearchFilter.ifOrFalse(condition: Boolean): UsagesSearchFilter = ifElse(condition, False)

fun UsagesSearchFilter.ifOrTrue(condition: Boolean): UsagesSearchFilter = ifElse(condition, True)

val (PsiReference.() -> Boolean).searchFilter: UsagesSearchFilter
    get() = object: UsagesSearchFilter {
        override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = ref.this@searchFilter()
    }

val <T: PsiNamedElement> (PsiReference.(T) -> Boolean).searchFilter: UsagesSearchFilter
    get() = object: UsagesSearchFilter {
        @suppress("UNCHECKED_CAST")
        override fun accepts(ref: PsiReference, item: UsagesSearchRequestItem): Boolean = ref.this@searchFilter(item.target.element as T)
    }
