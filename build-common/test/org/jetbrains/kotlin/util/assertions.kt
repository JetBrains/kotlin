/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

inline fun <reified E : Exception> assertThrows(
    message: String = "Expected ${E::class.java.name} to be thrown",
    body: () -> Unit,
): Throwable {
    try {
        body()
    } catch (e: Throwable) {
        if (e is E) {
            return e
        }
    }
    throw AssertionError(message)
}

fun <R> assertDoesNotThrow(
    message: String = "Expected no exception, but {} was thrown",
    body: () -> R,
): R {
    try {
        return body()
    } catch (e: Throwable) {
        throw AssertionError(message.format(e))
    }
}

