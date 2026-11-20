/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.resolution

import org.jetbrains.kotlin.analysis.api.resolution.*
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaVariableSymbol

internal fun KaMultiCallResolutionAttempt.toCallInfo(): KaCallInfo = fold(
    onSuccess = { KaFirSuccessCallInfo(it.asKaCall()) },
    onFailure = { errors ->
        val firstDiagnostic = errors.first().diagnostic
        val candidateCalls = errors.flatMap { it.candidateCalls.map { call -> call.asKaCall() } }
        KaFirErrorCallInfo(candidateCalls, firstDiagnostic)
    },
)

internal fun KaCallCandidateInfo.asKaCallCandidate(): KaCallCandidate {
    val call = candidate as KaSimpleOrMultiCall
    return when (this) {
        is KaApplicableCallCandidateInfo -> KaFirApplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
        )

        is KaInapplicableCallCandidateInfo -> KaFirInapplicableCallCandidate(
            backingCandidate = call,
            backingIsInBestCandidates = isInBestCandidates,
            backingDiagnostic = diagnostic,
        )
    }
}

internal fun KaCallCandidate.asKaCallCandidateInfo(): KaCallCandidateInfo {
    val call = candidate.asKaCall()
    return when (this) {
        is KaApplicableCallCandidate -> KaFirApplicableCallCandidateInfo(
            backingCandidate = call,
            isInBestCandidates = isInBestCandidates,
        )

        is KaInapplicableCallCandidate -> KaFirInapplicableCallCandidateInfo(
            backingCandidate = call,
            isInBestCandidates = isInBestCandidates,
            diagnostic = diagnostic,
        )
    }
}

/**
 * Returns the legacy [KaCall] view of [this] [KaSimpleOrMultiCall]. Most resolution result types
 * implement [KaCall] directly. The exception is [KaCallableReferenceCall], which is part of the
 * new resolution API and intentionally does not extend the deprecated [KaCall] hierarchy. For
 * that case we emulate a legacy [KaCall] by re-encoding the reference as the corresponding
 * `KaSimpleFunctionCall` / `KaSimpleVariableAccessCall` view.
 */
internal fun KaSimpleOrMultiCall.asKaCall(): KaCall = when (this) {
    is KaFirCallableReferenceCall<*, *> -> asLegacyKaCall()
    else -> this as KaCall
}

@Suppress("UNCHECKED_CAST")
private fun KaFirCallableReferenceCall<*, *>.asLegacyKaCall(): KaCall {
    val partiallyAppliedSymbol = backingPartiallyAppliedSymbol
    return when (partiallyAppliedSymbol.symbol) {
        is KaFunctionSymbol -> KaFirSimpleFunctionCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedFunctionSymbol<KaFunctionSymbol>,
            backingArgumentMapping = emptyMap(),
            backingTypeArgumentsMapping = typeArgumentsMapping,
        )

        is KaVariableSymbol -> KaFirSimpleVariableAccessCall(
            backingPartiallyAppliedSymbol = partiallyAppliedSymbol as KaPartiallyAppliedVariableSymbol<KaVariableSymbol>,
            backingTypeArgumentsMapping = typeArgumentsMapping,
            backingKind = KaFirVariableReadAccess,
            backingIsContextSensitive = false,
        )
    }
}
