/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.compiler.based

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.test.services.AssertionsService
import org.jetbrains.kotlin.test.util.JUnit4Assertions
import org.opentest4j.MultipleFailuresError
import java.io.File
import java.util.function.Consumer

internal object JUnit4AssertionsService : AssertionsService() {
    override fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String, message: () -> String) {
        JUnit4Assertions.assertEqualsToFile(expectedFile, actual, sanitizer, message)
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit4Assertions.assertEquals(expected, actual, message)
    }

    override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit4Assertions.assertNotEquals(expected, actual, message)
    }

    override fun assertTrue(value: Boolean, message: (() -> String)?) {
        JUnit4Assertions.assertTrue(value, message)
    }

    override fun assertFalse(value: Boolean, message: (() -> String)?) {
        JUnit4Assertions.assertFalse(value, message)
    }

    override fun assertNotNull(value: Any?, message: (() -> String)?) {
        JUnit4Assertions.assertNotNull(value, message)
    }

    override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
        JUnit4Assertions.assertSameElements(expected, actual, message)
    }

    override fun assertAll(exceptions: List<Throwable>) {
        when (exceptions.size) {
            0 -> Unit
            1 -> throw exceptions.single()
            else -> {
                throwMultipleExceptions(exceptions)
            }
        }

    }

    private fun throwMultipleExceptions(exceptions: List<Throwable>) {
        val multipleFailuresError = MultipleFailuresError("", exceptions)
        exceptions.forEach(multipleFailuresError::addSuppressed)
        throw multipleFailuresError
    }

    override fun fail(message: () -> String): Nothing {
        JUnit4Assertions.fail(message)
    }
}