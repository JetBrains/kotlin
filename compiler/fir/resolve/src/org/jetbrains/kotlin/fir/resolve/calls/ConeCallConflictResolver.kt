/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.componentArrayAccessor
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

abstract class ConeCallConflictResolver {
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
        discriminateGenerics: Boolean
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates.toSet(), discriminateGenerics)

    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean
    ): Set<Candidate>
}

abstract class ConeCallConflictResolverFactory : FirSessionComponent {
    abstract fun create(typeSpecificityComparator: TypeSpecificityComparator, components: InferenceComponents): ConeCallConflictResolver
}
val FirSession.callConflictResolverFactory by componentArrayAccessor<ConeCallConflictResolverFactory>()