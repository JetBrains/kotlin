/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KaVisibilityChecker : KaSessionComponent() {
    public abstract fun isVisible(
        candidateSymbol: KaSymbolWithVisibility,
        useSiteFile: KaFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean

    public abstract fun isPublicApi(symbol: KaSymbolWithVisibility): Boolean
}

public typealias KtVisibilityChecker = KaVisibilityChecker

public interface KaVisibilityCheckerMixIn : KaSessionMixIn {
    public fun isVisible(
        candidateSymbol: KaSymbolWithVisibility,
        useSiteFile: KaFileSymbol,
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
    public fun isPublicApi(symbol: KaSymbolWithVisibility): Boolean = withValidityAssertion {
        analysisSession.visibilityChecker.isPublicApi(symbol)
    }
}

public typealias KtVisibilityCheckerMixIn = KaVisibilityCheckerMixIn