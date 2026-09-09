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
 * Checks whether the given [KaCallableSymbol] is visible in the given [classSymbol], that is, whether the code inside [classSymbol] can
 * access the callable.
 *
 * The callable is expected to be a member of [classSymbol], either declared in it or inherited from one of its supertypes, e.g., a
 * callable from the class's member scope. This makes the check suitable for operations on a class as a whole, such as finding out which
 * inherited members can be overridden or called from a new member of the class. To check the visibility of an arbitrary symbol from a
 * specific position in code, use [createUseSiteVisibilityChecker] instead.
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
