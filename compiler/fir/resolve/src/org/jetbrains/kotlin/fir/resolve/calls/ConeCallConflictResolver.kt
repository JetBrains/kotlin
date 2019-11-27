/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

interface ConeCallConflictResolver {
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
        discriminateGenerics: Boolean
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates.toSet(), discriminateGenerics)

    fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean
    ): Set<Candidate>
}