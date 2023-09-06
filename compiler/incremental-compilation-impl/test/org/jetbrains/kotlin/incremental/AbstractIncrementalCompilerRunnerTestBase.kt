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

import org.jetbrains.kotlin.TestWithWorkingDir
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.incremental.testingUtils.*
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.junit.Assert
import java.io.File

abstract class AbstractIncrementalCompilerRunnerTestBase<Args : CommonCompilerArguments> : TestWithWorkingDir() {
    protected lateinit var lookupsDuringTest: MutableSet<LookupSymbol>
    protected abstract fun createCompilerArguments(destinationDir: File, testDir: File): Args

    protected open val moduleNames: Collection<String>? get() = null

    override fun setUp() {
        lookupsDuringTest = hashSetOf()
        super.setUp()
    }

    override fun tearDown() {
        try {
            super.tearDown()
        } finally {
            lookupsDuringTest.clear()
        }
    }


    protected open fun setupTest(testDir: File, srcDir: File, cacheDir: File, outDir: File): List<File> =
        listOf(srcDir)

    protected open fun resetTest(testDir: File, newOutDir: File, newCacheDir: File) {}

    private fun createCompilerArgumentsImpl(destinationDir: File, testDir: File): Args =
        createCompilerArguments(destinationDir, testDir).apply {
            parseCommandLineArguments(parseAdditionalArgs(testDir), this)
        }

    open fun failFile(testDir: File): File = testDir.resolve(FAIL_FILE_NAME)

    fun doTest(path: String) {
        val testDir = File(path)
        val failFile = failFile(testDir)
        var testPassed = false
        try {
            doTestImpl(testDir)
            testPassed = true
        } catch (e: Throwable) {
            if (!failFile.exists()) {
                throw e
            }
        }
        if (testPassed && failFile.exists()) {
            fail("Test is successful and $FAIL_FILE_NAME can be removed")
        }
    }

    private fun doTestImpl(testDir: File) {
        fun Iterable<File>.relativePaths() =
            map { it.relativeTo(workingDir).path.replace('\\', '/') }

        val srcDir = File(workingDir, "src").apply { mkdirs() }
        val cacheDir = File(workingDir, "incremental-data").apply { mkdirs() }
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val mapWorkingToOriginalFile = HashMap(copyTestSources(testDir, srcDir, filePrefix = ""))
        val sourceRoots = setupTest(testDir, srcDir, cacheDir, outDir)
        val args = createCompilerArgumentsImpl(outDir, testDir)

        val (initialExitCode, _, errors, initialCachesDump) = initialMake(cacheDir, outDir, sourceRoots, args)
        var lastExitCode = initialExitCode
        var lastCachesDump = initialCachesDump
        check(errors.isEmpty()) { "Initial build failed: \n${errors.joinToString("\n")}" }

        // modifications
        val buildLogFile = buildLogFinder.findBuildLog(testDir) ?: throw IllegalStateException("build log file not found in $workingDir")
        val buildLogSteps = parseTestBuildLog(buildLogFile)
        val modifications = getModificationsToPerform(
            testDir,
            moduleNames = moduleNames,
            allowNoFilesWithSuffixInTestData = false,
            touchPolicy = TouchPolicy.CHECKSUM
        )

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
            val (incrementalExitCode, compiledSources, compileErrors, incrementalCachesDump) = incrementalMake(
                cacheDir,
                outDir,
                sourceRoots,
                createCompilerArgumentsImpl(
                    outDir,
                    testDir
                )
            )

            lastExitCode = incrementalExitCode
            lastCachesDump = incrementalCachesDump

            expectedSB.appendLine(stepLogAsString(step, buildLogStep.compiledKotlinFiles, buildLogStep.compileErrors))
            expectedSBWithoutErrors.appendLine(
                stepLogAsString(
                    step,
                    buildLogStep.compiledKotlinFiles,
                    buildLogStep.compileErrors,
                    includeErrors = false
                )
            )
            actualSB.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors))
            actualSBWithoutErrors.appendLine(stepLogAsString(step, compiledSources.relativePaths(), compileErrors, includeErrors = false))
            step++
        }

        if (expectedSBWithoutErrors.toString() != actualSBWithoutErrors.toString()) {
            if (BuildLogFinder.isJpsLogFile(buildLogFile)) {
                // JPS logs should be updated carefully, because standalone logs are a bit different (no removed classes, iterations, etc)
                Assert.assertEquals(expectedSB.toString(), actualSB.toString())
            } else {
                KtUsefulTestCase.assertSameLinesWithFile(buildLogFile.canonicalPath, actualSB.toString(), false)
            }
        }

        rebuildAndCompareOutput(sourceRoots, testDir, buildLogSteps, outDir, lastExitCode, lastCachesDump)
    }

    // these functions are needed only to simplify debugging of IC tests
    private fun initialMake(cacheDir: File, outDir: File, sourceRoots: List<File>, args: Args) = make(cacheDir, outDir, sourceRoots, args)

    private fun incrementalMake(cacheDir: File, outDir: File, sourceRoots: List<File>, args: Args) =
        make(cacheDir, outDir, sourceRoots, args)

    private fun rebuildAndCompareOutput(
        sourceRoots: List<File>,
        testDir: File,
        buildLogSteps: List<BuildStep>,
        outDir: File,
        finalExitCode: ExitCode,
        finalMappingDump: String?
    ) {
        val rebuildOutDir = File(workingDir, "rebuild-out").apply { mkdirs() }
        val rebuildCacheDir = File(workingDir, "rebuild-cache").apply { mkdirs() }
        resetTest(testDir, rebuildOutDir, rebuildCacheDir)

        val rebuildResult = make(rebuildCacheDir, rebuildOutDir, sourceRoots, createCompilerArgumentsImpl(rebuildOutDir, testDir))

        val rebuildExpectedToSucceed = buildLogSteps.last().compileSucceeded
        val rebuildSucceeded = rebuildResult.exitCode == ExitCode.OK
        Assert.assertEquals("Rebuild exit code differs from incremental exit code", rebuildExpectedToSucceed, rebuildSucceeded)

        Assert.assertEquals("Compilation result differs", rebuildResult.exitCode, finalExitCode)
        if (finalExitCode != ExitCode.OK) {
            return
        }
        if (rebuildSucceeded) {
            assertEqualDirectories(rebuildOutDir, outDir, forgiveExtraFiles = false)
        }

        // compare caches
        assertEquals(rebuildResult.mappingsDump, finalMappingDump)
    }

    protected open val buildLogFinder: BuildLogFinder
        get() = BuildLogFinder(isGradleEnabled = true)

    protected abstract fun make(cacheDir: File, outDir: File, sourceRoots: Iterable<File>, args: Args): TestCompilationResult

    private fun stepLogAsString(step: Int, ktSources: Iterable<String>, errors: Collection<String>, includeErrors: Boolean = true): String {
        val sb = StringBuilder()

        sb.appendLine("================ Step #$step =================")
        sb.appendLine()
        sb.appendLine("Compiling files:")
        ktSources.toSet().toTypedArray().sortedArray().forEach { sb.appendLine("  $it") }
        sb.appendLine("End of files")
        sb.appendLine("Exit code: ${if (errors.isEmpty()) "OK" else "ABORT"}")

        if (errors.isNotEmpty() && includeErrors) {
            sb.appendLine("------------------------------------------")
            sb.appendLine("COMPILATION FAILED")
            errors.filter(String::isNotEmpty).forEach { sb.appendLine(it) }
        }

        return sb.toString()
    }

    private fun StringBuilder.appendLine(line: String = "") {
        append(line)
        append('\n')
    }

    companion object {
        @JvmStatic
        private val distKotlincLib: File = File("dist/kotlinc/lib")

        @JvmStatic
        protected val kotlinStdlibJvm: File = File(distKotlincLib, "kotlin-stdlib.jar").also {
            KtUsefulTestCase.assertExists(it)
        }

        @JvmStatic
        protected fun buildHistoryFile(cacheDir: File): File = File(cacheDir, "build-history.bin")

        @JvmStatic
        protected fun abiSnapshotFile(cacheDir: File): File = File(cacheDir, IncrementalCompilerRunner.ABI_SNAPSHOT_FILE_NAME)

        private const val ARGUMENTS_FILE_NAME = "args.txt"
        private const val FAIL_FILE_NAME = "fail.txt"

        private fun parseAdditionalArgs(testDir: File): List<String> {
            return File(testDir, ARGUMENTS_FILE_NAME)
                .takeIf { it.exists() }
                ?.readText()
                ?.split(" ", "\n")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }
}
