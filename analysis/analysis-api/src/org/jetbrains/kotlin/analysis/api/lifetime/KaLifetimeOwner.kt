/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

/**
 * An Analysis API entity with a *lifetime* bound to the [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] where the entity was
 * created.
 *
 * The most common lifetime owners are [symbols][org.jetbrains.kotlin.analysis.api.symbols.KaSymbol] and [types][org.jetbrains.kotlin.analysis.api.types.KaType],
 * but there are many additional kinds of lifetime owners created by the Analysis API which aren't symbols or types.
 *
 * See the documentation for [KaSession][org.jetbrains.kotlin.analysis.api.KaSession] to find out more about lifetime management.
 */
public interface KaLifetimeOwner {
    /**
     * The [KaLifetimeToken] which determines the lifetime of the lifetime owner.
     */
    public val token: KaLifetimeToken
}

/**
 * Whether the lifetime owner is still valid, i.e. we are still in the scope of the lifetime owner's regular lifetime.
 *
 * [isValid] does *not* check whether the lifetime owner is [accessible][KaLifetimeToken.isAccessible].
 */
public fun KaLifetimeOwner.isValid(): Boolean = token.isValid()

/**
 * @see withValidityAssertion
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun KaLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

/**
 * Executes [action] only if the [KaLifetimeOwner] is still [valid][KaLifetimeToken.isValid] and [accessible][KaLifetimeToken.isAccessible].
 * Otherwise, throws a validity exception based on the concrete violation.
 *
 * All public endpoints of the Analysis API are protected by validity assertions to ensure that lifetime owners aren't misused outside the
 * scope of their regular lifetime.
 */
public inline fun <R> KaLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}

/**
 * This is a helper function to properly expose parameters in [KaLifetimeOwner] implementations.
 *
 * #### Example
 *
 * ```kotlin
 * public class KaCall(symbol: KaSymbol) : KaLifetimeOwner {
 *     public val symbol: KaSymbol by validityAsserted(symbol)
 * }
 * ```
 *
 * @see KaLifetimeOwner
 * @see KaLifetimeOwnerField
 */
public fun <T> KaLifetimeOwner.validityAsserted(value: T): KaLifetimeOwnerField<T> = KaLifetimeOwnerField(value)
