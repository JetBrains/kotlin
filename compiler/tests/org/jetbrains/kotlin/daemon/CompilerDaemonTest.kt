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

    val daemonOptions by lazy(LazyThreadSafetyMode.NONE) { DaemonOptions(runFilesPath = tmpdir.absolutePath) }
    val compilerId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(
                File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"),
                File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib/kotlin-runtime.jar"),
                File("dependencies/bootstrap-compiler/Kotlin/kotlinc/lib/kotlin-reflect.jar"))
    }

    private fun compileOnDaemon(daemonJVMOptions: DaemonJVMOptions, args: Array<out String>): CompilerResults {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true, checkId = true)
        TestCase.assertNotNull("failed to connect daemon", daemon)
        val strm = ByteArrayOutputStream()
        val code = KotlinCompilerClient.compile(daemon!!, args, strm)
        return CompilerResults(code, strm.toString())
    }

    private fun runDaemonCompilerTwice(logName: String, vararg arguments: String): Unit {
        KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

        System.setProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY, "")
        val logFile = File.createTempFile("kotlin-daemon-test.", ".log")
        System.setProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY, logFile.absolutePath)

        val daemonJVMOptions = configureDaemonJVMOptions(false)
        var daemonShotDown = false
//        daemonJVMOptions.jvmParams.add("agentlib:jdwp=transport=dt_socket\\,server=y\\,suspend=y\\,address=5005")

        try {
            val res1 = compileOnDaemon(daemonJVMOptions, arguments)
            TestCase.assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)
            val res2 = compileOnDaemon(daemonJVMOptions, arguments)
            TestCase.assertEquals("second compilation failed:\n${res2.out}", 0, res2.resultCode)
            TestCase.assertEquals("build results differ", CliBaseTest.removePerfOutput(res1.out), CliBaseTest.removePerfOutput(res2.out))
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            daemonShotDown = true
            logFile.reader().useLines {
                it.ifNotContainsSequence( LinePattern("Kotlin compiler daemon version"),
                                          LinePattern("Starting compilation with args: "),
                                          LinePattern("Starting compilation with args: "),
                                          LinePattern("Shutdown complete"))
                { (unmatchedPattern, lineNo) ->
                    TestCase.fail("pattern not found in the input: " + unmatchedPattern.regex +
                                  "\nunmatched part of the log file (" + logFile.absolutePath +
                                  ") from line " + lineNo + ":\n\n" + logFile.reader().useLines { it.drop(lineNo).joinToString("\n") })
                }
            }
            logFile.delete()
            // TODO: add performance comparison assert
        }
        finally {
            if (!daemonShotDown)
                KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            System.clearProperty(COMPILE_DAEMON_LOG_PATH_PROPERTY)
            System.clearProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)
        }
    }

    private fun getTestBaseDir(): String = JetTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)

    public fun testHelloApp() {
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"

        runDaemonCompilerTwice("hello.compile", "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar)
        run("hello.run", "-cp", jar, "Hello.HelloPackage")
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
}

fun restoreSystemProperty(propertyName: String, backupValue: String?) {
    if (backupValue == null) {
        System.clearProperty(propertyName)
    }
    else {
        System.setProperty(propertyName, backupValue)
    }
}
