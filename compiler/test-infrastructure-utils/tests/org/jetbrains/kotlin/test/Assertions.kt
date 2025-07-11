/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import java.io.File
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

abstract class Assertions {
    val isTeamCityBuild: Boolean = System.getenv("TEAMCITY_VERSION") != null

    fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile.toPath(), actual, sanitizer)
    }

    fun assertEqualsToFile(expectedFile: Path, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile, actual, sanitizer) { "Actual data differs from file content" }
    }

    fun doesEqualToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }): Boolean {
        return doesEqualToFile(expectedFile.toPath(), actual, sanitizer)
    }

    abstract fun doesEqualToFile(expectedFile: Path, actual: String, sanitizer: (String) -> String = { it }): Boolean

    fun assertEqualsToFile(
        expectedFile: File,
        actual: String,
        sanitizer: (String) -> String = { it },
        message: (() -> String)
    ) {
        assertEqualsToFile(expectedFile.toPath(), actual, sanitizer, message)
    }

    abstract fun assertEqualsToFile(
        expectedFile: Path,
        actual: String,
        sanitizer: (String) -> String = { it },
        message: (() -> String),
    )

    fun assertFileDoesntExist(file: File, errorMessage: () -> String) {
        assertFileDoesntExist(file.toPath(), errorMessage)
    }

    fun assertFileDoesntExist(file: Path, errorMessage: () -> String) {
        if (file.exists()) {
            if (!isTeamCityBuild) {
                file.deleteIfExists()
            }
            fail(errorMessage)
        }
    }

    abstract fun assertEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    abstract fun assertNotEquals(expected: Any?, actual: Any?, message: (() -> String)? = null)
    abstract fun assertTrue(value: Boolean, message: (() -> String)? = null)
    abstract fun assertFalse(value: Boolean, message: (() -> String)? = null)
    abstract fun assertNotNull(value: Any?, message: (() -> String)? = null)
    abstract fun <T> assertSameElements(expected: Collection<T>, actual: Collection<T>, message: (() -> String)?)

    fun <T> assertContainsElements(collection: Collection<T>, vararg expected: T) {
        assertContainsElements(collection, expected.toList())
    }

    fun <T> assertContainsElements(collection: Collection<T>, expected: Collection<T>) {
        val copy = ArrayList(collection)
        copy.retainAll(expected)
        assertSameElements(copy, expected) { renderCollectionToString(collection) }
    }

    fun renderCollectionToString(collection: Iterable<*>): String {
        if (!collection.iterator().hasNext()) {
            return "<empty>"
        }

        return collection.joinToString("\n")
    }

    abstract fun failAll(exceptions: List<Throwable>)
    abstract fun assertAll(conditions: List<() -> Unit>)

    fun assertAll(vararg conditions: () -> Unit) {
        assertAll(conditions.toList())
    }

    abstract fun fail(message: () -> String): Nothing
}
