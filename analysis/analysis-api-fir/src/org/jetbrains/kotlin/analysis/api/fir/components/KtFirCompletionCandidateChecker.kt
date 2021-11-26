/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.weakRef
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LowLevelFirApiFacadeForResolveOnAir.getTowerContextProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.ResolutionParameters
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.getElementTextInContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class KtFirCompletionCandidateChecker(
    analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCompletionCandidateChecker(), KtFirAnalysisSessionComponent {
    override val analysisSession: KtFirAnalysisSession by weakRef(analysisSession)

    override fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult = withValidityAssertion {
        require(firSymbolForCandidate is KtFirSymbol<*>)
        return firSymbolForCandidate.firRef.withFir(
            phase = FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
        ) { declaration ->
            check(declaration is FirCallableDeclaration)
            checkExtension(declaration, originalFile, nameExpression, possibleExplicitReceiver)
        }
    }

    private fun checkExtension(
        candidateSymbol: FirCallableDeclaration,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult {
        val file = originalFile.getOrBuildFirFile(firResolveState)
        val explicitReceiverExpression = possibleExplicitReceiver?.getOrBuildFirOfType<FirExpression>(firResolveState)
        val resolver = SingleCandidateResolver(firResolveState.rootModuleSession, file)
        val implicitReceivers = getImplicitReceivers(nameExpression)
        for (implicitReceiverValue in implicitReceivers) {
            val resolutionParameters = ResolutionParameters(
                singleCandidateResolutionMode = SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION,
                callableSymbol = candidateSymbol.symbol,
                implicitReceiver = implicitReceiverValue,
                explicitReceiver = explicitReceiverExpression
            )
            resolver.resolveSingleCandidate(resolutionParameters)?.let {
                val substitutor = it.createSubstitutorFromTypeArguments() ?: return@let null
                return when {
                    candidateSymbol is FirVariable && candidateSymbol.returnTypeRef.coneType.receiverType(rootModuleSession) != null -> {
                        KtExtensionApplicabilityResult.ApplicableAsFunctionalVariableCall(substitutor)
                    }
                    else -> {
                        KtExtensionApplicabilityResult.ApplicableAsExtensionCallable(substitutor)
                    }
                }
            }
        }
        return KtExtensionApplicabilityResult.NonApplicable(KtSubstitutor.Empty(token))
    }

    private fun getImplicitReceivers(fakeNameExpression: KtSimpleNameExpression): Sequence<ImplicitReceiverValue<*>?> {
        val towerDataContext = analysisSession.firResolveState.getTowerContextProvider()
            .getClosestAvailableParentContext(fakeNameExpression)
            ?: error("Cannot find enclosing declaration for ${fakeNameExpression.getElementTextInContext()}")

        return sequence {
            yield(null) // otherwise explicit receiver won't be checked when there are no implicit receivers in completion position
            yieldAll(towerDataContext.implicitReceiverStack)
        }
    }
}
