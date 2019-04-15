/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind.*
import java.lang.IllegalStateException


abstract class ResolutionStage {
    abstract fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo)
}

abstract class CheckerStage : ResolutionStage()

internal object CheckExplicitReceiverConsistency : ResolutionStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val receiverKind = candidate.receiverKind
        val explicitReceiver = callInfo.explicitReceiver
        // TODO: add invoke cases
        when (receiverKind) {
            NO_EXPLICIT_RECEIVER -> {
                if (explicitReceiver != null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            EXTENSION_RECEIVER, DISPATCH_RECEIVER -> {
                if (explicitReceiver == null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
            }
            BOTH_RECEIVERS -> {
                if (explicitReceiver == null) return sink.reportApplicability(CandidateApplicability.WRONG_RECEIVER)
                // Here we should also check additional invoke receiver
            }
        }
    }

}

internal sealed class CheckReceivers : ResolutionStage() {
    object Dispatch : CheckReceivers()

    object Extension : CheckReceivers()

    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val callableSymbol = candidate.symbol as? FirCallableSymbol ?: return
        val callableId = callableSymbol.callableId
        val callable = callableSymbol.fir
    }
}

internal object MapArguments : ResolutionStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val symbol = candidate.symbol as? FirFunctionSymbol ?: return sink.reportApplicability(CandidateApplicability.HIDDEN)
        val function = symbol.firUnsafe<FirFunction>()
        val processor = FirCallArgumentsProcessor(function, callInfo.arguments)
        val mappingResult = processor.process()
        candidate.argumentMapping = mappingResult.argumentMapping
        if (!mappingResult.isSuccess) {
            return sink.reportApplicability(CandidateApplicability.PARAMETER_MAPPING_ERROR)
        }
    }

}

internal object CheckArguments : CheckerStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        val argumentMapping =
            candidate.argumentMapping ?: throw IllegalStateException("Argument should be already mapped while checking arguments!")
        for ((argument, parameter) in argumentMapping) {
            candidate.resolveArgument(argument, parameter, isReceiver = false, typeProvider = callInfo.typeProvider, sink = sink)
        }

        if (candidate.system.hasContradiction) {
            sink.reportApplicability(CandidateApplicability.INAPPLICABLE)
        }
    }
}

internal object DiscriminateSynthetics : CheckerStage() {
    override fun check(candidate: Candidate, sink: CheckerSink, callInfo: CallInfo) {
        if (candidate.symbol is SyntheticSymbol) {
            sink.reportApplicability(CandidateApplicability.SYNTHETIC_RESOLVED)
        }
    }

}


internal fun functionCallResolutionSequence() = listOf(
    CheckExplicitReceiverConsistency, CheckReceivers.Dispatch, CheckReceivers.Extension,
    MapArguments, CreateFreshTypeVariableSubstitutorStage, CheckArguments
)


internal fun qualifiedAccessResolutionSequence() = listOf(
    DiscriminateSynthetics, CheckExplicitReceiverConsistency, CheckReceivers.Dispatch, CheckReceivers.Extension,
    CreateFreshTypeVariableSubstitutorStage
)