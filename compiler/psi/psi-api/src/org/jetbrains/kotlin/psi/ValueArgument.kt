/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.name.Name

interface ValueArgument {
    @IfNotParsed
    fun getArgumentExpression(): KtExpression?

    fun getArgumentName(): ValueArgumentName?

    fun isNamed(): Boolean

    fun asElement(): KtElement

    /**
     * Returns the `*` spread token for a spread argument (`foo(*array)`, which passes an array as a series of vararg
     * arguments), or `null` if this argument is not spread.
     */
    fun getSpreadElement(): LeafPsiElement?

    /**
     * `true` if this is a spread argument (`*array`).
     *
     * @see getSpreadElement
     */
    val isSpread: Boolean get() = getSpreadElement() != null

    /**
     * Returns `true` if the argument is located outside of the call element, as with the range in a `when` condition
     * with a subject: `when (a) { in c -> }`.
     */
    fun isExternal(): Boolean
}

interface FakePositionalValueArgumentForCallableReference : ValueArgument {
    val index: Int
}

interface FakeImplicitSpreadValueArgumentForCallableReference : ValueArgument {
    val expression: ValueArgument
}

interface LambdaArgument : ValueArgument {
    fun getLambdaExpression(): KtLambdaExpression?
}

interface ValueArgumentName {
    val asName: Name
    val referenceExpression: KtSimpleNameExpression?
}
