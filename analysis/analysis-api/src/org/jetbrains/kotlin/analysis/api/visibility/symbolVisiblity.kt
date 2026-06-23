/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.visibility

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.internals
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.psi.KtExpression

/**
 * Creates a visibility checker for the given use-site position.
 *
 * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
 *  which the candidate symbol is called on, if applicable.
 *
 * @see KaUseSiteVisibilityChecker
 */
@KaExperimentalApi
context(session: KaSession)
public fun createUseSiteVisibilityChecker(
    useSiteFile: KaFileSymbol,
    receiverExpression: KtExpression?,
    position: PsiElement,
): KaUseSiteVisibilityChecker {
    @OptIn(KaImplementationDetail::class)
    return internals.visibilityChecker.createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position)
}

/**
 * Checks whether the given [KaCallableSymbol] (possibly inherited from a superclass) is visible in the given [classSymbol].
 */
@KaExperimentalApi
context(session: KaSession)
public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean {
    @OptIn(KaImplementationDetail::class)
    return internals.visibilityChecker.isVisibleInClass(this, classSymbol)
}

/**
 * Whether the symbol is effectively public, including internal declarations with the [PublishedApi] annotation.
 *
 * In ['Explicit API' mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md), explicit visibility modifiers
 * and explicit return types are required for such symbols.
 */
context(session: KaSession)
public val KaDeclarationSymbol.isPublicApi: Boolean
    get() {
        @OptIn(KaImplementationDetail::class)
        return internals.visibilityChecker.isPublicApi(this)
    }
