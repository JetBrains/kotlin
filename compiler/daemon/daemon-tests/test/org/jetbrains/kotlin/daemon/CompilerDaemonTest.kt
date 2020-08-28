/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.client.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.KotlinPaths
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.Charset
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver.ResolveResult
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.test.fail

val TIMEOUT_DAEMON_RUNNER_EXIT_MS = 10000L

class CompilerDaemonTest : KotlinIntegrationTestBase() {

    data class CompilerResults(val resultCode: Int, val out: String)

    val compilerClassPath = getKotlinPaths().classPath(KotlinPaths.ClassPaths.Compiler)

    val compilerWithScriptingClassPath = getKotlinPaths().classPath(KotlinPaths.ClassPaths.CompilerWithScripting)

    val daemonClientClassPath = listOf( File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-daemon-client.jar"),
                                        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"))
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    val compilerWithScriptingId by lazy(LazyThreadSafetyMode.NONE) {
        CompilerId.makeCompilerId(compilerWithScriptingClassPath)
    }

    override fun shouldContainTempFiles(): Boolean = true

    // Using tmpDir from TestCaseWithTmpdir leads to the file paths with >255 chars (see e.g. #KT-32490), while KtUsefulTestCase already
    // setups a separate temp dir for each tests, if shouldContainTempFiles() returns true. Therefore current temp dir is used directly
    val testTempDir = File(FileUtilRt.getTempDirectory())

    private fun compileOnDaemon(clientAliveFile: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, vararg args: String): CompilerResults {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientAliveFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
        assertNotNull("failed to connect daemon", daemon)
        daemon?.registerClient(clientAliveFile.absolutePath)
        val strm = ByteArrayOutputStream()
        val code = KotlinCompilerClient.compile(daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM,
                                                args, PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true),
                                                reportSeverity = ReportSeverity.DEBUG)
        return CompilerResults(code, strm.toString())
    }

    private fun runDaemonCompilerTwice(clientAliveFile: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, vararg args: String): Unit {
            val res1 = compileOnDaemon(clientAliveFile, compilerId, daemonJVMOptions, daemonOptions, *args)
            assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)
            val res2 = compileOnDaemon(clientAliveFile, compilerId, daemonJVMOptions, daemonOptions, *args)
            assertEquals("second compilation failed:\n${res2.out}", 0, res2.resultCode)
            assertEquals("build results differ", AbstractCliTest.removePerfOutput(res1.out), AbstractCliTest.removePerfOutput(res2.out))
    }

    private fun getTestBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/" + getTestName(true)
    private fun getHelloAppBaseDir(): String = KotlinTestUtils.getTestDataPathBase() + "/integration/smoke/helloApp"

    private fun run(logName: String, vararg args: String): Int = runJava(getTestBaseDir(), logName, *args)

    fun makeTestDaemonOptions(testName: String, shutdownDelay: Int = 5) =
            DaemonOptions(runFilesPath = File(testTempDir, testName).absolutePath,
                                      shutdownDelayMilliseconds = shutdownDelay.toLong(),
                                      verbose = true,
                                      reportPerf = true)

    fun makeTestDaemonJvmOptions(logFile: File? = null, xmx: Int = 384, args: Iterable<String> = listOf()): DaemonJVMOptions {
        val additionalArgs = arrayListOf<String>()
        if (logFile != null) {
            additionalArgs.add("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"")
        }
        args.forEach { additionalArgs.add(it) }
        val baseOpts = if (xmx > 0) DaemonJVMOptions(maxMemory = "${xmx}m") else DaemonJVMOptions()
        return configureDaemonJVMOptions(
                baseOpts,
                *additionalArgs.toTypedArray(),
                inheritMemoryLimits = xmx <= 0,
                inheritAdditionalProperties = false,
                inheritOtherJvmOptions = false)
    }

    fun testHelloApp() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                var daemonShotDown = false

                try {
                    val jar = testTempDir.absolutePath + File.separator + "hello.jar"
                    runDaemonCompilerTwice(flagFile, compilerId, daemonJVMOptions, daemonOptions,
                                           "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar)

                    KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                    Thread.sleep(100)
                    daemonShotDown = true
                    var compileTime1 = 0L
                    var compileTime2 = 0L
                    logFile.assertLogContainsSequence(
                            LinePattern("Kotlin compiler daemon version"),
                            LinePattern("Starting compilation with args: "),
                            LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime1 = it }; true }),
                            LinePattern("Starting compilation with args: "),
                            LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime2 = it }; true }),
                            LinePattern("Shutdown started"))
                    assertTrue("Expecting that compilation 1 ($compileTime1 ms) is at least two times longer than compilation 2 ($compileTime2 ms)",
                               compileTime1 > compileTime2 * 2)
                    logFile.delete()
                    run("hello.run", "-cp", jar, "Hello.HelloKt")
                }
                finally {
                    if (!daemonShotDown)
                        KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                }
            }
        }
    }

    fun testDaemonJvmOptionsParsing() {
        val backupJvmOptions = System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, "-aaa,-bbb\\,ccc,-ddd,-Xmx200m,-XX:MaxMetaspaceSize=10k,-XX:ReservedCodeCacheSize=100,-xxx\\,yyy")
            val opts = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false, inheritOtherJvmOptions = false)
            assertEquals("200m", opts.maxMemory)
            assertEquals("10k", opts.maxMetaspaceSize)
            assertEquals("100", opts.reservedCodeCacheSize)
            assertEquals(arrayListOf("aaa", "bbb,ccc", "ddd", "xxx,yyy", "ea"), opts.jvmParams)

            System.setProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, "-Xmx300m,-XX:MaxMetaspaceSize=10k,-XX:ReservedCodeCacheSize=100")
            val opts2 = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false, inheritOtherJvmOptions = false)
            assertEquals("300m", opts2.maxMemory)
            assertEquals( -1, DaemonJVMOptionsMemoryComparator().compare(opts, opts2))
            assertEquals("300m", listOf(opts, opts2).maxWithOrNull(DaemonJVMOptionsMemoryComparator())?.maxMemory)

            val myXmxParam = ManagementFactory.getRuntimeMXBean().inputArguments.first { it.startsWith("-Xmx") }
            TestCase.assertNotNull(myXmxParam)
            val myXmxVal = myXmxParam.substring(4)
            System.clearProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
            val opts3 = configureDaemonJVMOptions(inheritMemoryLimits = true,
                                                  inheritOtherJvmOptions = true,
                                                  inheritAdditionalProperties = false)
            assertEquals(myXmxVal, opts3.maxMemory)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, backupJvmOptions)
        }
    }

    fun testDaemonAssertsOptions() {
        val allAssetionsArgs = setOf(
            "-ea", "-enableassertions",
            "-da", "-disableassertions",
            "-esa", "-enablesystemassertions",
            "-dsa", "-disablesystemassertions"
        )

        fun assertionsJvmArgs() = configureDaemonJVMOptions(
            inheritMemoryLimits = true,
            inheritOtherJvmOptions = false,
            inheritAdditionalProperties = true
        ).mappers.flatMap { it.toArgs("-") }.filter { it in allAssetionsArgs }.joinToString(", ")

        for (assertArgValue in allAssetionsArgs) {
            withDaemonJvmOptionsSetTo(assertArgValue) {
                assertEquals(assertArgValue, assertionsJvmArgs())
            }
        }

        withDaemonJvmOptionsSetTo(null) {
            assertEquals("-ea", assertionsJvmArgs())
        }
    }

    private fun withDaemonJvmOptionsSetTo(newValue: String?, fn: () -> Unit) {
        val backup = getAndSetSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, newValue)

        try {
            fn()
        } finally {
            getAndSetSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, backup)
        }
    }

    private fun getAndSetSystemProperty(property: String, newValue: String?): String? {
        val oldValue = System.getProperty(property)

        if (newValue != null) {
            System.setProperty(property, newValue)
        } else {
            System.clearProperty(property)
        }

        return oldValue
    }

    fun testDaemonOptionsParsing() {
        val backupOptions = System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, "runFilesPath=abcd,autoshutdownIdleSeconds=1111")
            val opts = configureDaemonOptions(DaemonOptions(shutdownDelayMilliseconds = 1))
            assertEquals("abcd", opts.runFilesPath)
            assertEquals(1111, opts.autoshutdownIdleSeconds)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, backupOptions)
        }
    }

    fun testDaemonInstancesSimple() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))
            val compilerId2 = CompilerId.makeCompilerId(compilerClassPath +
                                                        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler-sources.jar"))

            withLogFile("kotlin-daemon1-test") { logFile1 ->
                withLogFile("kotlin-daemon2-test") { logFile2 ->
                    val daemonJVMOptions1 = makeTestDaemonJvmOptions(logFile1)
                    val daemonJVMOptions2 = makeTestDaemonJvmOptions(logFile2)

                    assertTrue(logFile1.length() == 0L && logFile2.length() == 0L)

                    val jar1 = testTempDir.absolutePath + File.separator + "hello1.jar"
                    val res1 = compileOnDaemon(flagFile, compilerId, daemonJVMOptions1, daemonOptions, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar1)
                    assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)

                    logFile1.assertLogContainsSequence("Starting compilation with args: ")
                    assertEquals("expecting '${logFile2.absolutePath}' to be empty", 0L, logFile2.length())

                    val jar2 = testTempDir.absolutePath + File.separator + "hello2.jar"
                    val res2 = compileOnDaemon(flagFile, compilerId2, daemonJVMOptions2, daemonOptions, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar2)
                    assertEquals("second compilation failed:\n${res2.out}", 0, res1.resultCode)

                    logFile2.assertLogContainsSequence("Starting compilation with args: ")

                    KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                    KotlinCompilerClient.shutdownCompileService(compilerId2, daemonOptions)

                    Thread.sleep(100)

                    logFile1.assertLogContainsSequence("Shutdown started")
                    logFile2.assertLogContainsSequence("Shutdown started")
                }
            }
        }
    }

    fun testDaemonRunError() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(shutdownDelayMilliseconds = 1, verbose = true, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            val daemonJVMOptions = configureDaemonJVMOptions("-abracadabra", inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false)

            val messageCollector = TestMessageCollector()

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions,
                                                                      DaemonReportingTargets(messageCollector = messageCollector), autostart = true)

            assertNull(daemon)

            messageCollector.assertHasMessage("Unrecognized option: --abracadabra")
        }
    }

    // TODO: find out how to reliably cause the retry
    fun ignore_testDaemonStartRetry() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(shutdownDelayMilliseconds = 1, verbose = true, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritOtherJvmOptions = false, inheritAdditionalProperties = false)

            val messageCollector = TestMessageCollector()

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions,
                                                                      DaemonReportingTargets(messageCollector = messageCollector), autostart = true)

            assertNull(daemon)

            messageCollector.assertHasMessage("retrying(0) on:")
            messageCollector.assertHasMessage("retrying(1) on:")
            // TODO: messageCollector.assertHasNoMessage("retrying(2) on:")
            messageCollector.assertHasMessage("no more retries on:")
        }
    }

    fun testDaemonAutoshutdownOnUnused() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownUnusedSeconds = 1, shutdownDelayMilliseconds = 1, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.registerClient(flagFile.absolutePath)

                // wait up to 4s (more than 1s unused timeout)
                for (attempts in 1..20) {
                    if (logFile.isLogContainsSequence("Unused timeout exceeded 1s")) break
                    Thread.sleep(200)
                }
                Thread.sleep(200)

                logFile.assertLogContainsSequence("Unused timeout exceeded 1s",
                                                  "Shutdown started")
            }
        }
    }

    fun testDaemonAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, shutdownDelayMilliseconds = 1, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.registerClient(flagFile.absolutePath)
                val jar = testTempDir.absolutePath + File.separator + "hello1.jar"
                val strm = ByteArrayOutputStream()
                val code = KotlinCompilerClient.compile(daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM,
                                                        arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                                                        PrintingMessageCollector(PrintStream(strm), MessageRenderer.WITHOUT_PATHS, true),
                                                        reportSeverity = ReportSeverity.DEBUG)
                assertEquals("compilation failed:\n$strm", 0, code)

                logFile.assertLogContainsSequence("Starting compilation with args: ")

                // wait up to 4s (more than 1s idle timeout)
                for (attempts in 1..20) {
                    if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                    Thread.sleep(200)
                }
                Thread.sleep(200)
                logFile.assertLogContainsSequence("Idle timeout exceeded 1s",
                                                  "Shutdown started")
            }
        }
    }

    fun testDaemonGracefulShutdown() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, shutdownDelayMilliseconds = 1, forceShutdownTimeoutMilliseconds = 60000, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.registerClient(flagFile.absolutePath)
                val sessionId = daemon?.leaseCompileSession(null)

                val scheduleShutdownRes = daemon?.scheduleShutdown(true)

                assertTrue("failed to schedule shutdown ($scheduleShutdownRes)", scheduleShutdownRes?.let { it.isGood && it.get() } ?: false)

                Thread.sleep(100) // to allow timer task to run in the daemon

                logFile.assertLogContainsSequence("Some sessions are active, waiting for them to finish")

                val res = daemon?.leaseCompileSession(null)

                assertEquals("Invalid state", CompileService.CallResult.Dying(), res)

                daemon?.releaseCompileSession(sessionId!!.get())

                Thread.sleep(100) // allow after session timed action to run

                logFile.assertLogContainsSequence("All sessions finished",
                                                  "Shutdown started")
            }
        }
    }

    fun testDaemonExitsOnClientFlagDeletedWithActiveSessions() {
        val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1000, shutdownDelayMilliseconds = 1, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)
        val clientFlag = FileUtil.createTempFile(getTestName(true), "-client.alive")
        val sessionFlag = FileUtil.createTempFile(getTestName(true), "-session.alive")
        try {
            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientFlag, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.leaseCompileSession(sessionFlag.canonicalPath)

                clientFlag.delete()

                Thread.sleep(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o
                // TODO: consider possibility to set DAEMON_PERIODIC_CHECK_INTERVAL_MS from tests, to allow shorter sleeps

                logFile.assertLogContainsSequence("No more clients left",
                                                  "Shutdown started")
            }
        }
        finally {
            sessionFlag.delete()
            clientFlag.delete()
        }
    }

    fun testDaemonExitsOnClientFlagDeletedWithAllSessionsReleased() {
        val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1000, shutdownDelayMilliseconds = 1, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)
        val clientFlag = FileUtil.createTempFile(getTestName(true), "-client.alive")
        val sessionFlag = FileUtil.createTempFile(getTestName(true), "-session.alive")
        try {
            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientFlag, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.leaseCompileSession(sessionFlag.canonicalPath)

                sessionFlag.delete()

                Thread.sleep(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o

                clientFlag.delete()

                Thread.sleep(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o

                logFile.assertLogContainsSequence("No more clients left",
                                                  "Shutdown started")
            }
        }
        finally {
            sessionFlag.delete()
            clientFlag.delete()
        }
    }

    fun testDaemonCancelShutdownOnANewClient() {
        val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1000, shutdownDelayMilliseconds = 3000, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)
        val clientFlag = FileUtil.createTempFile(getTestName(true), "-client.alive")
        val clientFlag2 = FileUtil.createTempFile(getTestName(true), "-client.alive")
        try {
            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientFlag, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.leaseCompileSession(null)

                clientFlag.delete()

                Thread.sleep(2100) // allow deleted file detection, should be 2 * DAEMON_PERIODIC_CHECK_INTERVAL_MS + o
                // TODO: consider possibility to set DAEMON_PERIODIC_CHECK_INTERVAL_MS from tests, to allow shorter sleeps

                val daemon2 = KotlinCompilerClient.connectToCompileService(compilerId, clientFlag2, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon2)
                daemon2?.leaseCompileSession(null)

                Thread.sleep(3000) // allow to trigger delayed shutdown timer

                logFile.assertLogContainsSequence("No more clients left",
                                                  "Delayed shutdown in 3000ms",
                                                  "Cancel delayed shutdown due to a new activity")
            }
        }
        finally {
            clientFlag.delete()
            clientFlag2.delete()
        }
    }

    /** Testing that running daemon in the child process doesn't block on s child process.waitFor()
     *  that may happen on windows if simple processBuilder.start is used due to handles inheritance:
     *  - process A starts process B using ProcessBuilder and waits for it using process.waitFor()
     *  - process B starts daemon and exits
     *  - due to default behavior of CreateProcess on windows, the handles of process B are inherited by the daemon
     *    (in particular handles of stdin/out/err) and therefore these handles remain open while daemon is running
     *  - (seems) due to the way how waiting for process is implemented, waitFor() hangs until daemon is killed
     *  This seems a known problem, e.g. gradle uses a library with native code that prevents io handles inheritance when launching it's daemon
     *  (the same solution is used in kotlin daemon client - see next commit)
     */
    fun testDaemonExecutionViaIntermediateProcess() {
        val clientAliveFile = FileUtil.createTempFile("kotlin-daemon-transitive-run-test", ".run")
        val daemonOptions = makeTestDaemonOptions(getTestName(true))
        val jar = testTempDir.absolutePath + File.separator + "hello.jar"
        val args = listOf(
                File(File(System.getProperty("java.home"), "bin"), "java").absolutePath,
                "-Xmx256m",
                "-D$COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY",
                "-cp",
                daemonClientClassPath.joinToString(File.pathSeparator) { it.absolutePath },
                KotlinCompilerClient::class.qualifiedName!!) +
                   daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   File(getHelloAppBaseDir(), "hello.kt").absolutePath +
                   "-d" + jar
        try {
            var resOutput: String? = null
            var resCode: Int? = null
            // running intermediate process (daemon command line controller) that executes the daemon
            val runnerProcess = ProcessBuilder(args).redirectErrorStream(true).start()
            thread {
                resOutput = runnerProcess.inputStream.reader().readText()
            }
            val waitThread = thread {
                resCode = runnerProcess.waitFor()
            }
            waitThread.join(TIMEOUT_DAEMON_RUNNER_EXIT_MS)

            assertFalse("process.waitFor() hangs:\n$resOutput", waitThread.isAlive)
            assertEquals("Compilation failed:\n$resOutput", 0, resCode)
        }
        finally {
            if (clientAliveFile.exists())
                clientAliveFile.delete()
        }
    }

    private val PARALLEL_THREADS_TO_COMPILE = 10
    private val PARALLEL_WAIT_TIMEOUT_S = 60L

    fun testParallelCompilationOnDaemon() {

        assertTrue(PARALLEL_THREADS_TO_COMPILE <= LoopbackNetworkInterface.SERVER_SOCKET_BACKLOG_SIZE)

        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile, xmx = -1)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)

                val (_, port) = findPortAndCreateRegistry(10, 16384, 65535)
                val resultCodes = arrayOfNulls<Int>(PARALLEL_THREADS_TO_COMPILE)
                val localEndSignal = CountDownLatch(PARALLEL_THREADS_TO_COMPILE)
                val outStreams = Array(PARALLEL_THREADS_TO_COMPILE, { ByteArrayOutputStream() })

                fun runCompile(threadNo: Int) =
                        thread {
                            val jar = testTempDir.absolutePath + File.separator + "hello.$threadNo.jar"
                            val res = KotlinCompilerClient.compile(
                                    daemon!!,
                                    CompileService.NO_SESSION,
                                    CompileService.TargetPlatform.JVM,
                                    arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                                    PrintingMessageCollector(PrintStream(outStreams[threadNo]), MessageRenderer.WITHOUT_PATHS, true),
                                    port = port)
                            synchronized(resultCodes) {
                                resultCodes[threadNo] = res
                            }
                            localEndSignal.countDown()
                        }

                (1..PARALLEL_THREADS_TO_COMPILE).forEach { runCompile(it - 1) }

                val succeeded = localEndSignal.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)
                assertTrue("parallel compilation failed to complete in $PARALLEL_WAIT_TIMEOUT_S s, ${localEndSignal.count} unfinished threads", succeeded)

                (1..PARALLEL_THREADS_TO_COMPILE).forEach {
                    assertEquals("Compilation on thread $it failed:\n${outStreams[it - 1]}", 0, resultCodes[it - 1])
                }
            }
        }
    }

    private object ParallelStartParams {
        const val threads = 32
        const val performCompilation = false
        const val connectionFailedErr = -100
    }

    fun testParallelDaemonStart() {

        val doneLatch = CountDownLatch(ParallelStartParams.threads)

        val resultCodes = arrayOfNulls<Int>(ParallelStartParams.threads)
        val outStreams = Array(ParallelStartParams.threads, { ByteArrayOutputStream() })
        val logFiles = arrayOfNulls<File>(ParallelStartParams.threads)
        val daemonInfos = arrayOfNulls<Pair<CompileService.CallResult<String>?, Int?>>(ParallelStartParams.threads)

        val daemonOptions = makeTestDaemonOptions(getTestName(true), 100)

        fun connectThread(threadNo: Int) = thread(name = "daemonConnect$threadNo") {
            try {
                withFlagFile(getTestName(true), ".alive") { flagFile ->
                    withLogFile("kotlin-daemon-test", printLogOnException = false) { logFile ->
                        logFiles[threadNo] = logFile
                        val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                        val compileServiceSession =
                                KotlinCompilerClient.connectAndLease(compilerId, flagFile, daemonJVMOptions, daemonOptions,
                                                                     DaemonReportingTargets(out = PrintStream(outStreams[threadNo])), autostart = true,
                                                                     leaseSession = true)
                        daemonInfos[threadNo] = compileServiceSession?.compileService?.getDaemonInfo() to compileServiceSession?.sessionId

                        resultCodes[threadNo] = when {
                            compileServiceSession?.compileService == null -> {
                                ParallelStartParams.connectionFailedErr
                            }
                            ParallelStartParams.performCompilation -> {
                                val jar = testTempDir.absolutePath + File.separator + "hello.$threadNo.jar"
                                KotlinCompilerClient.compile(
                                    compileServiceSession.compileService,
                                    compileServiceSession.sessionId,
                                    CompileService.TargetPlatform.JVM,
                                    arrayOf(File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                                    PrintingMessageCollector(PrintStream(outStreams[threadNo]), MessageRenderer.WITHOUT_PATHS, true))
                            }
                            else -> 0 // compilation skipped, assuming - successful
                        }
                    }
                }
            }
            finally {
                doneLatch.countDown()
            }
        }

        System.setProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY, "true")
        System.setProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY, "100000")

        val succeeded = try {
            (1..ParallelStartParams.threads).forEach { connectThread(it - 1) }
            doneLatch.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)
        }
        finally {
            System.clearProperty(COMPILE_DAEMON_STARTUP_TIMEOUT_PROPERTY)
            System.clearProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)
        }

        Thread.sleep(100) // Wait for processes to finish and close log files

        val electionLogs = arrayOfNulls<String>(ParallelStartParams.threads)
        val port2logs = arrayOfNulls<Pair<Int?, File?>>(ParallelStartParams.threads)

        for (i in 0..(ParallelStartParams.threads - 1)) {
            val logContents = logFiles[i]?.readLines()
            port2logs[i] = logContents?.find { it.contains("daemon is listening on port") }?.split(" ")?.last()?.toIntOrNull() to logFiles[i]
            electionLogs[i] = logContents?.find { it.contains(LOG_PREFIX_ASSUMING_OTHER_DAEMONS_HAVE) }
        }

        val electionsSuccess = electionLogs.any { it != null && (it.contains("lower prio") || it.contains("equal prio")) }
        val resultsFailures = resultCodes.count { it != null && it == 0 }

        if (!succeeded || !electionsSuccess || resultsFailures > 0) {
            val msg = buildString {
                for (i in 0..ParallelStartParams.threads - 1) {
                    val daemonInfoRes = daemonInfos[i]?.first
                    val daemonInfo = when (daemonInfoRes) {
                        is CompileService.CallResult.Good -> daemonInfoRes.get()
                        is CompileService.CallResult.Dying -> "<dying>"
                        is CompileService.CallResult.Error -> "<error: ${daemonInfoRes.message}>"
                        else -> "?"
                    }
                    val compiledPort: Int? = daemonInfo.trim().split(" ").last().toIntOrNull()
                    appendLine("#$i\tcompiled on $daemonInfo, session ${daemonInfos[i]?.second}, result ${resultCodes[i]}; started daemon on port ${port2logs[i]?.first}, log: ${logFiles[i]?.canonicalPath}")
                    if (resultCodes[i] != 0 || electionLogs[i] == null) {
                        appendLine("--- out $i, result ${resultCodes[i]}:\n${outStreams[i].toByteArray().toString(Charset.defaultCharset())}\n---")
                        compiledPort?.let { port -> port2logs.find { it?.first == port } }?.second?.let { logFile ->
                            appendLine("--- log file ${logFile.name}:\n${logFile.readText()}\n---")
                        }
                        ?: appendLine("--- log not found (port: $compiledPort)")
                    }
                }
            }
            assertTrue("parallel daemons start failed to complete in $PARALLEL_WAIT_TIMEOUT_S s, ${doneLatch.count} unfinished threads:\n\n$msg", succeeded)
            assertTrue("No daemon elected:\n\n$msg\n--- elections:\n${electionLogs.joinToString("\n")}\n---", electionsSuccess)
            assertTrue("Compilations failed: $resultsFailures of ${ParallelStartParams.threads}:\n\n$msg", resultsFailures > 0)
        }
    }

    fun testDaemonConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))
            val daemonJVMOptions = makeTestDaemonJvmOptions()
            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)
            daemon?.registerClient(flagFile.absolutePath)

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            Thread.sleep(200)

            val exception: Exception? =
                    try {
                        daemon!!.getUsedMemory()
                        null
                    }
                    catch (e: java.rmi.ConnectException) {
                        e
                    }
                    catch (e: java.rmi.UnmarshalException) {
                        e
                    }
                    catch (e: java.rmi.ConnectIOException) {
                        e
                    }
            assertNotNull(exception)
        }
    }

    fun testDaemonCallbackConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)
                daemon?.registerClient(flagFile.absolutePath)

                val file = File(testTempDir, "largeKotlinFile.kt")
                file.writeText(generateLargeKotlinFile(10))
                val jar = File(testTempDir, "largeKotlinFile.jar").absolutePath

                var callbackServices: CompilerCallbackServicesFacadeServer? = null
                callbackServices = CompilerCallbackServicesFacadeServer(compilationCanceledStatus = object : CompilationCanceledStatus {
                    override fun checkCanceled() {
                        thread {
                            Thread.sleep(10)
                            UnicastRemoteObject.unexportObject(callbackServices, true)
                        }
                    }
                }, port = SOCKET_ANY_FREE_PORT)
                val strm = ByteArrayOutputStream()
                @Suppress("DEPRECATION")
                val code = daemon!!.remoteCompile(CompileService.NO_SESSION,
                                                  CompileService.TargetPlatform.JVM,
                                                  arrayOf("-include-runtime", file.absolutePath, "-d", jar),
                                                  callbackServices,
                                                  RemoteOutputStreamServer(strm, SOCKET_ANY_FREE_PORT),
                                                  CompileService.OutputFormat.XML,
                                                  RemoteOutputStreamServer(strm, SOCKET_ANY_FREE_PORT),
                                                  null).get()

                TestCase.assertEquals(0, code)

                val compilerOutput = strm.toString()
                assertTrue("Expecting cancellation message in:\n$compilerOutput", compilerOutput.contains("Compilation was canceled"))
                logFile.assertLogContainsSequence("error communicating with host, assuming compilation canceled")

            }
        }
    }

    fun testDaemonReplScriptingNotInClasspathError() {
        withDaemon(compilerId) { daemon ->
            var repl: KotlinRemoteReplCompilerClient? = null
            var isErrorThrown = false
            try {
                repl = KotlinRemoteReplCompilerClient(
                    daemon, null, CompileService.TargetPlatform.JVM, emptyArray(), TestMessageCollector(),
                    classpathFromClassloader(), ScriptWithNoParam::class.qualifiedName!!
                )
                repl.createState()
            } catch (e: Exception) {
                TestCase.assertEquals(
                    "Unable to use scripting/REPL in the daemon: no scripting plugin loaded",
                    e.message
                )
                isErrorThrown = true
            } finally {
                repl?.dispose()
            }
            TestCase.assertTrue("Expecting exception that scripting plugin is not loaded", isErrorThrown)
        }
    }

    fun testDaemonReplLocalEvalNoParams() {
        withDaemon(compilerWithScriptingId) { daemon ->
            val repl = KotlinRemoteReplCompilerClient(daemon, null, CompileService.TargetPlatform.JVM,
                                                      emptyArray(),
                                                      TestMessageCollector(),
                                                      classpathFromClassloader(),
                                                      ScriptWithNoParam::class.qualifiedName!!)

            val localEvaluator = GenericReplEvaluator(emptyList(), Thread.currentThread().contextClassLoader)

            doReplTestWithLocalEval(repl, localEvaluator)
            repl.dispose()
        }
    }

    fun testDaemonReplLocalEvalStandardTemplate() {
        withDaemon(compilerWithScriptingId) { daemon ->
            val repl = KotlinRemoteReplCompilerClient(daemon, null, CompileService.TargetPlatform.JVM, emptyArray(),
                                                      TestMessageCollector(),
                                                      classpathFromClassloader(),
                                                      "kotlin.script.templates.standard.ScriptTemplateWithArgs")

            val localEvaluator = GenericReplEvaluator(emptyList(), Thread.currentThread().contextClassLoader,
                                                      ScriptArgsWithTypes(arrayOf(emptyArray<String>()), arrayOf(Array<String>::class)))

            doReplTestWithLocalEval(repl, localEvaluator)
            repl.dispose()
        }
    }

    private fun doReplTestWithLocalEval(replCompiler: KotlinRemoteReplCompilerClient, localEvaluator: ReplEvaluator) {

        val compilerState = replCompiler.createState()
        val evaluatorState = localEvaluator.createState()

        val res0 = replCompiler.check(compilerState, ReplCodeLine(0, 0, "val x ="))
        TestCase.assertTrue("Unexpected check results: $res0", res0 is ReplCheckResult.Incomplete)

        val codeLine1 = ReplCodeLine(1, 0, "val lst = listOf(1)\nval x = 5")
        val res1 = replCompiler.compile(compilerState, codeLine1)
        val res1c = res1 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res1", res1c)

        val res11 = localEvaluator.eval(evaluatorState, res1c!!)
        val res11e = res11 as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $res11", res11e)

        val codeLine2 = ReplCodeLine(2, 0, "x + 2")
        val res2 = replCompiler.compile(compilerState, codeLine2)
        val res2c = res2 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res2", res2c)

        val res21 = localEvaluator.eval(evaluatorState, res2c!!)
        val res21e = res21 as? ReplEvalResult.ValueResult
        TestCase.assertNotNull("Unexpected eval result: $res21", res21e)
        TestCase.assertEquals(7, res21e!!.value)
    }

    fun testDaemonReplAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, autoshutdownUnusedSeconds = 1, shutdownDelayMilliseconds = 1, runFilesPath = File(testTempDir, getTestName(true)).absolutePath)

            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)

                val daemon = KotlinCompilerClient.connectToCompileService(compilerWithScriptingId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)

                val replCompiler = KotlinRemoteReplCompilerClient(daemon!!, null, CompileService.TargetPlatform.JVM,
                                                                  emptyArray(),
                                                                  TestMessageCollector(),
                                                                  classpathFromClassloader(),
                                                                  ScriptWithNoParam::class.qualifiedName!!)

                val compilerState = replCompiler.createState()

                // use repl compiler for >> 1s, making sure that idle/unused timeouts are not firing
                for (attempts in 1..10) {
                    val codeLine1 = ReplCodeLine(attempts, 0, "3 + 5")
                    val res1 = replCompiler.compile(compilerState, codeLine1)
                    val res1c = res1 as? ReplCompileResult.CompiledClasses
                    TestCase.assertNotNull("Unexpected compile result: $res1", res1c)
                    Thread.sleep(200)
                }

                // wait up to 4s (more than 1s idle timeout)
                for (attempts in 1..20) {
                    if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                    Thread.sleep(200)
                }
                replCompiler.dispose()

                Thread.sleep(200)
                logFile.assertLogContainsSequence("Idle timeout exceeded 1s",
                                                  "Shutdown started")
            }
        }
    }

    internal fun withDaemon(compilerId: CompilerId = this.compilerId, body: (CompileService) -> Unit) {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = makeTestDaemonOptions(getTestName(true))
            withLogFile("kotlin-daemon-test") { logFile ->
                val daemonJVMOptions = makeTestDaemonJvmOptions(logFile)
                val daemon: CompileService? = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
                assertNotNull("failed to connect daemon", daemon)

                body(daemon!!)
            }
        }
    }
}


// stolen from CompilerFileLimitTest
internal fun generateLargeKotlinFile(size: Int): String {
    return buildString {
        append("package large\n\n")
        (0..size).forEach {
            appendLine("class Class$it")
            appendLine("{")
            appendLine("\tfun foo(): Long = $it")
            appendLine("}")
            appendLine("\n")
            repeat(2000) {
                appendLine("// kotlin rules ... and stuff")
            }
        }
        appendLine("fun main(args: Array<String>)")
        appendLine("{")
        appendLine("\tval result = Class5().foo() + Class$size().foo()")
        appendLine("\tprintln(result)")
        appendLine("}")
    }

}


internal fun File.ifLogNotContainsSequence(vararg patterns: String, body: (LinePattern, Int) -> Unit) {
    reader().useLines {
        it.asSequence().ifNotContainsSequence(patterns.map { LinePattern(it) }, body)
    }
}

internal fun File.assertLogContainsSequence(vararg patterns: String) = assertLogContainsSequence(patterns.map { LinePattern(it) })

internal fun File.assertLogContainsSequence(vararg patterns: LinePattern) = assertLogContainsSequence(patterns.asIterable())

internal fun File.assertLogContainsSequence(patterns: Iterable<LinePattern>) {
    val lines = reader().readLines()
    lines.asSequence().ifNotContainsSequence(patterns.iterator())
    { unmatchedPattern, lineNo ->
        fail("pattern not found in the input: ${unmatchedPattern.regex}\nunmatched part of the log file ($absolutePath) from line $lineNo:\n\n${lines.asSequence().drop(lineNo).joinToString("\n")}\n-------")
    }
}

internal fun File.isLogContainsSequence(vararg patterns: String): Boolean {
    var res = true
    ifLogNotContainsSequence(*patterns) { _, _ -> res = false }
    return res
}

fun restoreSystemProperty(propertyName: String, backupValue: String?) {
    if (backupValue == null) {
        System.clearProperty(propertyName)
    }
    else {
        System.setProperty(propertyName, backupValue)
    }
}

internal inline fun withFlagFile(prefix: String, suffix: String? = null, body: (File) -> Unit) {
    val file = FileUtil.createTempFile(prefix, suffix)
    try {
        body(file)
    }
    finally {
        file.delete()
    }
}

internal inline fun withLogFile(prefix: String, suffix: String = ".log", printLogOnException: Boolean = true, body: (File) -> Unit) {
    val logFile = FileUtil.createTempFile(prefix, suffix)
    try {
        body(logFile)
    }
    catch (e: Exception) {
        if (printLogOnException) {
            Thread.sleep(100) // trying to wait log flushing
            System.err.println("--- log file ${logFile.name}:\n${logFile.readText()}\n---")
        }
        throw e
    }
}

// java.util.Logger used in the daemon silently forgets to log into a file specified in the config on Windows,
// if file path is given in windows form (using backslash as a separator); the reason is unknown
// this function makes a path with forward slashed, that works on windows too
internal val File.loggerCompatiblePath: String
    get() =
        if (OSKind.current == OSKind.Windows) absolutePath.replace('\\', '/')
        else absolutePath

open class TestKotlinScriptDummyDependenciesResolver : DependenciesResolver {

    override fun resolve(scriptContents: ScriptContents, environment: Environment): ResolveResult {
        return ScriptDependencies(
            classpath = classpathFromClassloader(),
            imports = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        ).asSuccess()
    }
}

@ScriptTemplateDefinition(resolver = TestKotlinScriptDummyDependenciesResolver::class)
abstract class ScriptWithNoParam

internal fun classpathFromClassloader(): List<File> {
    val additionalClasspath = System.getProperty("kotlin.test.script.classpath")?.split(File.pathSeparator)
            ?.map{ File(it) }.orEmpty()
    return ((TestKotlinScriptDummyDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
                   ?.mapNotNull(URL::toFile)
                   ?.filter { it.path.contains("out") && it.path.contains("test") }
           ?: emptyList()
           ) + additionalClasspath
}

internal fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        }
        catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

