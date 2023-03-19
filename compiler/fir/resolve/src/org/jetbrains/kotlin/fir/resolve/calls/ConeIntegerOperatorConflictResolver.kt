/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.scopes.impl.isWrappedIntegerOperator

object ConeIntegerOperatorConflictResolver : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (candidates.size <= 1) {
            return candidates
        }
        val candidateWithWrappedIntegerOperator = candidates.firstOrNull { it.symbol.isWrappedIntegerOperator() }
        return if (candidateWithWrappedIntegerOperator != null) {
            setOf(candidateWithWrappedIntegerOperator)
        } else {
            candidates
        }
    }
}
