/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.overloads

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.declarations.typeSpecificityComparatorProvider
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator

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
    fun create(
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ): ConeCallConflictResolver {
        val session = components.session
        val specificityComparator = session.typeSpecificityComparatorProvider?.typeSpecificityComparator
            ?: TypeSpecificityComparator.NONE
        // NB: Adding new resolvers is strongly discouraged because the results are order-dependent.
        return ConeCompositeConflictResolver(
            ConeEquivalentCallConflictResolver(session),
            *createAdditionalResolvers(session).toTypedArray(),
            ConeIntegerOperatorConflictResolver,
            ConeOverloadConflictResolver(specificityComparator, components, transformerComponents),
        )
    }

    abstract fun createAdditionalResolvers(session: FirSession): List<ConeCallConflictResolver>

    object Default : ConeCallConflictResolverFactory() {
        override fun createAdditionalResolvers(session: FirSession): List<ConeCallConflictResolver> {
            return emptyList()
        }
    }
}

val FirSession.callConflictResolverFactory: ConeCallConflictResolverFactory
        by FirSession.sessionComponentAccessorWithDefault(ConeCallConflictResolverFactory.Default)
