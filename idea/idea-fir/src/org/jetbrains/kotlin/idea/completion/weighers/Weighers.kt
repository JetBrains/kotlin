/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.weighers

import com.intellij.codeInsight.completion.CompletionSorter
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType

internal object Weighers {
    fun KtAnalysisSession.applyWeighsToLookupElement(lookupElement: LookupElement, symbol: KtSymbol, expectedType: KtType?) {
        with(ExpectedTypeWeigher) { addWeight(lookupElement, symbol, expectedType) }
    }

    fun addWeighersToCompletionSorter(sorter: CompletionSorter): CompletionSorter =
        sorter
            .weighBefore(PlatformWeighersIds.STATS, ExpectedTypeWeigher.Weigher)
            .weighBefore(ExpectedTypeWeigher.WEIGHER_ID, CompletionContributorGroupWeigher.Weigher)

    private object PlatformWeighersIds {
        const val PREFIX = "prefix"
        const val STATS = "stats"
    }
}