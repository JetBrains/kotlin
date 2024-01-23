/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.components.KtCompletionExtensionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult.*
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KtFirSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirFile
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.ResolutionParameters
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolutionMode
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolver.SingleCandidateResolver
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.llFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.ContextCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.resolve.SessionHolderImpl
import org.jetbrains.kotlin.fir.resolve.calls.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStatementExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KtFirCompletionCandidateChecker(
    override val analysisSession: KtFirAnalysisSession,
    override val token: KtLifetimeToken,
) : KtCompletionCandidateChecker(), KtFirAnalysisSessionComponent {
    override fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KtCompletionExtensionCandidateChecker = analysisSession.withValidityAssertion {
        return LazyKtCompletionExtensionCandidateChecker {
            // Double validity check is needed, as the checker may be requested some time later
            analysisSession.withValidityAssertion {
                KtFirCompletionExtensionCandidateChecker(analysisSession, nameExpression, explicitReceiver, originalFile)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult = analysisSession.withValidityAssertion {
        val checker = KtFirCompletionExtensionCandidateChecker(analysisSession, nameExpression, possibleExplicitReceiver, originalFile)
        return with(analysisSession) {
            checker.computeApplicability(firSymbolForCandidate)
        }
    }
}

private class KtFirCompletionExtensionCandidateChecker(
    override val analysisSession: KtFirAnalysisSession,
    private val nameExpression: KtSimpleNameExpression,
    explicitReceiver: KtExpression?,
    originalFile: KtFile,
) : KtCompletionExtensionCandidateChecker, KtFirAnalysisSessionComponent {
    private val implicitReceivers: List<ImplicitReceiverValue<*>>
    private val firCallSiteSession: FirSession
    private val firOriginalFile: FirFile
    private val firExplicitReceiver: FirExpression?

    init {
        val fakeFile = nameExpression.containingKtFile
        val firFakeFile = fakeFile.getOrBuildFirFile(firResolveSession)

        implicitReceivers = computeImplicitReceivers(firFakeFile)
        firCallSiteSession = firFakeFile.llFirSession
        firOriginalFile = originalFile.getOrBuildFirFile(firResolveSession)
        firExplicitReceiver = explicitReceiver?.let(::findReceiverFirExpression)
    }

    context(KtAnalysisSession)
    override fun computeApplicability(candidate: KtCallableSymbol): KtExtensionApplicabilityResult {
        require(candidate is KtFirSymbol<*>)

        analysisSession.withValidityAssertion {
            val firSymbol = candidate.firSymbol as FirCallableSymbol<*>
            firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

            val resolver = SingleCandidateResolver(firCallSiteSession, firOriginalFile)
            val token = analysisSession.token

            fun processReceiver(implicitReceiverValue: ImplicitReceiverValue<*>?): KtExtensionApplicabilityResult? {
                val resolutionParameters = ResolutionParameters(
                    singleCandidateResolutionMode = SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION,
                    callableSymbol = firSymbol,
                    implicitReceiver = implicitReceiverValue,
                    explicitReceiver = firExplicitReceiver,
                    allowUnsafeCall = true,
                    allowUnstableSmartCast = true,
                )

                val firResolvedCall = resolver.resolveSingleCandidate(resolutionParameters) ?: return null
                val substitutor = firResolvedCall.createSubstitutorFromTypeArguments() ?: return null

                val receiverCastRequired = firResolvedCall.calleeReference is FirErrorReferenceWithCandidate

                if (firSymbol is FirVariableSymbol<*> && firSymbol.resolvedReturnType.receiverType(firCallSiteSession) != null) {
                    return ApplicableAsFunctionalVariableCall(substitutor, receiverCastRequired, token)
                }

                return ApplicableAsExtensionCallable(substitutor, receiverCastRequired, token)
            }

            return implicitReceivers.firstNotNullOfOrNull(::processReceiver)
                ?: processReceiver(null)
                ?: NonApplicable(token)
        }
    }

    private fun computeImplicitReceivers(firFakeFile: FirFile): List<ImplicitReceiverValue<*>> {
        val sessionHolder = run {
            val firSession = firFakeFile.llFirSession
            val scopeSession = firResolveSession.getScopeSessionFor(firSession)
            SessionHolderImpl(firSession, scopeSession)
        }

        val elementContext = ContextCollector.process(firFakeFile, sessionHolder, nameExpression, bodyElement = null)

        val towerDataContext = elementContext?.towerDataContext
            ?: errorWithAttachment("Cannot find enclosing declaration for ${nameExpression::class}") {
                withPsiEntry("fakeNameExpression", nameExpression)
            }

        return buildList {
            addAll(towerDataContext.implicitReceiverStack)
            for (towerDataElement in towerDataContext.towerDataElements) {
                addAll(towerDataElement.contextReceiverGroup.orEmpty())
            }
        }
    }

    /**
     * Returns a [FirExpression] matching the given PSI [receiverExpression].
     *
     * @param receiverExpression a qualified expression receiver (e.g., `foo` in `foo?.bar()`, or in `foo.bar`).
     *
     * The function unwraps certain receiver expressions. For instance, for safe calls direct counterpart to a [KtSafeQualifiedExpression]
     * is (FirCheckedSafeCallSubject)[org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject] which requires additional unwrapping
     * to be used for call resolution.
     */
    private fun findReceiverFirExpression(receiverExpression: KtExpression): FirExpression? {
        if (receiverExpression is KtStatementExpression) {
            // FIR for 'KtStatementExpression' is not a 'FirExpression'
            return null
        }

        val parentCall = receiverExpression.getQualifiedExpressionForReceiver()
        if (parentCall !is KtSafeQualifiedExpression) {
            return receiverExpression.getOrBuildFirOfType<FirExpression>(firResolveSession)
        }

        val firSafeCall = parentCall.getOrBuildFirOfType<FirSafeCallExpression>(firResolveSession)
        return firSafeCall.checkedSubjectRef.value
    }
}

private class LazyKtCompletionExtensionCandidateChecker(
    delegateFactory: () -> KtCompletionExtensionCandidateChecker
) : KtCompletionExtensionCandidateChecker {
    private val delegate: KtCompletionExtensionCandidateChecker by lazy(delegateFactory)

    context(KtAnalysisSession)
    override fun computeApplicability(candidate: KtCallableSymbol): KtExtensionApplicabilityResult {
        return delegate.computeApplicability(candidate)
    }
}