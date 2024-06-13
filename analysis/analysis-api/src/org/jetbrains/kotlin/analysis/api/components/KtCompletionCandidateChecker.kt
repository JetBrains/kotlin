/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

public abstract class KaCompletionCandidateChecker : KaSessionComponent() {
    public abstract fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KaCompletionExtensionCandidateChecker

    @Deprecated("Use createExtensionCandidateChecker() instead.")
    public abstract fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KaCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KaExtensionApplicabilityResult
}

public typealias KtCompletionCandidateChecker = KaCompletionCandidateChecker

public interface KaCompletionExtensionCandidateChecker {
    public fun computeApplicability(candidate: KaCallableSymbol): KaExtensionApplicabilityResult
}

public typealias KtCompletionExtensionCandidateChecker = KaCompletionExtensionCandidateChecker

public sealed class KaExtensionApplicabilityResult : KaLifetimeOwner {
    public sealed class Applicable : KaExtensionApplicabilityResult() {
        public abstract val receiverCastRequired: Boolean
        public abstract val substitutor: KaSubstitutor
    }

    public class ApplicableAsExtensionCallable(
        substitutor: KaSubstitutor,
        receiverCastRequired: Boolean,
        override val token: KaLifetimeToken
    ) : Applicable() {
        override val substitutor: KaSubstitutor by validityAsserted(substitutor)
        override val receiverCastRequired: Boolean by validityAsserted(receiverCastRequired)
    }

    public class ApplicableAsFunctionalVariableCall(
        substitutor: KaSubstitutor,
        receiverCastRequired: Boolean,
        override val token: KaLifetimeToken
    ) : Applicable() {
        override val substitutor: KaSubstitutor by validityAsserted(substitutor)
        override val receiverCastRequired: Boolean by validityAsserted(receiverCastRequired)
    }

    public class NonApplicable(
        override val token: KaLifetimeToken
    ) : KaExtensionApplicabilityResult()
}

public typealias KtExtensionApplicabilityResult = KaExtensionApplicabilityResult

public interface KaCompletionCandidateCheckerMixIn : KaSessionMixIn {
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
    ): KaCompletionExtensionCandidateChecker {
        return analysisSession.completionCandidateChecker.createExtensionCandidateChecker(
            originalFile,
            nameExpression,
            explicitReceiver
        )
    }

    @Deprecated("Use createExtensionCandidateChecker() instead.")
    public fun KaCallableSymbol.checkExtensionIsSuitable(
        originalPsiFile: KtFile,
        psiFakeCompletionExpression: KtSimpleNameExpression,
        psiReceiverExpression: KtExpression?,
    ): KaExtensionApplicabilityResult = withValidityAssertion {
        @Suppress("DEPRECATION")
        analysisSession.completionCandidateChecker.checkExtensionFitsCandidate(
            this,
            originalPsiFile,
            psiFakeCompletionExpression,
            psiReceiverExpression
        )
    }
}

public typealias KtCompletionCandidateCheckerMixIn = KaCompletionCandidateCheckerMixIn