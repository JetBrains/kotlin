/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

public interface KaExpressionTypeProvider {
    /**
     * Get type of given expression.
     *
     * Return:
     * - [KtExpression] type if given [KtExpression] is real expression;
     * - `null` for [KtExpression] inside pacakges and import declarations;
     * - `Unit` type for statements;
     */
    public val KtExpression.expressionType: KaType?

    @Deprecated("Use 'expressionType' instead.", replaceWith = ReplaceWith("expressionType"))
    public fun KtExpression.getKaType(): KaType? = expressionType

    @Deprecated("Use 'expressionType' instead.", replaceWith = ReplaceWith("expressionType"))
    public fun KtExpression.getKtType(): KaType? = expressionType

    /**
     * Returns the return type of the given [KtDeclaration] as [KaType].
     *
     * IMPORTANT: For `vararg foo: T` parameter returns full `Array<out T>` type (unlike
     * [KaValueParameterSymbol.returnType][org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol.returnType],
     * which returns `T`).
     */
    public val KtDeclaration.returnType: KaType

    @Deprecated("Use 'returnType' instead.", ReplaceWith("returnType"))
    public fun KtDeclaration.getReturnKaType(): KaType = returnType

    @Deprecated("Use 'returnType' instead.", ReplaceWith("returnType"))
    public fun KtDeclaration.getReturnKtType(): KaType = returnType

    /**
     * Returns the functional type of the given [KtFunction].
     *
     * For a regular function, it would be kotlin.FunctionN<Ps, R> where
     *   N is the number of value parameters in the function;
     *   Ps are types of value parameters;
     *   R is the return type of the function.
     * Depending on the function's attributes, such as `suspend` or reflective access, different functional type,
     * such as `SuspendFunction`, `KFunction`, or `KSuspendFunction`, will be constructed.
     */
    public val KtFunction.functionType: KaType

    @Deprecated("Use 'functionType' instead.", ReplaceWith("functionType"))
    public fun KtFunction.getFunctionalType(): KaType = functionType

    /**
     * Returns the expected [KaType] of this [PsiElement] if it is an expression. The returned value should not be a
     * [org.jetbrains.kotlin.analysis.api.types.KaErrorType].
     */
    public val PsiElement.expectedType: KaType?

    /**
     * Returns `true` if this expression is definitely null, based on declared nullability and smart cast types derived from
     * data-flow analysis facts. Examples:
     * ```
     *   public fun <T : Any> foo(t: T, nt: T?, s: String, ns: String?) {
     *     t     // t.isDefinitelyNull()  == false && t.isDefinitelyNotNull()  == true
     *     nt    // nt.isDefinitelyNull() == false && nt.isDefinitelyNotNull() == false
     *     s     // s.isDefinitelyNull()  == false && s.isDefinitelyNotNull()  == true
     *     ns    // ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == false
     *
     *     if (ns != null) {
     *       ns  // ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == true
     *     } else {
     *       ns  // ns.isDefinitelyNull() == true  && ns.isDefinitelyNotNull() == false
     *     }
     *
     *     ns!!  // From this point on: ns.isDefinitelyNull() == false && ns.isDefinitelyNotNull() == true
     *   }
     * ```
     * Note that only nullability from "stable" smart cast types is considered. The
     * [spec](https://kotlinlang.org/spec/type-inference.html#smart-cast-sink-stability) provides an explanation on smart cast stability.
     */
    public val KtExpression.isDefinitelyNull: Boolean

    /**
     * Returns `true` if this expression is definitely not null. See [isDefinitelyNull] for examples.
     */
    public val KtExpression.isDefinitelyNotNull: Boolean
}