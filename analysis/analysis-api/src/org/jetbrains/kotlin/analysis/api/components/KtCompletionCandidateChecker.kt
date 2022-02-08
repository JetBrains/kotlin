/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.types.KtSubstitutor
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

public abstract class KtCompletionCandidateChecker : KtAnalysisSessionComponent() {
    public abstract fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): KtExtensionApplicabilityResult
}

public sealed class KtExtensionApplicabilityResult {
    public abstract val isApplicable: Boolean
    public abstract val substitutor: KtSubstitutor

    public class ApplicableAsExtensionCallable(override val substitutor: KtSubstitutor) : KtExtensionApplicabilityResult() {
        override val isApplicable: Boolean get() = true
    }

    public class ApplicableAsFunctionalVariableCall(override val substitutor: KtSubstitutor) : KtExtensionApplicabilityResult() {
        override val isApplicable: Boolean get() = true
    }

    public class NonApplicable(override val substitutor: KtSubstitutor) : KtExtensionApplicabilityResult() {
        override val isApplicable: Boolean = false
    }
}

public interface KtCompletionCandidateCheckerMixIn : KtAnalysisSessionMixIn {
    public fun KtCallableSymbol.checkExtensionIsSuitable(
        originalPsiFile: KtFile,
        psiFakeCompletionExpression: KtSimpleNameExpression,
        psiReceiverExpression: KtExpression?,
    ): KtExtensionApplicabilityResult =
        analysisSession.completionCandidateChecker.checkExtensionFitsCandidate(
            this,
            originalPsiFile,
            psiFakeCompletionExpression,
            psiReceiverExpression
        )
}