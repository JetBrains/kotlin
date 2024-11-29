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
 * A [receiver](https://kotlin.github.io/analysis-api/receivers.html) value of a call, which represents either an
 * [explicit][KaExplicitReceiverValue], [implicit][KaImplicitReceiverValue], or [smart-casted][KaSmartCastedReceiverValue] receiver.
 */
public sealed interface KaReceiverValue : KaLifetimeOwner {
    /**
     * The inferred [KaType] of the receiver. This is a smart-casted type in the case of a smart cast on the receiver.
     */
    public val type: KaType
}

/**
 * An explicit receiver value.
 *
 * #### Example
 *
 * ```
 * "".length    // explicit receiver `""`
 * ```
 */
public interface KaExplicitReceiverValue : KaReceiverValue {
    /**
     * The [KtExpression] of the explicit receiver.
     */
    public val expression: KtExpression

    /**
     * Whether [safe navigation](https://kotlinlang.org/docs/null-safety.html#safe-call-operator) is used on this receiver.
     *
     * #### Example
     *
     * ```kotlin
     * fun test(s1: String?, s2: String) {
     *   s1?.length // The explicit receiver `s1` has `isSafeNavigation = true`
     *   s2.length  // The explicit receiver `s2` has `isSafeNavigation = false`
     * }
     * ```
     */
    public val isSafeNavigation: Boolean
}

/**
 * An implicit receiver value.
 *
 * #### Example
 *
 * ```kotlin
 * class A {
 *   val i: Int = 1
 *   fun test() {
 *     i    // The implicit receiver of type `A` is bound to the `KaNamedClassSymbol` of class `A`.
 *   }
 * }
 *
 * fun String.test() {
 *   length // The implicit receiver of type `String` is bound to the `KaReceiverParameterSymbol` for the extension receiver of `test`.
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
 * A smart-casted receiver value.
 *
 * #### Example
 *
 * ```kotlin
 * fun Any.test() {
 *   if (this is String) {
 *     length
 *   }
 * }
 * ```
 *
 * `length` has a smart-casted receiver value of type `String`. Its [original] is an implicit receiver which is bound to the
 * [KaReceiverParameterSymbol][org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol] for the extension receiver of `test`.
 */
public interface KaSmartCastedReceiverValue : KaReceiverValue {
    /**
     * The original [KaReceiverValue] to which the smart cast was applied.
     */
    public val original: KaReceiverValue
}
