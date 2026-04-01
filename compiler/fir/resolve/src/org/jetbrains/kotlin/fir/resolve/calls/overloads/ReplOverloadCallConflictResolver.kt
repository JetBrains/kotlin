/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate

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
object ReplOverloadCallConflictResolver : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        // Candidates are (somehow?) naturally sorted from the most recent snippet to the least recent snippet.
        // Only candidates from *previous* snippets will have a non-null `originalReplSnippetSymbol`.
        val mostRecentSnippet = candidates.firstNotNullOfOrNull { it.symbol.fir.originalReplSnippetSymbol }
        return candidates.filterTo(mutableSetOf()) {
            val snippet = it.symbol.fir.originalReplSnippetSymbol
            snippet === null || snippet === mostRecentSnippet
        }
    }
}
