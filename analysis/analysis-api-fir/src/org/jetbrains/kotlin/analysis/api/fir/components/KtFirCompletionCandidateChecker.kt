/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.ResolutionParameters
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.analysis.utils.printer.getElementTextInContext
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver

internal class KtFirCompletionCandidateChecker(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtCompletionCandidateChecker(), KtFirAnalysisSessionComponent {
    override fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult {
        require(firSymbolForCandidate is KtFirSymbol<*>)
        firSymbolForCandidate.firSymbol.lazyResolveToPhase(FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE)
        val declaration = firSymbolForCandidate.firSymbol.fir as FirCallableDeclaration
        return checkExtension(declaration, originalFile, nameExpression, possibleExplicitReceiver)
    }

    private fun checkExtension(
        candidateSymbol: FirCallableDeclaration,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult {
        val file = originalFile.getOrBuildFirFile(firResolveSession)
        val explicitReceiverExpression = possibleExplicitReceiver?.getMatchingFirExpressionForCallReceiver()
        val resolver = SingleCandidateResolver(firResolveSession.useSiteFirSession, file)
        val implicitReceivers = getImplicitReceivers(originalFile, nameExpression)
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
                        KtExtensionApplicabilityResult.ApplicableAsFunctionalVariableCall(substitutor, token)
                    }
                    else -> {
                        KtExtensionApplicabilityResult.ApplicableAsExtensionCallable(substitutor, token)
                    }
                }
            }
        }
        return KtExtensionApplicabilityResult.NonApplicable(KtSubstitutor.Empty(token), token)
    }

    private fun getImplicitReceivers(
        originalFile: KtFile,
        fakeNameExpression: KtSimpleNameExpression
    ): Sequence<ImplicitReceiverValue<*>?> {
        val towerDataContext = analysisSession.firResolveSession.getTowerContextProvider(originalFile)
            .getClosestAvailableParentContext(fakeNameExpression)
            ?: error("Cannot find enclosing declaration for ${fakeNameExpression.getElementTextInContext()}")

        return sequence {
            yield(null) // otherwise explicit receiver won't be checked when there are no implicit receivers in completion position
            yieldAll(towerDataContext.implicitReceiverStack)
        }
    }

    /**
     * It is not enough to just call the `getOrBuildFirOfType` on [this] receiver expression, because for calls
     * like `foo?.bar()` the receiver is additionally wrapped into `FirCheckedSafeCallSubject`, which is important
     * for type-checks during resolve.
     *
     * @receiver PSI receiver expression in some qualified expression (e.g. `foo` in `foo?.bar()`, `a` in `a.b`)
     * @return A FIR expression which most precisely represents the receiver for the corresponding FIR call.
     */
    private fun KtExpression.getMatchingFirExpressionForCallReceiver(): FirExpression {
        val psiWholeCall = this.getQualifiedExpressionForReceiver()
        if (psiWholeCall !is KtSafeQualifiedExpression) return this.getOrBuildFirOfType<FirExpression>(firResolveSession)

        val firSafeCall = psiWholeCall.getOrBuildFirOfType<FirSafeCallExpression>(firResolveSession)
        return firSafeCall.checkedSubjectRef.value
    }
}
