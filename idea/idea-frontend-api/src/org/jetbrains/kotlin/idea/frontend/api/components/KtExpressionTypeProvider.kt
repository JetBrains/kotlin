/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.components

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtExpression

abstract class KtExpressionTypeProvider : KtAnalysisSessionComponent() {
    abstract fun getReturnTypeForKtDeclaration(declaration: KtDeclaration): KtType
    abstract fun getKtExpressionType(expression: KtExpression): KtType
    abstract fun getExpectedType(expression: PsiElement): KtType?
    abstract fun isDefinitelyNull(expression: KtExpression): Boolean
    abstract fun isDefinitelyNotNull(expression: KtExpression): Boolean
}

interface KtExpressionTypeProviderMixIn : KtAnalysisSessionMixIn {
    fun KtExpression.getKtType(): KtType =
        analysisSession.expressionTypeProvider.getKtExpressionType(this)

    fun KtDeclaration.getReturnKtType(): KtType =
        analysisSession.expressionTypeProvider.getReturnTypeForKtDeclaration(this)

    /**
     * Returns the expected [KtType] of this [PsiElement] if it is an expression. The returned value should not be a
     * [org.jetbrains.kotlin.idea.frontend.api.types.KtErrorType].
     */
    fun PsiElement.getExpectedType(): KtType? =
        analysisSession.expressionTypeProvider.getExpectedType(this)

    /**
     * Returns `true` if this expression is definitely null, based on declared nullability and smart cast types derived from
     * data-flow analysis facts. Examples:
     * ```
     *   fun <T : Any> foo(t: T, nt: T?, s: String, ns: String?) {
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
    fun KtExpression.isDefinitelyNull(): Boolean =
        analysisSession.expressionTypeProvider.isDefinitelyNull(this)

    /**
     * Returns `true` if this expression is definitely not null. See [isDefinitelyNull] for examples.
     */
    fun KtExpression.isDefinitelyNotNull(): Boolean =
        analysisSession.expressionTypeProvider.isDefinitelyNotNull(this)
}
