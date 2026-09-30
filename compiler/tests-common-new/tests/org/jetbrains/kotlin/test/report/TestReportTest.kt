/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.report

import org.jetbrains.kotlin.test.TestInfrastructureException
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestReportTest {
    @Test
    fun `given a test reported as both passed and failed then the report is rejected`() {
        val error = assertThrows(TestInfrastructureException::class.java) {
            TestReport(passedTests = setOf("a", "b"), failedTests = setOf("b"), ignoredTests = emptySet())
        }

        val message = error.message.orEmpty()
        assertTrue("passed and failed" in message, message)
        assertTrue("[b]" in message, message)
    }

    @Test
    fun `given a test reported as both passed and ignored then the report is rejected`() {
        assertThrows(TestInfrastructureException::class.java) {
            TestReport(passedTests = setOf("a"), failedTests = emptySet(), ignoredTests = setOf("a"))
        }
    }

    @Test
    fun `given a test reported as both failed and ignored then the report is rejected`() {
        assertThrows(TestInfrastructureException::class.java) {
            TestReport(passedTests = emptySet(), failedTests = setOf("a"), ignoredTests = setOf("a"))
        }
    }

    @Test
    fun `given disjoint outcomes then the report is accepted`() {
        assertDoesNotThrow {
            TestReport(passedTests = setOf("a"), failedTests = setOf("b"), ignoredTests = setOf("c"))
        }
        assertDoesNotThrow {
            TestReport(passedTests = emptySet<String>(), failedTests = emptySet(), ignoredTests = emptySet())
        }
    }
}
