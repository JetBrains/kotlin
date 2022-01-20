/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildPlaceholderProjection

sealed class TypeArgumentMapping {
    abstract operator fun get(typeParameterIndex: Int): FirTypeProjection

    object NoExplicitArguments : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection = buildPlaceholderProjection()
    }

    class Mapped(private val ordered: List<FirTypeProjection>) : TypeArgumentMapping() {
        override fun get(typeParameterIndex: Int): FirTypeProjection {
            return ordered.getOrElse(typeParameterIndex) { buildPlaceholderProjection() }
        }
    }
}

internal object MapTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        val typeArguments = callInfo.typeArguments
        if (typeArguments.isEmpty()) {
            candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
            return
        }

        val owner = candidate.symbol.fir as FirTypeParameterRefsOwner

        if (
            typeArguments.size == owner.typeParameters.size ||
            callInfo.callKind == CallKind.DelegatingConstructorCall ||
            (owner as? FirDeclaration)?.origin is FirDeclarationOrigin.DynamicScope
        ) {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(typeArguments)
        } else {
            candidate.typeArgumentMapping = TypeArgumentMapping.Mapped(emptyList())
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}

internal object NoTypeArguments : ResolutionStage() {
    override suspend fun check(candidate: Candidate, callInfo: CallInfo, sink: CheckerSink, context: ResolutionContext) {
        if (callInfo.typeArguments.isNotEmpty()) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
        candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
    }
}
