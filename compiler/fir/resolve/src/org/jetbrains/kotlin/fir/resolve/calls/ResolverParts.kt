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
        if (symbol.firUnsafe<FirFunction>().valueParameters.size != callInfo.arguments.size) {
            return sink.reportApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }

}

internal object CheckArguments : CheckerStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol ?: error("Can't check arguments for non function")
        val declaration = symbol.fir as FirFunction
        for ((parameter, argument) in declaration.valueParameters.zip(callInfo.arguments)) {

            candidate.resolveArgument(argument, parameter, isReceiver = false, typeProvider = callInfo.typeProvider, sink = sink)
        }

        if (candidate.system.hasContradiction) {
            sink.reportApplicability(CandidateApplicability.INAPPLICABLE)
        }
    }

}


internal fun functionCallResolutionSequence() =
    listOf<ResolutionStage>(MapArguments, CheckArguments)


internal fun qualifiedAccessResolutionSequence() =
    listOf<ResolutionStage>()