/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.resolve.descriptorUtil.OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION

class ConeOverloadResolutionByLambdaConflictResolve(private val session: FirSession) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates

        candidates.singleOrNull { candidate ->
            (candidate.symbol.fir as? FirCallableDeclaration<*>)?.annotations?.any {
                it.fqName(session) == OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION
            } == true
        }?.let {
            return setOf(it)
        }

        return candidates
    }
}
