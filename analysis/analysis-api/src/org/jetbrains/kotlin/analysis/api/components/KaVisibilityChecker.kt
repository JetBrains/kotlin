/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
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
    @Deprecated(
        "Use `createUseSiteVisibilityChecker` instead. It's much more performant for multiple visibility checks on the same use-site",
        replaceWith = ReplaceWith("createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position).isVisible(candidateSymbol)")
    )
    @KaExperimentalApi
    public fun isVisible(
        candidateSymbol: KaDeclarationSymbol,
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement,
    ): Boolean = withValidityAssertion {
        createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position).isVisible(candidateSymbol)
    }

    /**
     * Creates a visibility checker for the given use-site position.
     *
     * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
     *  which the candidate symbol is called on, if applicable.
     *
     * @see KaUseSiteVisibilityChecker
     */
    @KaExperimentalApi
    public fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker

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

/**
 * Allows checking if [KaDeclarationSymbol] is visible from the current use-site.
 *
 * [KaUseSiteVisibilityChecker] is created by [KaVisibilityChecker.createUseSiteVisibilityChecker].
 *
 * [KaUseSiteVisibilityChecker] is designed to be reused. Therefore, if you have multiple candidates to check from the same use-site position,
 * it will be more performant to reuse the same [KaUseSiteVisibilityChecker].
 */
@KaExperimentalApi
public interface KaUseSiteVisibilityChecker : KaLifetimeOwner {
    /**
     * Checks whether the [candidateSymbol] is visible at the current use-site.
     *
     * @param candidateSymbol The symbol whose visibility is to be checked.
     * @return `true` if the [candidateSymbol] is visible from the current use-site, `false` otherwise.
     */
    @KaExperimentalApi
    public fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean
}