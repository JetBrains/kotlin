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

package org.jetbrains.kotlin.daemon

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.CliBaseTest
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.rmi.*
import org.jetbrains.kotlin.rmi.kotlinr.DaemonReportingTargets
import org.jetbrains.kotlin.rmi.kotlinr.KotlinCompilerClient
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.ByteArrayOutputStream
import java.io.File

public class CompilerDaemonTest : KotlinIntegrationTestBase() {

    data class CompilerResults(val resultCode: Int, val out: String)

    val compilerClassPath = listOf(
            File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"))
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private fun compileOnDaemon(compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, vararg args: String): CompilerResults {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true, checkId = true)
        TestCase.assertNotNull("failed to connect daemon", daemon)
        val strm = ByteArrayOutputStream()
        val code = KotlinCompilerClient.compile(daemon!!, args, strm)
        return CompilerResults(code, strm.toString())
    }

    private fun runDaemonCompilerTwice(compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, vararg args: String): Unit {
            val res1 = compileOnDaemon(compilerId, daemonJVMOptions, daemonOptions, *args)
            TestCase.assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)
            val res2 = compileOnDaemon(compilerId, daemonJVMOptions, daemonOptions, *args)
            TestCase.assertEquals("second compilation failed:\n${res2.out}", 0, res2.resultCode)
            TestCase.assertEquals("build results differ", CliBaseTest.removePerfOutput(res1.out), CliBaseTest.removePerfOutput(res2.out))
    }

    private fun getTestBaseDir(): String = JetTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)
    private fun getHelloAppBaseDir(): String = JetTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp"

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)


    public fun testHelloApp() {
        val flagFile = createTempFile(getTestName(true), ".alive")
        flagFile.deleteOnExit()
        val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                                          clientAliveFlagPath = flagFile.absolutePath)

        KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

        val logFile = createTempFile("kotlin-daemon-test.", ".log")

        val daemonJVMOptions = configureDaemonJVMOptions(false,
                                                         "D$COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY",
                                                         "D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.absolutePath}\"")
        var daemonShotDown = false

        try {
            val jar = tmpdir.absolutePath + File.separator + "hello.jar"
            runDaemonCompilerTwice(compilerId, daemonJVMOptions, daemonOptions,
                                   "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar)

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            daemonShotDown = true
            var compileTime1 = 0L
            var compileTime2 = 0L
            logFile.reader().useLines {
                it.ifNotContainsSequence( LinePattern("Kotlin compiler daemon version"),
                                          LinePattern("Starting compilation with args: "),
                                          LinePattern("Elapsed time: (\\d+) ms", { it.groups.get(1)?.value?.toLong()?.let { compileTime1 = it }; true } ),
                                          LinePattern("Starting compilation with args: "),
                                          LinePattern("Elapsed time: (\\d+) ms", { it.groups.get(1)?.value?.toLong()?.let { compileTime2 = it }; true } ),
                                          LinePattern("Shutdown complete"))
                { unmatchedPattern, lineNo ->
                    TestCase.fail("pattern not found in the input: " + unmatchedPattern.regex +
                                  "\nunmatched part of the log file (" + logFile.absolutePath +
                                  ") from line " + lineNo + ":\n\n" + logFile.reader().useLines { it.drop(lineNo).joinToString("\n") })
                }
            }
            TestCase.assertTrue("Expecting that compilation 1 ($compileTime1 ms) is at least two times longer than compilation 2 ($compileTime2 ms)",
                                compileTime1 > compileTime2 * 2)
            logFile.delete()
            run("hello.run", "-cp", jar, "Hello.HelloPackage")
        }
        finally {
            if (!daemonShotDown)
                KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
        }
    }

    public fun testDaemonJvmOptionsParsing() {
        val backupJvmOptions = System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, "-aaa,-bbb\\,ccc,-ddd,-Xmx200m,-XX:MaxPermSize=10k,-XX:ReservedCodeCacheSize=100,-xxx\\,yyy")
            val opts = configureDaemonJVMOptions(inheritMemoryLimits = false)
            TestCase.assertEquals("200m", opts.maxMemory)
            TestCase.assertEquals("10k", opts.maxPermSize)
            TestCase.assertEquals("100", opts.reservedCodeCacheSize)
            TestCase.assertEquals(arrayListOf("aaa", "bbb,ccc", "ddd", "xxx,yyy"), opts.jvmParams)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, backupJvmOptions)
        }
    }

    public fun testDaemonOptionsParsing() {
        val backupOptions = System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, "runFilesPath=abcd,clientAliveFlagPath=efgh,autoshutdownIdleSeconds=1111")
            val opts = configureDaemonOptions()
            TestCase.assertEquals("abcd", opts.runFilesPath)
            TestCase.assertEquals("efgh", opts.clientAliveFlagPath)
            TestCase.assertEquals(1111, opts.autoshutdownIdleSeconds)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, backupOptions)
        }
    }

    public fun testDaemonInstances() {
        val jar = tmpdir.absolutePath + File.separator + "hello1.jar"
        val flagFile = createTempFile(getTestName(true), ".alive")
        flagFile.deleteOnExit()
        val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                                          clientAliveFlagPath = flagFile.absolutePath)
        val compilerId2 = CompilerId.makeCompilerId(compilerClassPath +
                                File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler-sources.jar"))

        KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
        KotlinCompilerClient.shutdownCompileService(compilerId2, daemonOptions)

        val logFile1 = createTempFile("kotlin-daemon1-test", ".log")
        val logFile2 = createTempFile("kotlin-daemon2-test", ".log")
        val daemonJVMOptions1 =
                configureDaemonJVMOptions(false,
                                          "D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile1.absolutePath}\"")
        val daemonJVMOptions2 =
                configureDaemonJVMOptions(false,
                                          "D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile2.absolutePath}\"")

        TestCase.assertTrue(logFile1.length() == 0L && logFile2.length() == 0L)

        val res1 = compileOnDaemon(compilerId, daemonJVMOptions1, daemonOptions,
                                   "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar)
        TestCase.assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)

        logFile1.assertLogContainsSequence("Starting compilation with args: ")
        TestCase.assertEquals("expecting '${logFile2.absolutePath}' to be empty", 0L, logFile2.length())

        val res2 = compileOnDaemon(compilerId2, daemonJVMOptions2, daemonOptions,
                                   "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar)
        TestCase.assertEquals("second compilation failed:\n${res2.out}", 0, res1.resultCode)

        logFile2.assertLogContainsSequence("Starting compilation with args: ")

        KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
        logFile1.assertLogContainsSequence("Shutdown complete")
        logFile1.delete()

        KotlinCompilerClient.shutdownCompileService(compilerId2, daemonOptions)
        logFile2.assertLogContainsSequence("Shutdown complete")
        logFile2.delete()
    }
}


fun File.assertLogContainsSequence(vararg patterns: String) {
    reader().useLines {
        it.ifNotContainsSequence( patterns.map { LinePattern(it) })
        {
            pattern,lineNo -> TestCase.fail("Pattern '${pattern.regex}' is not found in the log file '$absolutePath'")
        }
    }
}

fun restoreSystemProperty(propertyName: String, backupValue: String?) {
    if (backupValue == null) {
        System.clearProperty(propertyName)
    }
    else {
        System.setProperty(propertyName, backupValue)
    }
}
