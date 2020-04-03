/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirTypeParametersOwner
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.impl.FirTypePlaceholderProjection

sealed class TypeArgumentMapping {

    abstract operator fun get(typeParameterIndex: Int): FirTypeProjection

    object NoExplicitArguments : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection = FirTypePlaceholderProjection
    }

    class Mapped(private val ordered: List<FirTypeProjection>) : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection {
            return ordered.getOrElse(typeParameterIndex) { FirTypePlaceholderProjection }
        }
    }
}


internal object MapTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val typeArguments = callInfo.typeArguments
        if (typeArguments.isEmpty()) {
            candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
            return
        }

        val owner = candidate.symbol.fir as FirTypeParametersOwner

        if (typeArguments.size == owner.typeParameters.size) {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(typeArguments)
        } else {
            sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(emptyList())
        }
    }
}

internal object NoTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {

        if (callInfo.typeArguments.isNotEmpty()) {
            sink.yieldApplicability(CandidateApplicability.INAPPLICABLE)
        }
        candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
    }
}