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
import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.test.CompilerTestUtil
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase.assertExists
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.metadata.isExpect

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
        assertTrue(launcherFile.exists()) { "Launcher script not found, run dist task: ${launcherFile.absolutePath}" }

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
            assertEquals(expectedStdout.trim(), stdout.trim())
            assertEquals(expectedStderr.trim(), stderr.trim())
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
        get() = ForTestCompileRuntime.transformTestDataPath("compiler/tests-integration/testData/launcher").absolutePath

    private fun kotlincInProcess(vararg args: String) {
        val [output, exitCode] = AbstractCliTest.executeCompilerGrabOutput(K2JVMCompiler(), args.toList())
        if (exitCode != ExitCode.OK) error("Failed to compile: ${args.joinToString(" ")}\nOutput:\n$output")
    }

    @Test
    fun testKotlincSimple() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )
    }

    @Test
    fun testKotlincJvmSimple() {
        runProcess(
            "kotlinc-jvm",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )
    }

    @Test
    fun testKotlincJvmScriptWithClassPathFromSysProp() {
        runProcess(
            "kotlinc-jvm",
            "-script",
            "$testDataDirectory/classPathPropTest.kts",
            expectedStdout = "kotlin-compiler.jar\n"
        )
    }

    @Test
    fun testKotlinJvmContextClassLoader() {
        val kotlinTestJar = File(PathUtil.kotlinPathsForDistDirectory.homePath, "lib/kotlin-test.jar")
        assertTrue(kotlinTestJar.exists()) { "kotlin-main-kts.jar not found, run dist task: ${kotlinTestJar.absolutePath}" }
        kotlincInProcess(
            K2JVMCompilerArguments::classpath.cliArgument, kotlinTestJar.path,
            "$testDataDirectory/contextClassLoaderTester.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )

        runProcess(
            "kotlinr",
            K2JVMCompilerArguments::classpath.cliArgument, listOf(tmpdir.path, kotlinTestJar.path).joinToString(File.pathSeparator),
            "ContextClassLoaderTester",
            expectedStdout = "${kotlinTestJar.name}\n"
        )
    }

    @Test
    fun testKotlincJsSimple() {
        runProcess(
            "kotlinc-js",
            "$testDataDirectory/emptyMain.kt",
            K2JSCompilerArguments::suppressWarnings.cliArgument,
            K2JSCompilerArguments::libraries.cliArgument,
            PathUtil.kotlinPathsForCompiler.jsStdLibKlibPath.absolutePath,
            K2JSCompilerArguments::nopack.cliArgument,
            K2JSCompilerArguments::outputDir.cliArgument,
            tmpdir.path,
            K2JSCompilerArguments::moduleName.cliArgument,
            "out",
            environment = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath)
        )
    }

    @Test
    fun testKotlincWasmSimple() {
        runProcess(
            "kotlinc-wasm",
            "$testDataDirectory/emptyMain.kt",
            KotlinWasmCompilerArguments::suppressWarnings.cliArgument,
            KotlinWasmCompilerArguments::libraries.cliArgument(PathUtil.kotlinPathsForCompiler.wasmJsStdLibKlibPath.absolutePath),
            KotlinWasmCompilerArguments::nopack.cliArgument,
            KotlinWasmCompilerArguments::outputDir.cliArgument(tmpdir.path),
            KotlinWasmCompilerArguments::moduleName.cliArgument("out"),
            environment = mapOf("JAVA_HOME" to KtTestUtil.getJdk8Home().absolutePath)
        )
    }

    @Test
    fun testKotlinNoReflect() {
        kotlincInProcess("$testDataDirectory/reflectionUsage.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess(
            "kotlinr",
            K2JVMCompilerArguments::classpath.cliArgument, tmpdir.path,
            K2JVMCompilerArguments::noReflect.cliArgument,
            "ReflectionUsageKt",
            expectedStdout = "no reflection"
        )
    }

    @Test
    fun testDoNotAppendCurrentDirToNonEmptyClasspath() {
        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess("kotlinr", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)

        val emptyDir = KotlinTestUtils.tmpDirForTest(testInfo)
        runProcess(
            "kotlinr",
            K2JVMCompilerArguments::classpath.cliArgument, emptyDir.path,
            "test.HelloWorldKt",
            expectedStderr = "error: could not find or load main class test.HelloWorldKt\n",
            expectedExitCode = 1,
            workDirectory = tmpdir
        )
    }

    @Test
    fun testRunnerExpression() {
        runProcess(
            "kotlinr",
            "-e",
            "val x = 2; (args + listOf(2,1).map { (it * x).toString() }).joinToString()",
            "--",
            "a",
            "b",
            expectedStdout = "a, b, 4, 2\n"
        )
    }

    @Test
    fun testRunnerExpressionK2() {
        runProcess(
            "kotlinr",
            CommonCompilerArguments::languageVersion.cliArgument, LanguageVersion.FIRST_NON_DEPRECATED.versionString, "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n",
        )
    }

    @Test
    fun testCommandlineProcessing() {
        runProcess(
            "kotlinr",
            "-e",
            "println(args.joinToString())",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlinr",
            "-e",
            "println(args.joinToString())",
            "--",
            "-e",
            "b",
            expectedStdout = "-e, b\n"
        )
        runProcess(
            "kotlinr",
            "$testDataDirectory/printargs.kts",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
        runProcess(
            "kotlinr",
            "$testDataDirectory/printargs.kts",
            "--",
            "-a",
            "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testLegacyAssert() {
        kotlincInProcess(
            "$testDataDirectory/legacyAssertDisabled.kt",
            K2JVMCompilerArguments::assertionsMode.cliArgument("legacy"),
            K2JVMCompilerArguments::destination.cliArgument,
            tmpdir.path
        )

        runProcess("kotlinr", "LegacyAssertDisabledKt", "-J-da:kotlin._Assertions", workDirectory = tmpdir)

        kotlincInProcess(
            "$testDataDirectory/legacyAssertEnabled.kt",
            K2JVMCompilerArguments::assertionsMode.cliArgument("legacy"),
            K2JVMCompilerArguments::destination.cliArgument,
            tmpdir.path
        )

        runProcess("kotlinr", "LegacyAssertEnabledKt", "-J-ea:kotlin._Assertions", workDirectory = tmpdir)
    }

    @Test
    fun testScriptWithXArguments() {
        runProcess(
            "kotlinr", K2JVMCompilerArguments::noInline.cliArgument, "$testDataDirectory/noInline.kts",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline
	at NoInline.<init>(noInline.kts:1)
"""
        )
        runProcess("kotlinr", "$testDataDirectory/noInline.kts", expectedStdout = "OK\n")
    }

    @Test
    fun testNoStdLib() {
        runProcess("kotlinr", "-e", "println(42)", expectedStdout = "42\n")
        runProcess(
            "kotlinr", "-no-stdlib", "-e", "println(42)",
            expectedExitCode = 1,
            expectedStderr = """
                script.kts:1:1: error: unresolved reference 'println'.
                println(42)
                ^
                """.trimIndent()
        )
    }

    @Test
    fun testProperty() {
        kotlincInProcess("$testDataDirectory/property.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess(
            "kotlinr", "PropertyKt", "-Dresult=OK",
            workDirectory = tmpdir, expectedStdout = "OK\n"
        )
    }

    @Test
    fun testHowToRunExpression() {
        runProcess(
            "kotlinr", "-howtorun", "jar", "-e", "println(args.joinToString())", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: expression evaluation is not compatible with -howtorun argument jar\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "-e", "println(args.joinToString())", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testHowToRunScript() {
        runProcess(
            "kotlinr", "-howtorun", "classfile", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/printargs.kts\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "$testDataDirectory/printargs.kts", "--", "-a", "b",
            expectedStdout = "-a, b\n"
        )
    }

    @Test
    fun testHowToRunCustomScript() {
        runProcess(
            "kotlinr", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1, expectedStderr = "error: could not find or load main class \$TESTDATA_DIR\$/noInline.myscript\n"
        )
        runProcess(
            "kotlinr", "-howtorun", "script", "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = "error: unrecognized script type: noInline.myscript; Specify path to the script file as the first argument\n"
        )
        runProcess(
            "kotlinr",
            K2JVMCompilerArguments::allowAnyScriptsInSourceRoots.cliArgument,
            "-howtorun",
            ".kts",
            "$testDataDirectory/noInline.myscript",
            expectedExitCode = 1,
            expectedStderr = """compiler/tests-integration/testData/launcher/noInline.myscript:1:7: error: unresolved reference 'CompilerOptions'.
@file:CompilerOptions("-Xno-inline")
      ^
"""
        )
        runProcess(
            "kotlinr", "-howtorun", ".main.kts",
            "-P", "plugin:kotlin.scripting:disable-script-compilation-cache=true",
            "$testDataDirectory/noInline.myscript",
            expectedExitCode = 3,
            expectedStderr = """java.lang.IllegalAccessError: tried to access method kotlin.io.ConsoleKt.println(Ljava/lang/Object;)V from class NoInline
	at NoInline.<init>(noInline.myscript:3)
"""
        )
    }

    @Test
    fun testHowToRunClassFile() {
        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)

        runProcess(
            "kotlinr", "-howtorun", "jar", "test.HelloWorldKt", workDirectory = tmpdir,
            expectedExitCode = 1,
            expectedStderr = "error: could not read manifest from test.HelloWorldKt: test.HelloWorldKt (No such file or directory)\n"
        )
        runProcess("kotlinr", "-howtorun", "classfile", "test.HelloWorldKt", expectedStdout = "Hello!\n", workDirectory = tmpdir)
    }

    @Test
    fun testKotlincJdk17() {
        val jdk17 = mapOf("JAVA_HOME" to KtTestUtil.getJdk17Home().absolutePath)
        runProcess(
            "kotlinc", "$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            environment = jdk17,
        )

        runProcess(
            "kotlinr", "-e", "listOf('O'.toString() + 'K')",
            expectedStdout = "[OK]\n", environment = jdk17,
        )
    }

    @Test
    fun testEmptyJArgument() {
        runProcess(
            "kotlinc",
            "$testDataDirectory/helloWorld.kt",
            K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            "-J", expectedStdout = "error: empty -J argument\n",
            expectedExitCode = 1
        )
    }

    @Test
    fun testNoClassDefFoundErrorWhenClassInDefaultPackage() {
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/defaultPackage.kt", K2JVMCompilerArguments::destination.cliArgument, testDir.path)
        assertExists(File("${testDir.path}/DefaultPackageKt.class"))

        runProcess(
            "kotlinr", "test.DefaultPackageKt", workDirectory = tmpdir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class test.DefaultPackageKt
            Caused by: java.lang.NoClassDefFoundError: test/DefaultPackageKt (wrong name: DefaultPackageKt)

        """.trimIndent()
        )
    }

    @Test
    fun testNoClassDefFoundErrorWhenClassNotInDefaultPackage() {
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)
        assertExists(File("${testDir.path}/HelloWorldKt.class"))

        runProcess(
            "kotlinr", "HelloWorldKt", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)

        """.trimIndent()
        )
    }

    /**
     * A class whose full qualified name is `DefaultPackageKt` and is located in path `$tmpdir/test/DefaultPackageKt.class`
     */
    @Test
    fun testRunClassFileWithExtensionInDefaultPackage() {
        val subDir = File("$tmpdir/test/sub").apply { mkdirs() }
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/defaultPackage.kt", K2JVMCompilerArguments::destination.cliArgument, testDir.path)
        assertExists(File("${testDir.path}/DefaultPackageKt.class"))

        runProcess(
            "kotlinr", "test/DefaultPackageKt.class", workDirectory = tmpdir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class test.DefaultPackageKt
            Caused by: java.lang.NoClassDefFoundError: test/DefaultPackageKt (wrong name: DefaultPackageKt)
            
        """.trimIndent()
        )

        runProcess("kotlinr", "DefaultPackageKt.class", expectedStdout = "ok", workDirectory = testDir)
        runProcess("kotlinr", "./sub/../DefaultPackageKt.class", expectedStdout = "ok", workDirectory = testDir)
        runProcess(
            "kotlinr", "../DefaultPackageKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class ../DefaultPackageKt.class\n",
            workDirectory = subDir
        )
    }

    /**
     * A class whose full qualified name is `test.HelloWorldKt` and is located in path `$tmpdir/test/HelloWorldKt.class`
     */
    @Test
    fun testRunClassFileWithExtensionNotInDefaultPackage() {
        val subDir = File("$tmpdir/test/sub").apply { mkdirs() }
        val testDir = File("$tmpdir/test")

        kotlincInProcess("$testDataDirectory/helloWorld.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path)
        assertExists(File("${testDir.path}/HelloWorldKt.class"))

        runProcess("kotlinr", "test/HelloWorldKt.class", expectedStdout = "Hello!\n", workDirectory = tmpdir)
        runProcess(
            "kotlinr", "test.HelloWorldKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class test.HelloWorldKt.class\n",
            workDirectory = tmpdir
        )
        runProcess("kotlinr", "test/sub/../../test/HelloWorldKt.class", expectedStdout = "Hello!\n", workDirectory = tmpdir)
        runProcess(
            "kotlinr", "./HelloWorldKt.class", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)
            
        """.trimIndent()
        )
        runProcess(
            "kotlinr", "HelloWorldKt.class", workDirectory = testDir, expectedExitCode = 1,
            expectedStderr = """
            error: could not find or load main class HelloWorldKt
            Caused by: java.lang.NoClassDefFoundError: HelloWorldKt (wrong name: test/HelloWorldKt)
            
        """.trimIndent()
        )
        runProcess(
            "kotlinr", "../HelloWorldKt.class", expectedExitCode = 1,
            expectedStderr = "error: could not find or load main class ../HelloWorldKt.class\n",
            workDirectory = subDir
        )
    }

    @Test
    fun testKotlinUseJdkModuleFromMainClass() {
        val jdk11 = mapOf("JAVA_HOME" to KtTestUtil.getJdk11Home().absolutePath)
        runProcess(
            "kotlinc", "$testDataDirectory/jdkModuleUsage.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            environment = jdk11,
        )
        runProcess(
            "kotlinr", K2JVMCompilerArguments::classpath.cliArgument, tmpdir.path, "test.JdkModuleUsageKt",
            expectedStdout = "interface java.sql.Driver\n",
            environment = jdk11,
        )
    }

    @Test
    fun testKotlinUseJdkModuleFromJar() {
        val jdk11 = mapOf("JAVA_HOME" to KtTestUtil.getJdk11Home().absolutePath)
        val output = tmpdir.resolve("out.jar")
        runProcess(
            "kotlinc", "$testDataDirectory/jdkModuleUsage.kt", K2JVMCompilerArguments::destination.cliArgument, output.path,
            environment = jdk11,
        )
        runProcess(
            "kotlinr", output.path,
            expectedStdout = "interface java.sql.Driver\n",
            environment = jdk11,
        )
    }

    @Test
    fun testInterpreterClassLoader() {
        runProcess(
            "kotlinc", "$testDataDirectory/interpreterClassLoader.kt", K2JVMCompilerArguments::destination.cliArgument, tmpdir.path
        )
    }

    @Test
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

    @Test
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
                LanguageVersion.FIRST_NON_DEPRECATED.versionString,
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
            LanguageVersion.FIRST_NON_DEPRECATED.versionString,
            file2kt.absolutePath,
            workDirectory = tmpdir,
            expectedStdout = "",
        )
    }

    @Test
    fun testKotlinSimple() {
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path)
        runProcess(
            "kotlinr",
            "-cp", tmpdir.path,
            "test.HelloWorldKt",
            expectedStdout = "Hello!\n"
        )
    }

    @Test
    fun testKotlinExecutableIsdeprecated() {
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", tmpdir.path)
        runProcess(
            "kotlin",
            "-cp", tmpdir.path,
            "test.HelloWorldKt",
            expectedStdout = "Hello!\n",
            expectedStderr = "warning: the 'kotlin' executable is deprecated; use 'kotlinr' instead to avoid ambiguity with the Kotlin toolchain's 'kotlin' command.\n"
        )
    }

    @Test
    fun testKotlinFromJar() {
        val jarFile = File(tmpdir, "out.jar").path
        runProcess("kotlinc", "$testDataDirectory/helloWorld.kt", "-d", jarFile)
        runProcess(
            "kotlinr",
            "-cp", jarFile,
            "test.HelloWorldKt",
            expectedStdout = "Hello!\n"
        )
    }

    @Test
    fun testPassSystemProperties() {
        runProcess("kotlinc", "$testDataDirectory/systemProperties.kt", "-d", tmpdir.path)
        runProcess(
            "kotlinr",
            "-cp", tmpdir.path,
            "-Dfoo.name=foo.value",
            "-J-Dbar.name=bar.value",
            "test.SystemPropertiesKt",
            expectedStdout = "foo.name=foo.value\nbar.name=bar.value\n"
        )
    }

    @Test
    fun testSanitizedStackTrace() {
        runProcess("kotlinc", "$testDataDirectory/throwException.kt", "-d", tmpdir.path)
        runProcess(
            "kotlinr",
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

    @Test
    fun testColorAlwaysProperty() {
        val testKt = tmpdir.resolve("test.kt").apply {
            writeText("val result: String = 42")
        }
        runProcess(
            "kotlinc", "-Dkotlin.colors.enabled=always", testKt.absolutePath, K2JVMCompilerArguments::destination.cliArgument, tmpdir.path,
            expectedExitCode = 1,
            expectedStdout = "",
            expectedStderr = $$"""
                $TMP_DIR$/test.kt:1:20: [1;31merror: [0;1minitializer type mismatch: expected 'String', actual 'Int'.[m
                val result: String = 42
                                   ^
                
            """.trimIndent(),
        )
    }

    @Test
    fun testKaptVersion() {
        val info = $$"info: kotlinc-jvm $VERSION$ (JRE $JVM_VERSION$)\n"
        runProcess("kapt", "-version", expectedStderr = info)
    }

    @Test
    fun testCommonFragmentsMetadataDestination() {
        val metadataDir = compileSimpleCommonPlatformProject()

        val library = KlibLoader { libraryPaths(metadataDir.resolve("common").absolutePath) }.load().librariesStdlibFirst.single()
        val klibMetadata = library.metadata
        val module = KlibModuleMetadata.readStrict(object : KlibModuleMetadata.MetadataLibraryProvider {
            override val moduleHeaderData: ByteArray = klibMetadata.moduleHeaderData
            override val metadataVersion: KlibMetadataVersion =
                KlibMetadataVersion(library.versions.metadataVersion!!.toArray())

            override fun packageMetadataParts(fqName: String): Set<String> =
                klibMetadata.getPackageFragmentNames(fqName)

            override fun packageMetadata(fqName: String, partName: String): ByteArray =
                klibMetadata.getPackageFragment(fqName, partName)
        })

        val someClass = module.fragments.flatMap { it.classes }.singleOrNull { it.name == "Some" }
        assertNotNull(someClass) { "Class 'Some' must be present in the common-fragments metadata" }
        requireNotNull(someClass)
        assertTrue(someClass.isExpect) { "Class 'Some' must be marked as expect" }
        assertTrue(someClass.functions.any { it.name == "foo" })
        assertFalse(someClass.functions.any { it.name == "bar" })
    }

    /**
     * @return metadata output directory
     */
    private fun compileSimpleCommonPlatformProject(): File {
        val commonKt = tmpdir.resolve("common.kt").apply {
            writeText(
                """
                    expect class Some {
                        fun foo()
                    }
                    
                    internal fun baz() {}
                """.trimIndent()
            )
        }
        val platformKt = tmpdir.resolve("platform.kt").apply {
            writeText(
                """
                    actual class Some {
                        actual fun foo() {}
                        fun bar() {}
                    }
                """.trimIndent()
            )
        }

        val classesDir = tmpdir.resolve("classes")
        val metadataDir = tmpdir.resolve("common-metadata")

        runProcess(
            "kotlinc-jvm",
            commonKt.absolutePath,
            platformKt.absolutePath,
            K2JVMCompilerArguments::destination.cliArgument, classesDir.absolutePath,
            K2JVMCompilerArguments::multiPlatform.cliArgument,
            K2JVMCompilerArguments::expectActualClasses.cliArgument,
            K2JVMCompilerArguments::fragments.cliArgument("common,platform"),
            K2JVMCompilerArguments::fragmentSources.cliArgument("common:${commonKt.absolutePath},platform:${platformKt.absolutePath}"),
            K2JVMCompilerArguments::fragmentRefines.cliArgument("platform:common"),
            K2JVMCompilerArguments::commonFragmentsMetadataDestination.cliArgument(metadataDir.absolutePath),
        )
        return metadataDir
    }
}
