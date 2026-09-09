/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.psi.KtExpression

@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaVisibilityChecker : KaSessionComponent {
    /**
     * Checks whether the [candidateSymbol] is visible in the [useSiteFile] from the given [position].
     *
     * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
     *  which the [candidateSymbol] is called on, if applicable.
     */
    @KaNoContextParameterBridgeRequired
    @KaExperimentalApi
    @Deprecated(
        "Use `createUseSiteVisibilityChecker` instead. It's much more performant for multiple visibility checks on the same use-site",
        replaceWith = ReplaceWith("createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position).isVisible(candidateSymbol)")
    )
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
     * Checks whether the given [KaCallableSymbol] is visible in the given [classSymbol], that is, whether the code inside [classSymbol]
     * can access the callable.
     *
     * The callable is expected to be a member of [classSymbol], either declared in it or inherited from one of its supertypes, e.g., a
     * callable from the class's member scope. This makes the check suitable for operations on a class as a whole, such as finding out
     * which inherited members can be overridden or called from a new member of the class. To check the visibility of an arbitrary symbol
     * from a specific position in code, use [createUseSiteVisibilityChecker] instead.
     *
     * ### Visibility rules
     *
     * - Public callables are always visible.
     * - Protected callables are always visible, since [classSymbol] is assumed to be the owner of the callable or its subclass.
     * - Internal callables are visible if the module of [classSymbol] is the module of the callable or its friend.
     * - Package-private Java callables are visible if [classSymbol] is in the same package as the callable.
     * - Private callables are visible if they are declared in [classSymbol], in one of the classes containing it (including the classes
     *   containing a local [classSymbol]), or in a companion object of any of these classes. A private callable of a supertype is not
     *   visible in its subclasses.
     *
     * #### Example
     *
     * ```
     * open class Base {
     *     protected fun inherited() {}
     *     private fun hidden() {}
     * }
     *
     * class Derived : Base() {
     *     private fun own() {}
     * }
     * ```
     *
     * In `Derived`, `inherited` and `own` are visible, while `hidden` is not.
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
 * **The type has been moved to a new package. Use [org.jetbrains.kotlin.analysis.api.visibility.KaUseSiteVisibilityChecker] instead.**
 *
 * Allows checking if [KaDeclarationSymbol] is visible from the current use-site.
 *
 * [KaUseSiteVisibilityChecker] is created by [KaVisibilityChecker.createUseSiteVisibilityChecker].
 *
 * [KaUseSiteVisibilityChecker] is designed to be reused. Therefore, if you have multiple candidates to check from the same use-site position,
 * it will be more performant to reuse the same [KaUseSiteVisibilityChecker].
 */
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
 * Creates a visibility checker for the given use-site position.
 *
 * @param receiverExpression The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) expression
 *  which the candidate symbol is called on, if applicable.
 *
 * @see KaUseSiteVisibilityChecker
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.visibility' endpoint instead.",
    replaceWith = ReplaceWith(
        "createUseSiteVisibilityChecker(useSiteFile, receiverExpression, position)",
        "org.jetbrains.kotlin.analysis.api.visibility.createUseSiteVisibilityChecker",
    ),
    level = DeprecationLevel.ERROR,
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun createUseSiteVisibilityChecker(
    useSiteFile: KaFileSymbol,
    receiverExpression: KtExpression? = null,
    position: PsiElement,
): KaUseSiteVisibilityChecker {
    return with(session) {
        createUseSiteVisibilityChecker(
            useSiteFile = useSiteFile,
            receiverExpression = receiverExpression,
            position = position,
        )
    }
}

/**
 * Checks whether the given [KaCallableSymbol] (possibly inherited from a superclass) is visible in the given [classSymbol].
 */
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.visibility' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.isVisibleInClass(classSymbol)",
        "org.jetbrains.kotlin.analysis.api.visibility.isVisibleInClass",
    ),
    level = DeprecationLevel.ERROR,
)
@KaExperimentalApi
@KaContextParameterApi
context(session: KaSession)
public fun KaCallableSymbol.isVisibleInClass(classSymbol: KaClassSymbol): Boolean {
    return with(session) {
        isVisibleInClass(
            classSymbol = classSymbol,
        )
    }
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
    level = DeprecationLevel.ERROR,
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
