/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION

class ConeOverloadResolutionByLambdaConflictResolve(private val session: FirSession) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
        discriminateGenerics: Boolean,
        discriminateAbstracts: Boolean
    ): Set<Candidate> {
        if (candidates.size == 1) return candidates

        val (candidatesWithAnnotation, candidatesWithoutAnnotation) = candidates.partition { candidate ->
            (candidate.symbol.fir as? FirCallableDeclaration<*>)?.annotations?.any { annotation ->
                annotation.fqName(session) == OVERLOAD_RESOLUTION_BY_LAMBDA_ANNOTATION
            } == true
        }

        if (candidatesWithoutAnnotation.size == 1) return setOf(candidatesWithoutAnnotation.single())

        candidatesWithAnnotation.singleOrNull { candidate ->
            (candidate.symbol.fir as? FirFunction<*>)
                ?.valueParameters?.singleOrNull()?.returnTypeRef?.coneTypeSafe<ConeKotlinType>()
                .let { type ->
                    type != null && type.isBuiltinFunctionalType(session) && type.typeArguments.last() is ConeTypeParameterType
                }
        }?.let {
            return setOf(it)
        }

        return candidates
    }
}
