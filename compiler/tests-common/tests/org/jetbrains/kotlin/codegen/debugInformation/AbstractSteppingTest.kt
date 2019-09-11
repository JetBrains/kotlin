/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.debugInformation

import com.intellij.openapi.util.SystemInfo
import com.intellij.util.PathUtil
import com.intellij.util.SystemProperties
import com.sun.jdi.VirtualMachine
import com.sun.jdi.event.*
import com.sun.jdi.request.EventRequest.SUSPEND_ALL
import com.sun.jdi.request.EventRequest.SUSPEND_NONE
import com.sun.jdi.request.StepRequest
import com.sun.tools.jdi.SocketAttachingConnector
import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.cli.common.output.writeAllTo
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.clientserver.getGeneratedClass
import org.jetbrains.kotlin.test.clientserver.TestProcessServer
import org.jetbrains.kotlin.test.clientserver.TestProxy
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import java.io.File
import java.lang.IllegalStateException
import java.net.URLClassLoader
import kotlin.properties.Delegates

abstract class AbstractSteppingTest : CodegenTestCase() {

    companion object {
        const val DEBUG_ADDRESS = "127.0.0.1"
        const val MAIN_CLASS = "org.jetbrains.kotlin.test.clientserver.TestProcessServer"
        const val TEST_CLASS = "TestKt"
        const val BOX_METHOD = "box"
        const val LINENUMBER_PREFIX = "// LINENUMBERS"
        var proxyPort = 0
        lateinit var process: Process
        lateinit var virtualMachine: VirtualMachine

        @BeforeClass
        @JvmStatic
        fun setUpTest() {
            val debugPort = startDebuggeeProcess()
            virtualMachine = attachDebugger(debugPort)
            setUpVM(virtualMachine)

            val reader = process.inputStream.bufferedReader()
            reader.readLine()
            proxyPort = reader.readLine()
                .split("port ")
                .last()
                .trim()
                .toInt()
            reader.close()
        }

        private fun setUpVM(virtualMachine: VirtualMachine) {
            val manager = virtualMachine.eventRequestManager()

            val methodEntryReq = manager.createMethodEntryRequest()
            methodEntryReq.addClassFilter(TEST_CLASS)
            methodEntryReq.setSuspendPolicy(SUSPEND_ALL)
            methodEntryReq.enable()

            val methodExitReq = manager.createMethodExitRequest()
            methodExitReq.addClassFilter(TEST_CLASS)
            methodExitReq.setSuspendPolicy(SUSPEND_NONE)
            methodExitReq.enable()
        }

        private fun startDebuggeeProcess(): Int {
            val classpath = listOf(
                PathUtil.getJarPathForClass(TestProcessServer::class.java),
                PathUtil.getJarPathForClass(Delegates::class.java) // Add Kotlin runtime JAR
            )

            val javaExecutablePath = findJavaExecutable().absolutePath
            val command = arrayOf(
                if (SystemInfo.isWindows) "\"$javaExecutablePath\"" else javaExecutablePath,
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:0",
                "-ea",
                "-classpath", classpath.joinToString(File.pathSeparator),
                MAIN_CLASS,
                TestProcessServer.DEBUG_TEST
            )

            process = ProcessBuilder(*command).start()
            return process.inputStream.bufferedReader().readLine()
                .split("address:")
                .last()
                .trim()
                .toInt()
        }

        private fun attachDebugger(port: Int): VirtualMachine {
            val connector = SocketAttachingConnector()
            return connector.attach(connector.defaultArguments().toMutableMap().apply {
                getValue("port").setValue("$port")
                getValue("hostname").setValue(DEBUG_ADDRESS)
            })
        }

        private fun findJavaExecutable(): File {
            val javaBin = File(SystemProperties.getJavaHome(), "bin")
            return File(javaBin, "java.exe").takeIf { it.exists() }
                ?: File(javaBin, "java").also { assert(it.exists()) }
        }

        @AfterClass
        @JvmStatic
        fun tearDownTest() {
            process.destroy()
        }
    }

    @Before
    public override fun setUp() {
        super.setUp()
    }

    @After
    public override fun tearDown() {
        super.tearDown()
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(
            ConfigurationKind.ALL, *listOfNotNull(
                writeJavaFiles(
                    files
                )
            ).toTypedArray()
        )

        val lineNumbers = wholeFile
            .readLines()
            .dropWhile { !it.startsWith(LINENUMBER_PREFIX) }
            .drop(1)
            .map { it.drop(3).trim() }
            .joinToString(" ")

        loadMultiFiles(files)
        val classBuilderFactory =
            OriginCollectingClassBuilderFactory(ClassBuilderMode.FULL)
        val generationState =
            GenerationUtils.compileFiles(myFiles.psiFiles, myEnvironment, classBuilderFactory)
        classFileFactory = generationState.factory

        val tempDirForTest = KotlinTestUtils.tmpDir("debuggerTest")
        val classesDir = File(tempDirForTest, "classes")
        try {
            classFileFactory.writeAllTo(classesDir)

            try {
                doTest(lineNumbers, classesDir, virtualMachine, classBuilderFactory, classFileFactory, generationState)
            } finally {

            }
        } finally {
            tempDirForTest.deleteRecursively()
        }
    }

    private fun invokeBoxInSeparateProcess(classLoader: URLClassLoader, aClass: Class<*>, port: Int): String {

        val classPath = classLoader.extractUrls().toMutableList()
        if (classLoader is GeneratedClassLoader) {
            val outDir = KotlinTestUtils.tmpDirForTest(this)
            val currentOutput = SimpleOutputFileCollection(classLoader.allGeneratedFiles)
            currentOutput.writeAllTo(outDir)
            classPath.add(0, outDir.toURI().toURL())
        }

        return TestProxy(Integer.valueOf(port), aClass.canonicalName, classPath).runTestNoOutput()
    }

    private fun createGeneratedClassLoader(classesDir: File): URLClassLoader {
        return URLClassLoader(
            listOf(classesDir.toURI().toURL()).toTypedArray(),
            ForTestCompileRuntime.runtimeJarClassLoader()
        )
    }

    protected open fun doTest(
        expectedLineNumbers: String,
        classesDir: File,
        virtualMachine: VirtualMachine,
        factory: OriginCollectingClassBuilderFactory,
        classFileFactory: ClassFileFactory,
        state: GenerationState
    ) {

        val classLoader = createGeneratedClassLoader(classesDir)
        val aClass = getGeneratedClass(classLoader, TEST_CLASS)

        val manager = virtualMachine.eventRequestManager()

        invokeBoxInSeparateProcess(classLoader, aClass, proxyPort)
        val loggedEvents = ArrayList<Event>()
        var shouldRecordSteps = false
        vmLoop@
        while (true) {
            val eventSet = virtualMachine.eventQueue().remove()
            for (event in eventSet) {
                when (event) {
                    is VMDeathEvent, is VMDisconnectEvent -> {
                        break@vmLoop
                    }
                    // We start VM with option 'suspend=n', in case VMStartEvent is still received, discard.
                    is VMStartEvent -> {

                    }
                    is MethodEntryEvent -> {
                        if (!shouldRecordSteps && event.location().method().name() == BOX_METHOD) {
                            if (manager.stepRequests().isEmpty()) {
                                val stepReq = manager.createStepRequest(event.thread(), StepRequest.STEP_LINE, StepRequest.STEP_INTO)
                                stepReq.setSuspendPolicy(SUSPEND_NONE)
                                stepReq.addClassExclusionFilter("java.*")
                                stepReq.addClassExclusionFilter("sun.*")
                                stepReq.addClassExclusionFilter("kotlin.*")
                            }
                            manager.stepRequests().map { it.enable() }
                            shouldRecordSteps = true
                            loggedEvents.add(event)
                        }
                        virtualMachine.resume()
                    }
                    is StepEvent -> {
                        if (shouldRecordSteps && event.location().method().name() == "run") {
                            break@vmLoop
                        }
                        if (shouldRecordSteps) {
                            loggedEvents.add(event)
                        }
                    }
                    is MethodExitEvent -> {
                        if (event.location().method().name() == BOX_METHOD) {
                            manager.stepRequests().map { it.disable() }
                            break@vmLoop
                        }
                    }
                    else -> {
                        throw IllegalStateException("event not handled: $event")
                    }
                }
            }
        }

        val actualLineNumbers = loggedEvents
            .map { event ->
                "${(event as LocatableEvent).location().method()}:${event.location().lineNumber()}"
            }
        TestCase.assertEquals(expectedLineNumbers, actualLineNumbers.joinToString(" "))
    }

    internal val OutputFile.internalName
        get() = relativePath.substringBeforeLast(".class")

    internal val OutputFile.qualifiedName
        get() = internalName.replace('/', '.')
}

