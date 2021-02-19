/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

abstract class KtCompletionCandidateChecker : KtAnalysisSessionComponent() {
    abstract fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?,
    ): Boolean
}

interface KtCompletionCandidateCheckerMixIn : KtAnalysisSessionMixIn {
    fun KtCallableSymbol.checkExtensionIsSuitable(
        originalPsiFile: KtFile,
        psiFakeCompletionExpression: KtSimpleNameExpression,
        psiReceiverExpression: KtExpression?,
    ): Boolean =
        analysisSession.completionCandidateChecker.checkExtensionFitsCandidate(
            this,
            originalPsiFile,
            psiFakeCompletionExpression,
            psiReceiverExpression
        )
}