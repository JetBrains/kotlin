/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.replHistoryProvider
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol

/**
 * REPL snippets are allowed to redefine functions or properties from previous snippets, and only
 * the most recent declaration should be used. This resolver reduces the set of candidates to those
 * *not* from a REPL snippet or those from the *most recent* REPL snippet.
 *
 * For functions, this means that it is possible to redefine a function with different parameter
 * types and for the most recent function to be resolved instead of the most accurate.
 *
 * ```kotlin
 * // SNIPPET
 * fun foo(x: Int): Int = 1
 *
 * // SNIPPET
 * fun foo(x: Any): Int = 2
 *
 * // SNIPPET
 * foo(0) // returns `2`
 * ```
 */
class ReplOverloadCallConflictResolver(private val session: FirSession) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        val historyProvider = session.replHistoryProvider ?: return candidates
        val containingSnippets = candidates.associateWith { historyProvider.getContainingSnippet(it.symbol) }
        if (containingSnippets.values.all { it == null }) return candidates

        val history = historyProvider.getSnippets().toList()

        fun recency(snippet: FirReplSnippetSymbol): Int =
            // A snippet that is not registered in the history, counts as the most recent one.
            history.indexOfFirst { it === snippet }.takeIf { it >= 0 } ?: Int.MAX_VALUE

        val mostRecentSnippet = containingSnippets.values.filterNotNull().maxBy(::recency)
        return candidates.filterTo(mutableSetOf()) {
            val snippet = containingSnippets[it]
            snippet === null || snippet === mostRecentSnippet
        }
    }
}
