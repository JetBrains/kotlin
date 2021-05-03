/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.weighers

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementWeigher
import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.UserDataProperty


internal object ExpectedTypeWeigher {
    object Weigher : LookupElementWeigher(WEIGHER_ID) {
        override fun weigh(element: LookupElement): Comparable<*>? =
            element.matchesExpectedType
    }

    fun KtAnalysisSession.addWeight(lookupElement: LookupElement, symbol: KtSymbol, expectedType: KtType?) {
        lookupElement.matchesExpectedType = matchesExpectedType(symbol, expectedType)
    }

    private fun KtAnalysisSession.matchesExpectedType(
        symbol: KtSymbol,
        expectedType: KtType?
    ) = when {
        expectedType == null -> MatchesExpectedType.NON_TYPABLE
        symbol !is KtCallableSymbol -> MatchesExpectedType.NON_TYPABLE
        else -> MatchesExpectedType.matches(symbol.annotatedType.type isSubTypeOf expectedType)
    }


    private var LookupElement.matchesExpectedType by UserDataProperty(Key<MatchesExpectedType>("MATCHES_EXPECTED_TYPE"))

    enum class MatchesExpectedType {
        MATCHES, NON_TYPABLE, NOT_MATCHES, ;

        companion object {
            fun matches(matches: Boolean) = if (matches) MATCHES else NOT_MATCHES
        }
    }

    const val WEIGHER_ID = "kotlin.expected.type"
}


