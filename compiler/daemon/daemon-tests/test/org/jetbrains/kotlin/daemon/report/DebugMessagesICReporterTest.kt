/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.report

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.build.report.info
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.jetbrains.kotlin.daemon.testfixtures.FakeCompilerServicesFacadeBase
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class DebugMessagesICReporterTest {

    @Test
    fun testWarningLevel() {
        val compilerServices = FakeCompilerServicesFacadeBase()
        val reporter = DebugMessagesICReporter(compilerServices, File("unusedRootDir"), ICReporter.ReportSeverity.WARNING)
        reporter.warn { WARNING_MESSAGE }
        reporter.info { INFO_MESSAGE }
        reporter.debug { DEBUG_MESSAGE }

        assertEquals(
            expected = mapOf(
                WARNING_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.WARNING),
            ),
            actual = compilerServices.messages
        )
    }

    @Test
    fun testInfoLevel() {
        val compilerServices = FakeCompilerServicesFacadeBase()
        val reporter = DebugMessagesICReporter(compilerServices, File("unusedRootDir"), ICReporter.ReportSeverity.INFO)
        reporter.warn { WARNING_MESSAGE }
        reporter.info { INFO_MESSAGE }
        reporter.debug { DEBUG_MESSAGE }

        assertEquals(
            expected = mapOf(
                WARNING_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.WARNING),
                INFO_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.INFO),
            ),
            actual = compilerServices.messages
        )
    }

    @Test
    fun testDebugLevel() {
        val compilerServices = FakeCompilerServicesFacadeBase()
        val reporter = DebugMessagesICReporter(compilerServices, File("unusedRootDir"), ICReporter.ReportSeverity.DEBUG)
        reporter.warn { WARNING_MESSAGE }
        reporter.info { INFO_MESSAGE }
        reporter.debug { DEBUG_MESSAGE }

        assertEquals(
            mapOf(
                WARNING_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.WARNING),
                INFO_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.INFO),
                DEBUG_MESSAGE to Pair(ReportCategory.IC_MESSAGE, ReportSeverity.DEBUG)
            ),
            actual = compilerServices.messages
        )
    }
}
