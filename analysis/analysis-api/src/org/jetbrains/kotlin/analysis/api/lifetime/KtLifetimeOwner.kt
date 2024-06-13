/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

public interface KaLifetimeOwner {
    public val token: KaLifetimeToken
}

public typealias KtLifetimeOwner = KaLifetimeOwner

public fun KaLifetimeOwner.isValid(): Boolean = token.isValid()

@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

public inline fun <R> KaLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}

/**
 * This is a helper function to properly expose parameters in some [KaLifetimeOwner] implementation.
 *
 * An example:
 * ```kotlin
 * public class KaCall(symbol: KaSymbol) : KaLifetimeTokenOwner {
 *     public val symbol: KaSymbol by validityAsserted(symbol)
 * }
 * ```
 *
 * @see KaLifetimeOwner
 * @see KaLifetimeOwnerField
 */
public fun <T> KaLifetimeOwner.validityAsserted(value: T): KaLifetimeOwnerField<T> = KaLifetimeOwnerField(value)