/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaSymbolWithVisibility
import org.jetbrains.kotlin.psi.KtExpression

public interface KaVisibilityChecker {
    @KaExperimentalApi
    public fun isVisible(
        candidateSymbol: KaSymbolWithVisibility,
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement
    ): Boolean

    /** Checks if the given symbol (possibly a symbol inherited from a super class) is visible in the given class. */
    @KaExperimentalApi
    public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassOrObjectSymbol): Boolean

    /**
     * Returns true for effectively public symbols, including internal declarations with @PublishedApi annotation.
     * In 'Explicit API' mode explicit visibility modifier and explicit return types are required for such symbols.
     * See FirExplicitApiDeclarationChecker.kt
     */
    public fun isPublicApi(symbol: KaSymbolWithVisibility): Boolean
}