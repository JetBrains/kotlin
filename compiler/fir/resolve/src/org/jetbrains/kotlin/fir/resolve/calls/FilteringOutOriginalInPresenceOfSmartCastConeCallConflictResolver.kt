/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

object FilteringOutOriginalInPresenceOfSmartCastConeCallConflictResolver : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        val (originalIfSmartCastPresent, other) = candidates.partition { it.isFromOriginalTypeInPresenceOfSmartCast }

        // If we have both successful candidates from smart cast and original, use the former one as they might have more correct return type
        if (originalIfSmartCastPresent.isNotEmpty() && other.isNotEmpty()) return other.toSet()

        return candidates
    }
}
