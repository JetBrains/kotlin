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
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
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
        environment: Map<String, String> = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath),
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
                null, testDataDirectory, tmpdir.absolutePath
            )
        val stderr =
            AbstractCliTest.getNormalizedCompilerOutput(
                StringUtil.convertLineSeparators(process.errorStream.bufferedReader().use { it.readText() }),
                null, testDataDirectory, tmpdir.absolutePath
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

    private fun quoteIfNeeded(args: Array<out String>): Array<String> {
        @Suppress("UNCHECKED_CAST")
        return if (SystemInfo.isWindows) args.map {
            if (it.contains('=') || it.contains(" ") || it.contains(";") || it.contains(",")) "\"$it\"" else it
        }.toTypedArray()
        else args as Array<String>
    }

    private val testDataDirectory: String
        get() = KtTestUtil.getTestDataPathBase() + "/launcher"

    private fun kotlincInProcess(vararg args: String) {
        val (output, exitCode) = AbstractCliTest.executeCompilerGrabOutput(K2JVMCompiler(), args.toList())
        if (exitCode != ExitCode.OK) error("Failed to compile: ${args.joinToString(" ")}\nOutput:\n$output")
    }

    fun testKotlincSimple() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )
    }

    fun testKotlincJvmSimple() {
        runProcess(
            "kotlinc-jvm",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
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
        kotlincInProcess(
            K2JVMCompilerArguments::classpath.cliArgument, kotlinTestJar.path,
            "$testDataDirectory/contextClassLoaderTester.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )

        runProcess(
            "kotlin",
            K2JVMCompilerArguments::classpath.cliArgument, listOf(tmpdir.path, kotlinTestJar.path).joinToString(File.pathSeparator),
            "ContextClassLoaderTester",
            expectedStdout = "${kotlinTestJar.name}\n"
        )
    }

    fun testKotlincJsSimple() {
        runProcess(
            "kotlinc-js",
            "$testDataDirectory/emptyMain.kt",
            K2JSCompilerArguments::suppressWarnings.cliArgument,
            K2JSCompilerArguments::libraries.cliArgument,
            PathUtil.kotlinPathsForCompiler.jsStdLibKlibPath.absolutePath,
            K2JSCompilerArguments::irProduceKlibDir.cliArgument,
            K2JSCompilerArguments::outputDir.cliArgument,
            tmpdir.path,
            K2JSCompilerArguments::moduleName.cliArgument,
            "out",
            environment = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath)
        )
    }

    fun testKotlinNoReflect() {
        kotlincInProcess("$testDataDirectory/reflectionUsage.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess(
            "kotlin",
            K2JVMCompilerArguments::classpath.cliArgument, tmpdir.path,
            K2JVMCompilerArguments::noReflect.cliArgument,
            "ReflectionUsageKt",
            expectedStdout = "no reflection"
        )
    }

    fun testDoNotAppendCurrentDirToNonEmptyClasspath() {
        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess("kotlin", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)

        val emptyDir = KotlinTestUtils.tmpDirForTest(this)
        runProcess(
            "kotlin",
            K2JVMCompilerArguments::classpath.cliArgument, emptyDir.path,
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

    fun testRunnerExpressionLanguageVersion20() {
        runProcess(
            "kotlin",
            CommonCompilerArguments::languageVersion.cliArgument, "2.0", "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n",
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
        kotlincInProcess(
            "$testDataDirectory/legacyAssertDisabled.kt",
            K2JVMCompilerArguments::assertionsMode.cliArgument("legacy"),
            K2JVMCompilerArguments::destination.cliArgument,
            tmpdir.path
        )

        runProcess("kotlin", "LegacyAssertDisabledKt", "-J-da:kotlin._Assertions", workDirectory = tmpdir)

        kotlincInProcess(
            "$testDataDirectory/legacyAssertEnabled.kt",
            K2JVMCompilerArguments::assertionsMode.cliArgument("legacy"),
            K2JVMCompilerArguments::destination.cliArgument,
            tmpdir.path
        )

        runProcess("kotlin", "LegacyAssertEnabledKt", "-J-ea:kotlin._Assertions", workDirectory = tmpdir)
    }

    fun testScriptWithXArguments() {
        runProcess(
            "kotlin", K2JVMCompilerArguments::noInline.cliArgument, "$testDataDirectory/noInline.kts",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline
	at NoInline.<init>(noInline.kts:1)
"""
        )
        runProcess("kotlin", "$testDataDirectory/noInline.kts", expectedStdout = "OK\n")
    }

    fun testNoStdLib() {
        runProcess("kotlin", "-e", "println(42)", expectedStdout = "42\n")
        runProcess(
            "kotlin", "-no-stdlib", "-e", "println(42)",
            expectedExitCode = 1,
            expectedStderr = """script.kts:1:1: error: unresolved reference 'println'.
println(42)
^
"""
        )
    }

    fun testProperty() {
        kotlincInProcess("$testDataDirectory/property.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

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
            "kotlin", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/noInline.myscript\n"
        )
        runProcess(
            "kotlin", "-howtorun", "script", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = "error: unrecognized script type: noInline.myscript; Specify path to the script file as the first argument\n"
        )
        runProcess(
            "kotlin",
            K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument,
            "-howtorun",
            ".kts",
            "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = """compiler/testData/launcher/noInline.myscript:1:7: error: unresolved reference 'CompilerOptions'.
@file:CompilerOptions("-Xno-inline")
      ^
"""
        )
        runProcess(
            "kotlin", "-howtorun", ".main.kts", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline_main
	at NoInline_main.<init>(noInline.myscript:3)
"""
        )
    }

    fun testHowToRunClassFile() {
        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess(
            "kotlin", "-howtorun", "jar", "test.HelloWorldKt", workDirectory = tmpdir,
            expectedExitCode = 1,
            expectedStderr = "error: could not read manifest from test.HelloWorldKt: test.HelloWorldKt (No such file or directory)\n"
        )
        runProcess("kotlin", "-howtorun", "classfile", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)
    }

    fun testKotlincJdk17() {
        val jdk17 = mapOf("JAVA_HOME" to KtTestUtil.getJdk17Home().absolutePath)
        runProcess(
            "kotlinc", "$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            environment = jdk17,
        )

        runProcess(
            "kotlin", "-e", "listOf('O'.toString() + 'K')",
            expectedStdout = "[OK]\n", environment = jdk17,
        )
    }

    fun testEmptyJArgument() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            "-J", expectedStdout = "error: empty -J argument\n",
            expectedExitCode = 1
        )
    }

    fun testNoClassDefFoundErrorWhenClassInDefaultPackage() {
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/defaultPackage.kt", K2JVMCompilerArguments::destination.cliArgument, testDir.path)
        assertExists(File("${testDir.path}/DefaultPackageKt.class"))

        runProcess(
            "kotlin", "test.DefaultPackageKt", workDirectory = tmpdir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class test.DefaultPackageKt
            Caused by: java.lang.NoClassDefFoundError: test/DefaultPackageKt (wrong name: DefaultPackageKt)

        """.trimIndent()
        )
    }

    fun testNoClassDefFoundErrorWhenClassNotInDefaultPackage() {
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)
        assertExists(File("${testDir.path}/HelloWorldKt.class"))

        runProcess(
            "kotlin", "HelloWorldKt", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)

        """.trimIndent()
        )
    }

    /**
     * A class whose full qualified name is `DefaultPackageKt` and is located in path `$tmpdir/test/DefaultPackageKt.class`
     */
    fun testRunClassFileWithExtensionInDefaultPackage() {
        val subDir = File("$tmpdir/test/sub").apply { mkdirs() }
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/defaultPackage.kt", K2JVMCompilerArguments::destination.cliArgument, testDir.path)
        assertExists(File("${testDir.path}/DefaultPackageKt.class"))

        runProcess(
            "kotlin", "test/DefaultPackageKt.class", workDirectory = tmpdir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class test.DefaultPackageKt
            Caused by: java.lang.NoClassDefFoundError: test/DefaultPackageKt (wrong name: DefaultPackageKt)
            
        """.trimIndent()
        )

        runProcess("kotlin", "DefaultPackageKt.class", expectedStdout = "ok", workDirectory = testDir)
        runProcess("kotlin", "./sub/../DefaultPackageKt.class", expectedStdout = "ok", workDirectory = testDir)
        runProcess(
            "kotlin", "../DefaultPackageKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class ../DefaultPackageKt.class\n",
            workDirectory = subDir
        )
    }

    /**
     * A class whose full qualified name is `test.HelloWorldKt` and is located in path `$tmpdir/test/HelloWorldKt.class`
     */
    fun testRunClassFileWithExtensionNotInDefaultPackage() {
        val subDir = File("$tmpdir/test/sub").apply { mkdirs() }
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)
        assertExists(File("${testDir.path}/HelloWorldKt.class"))

        runProcess("kotlin", "test/HelloWorldKt.class", expectedStdout = "Hello!\n", workDirectory = tmpdir)
        runProcess(
            "kotlin", "test.HelloWorldKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class test.HelloWorldKt.class\n",
            workDirectory = tmpdir
        )
        runProcess("kotlin", "test/sub/../../test/HelloWorldKt.class", expectedStdout = "Hello!\n", workDirectory = tmpdir)
        runProcess(
            "kotlin", "./HelloWorldKt.class", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)
            
        """.trimIndent()
        )
        runProcess(
            "kotlin", "HelloWorldKt.class", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)
            
        """.trimIndent()
        )
        runProcess(
            "kotlin", "../HelloWorldKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class ../HelloWorldKt.class\n",
            workDirectory = subDir
        )
    }

    fun testKotlinUseJdkModuleFromMainClass() {
        val jdk11 = mapOf("JAVA_HOME" to KtTestUtil.getJdk11Home().absolutePath)
        runProcess(
            "kotlinc", "$testDataDirectory/jdkModuleUsage.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            environment = jdk11,
        )
        runProcess(
            "kotlin", K2JVMCompilerArguments::classpath.cliArgument, tmpdir.path, "test.JdkModuleUsageKt",
            expectedStdout = "interface java.sql.Driver\n",
            environment = jdk11,
        )
    }

    fun testKotlinUseJdkModuleFromJar() {
        val jdk11 = mapOf("JAVA_HOME" to KtTestUtil.getJdk11Home().absolutePath)
        val output = tmpdir.resolve("out.jar")
        runProcess(
            "kotlinc", "$testDataDirectory/jdkModuleUsage.kt", K2JVMCompilerArguments::destination.cliArgument, output.path,
            environment = jdk11,
        )
        runProcess(
            "kotlin", output.path,
            expectedStdout = "interface java.sql.Driver\n",
            environment = jdk11,
        )
    }

    fun testInterpreterClassLoader() {
        runProcess(
            "kotlinc", "$testDataDirectory/interpreterClassLoader.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )
    }

    fun testImplicitModularJdk() {
        // see KT-54337
        val moduleInfo = tmpdir.resolve("module-info.java").apply {
            writeText(
                """
                    module test {
                        requires kotlin.stdlib;
                    }
                """.trimIndent()
            )
        }
        val testKt = tmpdir.resolve("test.kt").apply {
            writeText("fun main() {}")
        }
        val jdk11 = mapOf("JAVA_HOME" to KtTestUtil.getJdk11Home().absolutePath)
        runProcess(
            "kotlinc", moduleInfo.absolutePath, testKt.absolutePath, K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            environment = jdk11,
            expectedExitCode = 0,
            expectedStdout = "",
            expectedStderr = ""
        )
    }

    fun testK2ClassPathWithRelativeDir() {
        val file1kt = tmpdir.resolve("file1.kt").apply {
            writeText("class C")
        }
        CompilerTestUtil.executeCompilerAssertSuccessful(
            K2JVMCompiler(),
            listOf(
                K2JVMCompilerArguments::destination.cliArgument,
                tmpdir.absolutePath,
                CommonCompilerArguments::languageVersion.cliArgument,
                "2.0",
                file1kt.absolutePath
            )
        )
        val file2kt = tmpdir.resolve("file1.kt").apply {
            writeText("val c = C()")
        }
        runProcess(
            "kotlinc",
            K2JVMCompilerArguments::classpath.cliArgument,
            ".",
            K2JVMCompilerArguments::destination.cliArgument,
            ".",
            CommonCompilerArguments::languageVersion.cliArgument,
            "2.0",
            file2kt.absolutePath,
            workDirectory = tmpdir,
            expectedStdout = "",
        )
    }

    fun testKotlinSimple() {
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path)
        runProcess(
            "kotlin",
            "-cp", tmpdir.path,
            "test.HelloWorldKt",
            expectedStdout = "Hello!\n"
        )
    }

    fun testKotlinFromJar() {
        val jarFile = File(tmpdir, "out.jar").path
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", jarFile)
        runProcess(
            "kotlin",
            "-cp", jarFile,
            "test.HelloWorldKt",
            expectedStdout = "Hello!\n"
        )
    }

    fun testPassSystemProperties() {
        runProcess("kotlinc", "$testDataDirectory/systemProperties.kt", "-d", tmpdir.path)
        runProcess(
            "kotlin",
            "-cp", tmpdir.path,
            "-Dfoo.name=foo.value",
            "-J-Dbar.name=bar.value",
            "test.SystemPropertiesKt",
            expectedStdout = "foo.name=foo.value\nbar.name=bar.value\n"
        )
    }

    fun testSanitizedStackTrace() {
        runProcess("kotlinc", "$testDataDirectory/throwException.kt", "-d", tmpdir.path)
        runProcess(
            "kotlin",
            "-cp", tmpdir.path,
            "test.ThrowExceptionKt",
            expectedExitCode = 1,
            expectedStderr = """
Exception in thread "main" java.lang.RuntimeException: RE
	at test.ThrowExceptionKt.f7(throwException.kt:40)
	at test.ThrowExceptionKt.f8(throwException.kt:45)
	at test.ThrowExceptionKt.f9(throwException.kt:49)
	at test.ThrowExceptionKt.main(throwException.kt:53)
Caused by: java.lang.IllegalStateException: ISE
	at test.ThrowExceptionKt.f4(throwException.kt:23)
	at test.ThrowExceptionKt.f5(throwException.kt:28)
	at test.ThrowExceptionKt.f6(throwException.kt:32)
	at test.ThrowExceptionKt.f7(throwException.kt:37)
	... 3 more
Caused by: java.lang.AssertionError: assert
	at test.ThrowExceptionKt.f1(throwException.kt:7)
	at test.ThrowExceptionKt.f2(throwException.kt:11)
	at test.ThrowExceptionKt.f3(throwException.kt:15)
	at test.ThrowExceptionKt.f4(throwException.kt:20)
	... 6 more
""".trimStart()
        )
    }

    fun testColorAlwaysProperty() {
        val testKt = tmpdir.resolve("test.kt").apply {
            writeText("val result: String = 42")
        }
        runProcess(
            "kotlinc", "-Dkotlin.colors.enabled=always", testKt.absolutePath, K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            expectedExitCode = 1,
            expectedStdout = "",
            expectedStderr = """
                ${"\$TMP_DIR\$"}/test.kt:1:22: [1;31merror: [0;1minitializer type mismatch: expected 'kotlin.String', actual 'kotlin.Int'.[m
                val result: String = 42
                                     ^^
                
            """.trimIndent(),
        )
    }
}
