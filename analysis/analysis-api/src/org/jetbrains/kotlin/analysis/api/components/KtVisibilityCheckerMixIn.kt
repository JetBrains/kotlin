/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KtVisibilityChecker : KtAnalysisSessionComponent() {
    public abstract fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean

    public abstract fun isPublicApi(symbol: KtSymbolWithVisibility): Boolean
}

public interface KtVisibilityCheckerMixIn : KtAnalysisSessionMixIn {
    public fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement
    ): Boolean = withValidityAssertion {
        analysisSession.visibilityChecker.isVisible(candidateSymbol, useSiteFile, position, receiverExpression)
    }

    /**
     * Returns true for effectively public symbols, including internal declarations with @PublishedApi annotation.
     * In 'Explicit API' mode explicit visibility modifier and explicit return types are required for such symbols.
     * See FirExplicitApiDeclarationChecker.kt
     */
    public fun isPublicApi(symbol: KtSymbolWithVisibility): Boolean = withValidityAssertion {
        analysisSession.visibilityChecker.isPublicApi(symbol)
    }
}
