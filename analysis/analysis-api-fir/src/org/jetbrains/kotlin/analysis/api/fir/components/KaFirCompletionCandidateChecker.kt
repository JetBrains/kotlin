/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.components.KaCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KaCompletionExtensionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KaExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.components.KaExtensionApplicabilityResult.*
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.symbols.KaFirSymbol
import org.jetbrains.kotlin.analysis.api.fir.utils.createSubstitutorFromTypeArguments
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
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
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildExpressionStub
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirErrorReferenceWithCandidate
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.receiverType
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

internal class KaFirCompletionCandidateChecker(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaCompletionCandidateChecker, KaFirSessionComponent {
    override fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KaCompletionExtensionCandidateChecker = withPsiValidityAssertion(originalFile, nameExpression, explicitReceiver) {
        KaLazyCompletionExtensionCandidateChecker(analysisSession.token) {
            KaFirCompletionExtensionCandidateChecker(analysisSession, nameExpression, explicitReceiver, originalFile)
        }
    }
}

private class KaFirCompletionExtensionCandidateChecker(
    private val analysisSession: KaFirSession,
    private val nameExpression: KtSimpleNameExpression,
    explicitReceiver: KtExpression?,
    originalFile: KtFile,
) : KaCompletionExtensionCandidateChecker {
    private val resolutionFacade = analysisSession.resolutionFacade

    private val implicitReceivers: List<ImplicitReceiverValue<*>>
    private val firCallSiteSession: FirSession
    private val firOriginalFile: FirFile
    private val explicitReceiverInfo: ExplicitReceiverInfo?
    private val candidateResolver: SingleCandidateResolver
    private val containingCallableReference: KtCallableReferenceExpression?

    init {
        val fakeFile = nameExpression.containingKtFile
        val firFakeFile = fakeFile.getOrBuildFirFile(resolutionFacade)

        containingCallableReference = explicitReceiver?.parent as? KtCallableReferenceExpression
        implicitReceivers = computeImplicitReceivers(firFakeFile)
        firCallSiteSession = firFakeFile.llFirSession
        firOriginalFile = originalFile.getOrBuildFirFile(resolutionFacade)
        explicitReceiverInfo = explicitReceiver?.let(::getExplicitReceiverInfo)
        candidateResolver = SingleCandidateResolver(firCallSiteSession, firOriginalFile)
    }

    override val token: KaLifetimeToken
        get() = analysisSession.token

    override fun computeApplicability(candidate: KaCallableSymbol): KaExtensionApplicabilityResult = withValidityAssertion {
        if (candidate is KaReceiverParameterSymbol) {
            return NonApplicable(token)
        }

        require(candidate is KaFirSymbol<*>)

        val firSymbol = candidate.firSymbol as FirCallableSymbol<*>
        firSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)

        val resolutionMode = if (containingCallableReference != null) {
            SingleCandidateResolutionMode.CHECK_EXTENSION_CALLABlE_REFERENCE_FOR_COMPLETION
        } else {
            SingleCandidateResolutionMode.CHECK_EXTENSION_FOR_COMPLETION
        }

        fun processReceiver(implicitReceiverValue: ImplicitReceiverValue<*>?): KaExtensionApplicabilityResult? {
            val resolutionParameters = ResolutionParameters(
                singleCandidateResolutionMode = resolutionMode,
                callableSymbol = firSymbol,
                implicitReceiver = implicitReceiverValue,
                explicitReceiver = explicitReceiverInfo?.receiverExpression,
                allowUnsafeCall = true,
                allowUnstableSmartCast = true,
                callableReferenceLHS = explicitReceiverInfo?.callableReferenceLHS
            )

            val firResolvedCall = candidateResolver.resolveSingleCandidate(resolutionParameters) ?: return null
            val substitutor = firResolvedCall.createSubstitutorFromTypeArguments(analysisSession) ?: return null

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

    private fun computeImplicitReceivers(firFakeFile: FirFile): List<ImplicitReceiverValue<*>> {
        val elementContext = ContextCollector.process(
            resolutionFacade = resolutionFacade,
            file = firFakeFile,
            targetElement = nameExpression,
            preferBodyContext = false
        )

        val towerDataContext = elementContext?.towerDataContext
            ?: errorWithAttachment("Cannot find enclosing declaration for ${nameExpression::class}") {
                withPsiEntry("fakeNameExpression", nameExpression)
            }

        return buildList {
            addAll(towerDataContext.implicitValueStorage.implicitReceivers)
        }
    }

    /**
     * Returns a [ExplicitReceiverInfo] matching the given PSI [receiverExpression].
     *
     * @param receiverExpression a qualified expression receiver (e.g., `foo` in `foo?.bar()`, or in `foo.bar`).
     *
     * The function unwraps certain receiver expressions. For instance, for safe calls direct counterpart to a [KtSafeQualifiedExpression]
     * is (FirCheckedSafeCallSubject)[org.jetbrains.kotlin.fir.expressions.FirCheckedSafeCallSubject] which requires additional unwrapping
     * to be used for call resolution.
     */
    private fun getExplicitReceiverInfo(receiverExpression: KtExpression): ExplicitReceiverInfo? {
        if (receiverExpression is KtStatementExpression) {
            // FIR for 'KtStatementExpression' is not a 'FirExpression'
            return null
        }

        val parentCall = receiverExpression.getQualifiedExpressionForReceiver()
        if (parentCall is KtSafeQualifiedExpression) {
            val firSafeCall = parentCall.getOrBuildFirOfType<FirSafeCallExpression>(resolutionFacade)
            return ExplicitReceiverInfo(firSafeCall.checkedSubjectRef.value)
        }

        val receiverExpressionFir = receiverExpression.getOrBuildFirOfType<FirExpression>(resolutionFacade)

        val callableReferenceLHS =
            if (containingCallableReference != null) {
                val callableReferenceFir = containingCallableReference.getOrBuildFirOfType<FirCallableReferenceAccess>(resolutionFacade)
                val resolver = SingleCandidateResolver(firCallSiteSession, firOriginalFile)
                val components = resolver.bodyResolveComponents
                val context = components.context
                context.withFile(firOriginalFile, components) {
                    components.doubleColonExpressionResolver.resolveDoubleColonLHS(callableReferenceFir)
                }
            } else {
                null
            }

        val refinedReceiverExpression =
            if (containingCallableReference != null &&
                receiverExpressionFir is FirResolvedQualifier &&
                callableReferenceLHS is DoubleColonLHS.Type
            ) {
                /**
                 * If it's a callable reference completion and the LHS is a regular name reference,
                 * we need to create a stub expression with the type of the referenced class.
                 * Otherwise, the type of the receiver would be `Unit`.
                 * The same mechanism is used when creating callable reference info in the compiler.
                 *
                 * ```kotlin
                 * class A
                 *
                 * fun A.foo() {}
                 *
                 * val x = A::foo
                 * ```
                 */
                buildExpressionStub {
                    source = receiverExpressionFir.source
                    coneTypeOrNull = callableReferenceLHS.type
                }
            } else {
                receiverExpressionFir
            }

        return ExplicitReceiverInfo(refinedReceiverExpression, callableReferenceLHS)
    }

    private data class ExplicitReceiverInfo(
        val receiverExpression: FirExpression?,
        val callableReferenceLHS: DoubleColonLHS? = null
    )
}

private class KaLazyCompletionExtensionCandidateChecker(
    override val token: KaLifetimeToken,
    delegateFactory: () -> KaCompletionExtensionCandidateChecker,
) : KaCompletionExtensionCandidateChecker {
    private val delegate: KaCompletionExtensionCandidateChecker by lazy(delegateFactory)

    override fun computeApplicability(candidate: KaCallableSymbol): KaExtensionApplicabilityResult =
        withValidityAssertion { delegate.computeApplicability(candidate) }
}
