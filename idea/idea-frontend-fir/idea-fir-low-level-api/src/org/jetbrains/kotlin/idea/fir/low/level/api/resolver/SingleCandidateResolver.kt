/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef

class SingleCandidateResolver(
    private val firSession: FirSession,
    private val firFile: FirFile,
) {
    private val scopeSession = ScopeSession()

    // TODO This transformer is not intended for actual transformations and created here only to simplify access to body resolve components
    private val stubBodyResolveTransformer = object : FirBodyResolveTransformer(
        session = firSession,
        phase = FirResolvePhase.BODY_RESOLVE,
        implicitTypeOnly = false,
        scopeSession = scopeSession,
    ) {}
    private val bodyResolveComponents =
        FirAbstractBodyResolveTransformer.BodyResolveTransformerComponents(
            firSession,
            scopeSession,
            stubBodyResolveTransformer,
            stubBodyResolveTransformer.context,
        )
    private val firCallCompleter = FirCallCompleter(
        stubBodyResolveTransformer,
        bodyResolveComponents,
    )
    private val resolutionStageRunner = ResolutionStageRunner(bodyResolveComponents.inferenceComponents)

    fun resolveSingleCandidate(
        resolutionParameters: ResolutionParameters
    ): FirFunctionCall? {

        val partProvider = createCandidatePartsProvider(resolutionParameters)
        if (partProvider.shouldFailBeforeResolve())
            return null

        val callInfo = partProvider.callInfo()
        val explicitReceiverKind = partProvider.explicitReceiverKind()
        val dispatchReceiverValue = partProvider.dispatchReceiverValue()
        val implicitExtensionReceiverValue = partProvider.implicitExtensionReceiverValue()

        val candidate = CandidateFactory(bodyResolveComponents, callInfo).createCandidate(
            resolutionParameters.callableSymbol,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiverValue = dispatchReceiverValue,
            implicitExtensionReceiverValue = implicitExtensionReceiverValue,
        )

        val applicability = resolutionStageRunner.processCandidate(candidate, stopOnFirstError = true)
        if (applicability >= CandidateApplicability.SYNTHETIC_RESOLVED) {
            return completeResolvedCandidate(candidate, resolutionParameters)
        }
        return null
    }

    private fun createCandidatePartsProvider(resolutionParameters: ResolutionParameters): CandidateInfoProvider {
        return when (resolutionParameters.singleCandidateResolutionMode) {
            SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION -> CheckExtensionForCompletionCandidateInfoProvider(
                resolutionParameters,
                firFile,
                firSession
            )
        }
    }

    private fun completeResolvedCandidate(candidate: Candidate, resolutionParameters: ResolutionParameters): FirFunctionCall? {
        val fakeCall = buildFunctionCall {
            calleeReference = FirNamedReferenceWithCandidate(
                source = null,
                name = resolutionParameters.callableSymbol.callableId.callableName,
                candidate = candidate
            )
        }
        val completionResult = firCallCompleter.completeCall(fakeCall, resolutionParameters.expectedType)
        return if (completionResult.callCompleted) {
            completionResult.result
        } else null
    }
}

class ResolutionParameters(
    val singleCandidateResolutionMode: SingleCandidateResolutionMode,
    val callableSymbol: FirCallableSymbol<*>,
    val implicitReceiver: ImplicitReceiverValue<*>? = null,
    val expectedType: FirTypeRef? = null,
    val explicitReceiver: FirExpression? = null,
    val argumentList: FirArgumentList = FirEmptyArgumentList,
    val typeArgumentList: List<FirTypeProjection> = emptyList(),
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
