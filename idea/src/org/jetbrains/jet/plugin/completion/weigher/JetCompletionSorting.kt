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

package org.jetbrains.jet.plugin.completion.weigher

import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionSorter
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.WeighingContext

public fun CompletionResultSet.addJetSorting(parameters: CompletionParameters): CompletionResultSet {
    var sorter = CompletionSorter.defaultSorter(parameters, getPrefixMatcher())!!

    sorter = sorter.weighBefore("stats", JetKindWeigher())

    sorter = sorter.weighAfter(
            "stats",
            JetDeclarationRemotenessWeigher(parameters.getOriginalFile() as JetFile),
            JetAccessibleWeigher())

    sorter = sorter.weighBefore("middleMatching", PreferMatchingItemWeigher())

    return withRelevanceSorter(sorter)
}

class PreferMatchingItemWeigher : LookupElementWeigher("preferMatching", false, true) {
    override fun weigh(element: LookupElement, context: WeighingContext): Comparable<Int> {
        val prefix = context.itemPattern(element)
        return if (element.getLookupString() == prefix) 0 else 1
    }
}
