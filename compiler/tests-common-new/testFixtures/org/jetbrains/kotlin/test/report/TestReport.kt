/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

/**
 * Per-test outcomes of a single (possibly batched) test run. [ID] is opaque: Kotlin/Native keys by test name, the
 * grouped Wasm runners by their synthetic launcher class name.
 */
data class TestReport<ID>(
    val passedTests: Set<ID>,
    val failedTests: Set<ID>,
    val ignoredTests: Set<ID>,
) {
    fun isEmpty(): Boolean = passedTests.isEmpty() && failedTests.isEmpty() && ignoredTests.isEmpty()

    val reportedIds: Set<ID> get() = passedTests + failedTests + ignoredTests

    override fun toString(): String = """
        TestReport:
         * Passed:  $passedTests
         * Failed:  $failedTests
         * Ignored: $ignoredTests
    """.trimIndent()
}
