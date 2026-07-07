/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.name.Name

/**
 * Represents a single argument passed in a call, such as `foo(1, message = "hi", *array)`.
 *
 * A value argument carries the argument expression, an optional argument name (for named arguments), and an optional
 * spread element (for `*`-spread arguments). It is not itself a [KtElement]; use [asElement] to obtain the underlying
 * PSI element. Besides the regular PSI-backed [KtValueArgument], synthetic implementations exist for modeling
 * arguments of callable references.
 */
interface ValueArgument {
    /**
     * Returns the argument expression, or `null` if it is missing in incomplete code.
     */
    @IfNotParsed
    fun getArgumentExpression(): KtExpression?

    /**
     * Returns the argument name for a named argument (`name = value`), or `null` if this argument is positional.
     */
    fun getArgumentName(): ValueArgumentName?

    /**
     * Returns `true` if this is a named argument (`name = value`).
     */
    fun isNamed(): Boolean

    /**
     * Returns the underlying PSI element that this argument corresponds to.
     */
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

/**
 * A synthetic positional [ValueArgument] used when modeling the arguments of a callable reference, where arguments
 * have no corresponding source PSI.
 */
interface FakePositionalValueArgumentForCallableReference : ValueArgument {
    /** The zero-based position of this argument. */
    val index: Int
}

/**
 * A synthetic spread [ValueArgument] used when modeling the implicit vararg spread of a callable reference.
 */
interface FakeImplicitSpreadValueArgumentForCallableReference : ValueArgument {
    /** The wrapped argument that is being spread. */
    val expression: ValueArgument
}

/**
 * A [ValueArgument] that is a trailing lambda passed outside the call parentheses, as in `list.forEach { ... }`.
 */
interface LambdaArgument : ValueArgument {
    /**
     * Returns the lambda expression of this argument, or `null` if it is missing in incomplete code.
     */
    fun getLambdaExpression(): KtLambdaExpression?
}

/**
 * The name of a named [ValueArgument] (the `name` in `name = value`).
 */
interface ValueArgumentName {
    /** The argument name as a [Name]. */
    val asName: Name

    /** The reference expression that carries the name, or `null` if it is absent. */
    val referenceExpression: KtSimpleNameExpression?
}
