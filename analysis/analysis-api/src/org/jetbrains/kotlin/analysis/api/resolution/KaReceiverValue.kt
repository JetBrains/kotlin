/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.validityAsserted
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * A receiver value of a call.
 */
public sealed class KaReceiverValue : KaLifetimeOwner {
    /**
     * Returns inferred [KaType] of the receiver.
     *
     * In case of smart cast on the receiver returns smart cast type.
     *
     * For builder inference in FIR implementation it currently works incorrectly, see KT-50916.
     */
    public abstract val type: KaType
}


/**
 * An explicit expression receiver. For example
 * ```
 *   "".length // explicit receiver `""`
 * ```
 */
public class KaExplicitReceiverValue(
    expression: KtExpression,
    type: KaType,
    isSafeNavigation: Boolean,
    override val token: KaLifetimeToken,
) : KaReceiverValue() {
    public val expression: KtExpression by validityAsserted(expression)

    /**
     * Whether safe navigation is used on this receiver. For example
     * ```
     * fun test(s1: String?, s2: String) {
     *   s1?.length // explicit receiver `s1` has `isSafeNavigation = true`
     *   s2.length // explicit receiver `s2` has `isSafeNavigation = false`
     * }
     * ```
     */
    public val isSafeNavigation: Boolean by validityAsserted(isSafeNavigation)

    override val type: KaType by validityAsserted(type)
}

/**
 * An implicit receiver. For example
 * ```
 * class A {
 *   val i: Int = 1
 *   fun test() {
 *     i // implicit receiver bound to class `A`
 *   }
 * }
 *
 * fun String.test() {
 *   length // implicit receiver bound to the `KaReceiverParameterSymbol` of type `String` declared by `test`.
 * }
 * ```
 */
public class KaImplicitReceiverValue(
    symbol: KaSymbol,
    type: KaType,
) : KaReceiverValue() {
    private val backingSymbol: KaSymbol = symbol

    override val token: KaLifetimeToken get() = backingSymbol.token
    public val symbol: KaSymbol get() = withValidityAssertion { backingSymbol }
    override val type: KaType by validityAsserted(type)
}

/**
 * A smart-casted receiver. For example
 * ```
 * fun Any.test() {
 *   if (this is String) {
 *     length // smart-casted implicit receiver bound to the `KaReceiverParameterSymbol` of type `String` declared by `test`.
 *   }
 * }
 * ```
 */
public class KaSmartCastedReceiverValue(
    original: KaReceiverValue,
    smartCastType: KaType,
) : KaReceiverValue() {
    private val backingOriginal: KaReceiverValue = original

    override val token: KaLifetimeToken get() = backingOriginal.token
    public val original: KaReceiverValue get() = withValidityAssertion { backingOriginal }
    public override val type: KaType by validityAsserted(smartCastType)
}