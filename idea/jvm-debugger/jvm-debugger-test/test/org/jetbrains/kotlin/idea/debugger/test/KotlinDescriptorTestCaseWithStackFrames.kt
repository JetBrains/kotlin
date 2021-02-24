/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.jarRepository.RemoteRepositoryDescription
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.ui.OrderRoot
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.testFramework.EdtTestUtil
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.impl.frame.XDebuggerFramesList
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.jps.model.library.JpsMavenRepositoryLibraryDescriptor
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutinePreflightFrame
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CreationCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.invokeInSuspendManagerThread
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.idea.debugger.test.util.XDebuggerTestUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.io.PrintWriter
import java.io.StringWriter

abstract class KotlinDescriptorTestCaseWithStackFrames() : KotlinDescriptorTestCaseWithStepping() {
    private companion object {
        val ASYNC_STACKTRACE_EP_NAME = AsyncStackTraceProvider.EP.name
        val INDENT_FRAME = 1
        val INDENT_VARIABLES = 2
    }

    val agentList = mutableListOf<JpsMavenRepositoryLibraryDescriptor>()
    val classPath = mutableListOf<String>()

    protected fun out(frame: XStackFrame) {
        out(INDENT_FRAME, frame.javaClass.simpleName + " FRAME:" + XDebuggerTestUtil.getFramePresentation(frame))
        outVariables(frame)
    }

    private fun outVariables(stackFrame: XStackFrame) {
        val variables = XDebuggerTestUtil.collectChildrenWithError(stackFrame)
        val sorted = mutableListOf<String>()
        sorted.addAll(variables.first.mapNotNull { if (it is XNamedValue) it.name else null })
        sorted.sort()
        val varString = sorted.joinToString()
        out(INDENT_VARIABLES, "($varString)")
    }

    protected fun out(text: String) {
        println(text, ProcessOutputTypes.SYSTEM)
    }

    protected fun out(indent: Int, text: String) {
        println("\t".repeat(indent) + text, ProcessOutputTypes.SYSTEM)
        println(text)
    }

    protected fun Throwable.stackTraceAsString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    fun printStackFrame(files: TestFiles, preferences: DebuggerPreferences) {
        val asyncStackTraceProvider = getAsyncStackTraceProvider()

        doWhenXSessionPausedThenResume {
            printContext(debugProcess.debuggerContext)
            val suspendContext = debuggerSession.xDebugSession?.getSuspendContext()
            var executionStack = suspendContext?.getActiveExecutionStack()
            if (executionStack != null) {
                try {
                    out("Thread stack trace:")
                    val stackFrames: List<XStackFrame> = XDebuggerTestUtil.collectFrames(executionStack)
                    val suspendContextImpl = suspendContext as SuspendContextImpl
                    for (frame in stackFrames) {
                        if (frame is JavaStackFrame) {
                            out(frame)
                            if (frame is CoroutinePreflightFrame) {
                                val key = frame.coroutineInfoData.key
                                out(0, "CoroutineInfo: ${key.id} ${key.name} ${key.state}")
                            }
                            val stackFrames = suspendContext.invokeInSuspendManagerThread(debugProcess) {
                                asyncStackTraceProvider?.getAsyncStackTrace(frame, suspendContextImpl)
                            }
                            if (stackFrames != null) {
                                for (frameItem in stackFrames) {
                                    val frame: XStackFrame? =
                                        frameItem.createFrame(debugProcess)
                                    if (frame is XDebuggerFramesList.ItemWithSeparatorAbove && frame.hasSeparatorAbove())
                                        out(0, frame.captionAboveOf)

                                    frame?.let {
                                        out(frame)
                                    }
                                }
                                return@doWhenXSessionPausedThenResume
                            }
                        }
                    }
                } catch (e: Throwable) {
                    val stackTrace = e.stackTraceAsString()
                    System.err.println("Exception occurred on calculating async stack traces: $stackTrace")
                    throw e
                }
            } else {
                println("FrameProxy is 'null', can't calculate async stack trace", ProcessOutputTypes.SYSTEM)
            }
        }
    }

    protected fun getAsyncStackTraceProvider(): CoroutineAsyncStackTraceProvider? {
        val area = Extensions.getArea(null)
        if (!area.hasExtensionPoint(ASYNC_STACKTRACE_EP_NAME)) {
            System.err.println("${ASYNC_STACKTRACE_EP_NAME} extension point is not found (probably old IDE version)")
            return null
        }

        val extensionPoint = area.getExtensionPoint<Any>(ASYNC_STACKTRACE_EP_NAME)
        val provider = extensionPoint.extensions.firstIsInstanceOrNull<CoroutineAsyncStackTraceProvider>()

        if (provider == null) {
            System.err.println("Kotlin coroutine async stack trace provider is not found")
        }
        return provider
    }

    override fun addMavenDependency(compilerFacility: DebuggerTestCompilerFacility, library: String) {
        val regex = Regex(pattern = """maven\(([a-zA-Z0-9_\-\.]+)\:([a-zA-Z0-9_\-\.]+):([a-zA-Z0-9_\-\.]+)\)(\-javaagent)?""")
        val result = regex.matchEntire(library) ?: return
        val (_, groupId: String, artifactId: String, version: String, agent: String) = result.groupValues
        if ("-javaagent" == agent)
            agentList.add(JpsMavenRepositoryLibraryDescriptor(groupId, artifactId, version, false))
        val description = JpsMavenRepositoryLibraryDescriptor(groupId, artifactId, version)
        val artifacts = loadDependencies(description)
        compilerFacility.addDependencies(artifacts.map { it.file.presentableUrl })
        addLibraries(artifacts)
    }

    override fun createJavaParameters(mainClass: String?): JavaParameters {
        val params = super.createJavaParameters(mainClass)
        for (entry in classPath) {
            params.classPath.add(entry)
        }
        for (agent in agentList) {
            val dependencies = loadDependencies(agent)
            for (dependency in dependencies) {
                params.vmParametersList.add("-javaagent:${dependency.file.presentableUrl}")
            }
        }
        return params
    }

    private fun addLibraries(artifacts: MutableList<OrderRoot>) =
        EdtTestUtil.runInEdtAndWait {
            runWriteAction {
                val model = ModuleRootManager.getInstance(myModule).modifiableModel
                val customLibEditor = NewLibraryEditor().apply {
                    for (artifact in artifacts) {
                        classPath.add(artifact.file.presentableUrl) // for sandbox jvm
                        addRoot(artifact.file, artifact.type)
                    }
                }
                ConfigLibraryUtil.addLibrary(customLibEditor, model, null) // for kotlin compiler
                model.commit()
            }
        }

    private fun loadDependencies(
        description: JpsMavenRepositoryLibraryDescriptor
    ): MutableList<OrderRoot> {
        return JarRepositoryManager.loadDependenciesSync(
            project, description, setOf(ArtifactKind.ARTIFACT),
            RemoteRepositoryDescription.DEFAULT_REPOSITORIES, null
        ) ?: throw AssertionError("Maven Dependency not found: $description")
    }
}