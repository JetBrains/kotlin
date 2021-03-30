/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind

/**
 * A supplier of information for resolving a call against a single provided candidate.
 * Implementors of this interface form a candidate from provided resolution parameters to fit requested resolution mode.
 * This includes creating artificial CallInfo, combining receivers and generating CallKind with specific resolution sequence.
 */
interface CandidateInfoProvider {
    fun callInfo(): CallInfo

    fun callKind(): CallKind

    fun explicitReceiverKind(): ExplicitReceiverKind

    fun dispatchReceiverValue(): ReceiverValue?

    fun implicitExtensionReceiverValue(): ImplicitReceiverValue<*>?

    fun shouldFailBeforeResolve(): Boolean
}

abstract class AbstractCandidateInfoProvider(
    protected val resolutionParameters: ResolutionParameters,
    protected val firFile: FirFile,
    protected val firSession: FirSession,
) : CandidateInfoProvider {
    override fun callInfo(): CallInfo = with(resolutionParameters) {
        CallInfo(
            firFile, // TODO: consider passing more precise info here, if needed
            callKind = callKind(),
            name = callableSymbol.callableId.callableName,
            explicitReceiver = explicitReceiver,
            argumentList = argumentList,
            typeArguments = typeArgumentList,
            containingDeclarations = emptyList(), // TODO - maybe we should pass declarations from context here (no visible differences atm)
            containingFile = firFile,
            isPotentialQualifierPart = false,
            isImplicitInvoke = false,
            session = firSession,
        )
    }

    override fun shouldFailBeforeResolve(): Boolean = false
}

/**
 * Provider for CHECK_EXTENSION_FOR_COMPLETION mode.
 */
class CheckExtensionForCompletionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: FirFile,
    firSession: FirSession,
) : AbstractCandidateInfoProvider(resolutionParameters, firFile, firSession) {

    override fun callKind(): CallKind = buildCallKindWithCustomResolutionSequence {
        checkExtensionReceiver = true
    }

    override fun explicitReceiverKind(): ExplicitReceiverKind =
        if (resolutionParameters.explicitReceiver == null)
            ExplicitReceiverKind.NO_EXPLICIT_RECEIVER
        else ExplicitReceiverKind.EXTENSION_RECEIVER

    // Right now it's impossible to reason about dispatch receiver when candidate comes from arbitrary scope with no other information.
    // So dispatch receiver is not passed from provider and later not checked during the resolution sequence.
    override fun dispatchReceiverValue(): ReceiverValue? = null

    override fun implicitExtensionReceiverValue(): ImplicitReceiverValue<*>? = with(resolutionParameters) {
        if (explicitReceiver == null) implicitReceiver else null
    }

    // Candidates with inconsistent extension receivers are skipped in tower resolver before resolution stages.
    // Passing them through can lead to false positives.
    override fun shouldFailBeforeResolve(): Boolean = with(resolutionParameters) {
        val callHasExtensionReceiver = explicitReceiverKind() == ExplicitReceiverKind.EXTENSION_RECEIVER
                || implicitExtensionReceiverValue() != null
        val candidateHasExtensionReceiver = callableSymbol.fir.receiverTypeRef != null
        callHasExtensionReceiver != candidateHasExtensionReceiver
    }
}
