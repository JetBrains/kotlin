/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction

public abstract class KaExpressionTypeProvider : KaSessionComponent() {
    public abstract fun getKtExpressionType(expression: KtExpression): KaType?
    public abstract fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KaType
    public abstract fun getFunctionalTypeForKtFunction(declaration: KtFunction): KaType

    public abstract fun getExpectedType(expression: PsiElement): KaType?
    public abstract fun isDefinitelyNull(expression: KtExpression): Boolean
    public abstract fun isDefinitelyNotNull(expression: KtExpression): Boolean
}

public typealias KtExpressionTypeProvider = KaExpressionTypeProvider

public interface KaExpressionTypeProviderMixIn : KaSessionMixIn {
    /**
     * Get type of given expression.
     *
     * Return:
     * - [KtExpression] type if given [KtExpression] is real expression;
     * - `null` for [KtExpression] inside pacakges and import declarations;
     * - `Unit` type for statements;
     */
    public fun KtExpression.getKaType(): KaType? =
        withValidityAssertion { analysisSession.expressionTypeProvider.getKtExpressionType(this) }

    public fun KtExpression.getKtType(): KaType? = getKaType()

    /**
     * Returns the return type of the given [KtDeclaration] as [KaType].
     *
     * IMPORTANT: For `vararg foo: T` parameter returns full `Array<out T>` type (unlike
     * [KaValueParameterSymbol.returnType][org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol.returnType],
     * which returns `T`).
     */
    public fun KtDeclaration.getReturnKaType(): KaType =
        withValidityAssertion { analysisSession.expressionTypeProvider.getReturnTypeForKtDeclaration(this) }

    public fun KtDeclaration.getReturnKtType(): KaType = getReturnKaType()

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
    public fun KtFunction.getFunctionalType(): KaType =
        withValidityAssertion { analysisSession.expressionTypeProvider.getFunctionalTypeForKtFunction(this) }

    /**
     * Returns the expected [KaType] of this [PsiElement] if it is an expression. The returned value should not be a
     * [org.jetbrains.kotlin.analysis.api.types.KaErrorType].
     */
    public fun PsiElement.getExpectedType(): KaType? =
        withValidityAssertion { analysisSession.expressionTypeProvider.getExpectedType(this) }

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
    public fun KtExpression.isDefinitelyNull(): Boolean =
        withValidityAssertion { analysisSession.expressionTypeProvider.isDefinitelyNull(this) }

    /**
     * Returns `true` if this expression is definitely not null. See [isDefinitelyNull] for examples.
     */
    public fun KtExpression.isDefinitelyNotNull(): Boolean =
        withValidityAssertion { analysisSession.expressionTypeProvider.isDefinitelyNotNull(this) }
}

public typealias KtExpressionTypeProviderMixIn = KaExpressionTypeProviderMixIn