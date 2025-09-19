/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents

abstract class ConeCallConflictResolver {
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
        // It's set to 'true' only for `super.foo()`-like calls and used only at ConeOverloadConflictResolver
        discriminateAbstracts: Boolean = false
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates.toSet(), discriminateAbstracts)

    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateAbstracts: Boolean
    ): Set<Candidate>
}

abstract class ConeCallConflictResolverFactory : FirSessionComponent {
    abstract fun create(
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ): ConeCallConflictResolver
}

val FirSession.callConflictResolverFactory: ConeCallConflictResolverFactory
        by FirSession.sessionComponentAccessorWithDefault(DefaultCallConflictResolverFactory)
