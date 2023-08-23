/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

/**
 * A replacement for the JUnit Jupiter function to be used in JUnit 4 tests.
 *
 * Asserts that the given code block throws an exception of the specified type.
 *
 * @param E the type of exception that is expected to be thrown
 * @param message the error message to be used if the exception is not thrown
 * @param body the code block to be executed and verified
 * @return the caught exception if it is of the specified type
 * @throws AssertionError if the specified exception is not thrown
 */
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

/**
 * A replacement for the JUnit Jupiter function to be used in JUnit 4 tests.
 *
 * Asserts that the specified code block does not throw any exception.
 *
 * @param message The message to be included in the AssertionError if an exception is thrown.
 * It can contain the "{}" placeholder, which will be replaced with the thrown exception.
 * @param body The code block to be executed.
 *
 * @return The result of executing the code block.
 *
 * @throws AssertionError If the code block throws an exception.
 */
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

