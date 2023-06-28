/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.createConeDiagnosticForCandidateWithError
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

class SingleCandidateResolver(
    private val firSession: FirSession,
    private val firFile: FirFile,
) {
    private val bodyResolveComponents = createStubBodyResolveComponents(firSession)
    private val firCallCompleter = FirCallCompleter(
        bodyResolveComponents.transformer,
        bodyResolveComponents,
    )
    private val resolutionStageRunner = ResolutionStageRunner()

    fun resolveSingleCandidate(
        resolutionParameters: ResolutionParameters
    ): FirFunctionCall? {

        val infoProvider = createCandidateInfoProvider(resolutionParameters)
        if (infoProvider.shouldFailBeforeResolve())
            return null

        val callInfo = infoProvider.callInfo()
        val explicitReceiverKind = infoProvider.explicitReceiverKind()
        val dispatchReceiverValue = infoProvider.dispatchReceiverValue()
        val implicitExtensionReceiverValue = infoProvider.implicitExtensionReceiverValue()

        val resolutionContext = bodyResolveComponents.transformer.resolutionContext

        val candidate = CandidateFactory(resolutionContext, callInfo).createCandidate(
            callInfo,
            resolutionParameters.callableSymbol,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiver = dispatchReceiverValue?.receiverExpression,
            givenExtensionReceiverOptions = listOfNotNull(
                if (explicitReceiverKind.isExtensionReceiver)
                    callInfo.explicitReceiver
                else
                    implicitExtensionReceiverValue?.receiverExpression
            ),
            scope = null,
        )

        val applicability = resolutionStageRunner.processCandidate(candidate, resolutionContext, stopOnFirstError = true)

        val fakeCall = if (applicability.isSuccess) {
            buildCallForResolvedCandidate(candidate, resolutionParameters)
        } else if (
            resolutionParameters.allowUnsafeCall && applicability == CandidateApplicability.UNSAFE_CALL ||
            resolutionParameters.allowUnstableSmartCast && applicability == CandidateApplicability.UNSTABLE_SMARTCAST
        ) {
            buildCallForCandidateWithError(candidate, applicability, resolutionParameters)
        } else {
            return null
        }

        val completionResult = firCallCompleter.completeCall(
            fakeCall,
            (resolutionParameters.expectedType as? FirResolvedTypeRef)?.let { ResolutionMode.WithExpectedType(it) }
                ?: ResolutionMode.ContextIndependent
        )

        return completionResult
    }

    private fun createCandidateInfoProvider(resolutionParameters: ResolutionParameters): CandidateInfoProvider {
        return when (resolutionParameters.singleCandidateResolutionMode) {
            SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION -> CheckExtensionForCompletionCandidateInfoProvider(
                resolutionParameters,
                firFile,
                firSession
            )
        }
    }

    private fun buildCallForResolvedCandidate(candidate: Candidate, resolutionParameters: ResolutionParameters): FirFunctionCall =
        buildFunctionCall {
            calleeReference = FirNamedReferenceWithCandidate(
                source = null,
                name = resolutionParameters.callableSymbol.callableId.callableName,
                candidate = candidate
            )
        }

    private fun buildCallForCandidateWithError(
        candidate: Candidate,
        applicability: CandidateApplicability,
        resolutionParameters: ResolutionParameters
    ): FirFunctionCall {
        val diagnostic = createConeDiagnosticForCandidateWithError(applicability, candidate)
        val name = resolutionParameters.callableSymbol.callableId.callableName
        return buildFunctionCall {
            calleeReference = FirErrorReferenceWithCandidate(source = null, name, candidate, diagnostic)
        }
    }
}

/**
 * @param allowUnsafeCall if true, then candidate is resolved even if receiver's nullability doesn't match
 * @param allowUnstableSmartCast if true, then candidate is resolved even if it requires unstable smart cast
 */
class ResolutionParameters(
    val singleCandidateResolutionMode: SingleCandidateResolutionMode,
    val callableSymbol: FirCallableSymbol<*>,
    val implicitReceiver: ImplicitReceiverValue<*>? = null,
    val expectedType: FirTypeRef? = null,
    val explicitReceiver: FirExpression? = null,
    val argumentList: FirArgumentList = FirEmptyArgumentList,
    val typeArgumentList: List<FirTypeProjection> = emptyList(),
    val allowUnsafeCall: Boolean = false,
    val allowUnstableSmartCast: Boolean = false,
)

enum class SingleCandidateResolutionMode {
    /**
     * Run resolution stages necessary to type check extension receiver (explicit/implicit) for candidate function.
     * Candidate is expected to be taken from context scope.
     * Arguments and type arguments are not expected and not checked.
     * Explicit receiver can be passed and will always be interpreted as extension receiver.
     */
    CHECK_EXTENSION_FOR_COMPLETION
}
