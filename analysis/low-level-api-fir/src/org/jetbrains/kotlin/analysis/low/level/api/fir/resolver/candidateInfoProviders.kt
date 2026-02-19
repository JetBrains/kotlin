/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.ReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.candidate.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.receiverType
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

abstract class AbstractBaseCandidateInfoProvider(
    protected val resolutionParameters: ResolutionParameters,
    protected val firFile: FirFile,
    protected val firSession: FirSession,
) : CandidateInfoProvider {
    override fun callInfo(): CallInfo = with(resolutionParameters) {
        CallInfo(
            firFile, // TODO: consider passing more precise info here, if needed
            callKind = callKind(),
            name = callableSymbol.name,
            explicitReceiver = explicitReceiver,
            argumentList = argumentList,
            typeArguments = typeArgumentList,
            containingDeclarations = emptyList(), // TODO - maybe we should pass declarations from context here (no visible differences atm)
            containingFile = firFile,
            resolutionMode = ResolutionMode.ContextIndependent,
            isUsedAsGetClassReceiver = false,
            session = firSession,
            implicitInvokeMode = ImplicitInvokeMode.None,
        )
    }

    override fun shouldFailBeforeResolve(): Boolean = false
}

abstract class AbstractExtensionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: FirFile,
    firSession: FirSession,
) : AbstractBaseCandidateInfoProvider(resolutionParameters, firFile, firSession) {
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
        val fir = callableSymbol.fir
        val candidateHasExtensionReceiver = fir.receiverParameter != null
                || fir is FirVariable && fir.returnTypeRef.coneType.receiverType(firSession) != null
        callHasExtensionReceiver != candidateHasExtensionReceiver
    }
}

/**
 * Provider for [SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION] mode.
 */
class CheckExtensionForCompletionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: FirFile,
    firSession: FirSession,
) : AbstractExtensionCandidateInfoProvider(resolutionParameters, firFile, firSession)

/**
 * Provider for [SingleCandidateResolutionMode.CHECK_EXTENSION_CALLABlE_REFERENCE_FOR_COMPLETION] mode.
 */
class CheckCallableReferenceForCompletionCandidateInfoProvider(
    resolutionParameters: ResolutionParameters,
    firFile: FirFile,
    firSession: FirSession,
) : AbstractExtensionCandidateInfoProvider(resolutionParameters, firFile, firSession) {
    override fun callInfo(): CallInfo {
        return with(resolutionParameters) {
            CallableReferenceInfo(
                callSite = firFile,
                name = callableSymbol.name,
                explicitReceiver = explicitReceiver,
                containingDeclarations = emptyList(),
                containingFile = firFile,
                session = firSession,
                expectedType = null,
                lhs = resolutionParameters.callableReferenceLHS,
                hasSyntheticOuterCall = false,
                callKind = callKind(),
            )
        }
    }
}
