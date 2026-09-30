/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

import org.jetbrains.kotlin.test.checkTestInfrastructure

data class TestReport<ID>(
    val passedTests: Set<ID>,
    val failedTests: Set<ID>,
    val ignoredTests: Set<ID>,
) {
    init {
        checkDisjoint("passed", passedTests, "failed", failedTests)
        checkDisjoint("passed", passedTests, "ignored", ignoredTests)
        checkDisjoint("failed", failedTests, "ignored", ignoredTests)
    }

    fun isEmpty(): Boolean = passedTests.isEmpty() && failedTests.isEmpty() && ignoredTests.isEmpty()

    val reportedIds: Set<ID> get() = passedTests + failedTests + ignoredTests

    override fun toString(): String = """
        TestReport:
         * Passed:  $passedTests
         * Failed:  $failedTests
         * Ignored: $ignoredTests
    """.trimIndent()
}

private fun <ID> checkDisjoint(firstName: String, first: Set<ID>, secondName: String, second: Set<ID>) {
    if (first.isEmpty() || second.isEmpty()) return

    val sharedIds = first.filter { it in second }
    checkTestInfrastructure(sharedIds.isEmpty()) {
        "A test must not be reported as both $firstName and $secondName in one report. Such tests: $sharedIds"
    }
}
