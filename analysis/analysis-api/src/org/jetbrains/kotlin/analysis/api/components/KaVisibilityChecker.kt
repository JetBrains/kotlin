/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.psi.KtExpression

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaVisibilityChecker : KaLifetimeOwner {
    /**
     * Creates a visibility checker for the given use-site position.
     *
     * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
     *  which the candidate symbol is called on, if applicable.
     *
     * @see KaUseSiteVisibilityChecker
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.visibility.createUseSiteVisibilityChecker'" +
                " from the context-parameter API instead.",
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun createUseSiteVisibilityChecker(
        useSiteFile: KaFileSymbol,
        receiverExpression: KtExpression? = null,
        position: PsiElement,
    ): KaUseSiteVisibilityChecker

    /**
     * Checks whether the given [KaCallableSymbol] (possibly inherited from a superclass) is visible in the given [classSymbol].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.visibility.isVisibleInClass' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.isVisibleInClass(classSymbol)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.visibility.isVisibleInClass",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean

    /**
     * Whether the symbol is effectively public, including internal declarations with the [PublishedApi] annotation.
     *
     * In ['Explicit API' mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md), explicit visibility modifiers
     * and explicit return types are required for such symbols.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.visibility.isPublicApi' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "symbol.isPublicApi",
            imports = [
                "org.jetbrains.kotlin.analysis.api.visibility.isPublicApi",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    public fun isPublicApi(symbol: KaDeclarationSymbol): Boolean
}

/**
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker] instead.**
 *
 * Allows checking if [KaDeclarationSymbol] is visible from the current use-site.
 *
 * [KaUseSiteVisibilityChecker] is created by [KaVisibilityChecker.createUseSiteVisibilityChecker].
 *
 * [KaUseSiteVisibilityChecker] is designed to be reused. Therefore, if you have multiple candidates to check from the same use-site position,
 * it will be more performant to reuse the same [KaUseSiteVisibilityChecker].
 */
@Deprecated(
    message = "Use 'org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker' instead.",
    replaceWith = ReplaceWith(
        expression = "KaUseSiteVisibilityChecker",
        imports = [
            "org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker",
        ]
    ),
    level = DeprecationLevel.WARNING,
)
@KaObsoleteComponentApi
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaUseSiteVisibilityChecker : org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker {
    /**
     * Checks whether the [candidateSymbol] is visible at the current use-site.
     *
     * @param candidateSymbol The symbol whose visibility is to be checked.
     * @return `true` if the [candidateSymbol] is visible from the current use-site, `false` otherwise.
     */
    @KaExperimentalApi
    override fun isVisible(candidateSymbol: KaDeclarationSymbol): Boolean
}

/**
 * Whether the symbol is effectively public, including internal declarations with the [PublishedApi] annotation.
 *
 * In ['Explicit API' mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md), explicit visibility modifiers
 * and explicit return types are required for such symbols.
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.visibility' endpoint instead.",
    replaceWith = ReplaceWith(
        "symbol.isPublicApi",
        "org.jetbrains.kotlin.analysis.api.visibility.isPublicApi",
    ),
    level = DeprecationLevel.HIDDEN,
)
@KaContextParameterApi
context(session: KaSession)
public fun isPublicApi(symbol: KaDeclarationSymbol): Boolean {
    return with(session) {
        isPublicApi(
            symbol = symbol,
        )
    }
}
