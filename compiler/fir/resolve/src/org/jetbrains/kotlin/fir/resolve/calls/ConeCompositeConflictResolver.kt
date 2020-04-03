/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

class ConeCompositeConflictResolver(
    private vararg val conflictResolvers: AbstractConeCallConflictResolver
) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (candidates.size <= 1) return candidates
        var currentCandidates = candidates
        var index = 0
        while (currentCandidates.size > 1 && index < conflictResolvers.size) {
            val conflictResolver = conflictResolvers[index++]
            currentCandidates = conflictResolver.chooseMaximallySpecificCandidates(candidates, discriminateGenerics, discriminateAbstracts)
        }
        return currentCandidates
    }
}