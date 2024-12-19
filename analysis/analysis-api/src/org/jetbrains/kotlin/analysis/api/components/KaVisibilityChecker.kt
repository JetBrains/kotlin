/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.psi.KtExpression

public interface KaVisibilityChecker : KaSessionComponent {
    /**
     * Checks whether the [candidateSymbol] is visible in the [useSiteFile] from the given [position].
     *
     * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
     *  which the [candidateSymbol] is called on, if applicable.
     */
    @KaExperimentalApi
    public fun isVisible(
        candidateSymbol: KaDeclarationSymbol,
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement
    ): Boolean

    /**
     * Checks whether the given [KaCallableSymbol] (possibly inherited from a superclass) is visible in the given [classSymbol].
     */
    @KaExperimentalApi
    public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean

    /**
     * Whether the symbol is effectively public, including internal declarations with the [PublishedApi] annotation.
     *
     * In ['Explicit API' mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md), explicit visibility modifiers
     * and explicit return types are required for such symbols.
     */
    public fun isPublicApi(symbol: KaDeclarationSymbol): Boolean
}
