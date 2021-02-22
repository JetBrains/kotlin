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
        workDirectory: File? = null,
        environment: Map<String, String> = emptyMap(),
    ) {
        val executableFileName = if (SystemInfo.isWindows) "$executableName.bat" else executableName
        val launcherFile = File(PathUtil.kotlinPathsForDistDirectory.homePath, "bin/$executableFileName")
        assertTrue("Launcher script not found, run dist task: ${launcherFile.absolutePath}", launcherFile.exists())

        // For some reason, IntelliJ's ExecUtil screws quotes up on windows.
        // So, use ProcessBuilder instead.
        val pb = ProcessBuilder(
            launcherFile.absolutePath,
            // In cmd, `=` is delimiter, so we need to surround parameter with quotes.
            *quoteIfNeeded(args)
        )
        pb.environment().putAll(environment)
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
                .replace("The system cannot find the file specified", "No such file or directory") // win -> unix
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
            "-e",
            "b",
            expectedStdout = "-e, b\n"
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
//        runProcess(
//            "kotlin", "$testDataDirectory/funWithResultReturn.kts",
//            expectedExitCode = 1,
//            expectedStderr = """error: 'kotlin.Result' cannot be used as a return type (funWithResultReturn.kts:2:11)
//compiler/testData/launcher/funWithResultReturn.kts:2:11: error: 'kotlin.Result' cannot be used as a return type
//fun f() : Result<Int> = Result.success(42)
//          ^
//"""
//        )
//        runProcess("kotlin", "-Xallow-result-return-type", "$testDataDirectory/funWithResultReturn.kts", expectedStdout = "42\n")
    }

    fun testNoStdLib() {
        runProcess("kotlin", "-e", "println(42)", expectedStdout = "42\n")
        runProcess(
            "kotlin", "-no-stdlib", "-e", "println(42)",
            expectedExitCode = 1,
            expectedStderr = """error: unresolved reference: println (script.kts:1:1)
error: no script runtime was found in the classpath: class 'kotlin.script.templates.standard.ScriptTemplateWithArgs' not found. Please add kotlin-script-runtime.jar to the module dependencies. (script.kts:1:1)
script.kts:1:1: error: unresolved reference: println
println(42)
^
script.kts:1:1: error: no script runtime was found in the classpath: class 'kotlin.script.templates.standard.ScriptTemplateWithArgs' not found. Please add kotlin-script-runtime.jar to the module dependencies.
println(42)
^
"""
        )
    }

    fun testProperty() {
        runProcess("kotlinc", "$testDataDirectory/property.kt", "-d", tmpdir.path)

        runProcess(
            "kotlin", "PropertyKt", "-Dresult=OK",
            workDirectory = tmpdir, expectedStdout = "OK\n"
        )
    }

    fun testHowToRunExpression() {
        runProcess(
            "kotlin", "-howtorun", "jar", "-e", "println(args.joinToString())", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: expression evaluation is not compatible with -howtorun argument jar\n"
        )
        runProcess(
            "kotlin", "-howtorun", "script", "-e", "println(args.joinToString())", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    fun testHowToRunScript() {
        runProcess(
            "kotlin", "-howtorun", "classfile", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/printargs.kts\n"
        )
        runProcess(
            "kotlin", "-howtorun", "script", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    fun testHowToRunCustomScript() {
        runProcess(
            "kotlin", "$testDataDirectory/funWithResultReturn.myscript",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/funWithResultReturn.myscript\n"
        )
        runProcess(
            "kotlin", "-howtorun", "script", "$testDataDirectory/funWithResultReturn.myscript",
            expectedExitCode = 1, expectedStderr = "error: unrecognized script type: funWithResultReturn.myscript; Specify path to the script file as the first argument\n"
        )
        runProcess(
            "kotlin", "-howtorun", ".kts", "$testDataDirectory/funWithResultReturn.myscript",
            expectedExitCode = 1, expectedStderr = """error: unresolved reference: CompilerOptions (funWithResultReturn.myscript:1:7)
error: 'kotlin.Result' cannot be used as a return type (funWithResultReturn.myscript:3:11)
compiler/testData/launcher/funWithResultReturn.myscript:1:7: error: unresolved reference: CompilerOptions
@file:CompilerOptions("-Xallow-result-return-type")
      ^
compiler/testData/launcher/funWithResultReturn.myscript:3:11: error: 'kotlin.Result' cannot be used as a return type
fun f() : Result<Int> = Result.success(42)
          ^
"""
        )
        runProcess(
            "kotlin", "-howtorun", ".main.kts", "$testDataDirectory/funWithResultReturn.myscript",
            expectedStdout = "42\n"
        )
    }

    fun testHowToRunClassFile() {
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path)

        runProcess(
            "kotlin", "-howtorun", "jar", "test.HelloWorldKt", workDirectory = tmpdir,
            expectedExitCode = 1,
            expectedStderr = "error: could not read manifest from test.HelloWorldKt: test.HelloWorldKt (No such file or directory)\n"
        )
        runProcess("kotlin", "-howtorun", "classfile", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)
    }

    fun testKotlincJdk15() {
        val jdk15 = mapOf("JAVA_HOME" to KtTestUtil.getJdk15Home().absolutePath)
        runProcess(
            "kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path,
            environment = jdk15,
        )

        runProcess(
            "kotlin", "-e", "listOf('O'.toString() + 'K')",
            expectedStdout = "[OK]\n", environment = jdk15,
        )
    }

    fun testEmptyJArgument() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            "-d", tmpdir.path,
            "-J", expectedStdout = "error: empty -J argument\n",
            expectedExitCode = 1
        )
    }
}
