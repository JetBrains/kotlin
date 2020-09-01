/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.components

import com.intellij.psi.PsiElement
import com.jetbrains.rd.util.getOrCreate
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.idea.fir.getOrBuildFirOfType
import org.jetbrains.kotlin.idea.fir.low.level.api.LowLevelFirApiFacade
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.ResolutionParameters
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.idea.fir.low.level.api.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.idea.frontend.api.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.idea.frontend.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirFunctionSymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirPropertySymbol
import org.jetbrains.kotlin.idea.frontend.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal class KtFirCompletionCandidateChecker(
    override val analysisSession: KtFirAnalysisSession,
    override val token: ValidityToken,
) : KtCompletionCandidateChecker(), KtFirAnalysisSessionComponent {
    private val completionContextCache = HashMap<Pair<FirFile, KtNamedFunction>, LowLevelFirApiFacade.FirCompletionContext>()

    override fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        originalPosition: PsiElement?,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): Boolean = withValidityAssertion {
        val originalEnclosingFunction = originalPosition?.getNonStrictParentOfType<KtNamedFunction>()
            ?: error("Cannot find enclosing function for completion in provided position (or position is absent)")

        val functionFits = firSymbolForCandidate.withResolvedFirOfType<KtFirFunctionSymbol, FirSimpleFunction, Boolean> { firFunction ->
            checkExtension(firFunction, originalFile, originalEnclosingFunction, nameExpression, possibleExplicitReceiver)
        }
        val propertyFits = firSymbolForCandidate.withResolvedFirOfType<KtFirPropertySymbol, FirProperty, Boolean> { firProperty ->
            checkExtension(firProperty, originalFile, originalEnclosingFunction, nameExpression, possibleExplicitReceiver)
        }

        functionFits ?: propertyFits ?: false
    }

    private inline fun <reified T : KtFirSymbol<F>, F : FirDeclaration, R> KtCallableSymbol.withResolvedFirOfType(
        noinline action: (F) -> R,
    ): R? = this.safeAs<T>()?.firRef?.withFir(FirResolvePhase.BODY_RESOLVE, action)

    private fun checkExtension(
        candidateSymbol: FirCallableDeclaration<*>,
        originalFile: KtFile,
        originalEnclosingFunction: KtNamedFunction,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): Boolean {
        val file = originalFile.getOrBuildFirOfType<FirFile>(firResolveState)
        val explicitReceiverExpression = possibleExplicitReceiver?.getOrBuildFirOfType<FirExpression>(firResolveState)
        val resolver = SingleCandidateResolver(firResolveState.firIdeSourcesSession, file)
        val implicitReceivers = getImplicitReceivers(file, nameExpression, originalEnclosingFunction)

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
        file: FirFile,
        fakeNameExpression: KtElement,
        originalEnclosingFunction: KtNamedFunction,
    ): Sequence<ImplicitReceiverValue<*>?> {
        val fakeEnclosingFunction = fakeNameExpression.getNonStrictParentOfType<KtNamedFunction>()
            ?: error("Cannot find enclosing function for ${fakeNameExpression.getElementTextInContext()}")

        val completionContext = completionContextCache.getOrCreate(file to fakeEnclosingFunction) {
            LowLevelFirApiFacade.buildCompletionContextForFunction(
                file,
                fakeEnclosingFunction,
                originalEnclosingFunction,
                state = firResolveState
            )
        }

        val towerDataContext = completionContext.getTowerDataContext(fakeNameExpression)

        return sequence {
            yield(null) // otherwise explicit receiver won't be checked when there are no implicit receivers in completion position
            yieldAll(towerDataContext.implicitReceiverStack)
        }
    }
}
