/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate

object ReplOverloadCallConflictResolver : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        val firstReplCandidates = candidates.firstOrNull { it.symbol.isReplSnippetDeclaration == true }
        return candidates.filterTo(mutableSetOf()) { it === firstReplCandidates || it.symbol.isReplSnippetDeclaration != true }
    }
}
