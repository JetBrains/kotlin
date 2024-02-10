/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

public abstract class KtCompletionCandidateChecker : KtAnalysisSessionComponent() {
    public abstract fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KtCompletionExtensionCandidateChecker

    @Deprecated("Use createExtensionCandidateChecker() instead.")
    public abstract fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult
}

public interface KtCompletionExtensionCandidateChecker {
    context(KtAnalysisSession)
    public fun computeApplicability(candidate: KtCallableSymbol): KtExtensionApplicabilityResult
}

public sealed class KtExtensionApplicabilityResult : KtLifetimeOwner {
    public sealed class Applicable : KtExtensionApplicabilityResult() {
        public abstract val receiverCastRequired: Boolean
        public abstract val substitutor: KtSubstitutor
    }

    public class ApplicableAsExtensionCallable(
        private val _substitutor: KtSubstitutor,
        private val _receiverCastRequired: Boolean,
        override val token: KtLifetimeToken
    ) : Applicable() {
        override val substitutor: KtSubstitutor = withValidityAssertion { _substitutor }
        override val receiverCastRequired: Boolean get() = withValidityAssertion { _receiverCastRequired }
    }

    public class ApplicableAsFunctionalVariableCall(
        private val _substitutor: KtSubstitutor,
        private val _receiverCastRequired: Boolean,
        override val token: KtLifetimeToken
    ) : Applicable() {
        override val substitutor: KtSubstitutor get() = withValidityAssertion { _substitutor }
        override val receiverCastRequired: Boolean get() = withValidityAssertion { _receiverCastRequired }
    }

    public class NonApplicable(
        override val token: KtLifetimeToken
    ) : KtExtensionApplicabilityResult()
}

public interface KtCompletionCandidateCheckerMixIn : KtAnalysisSessionMixIn {
    /**
     * Returns an extension applicability checker for the given context [nameExpression].
     * The function is meant to only be used for providing auto-completion for Kotlin in IntelliJ IDEA.
     *
     * The returned checker does not cache the results for individual callable candidates.
     *
     * @param originalFile The file being edited.
     * @param nameExpression The expression under the caret in an in-memory copy of [originalFile]
     *     with a dummy identifier inserted. Also see `CompletionUtilCore.DUMMY_IDENTIFIER` in IntelliJ IDEA.
     * @param explicitReceiver A receiver expression, if available (also from the in-memory copy of [originalFile]).
     */
    public fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KtCompletionExtensionCandidateChecker {
        return analysisSession.completionCandidateChecker.createExtensionCandidateChecker(
            originalFile,
            nameExpression,
            explicitReceiver
        )
    }

    @Deprecated("Use createExtensionCandidateChecker() instead.")
    public fun KtCallableSymbol.checkExtensionIsSuitable(
        originalPsiFile: KtFile,
        psiFakeCompletionExpression: KtSimpleNameExpression,
        psiReceiverExpression: KtExpression?,
    ): KtExtensionApplicabilityResult = withValidityAssertion {
        @Suppress("DEPRECATION")
        analysisSession.completionCandidateChecker.checkExtensionFitsCandidate(
            this,
            originalPsiFile,
            psiFakeCompletionExpression,
            psiReceiverExpression
        )
    }
}