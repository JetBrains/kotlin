/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol


sealed class ResolutionStage {
    abstract fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

sealed class CheckerStage : ResolutionStage()

internal object MapArguments : ResolutionStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        if (symbol.firUnsafe<FirFunction>().valueParameters.size != callInfo.argumentCount) {
            return sink.reportApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }

}


internal fun functionCallResolutionSequence() =
    listOf<ResolutionStage>(MapArguments)


internal fun qualifiedAccessResolutionSequence() =
    listOf<ResolutionStage>()