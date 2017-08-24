/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

abstract class AbstractIncrementalCompilerRunnerTestBase<Args : CommonCompilerArguments> : TestWithWorkingDir() {
    protected abstract fun createCompilerArguments(destinationDir: File, testDir: File): Args

    fun doTest(path: String) {
        val testDir = File(path)

        fun Iterable<File>.relativePaths() =
                map { it.relativeTo(workingDir).path.replace('\\', '/') }

        val srcDir = File(workingDir, "src").apply { mkdirs() }
        val cacheDir = File(workingDir, "incremental-data").apply { mkdirs() }
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val mapWorkingToOriginalFile = HashMap(copyTestSources(testDir, srcDir, filePrefix = ""))
        val sourceRoots = listOf(srcDir)
        val args = createCompilerArguments(outDir, testDir)
        // initial build
        val (_, _, errors) = make(cacheDir, sourceRoots, args)
        if (errors.isNotEmpty()) {
            throw IllegalStateException("Initial build failed: \n${errors.joinToString("\n")}")
        }

        // modifications
        val buildLogFile = buildLogFinder.findBuildLog(testDir) ?: throw IllegalStateException("build log file not found in $workingDir")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(testDir,
                                                      moduleNames = null,
                                                      allowNoFilesWithSuffixInTestData = false,
                                                      touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        // Sometimes error messages differ.
        // This needs to be fixed, but it does not really matter much (e.g extra lines),
        // The workaround is to compare logs without errors, then logs with errors.
        // (if logs without errors differ then either compiled files differ or exit codes differ)
        val expectedSB = StringBuilder()
        val actualSB = StringBuilder()
        val expectedSBWithoutErrors = StringBuilder()
        val actualSBWithoutErrors = StringBuilder()
        var step = 1
        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(workingDir, mapWorkingToOriginalFile) }
            val (_, compiledSources, compileErrors) = make(cacheDir, sourceRoots, args)

            expectedSB.appendLine(stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors))
            expectedSBWithoutErrors.appendLine(stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors, includeErrors = false))
            actualSB.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors))
            actualSBWithoutErrors.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors, includeErrors = false))
            step++
        }

        if (expectedSBWithoutErrors.toString() != actualSBWithoutErrors.toString()) {
            Assert.assertEquals(expectedSB.toString(), actualSB.toString())
        }

        // todo: also compare caches
        run rebuildAndCompareOutput@ {
            val rebuildOutDir = File(workingDir, "rebuild-out").apply { mkdirs() }
            val rebuildCacheDir = File(workingDir, "rebuild-cache").apply { mkdirs() }
            val rebuildResult = make(rebuildCacheDir, sourceRoots, createCompilerArguments(rebuildOutDir, testDir))

            val rebuildExpectedToSucceed = buildLogSteps.last().compileSucceeded
            val rebuildSucceeded = rebuildResult.exitCode == ExitCode.OK
            Assert.assertEquals("Rebuild exit code differs from incremental exit code", rebuildExpectedToSucceed, rebuildSucceeded)

            if (rebuildSucceeded) {
                assertEqualDirectories(outDir, rebuildOutDir, forgiveExtraFiles = rebuildSucceeded)
            }
        }
    }

    protected open val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true)

    protected abstract fun make(cacheDir: File, sourceRoots: Iterable<File>, args: Args): TestCompilationResult

    private fun stepLogAsString(step: Int, ktSources: Iterable<String>, errors: Collection<String>, includeErrors: Boolean = true): String {
        val sb = StringBuilder()

        sb.appendLine("<======= STEP $step =======>")
        sb.appendLine()
        sb.appendLine("Compiled kotlin sources:")
        ktSources.toSet().toTypedArray().sortedArray().forEach { sb.appendLine(it) }
        sb.appendLine()

        if (errors.isEmpty()) {
            sb.appendLine("SUCCESS")
        }
        else {
            sb.appendLine("FAILURE")
            if (includeErrors) {
                errors.filter(String::isNotEmpty).forEach { sb.appendLine(it) }
            }
        }

        return sb.toString()
    }

    private fun StringBuilder.appendLine(line: String = "") {
        append(line)
        append('\n')
    }

    companion object {
        @JvmStatic
        protected val bootstrapKotlincLib: File = File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib")
    }
}