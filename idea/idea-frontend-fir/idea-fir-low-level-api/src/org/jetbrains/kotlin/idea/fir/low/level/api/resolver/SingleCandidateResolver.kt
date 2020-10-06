/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.resolver

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirEmptyArgumentList
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.inference.FirCallCompleter
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirAbstractBodyResolveTransformer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveTransformer
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess

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

        val resolutionContext = stubBodyResolveTransformer.resolutionContext

        val candidate = CandidateFactory(resolutionContext, callInfo).createCandidate(
            resolutionParameters.callableSymbol,
            explicitReceiverKind = explicitReceiverKind,
            dispatchReceiverValue = dispatchReceiverValue,
            implicitExtensionReceiverValue = implicitExtensionReceiverValue,
            scope = null,
        )

        val applicability = resolutionStageRunner.processCandidate(candidate, resolutionContext, stopOnFirstError = true)
        if (applicability.isSuccess) {
            return completeResolvedCandidate(candidate, resolutionParameters)
        }
        return null
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

    private fun completeResolvedCandidate(candidate: Candidate, resolutionParameters: ResolutionParameters): FirFunctionCall? {
        val fakeCall = buildFunctionCall {
            calleeReference = FirNamedReferenceWithCandidate(
                source = null,
                name = resolutionParameters.callableSymbol.callableId.callableName,
                candidate = candidate
            )
        }
        val expectedType = resolutionParameters.expectedType ?: bodyResolveComponents.noExpectedType
        val completionResult = firCallCompleter.completeCall(fakeCall, expectedType)
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
