/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KaSubstitutor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

@KaIdeApi
public interface KaCompletionCandidateChecker {
    /**
     * Returns an extension applicability checker for the given context [nameExpression].
     * The function is meant to only be used for providing auto-completion for Kotlin in IntelliJ IDEA.
     *
     * The returned checker does not cache the results for individual callable candidates.
     *
     * @param originalFile The file being edited.
     * @param nameExpression The expression under the caret in an in-memory copy of [originalFile]
     *     with a placeholder identifier inserted. Also see `CompletionUtilCore.DUMMY_IDENTIFIER` in IntelliJ IDEA.
     * @param explicitReceiver A receiver expression, if available (also from the in-memory copy of [originalFile]).
     */
    @KaIdeApi
    public fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KaCompletionExtensionCandidateChecker
}

@KaIdeApi
public interface KaCompletionExtensionCandidateChecker {
    /**
     * Checks if the given [candidate] is applicable as an extension callable in the current context.
     */
    @KaIdeApi
    @KaExperimentalApi
    public fun computeApplicability(candidate: KaCallableSymbol): KaExtensionApplicabilityResult
}

@KaIdeApi
@Deprecated("Use 'KaCompletionExtensionCandidateChecker' instead.", replaceWith = ReplaceWith("KaCompletionExtensionCandidateChecker"))
public typealias KtCompletionExtensionCandidateChecker = KaCompletionExtensionCandidateChecker

/**
 * Represents the result of checking the applicability of an extension callable in a specific context.
 */
@KaIdeApi
@KaExperimentalApi
public sealed class KaExtensionApplicabilityResult : KaLifetimeOwner {
    @KaIdeApi
    @KaExperimentalApi
    public sealed class Applicable : KaExtensionApplicabilityResult() {
        public abstract val receiverCastRequired: Boolean

        @KaExperimentalApi
        public abstract val substitutor: KaSubstitutor
    }

    @KaIdeApi
    @KaExperimentalApi
    public class ApplicableAsExtensionCallable(
        substitutor: KaSubstitutor,
        receiverCastRequired: Boolean,
        override val token: KaLifetimeToken
    ) : Applicable() {
        override val substitutor: KaSubstitutor by validityAsserted(substitutor)
        override val receiverCastRequired: Boolean by validityAsserted(receiverCastRequired)
    }

    @KaIdeApi
    @KaExperimentalApi
    public class ApplicableAsFunctionalVariableCall(
        substitutor: KaSubstitutor,
        receiverCastRequired: Boolean,
        override val token: KaLifetimeToken
    ) : Applicable() {
        override val substitutor: KaSubstitutor by validityAsserted(substitutor)
        override val receiverCastRequired: Boolean by validityAsserted(receiverCastRequired)
    }

    @KaIdeApi
    @KaExperimentalApi
    public class NonApplicable(
        override val token: KaLifetimeToken
    ) : KaExtensionApplicabilityResult()
}

@KaIdeApi
@KaExperimentalApi
@Deprecated("Use 'KaExtensionApplicabilityResult' instead.", replaceWith = ReplaceWith("KaExtensionApplicabilityResult"))
public typealias KtExtensionApplicabilityResult = KaExtensionApplicabilityResult