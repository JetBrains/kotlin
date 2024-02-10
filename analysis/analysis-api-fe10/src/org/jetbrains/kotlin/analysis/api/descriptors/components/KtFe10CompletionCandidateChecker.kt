/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import org.jetbrains.kotlin.analysis.api.components.KtCompletionCandidateChecker
import org.jetbrains.kotlin.analysis.api.components.KtExtensionApplicabilityResult
import org.jetbrains.kotlin.analysis.api.components.KtCompletionExtensionCandidateChecker
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

internal class KtFe10CompletionCandidateChecker(
    override val analysisSession: KtFe10AnalysisSession
) : KtCompletionCandidateChecker(), Fe10KtAnalysisSessionComponent {
    override val token: KtLifetimeToken
        get() = analysisSession.token

    override fun createExtensionCandidateChecker(
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        explicitReceiver: KtExpression?
    ): KtCompletionExtensionCandidateChecker {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun checkExtensionFitsCandidate(
        firSymbolForCandidate: KtCallableSymbol,
        originalFile: KtFile,
        nameExpression: KtSimpleNameExpression,
        possibleExplicitReceiver: KtExpression?
    ): KtExtensionApplicabilityResult {
        throw NotImplementedError("Method is not implemented for FE 1.0")
    }
}