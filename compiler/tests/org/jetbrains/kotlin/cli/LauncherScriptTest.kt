/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cli

import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File
import java.util.concurrent.TimeUnit

class LauncherScriptTest : TestCaseWithTmpdir() {
    private fun runProcess(
        executableName: String,
        vararg args: String,
        expectedStdout: String = "",
        expectedStderr: String = "",
        expectedExitCode: Int = 0,
        workDirectory: File? = null
    ) {
        val executableFileName = if (SystemInfo.isWindows) "$executableName.bat" else executableName
        val launcherFile = File(PathUtil.kotlinPathsForDistDirectory.homePath, "bin/$executableFileName")
        assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

        // For some reason, IntelliJ's ExecUtil screws quotes up on windows.
        // So, use ProcessBuilder instead.
        val pb = ProcessBuilder(
            launcherFile.absolutePath,
            // In cmd, `=` is delimeter, so we need to surround parameter with quotes.
            *quoteIfNeeded(args)
        )
        pb.directory(workDirectory)
        val process = pb.start()
        val stdout =
            AbstractCliTest.getNormalizedCompilerOutput(
                StringUtil.convertLineSeparators(process.inputStream.bufferedReader().use { it.readText() }),
                null, testDataDirectory
            )
        val stderr =
            AbstractCliTest.getNormalizedCompilerOutput(
                StringUtil.convertLineSeparators(process.errorStream.bufferedReader().use { it.readText() }),
                null, testDataDirectory
            ).replace("Picked up [_A-Z]+:.*\n".toRegex(), "")
        process.waitFor(10, TimeUnit.SECONDS)
        val exitCode = process.exitValue()
        try {
            assertEquals(expectedStdout, stdout)
            assertEquals(expectedStderr, stderr)
            assertEquals(expectedExitCode, exitCode)
        } catch (e: Throwable) {
            System.err.println("exit code $exitCode")
            System.err.println("=== STDOUT ===")
            System.err.println(stdout)
            System.err.println("=== STDERR ===")
            System.err.println(stderr)
            throw e
        } finally {
            process.destroy()
        }
    }

    private fun quoteIfNeeded(args: Array<out String>): Array<String> =
        if (SystemInfo.isWindows) args.map {
            if (it.contains('=') || it.contains(" ") || it.contains(";") || it.contains(",")) "\"$it\"" else it
        }.toTypedArray()
        else args.cast()

    private val testDataDirectory: String
        get() = KtTestUtil.getTestDataPathBase() + "/launcher"

    fun testKotlincSimple() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            "-d", tmpdir.path
        )
    }

    fun testKotlincJvmSimple() {
        runProcess(
            "kotlinc-jvm",
            "$testDataDirectory/helloWorld.kt",
            "-d", tmpdir.path
        )
    }

    fun testKotlincJvmScriptWithClassPathFromSysProp() {
        runProcess(
            "kotlinc-jvm",
            "-script",
            "$testDataDirectory/classPathPropTest.kts",
            expectedStdout = "kotlin-compiler.jar\n"
        )
    }

    fun testKotlinJvmContextClassLoader() {
        val kotlinTestJar = File(PathUtil.kotlinPathsForDistDirectory.homePath, "lib/kotlin-test.jar")
        assertTrue("kotlin-main-kts.jar not found, run dist task: ${kotlinTestJar.absolutePath}", kotlinTestJar.exists())
        runProcess(
            "kotlinc",
            "-cp", kotlinTestJar.path,
            "$testDataDirectory/contextClassLoaderTester.kt",
            "-d", tmpdir.path
        )

        runProcess(
            "kotlin",
            "-cp", listOf(tmpdir.path, kotlinTestJar.path).joinToString(File.pathSeparator),
            "ContextClassLoaderTester",
            expectedStdout = "${kotlinTestJar.name}\n"
        )
    }

    fun testKotlincJsSimple() {
        runProcess(
            "kotlinc-js",
            "$testDataDirectory/emptyMain.kt",
            "-output", File(tmpdir, "out.js").path
        )
    }

    fun testKotlinNoReflect() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/reflectionUsage.kt",
            "-d", tmpdir.path
        )

        runProcess(
            "kotlin",
            "-cp", tmpdir.path,
            "-no-reflect",
            "ReflectionUsageKt",
            expectedStdout = "no reflection"
        )
    }

    fun testDoNotAppendCurrentDirToNonEmptyClasspath() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            "-d", tmpdir.path
        )

        runProcess("kotlin", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)

        val emptyDir = KotlinTestUtils.tmpDirForTest(this)
        runProcess(
            "kotlin",
            "-cp", emptyDir.path,
            "test.HelloWorldKt",
            expectedStderr = "error: could not find or load main class test.HelloWorldKt\n",
            expectedExitCode = 1,
            workDirectory = tmpdir
        )
    }

    fun testRunnerExpression() {
        runProcess(
            "kotlin",
            "-e",
            "val x = 2; (args + listOf(2,1).map { (it * x).toString() }).joinToString()",
            "--",
            "a",
            "b",
            expectedStdout = "a, b, 4, 2\n"
        )
    }

    fun testCommandlineProcessing() {
        runProcess(
            "kotlin",
            "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlin",
            "-e",
            "println(args.joinToString())",
            "--",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlin",
            "$testDataDirectory/printargs.kts",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlin",
            "$testDataDirectory/printargs.kts",
            "--",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
    }

    fun testLegacyAssert() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/legacyAssertDisabled.kt", "-Xassertions=legacy", "-d", tmpdir.path
        )

        runProcess("kotlin", "LegacyAssertDisabledKt", "-J-da:kotlin._Assertions", workDirectory = tmpdir)

        runProcess(
            "kotlinc",
            "$testDataDirectory/legacyAssertEnabled.kt", "-Xassertions=legacy", "-d", tmpdir.path
        )

        runProcess("kotlin", "LegacyAssertEnabledKt", "-J-ea:kotlin._Assertions", workDirectory = tmpdir)
    }

    fun testScriptWithXArguments() {
        runProcess(
            "kotlin", "$testDataDirectory/funWithResultReturn.kts",
            expectedExitCode = 1,
            expectedStderr = """error: 'kotlin.Result' cannot be used as a return type (funWithResultReturn.kts:2:11)
compiler/testData/launcher/funWithResultReturn.kts:2:11: error: 'kotlin.Result' cannot be used as a return type
fun f() : Result<Int> = Result.success(42)
          ^
          """
        )
        runProcess("kotlin", "-Xallow-result-return-type", "$testDataDirectory/funWithResultReturn.kts", expectedStdout = "42\n")
    }

    fun testNoStdLib() {
        runProcess("kotlin", "-e", "println(42)", expectedStdout = "42\n")
        runProcess(
            "kotlin", "-no-stdlib", "-e", "println(42)",
            expectedExitCode = 1, expectedStderrContains = Regex("error: unresolved reference: println")
        )
    }

    fun testProperty() {
        runProcess("kotlinc", "$testDataDirectory/property.kt", "-d", tmpdir.path)

        runProcess(
            "kotlin", "PropertyKt", "-Dresult=OK",
            workDirectory = tmpdir, expectedStdout = "OK\n"
        )
    }
}
