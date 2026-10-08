/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.utils.withExtension
import org.opentest4j.AssertionFailedError
import org.opentest4j.FileInfo
import org.opentest4j.MultipleFailuresError
import org.opentest4j.TestAbortedException

/**
 * Checks the messages reported by [PhasedPipelineChecker] for tests with inconsistent phase directives.
 *
 * If a test is expected to fail, the messages of all its failures are compared with the content of the
 * [PHASED_FAILURE_EXTENSION] file next to the test. Diffs proposed for the test data are rendered as changed lines only,
 * and are never applied to the test data itself.
 *
 * Tests located in the `latestPhaseIsFir2Ir` and `noLatestPhase` directories are run with
 * [LATEST_PHASE_IN_PIPELINE] set to [FIR2IR][TestPhase.FIR2IR] and without [LATEST_PHASE_IN_PIPELINE] respectively.
 */
open class AbstractPhasedPipelineCheckerTest : AbstractFirPhasedDiagnosticTest(FirParser.LightTree) {
    companion object {
        const val PHASED_FAILURE_EXTENSION: String = ".phased-failure.txt"
    }

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        super.configure(builder)

        forTestsMatching("*/latestPhaseIsFir2Ir/*") {
            defaultDirectives {
                -LATEST_PHASE_IN_PIPELINE
                LATEST_PHASE_IN_PIPELINE with TestPhase.FIR2IR
            }
        }

        forTestsMatching("*/noLatestPhase/*") {
            defaultDirectives {
                -LATEST_PHASE_IN_PIPELINE
            }
        }
    }

    override fun runTest(filePath: String) {
        val failure = try {
            super.runTest(filePath)
            null
        } catch (e: TestAbortedException) {
            throw e
        } catch (e: Throwable) {
            e
        }

        val failureFile = ForTestCompileRuntime.transformTestDataPath(filePath).withExtension(PHASED_FAILURE_EXTENSION)
        when {
            failure != null -> JUnit5Assertions.assertEqualsToFile(failureFile, failure.render())
            failureFile.exists() -> JUnit5Assertions.fail {
                "The test passes, but `${failureFile.name}` exists. Please remove it if the test is expected to pass"
            }
        }
    }

    private fun Throwable.render(): String {
        val failures = (this as? MultipleFailuresError)?.failures?.takeIf { it.isNotEmpty() } ?: listOf(this)
        return failures.map { it.renderSingle() }.sorted().joinToString("\n\n")
    }

    private fun Throwable.renderSingle(): String = buildString {
        append(message)
        val expectedFile = (this@renderSingle as? AssertionFailedError)?.expected?.value as? FileInfo ?: return@buildString
        val expectedLines = expectedFile.getContentsAsString(Charsets.UTF_8).lines()
        val actualLines = actual.stringRepresentation.lines()
        for (line in expectedLines - actualLines.toSet()) {
            append("\n- ").append(line)
        }
        for (line in actualLines - expectedLines.toSet()) {
            append("\n+ ").append(line)
        }
    }
}
