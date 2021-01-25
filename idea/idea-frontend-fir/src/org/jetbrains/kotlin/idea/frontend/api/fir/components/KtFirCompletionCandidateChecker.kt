/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getFirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.api.getTowerDataContextUnsafe
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.ResolutionParameters
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirKotlinPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.concurrent.ConcurrentHashMap

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
    ): Boolean = withValidityAssertion {
        val functionFits = firSymbolForCandidate.withResolvedFirOfType<KtFirFunctionSymbol, FirSimpleFunction, Boolean> { firFunction ->
            checkExtension(firFunction, originalFile, nameExpression, possibleExplicitReceiver)
        }
        val propertyFits = firSymbolForCandidate.withResolvedFirOfType<KtFirKotlinPropertySymbol, FirProperty, Boolean> { firProperty ->
            checkExtension(firProperty, originalFile, nameExpression, possibleExplicitReceiver)
        }

        functionFits ?: propertyFits ?: false
    }

    private inline fun <reified T : KtFirSymbol<F>, F : FirDeclaration, R> KtCallableSymbol.withResolvedFirOfType(
        noinline action: (F) -> R,
    ): R? = this.safeAs<T>()?.firRef?.withFir(phase = FirResolvePhase.BODY_RESOLVE, action)

    private fun checkExtension(
        candidateSymbol: FirCallableDeclaration<*>,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): Boolean {
        val file = originalFile.getFirFile(firResolveState)
        val explicitReceiverExpression = possibleExplicitReceiver?.getOrBuildFirOfType<FirExpression>(firResolveState)
        val resolver = SingleCandidateResolver(firResolveState.rootModuleSession, file)
        val implicitReceivers = getImplicitReceivers(originalFile, file, nameExpression)
        for (implicitReceiverValue in implicitReceivers) {
            val resolutionParameters = ResolutionParameters(
                singleCandidateResolutionMode = SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION,
                callableSymbol = candidateSymbol.symbol,
                implicitReceiver = implicitReceiverValue,
                explicitReceiver = explicitReceiverExpression
            )
            resolver.resolveSingleCandidate(resolutionParameters)?.let {
                // not null if resolved and completed successfully
                return true
            }
        }
        return false
    }

    private fun getImplicitReceivers(
        originalFile: KtFile,
        firFile: FirFile,
        fakeNameExpression: KtSimpleNameExpression
    ): Sequence<ImplicitReceiverValue<*>?> {
        val towerDataContext = analysisSession.firResolveState.getTowerDataContextUnsafe(fakeNameExpression)

        return sequence {
            yield(null) // otherwise explicit receiver won't be checked when there are no implicit receivers in completion position
            yieldAll(towerDataContext.implicitReceiverStack)
        }
    }
}
