/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("KtAssert")

package org.jetbrains.kotlin.test

import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/*
 * Those functions are needed only in this module because it has no testing framework
 *   with assertions in it's dependencies
 */

internal fun fail(message: String) {
    throw AssertionError(message)
}

@OptIn(ExperimentalContracts::class)
internal fun assertNotNull(message: String, value: Any?) {
    contract {
        returns() implies (value != null)
    }
    if (value == null) {
        fail(message)
    }
}

internal fun assertTrue(message: String, value: Boolean) {
    if (!value) {
        fail(message)
    }
}

abstract class Assertions {
    fun assertEqualsToFile(expectedFile: File, actual: String, sanitizer: (String) -> String = { it }) {
        assertEqualsToFile(expectedFile, actual, sanitizer) { "Actual data differs from file content" }
    }

    abstract fun assertEqualsToFile(
        expectedFile: File,
        actual: String,
        sanitizer: (String) -> String = { it },
        message: (() -> String)
    )

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

    abstract fun assertAll(exceptions: List<AssertionError>)

    abstract fun fail(message: () -> String): Nothing
}
