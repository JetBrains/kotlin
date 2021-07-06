/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.psi.KtExpression

public abstract class KtVisibilityChecker : KtAnalysisSessionComponent() {
    public abstract fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        position: PsiElement,
        receiverExpression: KtExpression?
    ): Boolean
}

public interface KtVisibilityCheckerMixIn : KtAnalysisSessionMixIn {
    public fun isVisible(
        candidateSymbol: KtSymbolWithVisibility,
        useSiteFile: KtFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement
    ): Boolean =
        analysisSession.visibilityChecker.isVisible(candidateSymbol, useSiteFile, position, receiverExpression)
}
