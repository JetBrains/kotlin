/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.resolution

import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.psi.KtExpression

/**
 * A receiver value of a call.
 */
public sealed interface KaReceiverValue : KaLifetimeOwner {
    /**
     * Inferred [KaType] of the receiver.
     *
     * A smart cast type in the case of smart cast on the receiver.
     */
    public val type: KaType
}


/**
 * An explicit expression receiver. For example
 * ```
 *   "".length // explicit receiver `""`
 * ```
 */
public interface KaExplicitReceiverValue : KaReceiverValue {
    public val expression: KtExpression

    /**
     * Whether safe navigation is used on this receiver. For example
     * ```kotlin
     * fun test(s1: String?, s2: String) {
     *   s1?.length // explicit receiver `s1` has `isSafeNavigation = true`
     *   s2.length // explicit receiver `s2` has `isSafeNavigation = false`
     * }
     * ```
     */
    public val isSafeNavigation: Boolean
}

/**
 * An implicit receiver. For example
 * ```kotlin
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
public interface KaImplicitReceiverValue : KaReceiverValue {
    /**
     * The symbol which represents the implicit receiver.
     */
    public val symbol: KaSymbol
}

/**
 * A smart-casted receiver. For example
 * ```kotlin
 * fun Any.test() {
 *   if (this is String) {
 *     length // smart-casted implicit receiver bound to the `KaReceiverParameterSymbol` of type `String` declared by `test`.
 *   }
 * }
 * ```
 */
public interface KaSmartCastedReceiverValue : KaReceiverValue {
    /**
     * The original [KaReceiverValue] to which the smart cast is applied.
     */
    public val original: KaReceiverValue
}
