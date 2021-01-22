/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import com.intellij.rt.execution.junit.FileComparisonFailure
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.jetbrains.kotlin.test.util.trimTrailingWhitespacesAndAddNewlineAtEOF
import org.jetbrains.kotlin.utils.rethrow
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.io.IOException
import org.junit.jupiter.api.Assertions as JUnit5PlatformAssertions

object JUnit5Assertions : AssertionsService() {
    override fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String, message: () -> String) {
        try {
            val actualText = actual.trim { it <= ' ' }.convertLineSeparators().trimTrailingWhitespacesAndAddNewlineAtEOF()
            if (!expectedFile.exists()) {
                expectedFile.writeText(actualText)
                org.junit.jupiter.api.fail("Expected data file did not exist. Generating: $expectedFile")
            }
            val expected = expectedFile.readText().convertLineSeparators()
            val expectedText = expected.trim { it <= ' ' }.trimTrailingWhitespacesAndAddNewlineAtEOF()
            if (sanitizer.invoke(expectedText) != sanitizer.invoke(actualText)) {
                throw FileComparisonFailure(
                    "${message()}: ${expectedFile.name}",
                    expected, actual, expectedFile.absolutePath
                )
            }
        } catch (e: IOException) {
            throw rethrow(e)
        }
    }

    override fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertEquals(expected, actual, message?.invoke())
    }

    override fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotEquals(expected, actual, message?.invoke())
    }

    override fun assertTrue(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertTrue(value, message?.invoke())
    }

    override fun assertFalse(value: Boolean, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertFalse(value, message?.invoke())
    }

    override fun assertAll(exceptions: List<Throwable>) {
        exceptions.singleOrNull()?.let { throw it }
        JUnit5PlatformAssertions.assertAll(exceptions.map { Executable { throw it } })
    }

    override fun assertNotNull(value: Any?, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertNotNull(value, message)
    }

    override fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?) {
        JUnit5PlatformAssertions.assertIterableEquals(expected, actual, message)
    }

    override fun fail(message: () -> String): Nothing {
        org.junit.jupiter.api.fail(message)
    }
}
