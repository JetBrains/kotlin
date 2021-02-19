/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.tokens.assertIsValidAndAccessible

interface ValidityTokenOwner {
    val token: ValidityToken
}

fun ValidityTokenOwner.isValid(): Boolean = token.isValid()

@Suppress("NOTHING_TO_INLINE")
inline fun ValidityTokenOwner.assertIsValidAndAccessible() {
    token.assertIsValidAndAccessible()
}

inline fun <R> ValidityTokenOwner.withValidityAssertion(action: () -> R): R {
    assertIsValidAndAccessible()
    return action()
}