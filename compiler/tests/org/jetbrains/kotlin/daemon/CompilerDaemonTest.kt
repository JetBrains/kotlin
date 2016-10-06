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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import junit.framework.TestCase
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.client.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.integration.KotlinIntegrationTestBase
import org.jetbrains.kotlin.progress.CompilationCanceledStatus
import org.jetbrains.kotlin.script.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.rmi.server.UnicastRemoteObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.fail


val TIMEOUT_DAEMON_RUNNER_EXIT_MS = 10000L

class CompilerDaemonTest : KotlinIntegrationTestBase() {

    data class CompilerResults(val resultCode: Int, val out: String)

    val compilerClassPath = listOf(
            File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"))
    val daemonClientClassPath = listOf( File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-daemon-client.jar"),
                                        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler.jar"))
    val compilerId by lazy(LazyThreadSafetyMode.NONE) { CompilerId.makeCompilerId(compilerClassPath) }

    private fun compileOnDaemon(clientAliveFile: File, compilerId: CompilerId, daemonJVMOptions: DaemonJVMOptions, daemonOptions: DaemonOptions, vararg args: String): CompilerResults {
        val daemon = KotlinCompilerClient.connectToCompileService(compilerId, clientAliveFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
        assertNotNull("failed to connect daemon", daemon)
        daemon?.registerClient(clientAliveFile.absolutePath)
        val strm = ByteArrayOutputStream()
        val code = KotlinCompilerClient.compile(daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, args, strm)
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


    fun testHelloApp() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath,
                                              verbose = true,
                                              reportPerf = true)

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test.", ".log")

            val daemonJVMOptions = configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                                             inheritMemoryLimits = false, inheritAdditionalProperties = false)
            var daemonShotDown = false

            try {
                val jar = tmpdir.absolutePath + File.separator + "hello.jar"
                runDaemonCompilerTwice(flagFile, compilerId, daemonJVMOptions, daemonOptions,
                                       "-include-runtime", File(getTestBaseDir(), "hello.kt").absolutePath, "-d", jar)

                KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
                daemonShotDown = true
                var compileTime1 = 0L
                var compileTime2 = 0L
                logFile.reader().useLines {
                    it.ifNotContainsSequence(LinePattern("Kotlin compiler daemon version"),
                                             LinePattern("Starting compilation with args: "),
                                             LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime1 = it }; true }),
                                             LinePattern("Starting compilation with args: "),
                                             LinePattern("Compile on daemon: (\\d+) ms", { it.groups[1]?.value?.toLong()?.let { compileTime2 = it }; true }),
                                             LinePattern("Shutdown complete"))
                    { unmatchedPattern, lineNo ->
                        fail("pattern not found in the input: " + unmatchedPattern.regex +
                                      "\nunmatched part of the log file (" + logFile.absolutePath +
                                      ") from line " + lineNo + ":\n\n" + logFile.reader().useLines { it.drop(lineNo).joinToString("\n") })
                    }
                }
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

    fun testDaemonJvmOptionsParsing() {
        val backupJvmOptions = System.getProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, "-aaa,-bbb\\,ccc,-ddd,-Xmx200m,-XX:MaxPermSize=10k,-XX:ReservedCodeCacheSize=100,-xxx\\,yyy")
            val opts = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            assertEquals("200m", opts.maxMemory)
            assertEquals("10k", opts.maxPermSize)
            assertEquals("100", opts.reservedCodeCacheSize)
            assertEquals(arrayListOf("aaa", "bbb,ccc", "ddd", "xxx,yyy"), opts.jvmParams)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_JVM_OPTIONS_PROPERTY, backupJvmOptions)
        }
    }

    fun testDaemonOptionsParsing() {
        val backupOptions = System.getProperty(COMPILE_DAEMON_OPTIONS_PROPERTY)
        try {
            System.setProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, "runFilesPath=abcd,autoshutdownIdleSeconds=1111")
            val opts = configureDaemonOptions()
            assertEquals("abcd", opts.runFilesPath)
            assertEquals(1111, opts.autoshutdownIdleSeconds)
        }
        finally {
            restoreSystemProperty(COMPILE_DAEMON_OPTIONS_PROPERTY, backupOptions)
        }
    }

    fun testDaemonInstancesSimple() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            val compilerId2 = CompilerId.makeCompilerId(compilerClassPath +
                                                        File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-compiler-sources.jar"))

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            KotlinCompilerClient.shutdownCompileService(compilerId2, daemonOptions)

            val logFile1 = createTempFile("kotlin-daemon1-test", ".log")
            val logFile2 = createTempFile("kotlin-daemon2-test", ".log")
            val daemonJVMOptions1 =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile1.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            val daemonJVMOptions2 =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile2.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            assertTrue(logFile1.length() == 0L && logFile2.length() == 0L)

            val jar1 = tmpdir.absolutePath + File.separator + "hello1.jar"
            val res1 = compileOnDaemon(flagFile, compilerId, daemonJVMOptions1, daemonOptions, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar1)
            assertEquals("first compilation failed:\n${res1.out}", 0, res1.resultCode)

            logFile1.assertLogContainsSequence("Starting compilation with args: ")
            assertEquals("expecting '${logFile2.absolutePath}' to be empty", 0L, logFile2.length())

            val jar2 = tmpdir.absolutePath + File.separator + "hello2.jar"
            val res2 = compileOnDaemon(flagFile, compilerId2, daemonJVMOptions2, daemonOptions, "-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar2)
            assertEquals("second compilation failed:\n${res2.out}", 0, res1.resultCode)

            logFile2.assertLogContainsSequence("Starting compilation with args: ")

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            logFile1.assertLogContainsSequence("Shutdown complete")
            logFile1.delete()

            KotlinCompilerClient.shutdownCompileService(compilerId2, daemonOptions)
            logFile2.assertLogContainsSequence("Shutdown complete")
            logFile2.delete()
        }
    }

    fun testDaemonAutoshutdownOnUnused() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownUnusedSeconds = 1, runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test", ".log")
            val daemonJVMOptions =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

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
                                              "Shutdown complete")
            logFile.delete()
        }
    }

    fun testDaemonAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test", ".log")
            val daemonJVMOptions =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)
            daemon?.registerClient(flagFile.absolutePath)
            val jar = tmpdir.absolutePath + File.separator + "hello1.jar"
            val strm = ByteArrayOutputStream()
            val code = KotlinCompilerClient.compile(daemon!!, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar), strm)
            assertEquals("compilation failed:\n$strm", 0, code)

            logFile.assertLogContainsSequence("Starting compilation with args: ")

            // wait up to 4s (more than 1s idle timeout)
            for (attempts in 1..20) {
                if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                Thread.sleep(200)
            }
            Thread.sleep(200)
            logFile.assertLogContainsSequence("Idle timeout exceeded 1s",
                                              "Shutdown complete")
            logFile.delete()
        }
    }

    fun testDaemonGracefulShutdown() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test", ".log")
            val daemonJVMOptions =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)
            daemon?.registerClient(flagFile.absolutePath)
            val sessionId = daemon?.leaseCompileSession(null)

            val scheduleShutdownRes = daemon?.scheduleShutdown(true)

            assertTrue("failed to schedule shutdown ($scheduleShutdownRes)", scheduleShutdownRes?.let { it.isGood && it.get() } ?: false )

            Thread.sleep(100) // to allow timer task to run in the daemon

            logFile.assertLogContainsSequence("Some sessions are active, waiting for them to finish")

            val res = daemon?.getUsedMemory()

            assertEquals("Invalid state", CompileService.CallResult.Dying(), res)

            daemon?.releaseCompileSession(sessionId!!.get())

            Thread.sleep(100) // allow after session timed action to run

            logFile.assertLogContainsSequence("All sessions finished, shutting down",
                                              "Shutdown complete")
            logFile.delete()
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
        val clientAliveFile = createTempFile("kotlin-daemon-transitive-run-test", ".run")
        val runFilesPath = File(tmpdir, getTestName(true)).absolutePath
        val daemonOptions = DaemonOptions(runFilesPath = runFilesPath)
        val jar = tmpdir.absolutePath + File.separator + "hello.jar"
        val args = listOf(
                File(File(System.getProperty("java.home"), "bin"), "java").absolutePath,
                "-D$COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY",
                "-cp",
                daemonClientClassPath.joinToString(File.pathSeparator) { it.absolutePath },
                KotlinCompilerClient::class.qualifiedName!!) +
                   daemonOptions.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   compilerId.mappers.flatMap { it.toArgs(COMPILE_DAEMON_CMDLINE_OPTIONS_PREFIX) } +
                   File(getHelloAppBaseDir(), "hello.kt").absolutePath +
                   "-d" + jar
        try {
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
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

    private class SynchronizationTracer(val startSignal: CountDownLatch, val doneSignal: CountDownLatch, port: Int) : RemoteOperationsTracer,
            java.rmi.server.UnicastRemoteObject(port, LoopbackNetworkInterface.clientLoopbackSocketFactory, LoopbackNetworkInterface.serverLoopbackSocketFactory)
    {
        override fun before(id: String) {
            startSignal.await()
        }
        override fun after(id: String) {
            doneSignal.countDown()
        }
    }

    private val PARALLEL_THREADS_TO_COMPILE = 10
    private val PARALLEL_WAIT_TIMEOUT_S = 60L

    fun testParallelCompilationOnDaemon() {

        assertTrue(PARALLEL_THREADS_TO_COMPILE <= LoopbackNetworkInterface.SERVER_SOCKET_BACKLOG_SIZE)

        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = true, inheritAdditionalProperties = false)
            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)

            val (registry, port) = findPortAndCreateRegistry(10, 16384, 65535)
            val tracer = SynchronizationTracer(CountDownLatch(1), CountDownLatch(PARALLEL_THREADS_TO_COMPILE), port)

            val resultCodes = arrayOfNulls<Int>(PARALLEL_THREADS_TO_COMPILE)
            val localEndSignal = CountDownLatch(PARALLEL_THREADS_TO_COMPILE)
            val outStreams = Array(PARALLEL_THREADS_TO_COMPILE, { ByteArrayOutputStream() })

            fun runCompile(threadNo: Int) =
                    thread {
                        val jar = tmpdir.absolutePath + File.separator + "hello.$threadNo.jar"
                        val res = KotlinCompilerClient.compile(
                                daemon!!,
                                CompileService.NO_SESSION,
                                CompileService.TargetPlatform.JVM,
                                arrayOf("-include-runtime", File(getHelloAppBaseDir(), "hello.kt").absolutePath, "-d", jar),
                                outStreams[threadNo],
                                port = port,
                                operationsTracer = tracer as RemoteOperationsTracer)
                        synchronized(resultCodes) {
                            resultCodes[threadNo] = res
                        }
                        localEndSignal.countDown()
                    }

            (1..PARALLEL_THREADS_TO_COMPILE).forEach { runCompile(it - 1) }

            tracer.startSignal.countDown()
            val succeeded = tracer.doneSignal.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)
            assertTrue("parallel compilation failed to complete in $PARALLEL_WAIT_TIMEOUT_S ms, ${tracer.doneSignal.count} unfinished threads", succeeded)

            localEndSignal.await(PARALLEL_WAIT_TIMEOUT_S, TimeUnit.SECONDS)
            (1..PARALLEL_THREADS_TO_COMPILE).forEach {
                assertEquals("Compilation on thread $it failed:\n${outStreams[it - 1]}", 0, resultCodes[it - 1])
            }
        }
    }

    fun testDaemonConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)
            daemon?.registerClient(flagFile.absolutePath)

            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)
            Thread.sleep(200)

            val exception: Exception? = try {
                daemon!!.getUsedMemory()
                null
            }
            catch (e: java.rmi.ConnectException) {
                e
            }
            catch (e: java.rmi.UnmarshalException) {
                e
            }
            assertNotNull(exception)
        }
    }

    fun testDaemonCallbackConnectionProblems() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test", ".log")
            val daemonJVMOptions =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)
            daemon?.registerClient(flagFile.absolutePath)

            val file = File(tmpdir, "largeKotlinFile.kt")
            file.writeText(generateLargeKotlinFile(10))
            val jar = File(tmpdir, "largeKotlinFile.jar").absolutePath

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

            logFile.delete()
        }
    }

    fun testDaemonReplLocalEvalNoParams() {
        withDaemon { daemon ->
            withDisposable { disposable ->
                val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                                                    classpathFromClassloader(),
                                                    ScriptWithNoParam::class.qualifiedName!!,
                                                    System.err)

                val localEvaluator = GenericReplCompiledEvaluator(emptyList(), Thread.currentThread().contextClassLoader)

                doReplTestWithLocalEval(repl, localEvaluator)
            }
        }
    }

    fun testDaemonReplLocalEvalStandardTemplate() {
        withDaemon { daemon ->
            withDisposable { disposable ->
                val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                                                    classpathFromClassloader(),
                                                    "kotlin.script.StandardScriptTemplate",
                                                    System.err)

                val localEvaluator = GenericReplCompiledEvaluator(emptyList(),
                                                                  Thread.currentThread().contextClassLoader,
                                                                  arrayOf(emptyArray<String>()))

                doReplTestWithLocalEval(repl, localEvaluator)
            }
        }
    }

    private fun doReplTestWithLocalEval(repl: KotlinRemoteReplCompiler, localEvaluator: GenericReplCompiledEvaluator) {
        val res0 = repl.check(ReplCodeLine(0, "val x ="), emptyList())
        TestCase.assertTrue("Unexpected check results: $res0", res0 is ReplCheckResult.Incomplete)

        val codeLine1 = ReplCodeLine(1, "val lst = listOf(1)\nval x = 5")
        val res1 = repl.compile(codeLine1, emptyList())
        val res1c = res1 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res1", res1c)

        val res11 = localEvaluator.eval(codeLine1, emptyList(), res1c!!.classes, res1c.hasResult, res1c.newClasspath)
        val res11e = res11 as? ReplEvalResult.UnitResult
        TestCase.assertNotNull("Unexpected eval result: $res11", res11e)

        val codeLine2 = ReplCodeLine(2, "x + 2")
        val res2x = repl.compile(codeLine2, listOf(codeLine2))
        TestCase.assertNotNull("Unexpected compile result: $res2x", res2x as? ReplCompileResult.HistoryMismatch)

        val res2 = repl.compile(codeLine2, listOf(codeLine1))
        val res2c = res2 as? ReplCompileResult.CompiledClasses
        TestCase.assertNotNull("Unexpected compile result: $res2", res2c)

        val res21 = localEvaluator.eval(codeLine2, listOf(codeLine1), res2c!!.classes, res2c.hasResult, res2c.newClasspath)
        val res21e = res21 as? ReplEvalResult.ValueResult
        TestCase.assertNotNull("Unexpected eval result: $res21", res21e)
        TestCase.assertEquals(7, res21e!!.value)
    }

    fun testDaemonReplRemoteEval() {
        withDaemon { daemon ->
            withDisposable { disposable ->

                val evalOut = ByteArrayOutputStream()
                val evalErr = ByteArrayOutputStream()
                val evalIn = ByteArrayInputStream("42\n".toByteArray())

                val repl = KotlinRemoteReplEvaluator(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                                                     listOf(File(KotlinIntegrationTestBase.getCompilerLib(), "kotlin-runtime.jar")),
                                                     "kotlin.script.StandardScriptTemplate",
                                                     arrayOf(emptyArray<String>()), null,
                                                     System.err, evalOut, evalErr, evalIn)

                val res0 = repl.check(ReplCodeLine(0, "val x ="), emptyList())
                TestCase.assertTrue("Unexpected check results: $res0", res0 is ReplCheckResult.Incomplete)

                val codeLine1 = ReplCodeLine(1, "val x = 5")
                val res1 = repl.eval(codeLine1, emptyList())
                val res1e = res1 as? ReplEvalResult.UnitResult
                TestCase.assertNotNull("Unexpected eval result: $res1", res1e)

                val codeLine2 = ReplCodeLine(2, "x + 2")
                val res2x = repl.eval(codeLine2, listOf(codeLine2))
                TestCase.assertNotNull("Unexpected compile result: $res2x", res2x as? ReplEvalResult.HistoryMismatch)

                val res2 = repl.eval(codeLine2, listOf(codeLine1))
                val res2e = res2 as? ReplEvalResult.ValueResult
                TestCase.assertNotNull("Unexpected eval result: $res2", res2e)
                TestCase.assertEquals(7, res2e!!.value)
            }
        }
    }

    fun testDaemonReplAutoshutdownOnIdle() {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(autoshutdownIdleSeconds = 1, autoshutdownUnusedSeconds = 1, runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            KotlinCompilerClient.shutdownCompileService(compilerId, daemonOptions)

            val logFile = createTempFile("kotlin-daemon-test", ".log")
            val daemonJVMOptions =
                    configureDaemonJVMOptions("D$COMPILE_DAEMON_LOG_PATH_PROPERTY=\"${logFile.loggerCompatiblePath}\"",
                                              inheritMemoryLimits = false, inheritAdditionalProperties = false)

            val daemon = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)

            val disposable = Disposer.newDisposable()
            val repl = KotlinRemoteReplCompiler(disposable, daemon!!, null, CompileService.TargetPlatform.JVM,
                                                classpathFromClassloader(),
                                                ScriptWithNoParam::class.qualifiedName!!,
                                                System.err)

            // use repl compiler for >> 1s, making sure that idle/unused timeouts are not firing
            for (attempts in 1..10) {
                val codeLine1 = ReplCodeLine(1, "3 + 5")
                val res1 = repl.compile(codeLine1, emptyList())
                val res1c = res1 as? ReplCompileResult.CompiledClasses
                TestCase.assertNotNull("Unexpected compile result: $res1", res1c)
                Thread.sleep(200)
            }

            // wait up to 4s (more than 1s idle timeout)
            for (attempts in 1..20) {
                if (logFile.isLogContainsSequence("Idle timeout exceeded 1s")) break
                Thread.sleep(200)
            }
            Disposer.dispose(disposable)

            Thread.sleep(200)
            logFile.assertLogContainsSequence("Idle timeout exceeded 1s",
                                              "Shutdown complete")
            logFile.delete()
        }
    }

    internal fun withDaemon(body: (CompileService) -> Unit) {
        withFlagFile(getTestName(true), ".alive") { flagFile ->
            val daemonOptions = DaemonOptions(runFilesPath = File(tmpdir, getTestName(true)).absolutePath)
            val daemonJVMOptions = configureDaemonJVMOptions(inheritMemoryLimits = false, inheritAdditionalProperties = false)
            val daemon: CompileService? = KotlinCompilerClient.connectToCompileService(compilerId, flagFile, daemonJVMOptions, daemonOptions, DaemonReportingTargets(out = System.err), autostart = true)
            assertNotNull("failed to connect daemon", daemon)

            body(daemon!!)
        }
    }
}


// stolen from CompilerFileLimitTest
internal fun generateLargeKotlinFile(size: Int): String {
    return buildString {
        append("package large\n\n")
        (0..size).forEach {
            appendln("class Class$it")
            appendln("{")
            appendln("\tfun foo(): Long = $it")
            appendln("}")
            appendln("\n")
            repeat(2000) {
                appendln("// kotlin rules ... and stuff")
            }
        }
        appendln("fun main(args: Array<String>)")
        appendln("{")
        appendln("\tval result = Class5().foo() + Class$size().foo()")
        appendln("\tprintln(result)")
        appendln("}")
    }

}


internal fun File.ifLogNotContainsSequence(vararg patterns: String, body: (LinePattern, Int) -> Unit) {
    reader().useLines {
        it.ifNotContainsSequence( patterns.map { LinePattern(it) }, body)
    }
}

internal fun File.assertLogContainsSequence(vararg patterns: String) {
    ifLogNotContainsSequence(*patterns)
    {
        pattern,lineNo -> fail("Pattern '${pattern.regex}' is not found in the log file '$absolutePath'")
    }
}

internal fun File.isLogContainsSequence(vararg patterns: String): Boolean {
    var res = true
    ifLogNotContainsSequence(*patterns) { l,c -> res = false }
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
    val file = createTempFile(prefix, suffix)
    try {
        body(file)
    }
    finally {
        file.delete()
    }
}

internal inline fun withDisposable(body: (Disposable) -> Unit) {
    val disposable = Disposer.newDisposable()
    try {
        body(disposable)
    }
    finally {
        Disposer.dispose(disposable)
    }
}

// java.util.Logger used in the daemon silently forgets to log into a file specified in the config on Windows,
// if file path is given in windows form (using backslash as a separator); the reason is unknown
// this function makes a path with forward slashed, that works on windows too
private val File.loggerCompatiblePath: String
    get() =
        if (OSKind.current == OSKind.Windows) absolutePath.replace('\\', '/')
        else absolutePath

open class TestKotlinScriptDummyDependenciesResolver : ScriptDependenciesResolver {

    override fun resolve(script: ScriptContents,
                         environment: Map<String, Any?>?,
                         report: (ScriptDependenciesResolver.ReportSeverity, String, ScriptContents.Position?) -> Unit,
                         previousDependencies: KotlinScriptExternalDependencies?
    ): Future<KotlinScriptExternalDependencies?>
    {
        return object : KotlinScriptExternalDependencies {
            override val classpath: Iterable<File> = classpathFromClassloader()
            override val imports: Iterable<String> = listOf("org.jetbrains.kotlin.scripts.DependsOn", "org.jetbrains.kotlin.scripts.DependsOnTwo")
        }.asFuture()
    }
}

@ScriptTemplateDefinition(resolver = TestKotlinScriptDummyDependenciesResolver::class)
abstract class ScriptWithNoParam()

internal fun classpathFromClassloader(): List<File> =
        (TestKotlinScriptDummyDependenciesResolver::class.java.classLoader as? URLClassLoader)?.urLs
                ?.mapNotNull(URL::toFile)
                ?.filter { it.path.contains("out") && it.path.contains("test") }
        ?: emptyList()

internal fun URL.toFile() =
        try {
            File(toURI().schemeSpecificPart)
        }
        catch (e: java.net.URISyntaxException) {
            if (protocol != "file") null
            else File(file)
        }

