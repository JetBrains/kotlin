/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime

public interface KtLifetimeOwner {
    public val token: KtLifetimeToken
}

public fun KtLifetimeOwner.isValid(): Boolean = token.isValid()

@Suppress("NOTHING_TO_INLINE")
public inline fun KtLifetimeOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

public inline fun <R> KtLifetimeOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}