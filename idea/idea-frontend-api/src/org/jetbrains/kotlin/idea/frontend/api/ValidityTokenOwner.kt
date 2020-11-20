/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

interface ValidityTokenOwner {
    val token: ValidityToken
}

@Suppress("NOTHING_TO_INLINE")
inline fun ValidityTokenOwner.assertIsValid() {
    token.assertIsValid()
}

inline fun <R> ValidityTokenOwner.withValidityAssertion(action: () -> R): R {
    assertIsValid()
    return action()
}