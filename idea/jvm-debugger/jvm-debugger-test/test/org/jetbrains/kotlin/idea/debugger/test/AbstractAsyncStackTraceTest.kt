/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.getSafe
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Modifier

abstract class AbstractAsyncStackTraceTest : KotlinDescriptorTestCaseWithStepping() {
    private companion object {
        const val MARGIN = "    "
        val ASYNC_STACKTRACE_EP_NAME = AsyncStackTraceProvider.EP.name
    }

    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        val asyncStackTraceProvider = getAsyncStackTraceProvider()
        if (asyncStackTraceProvider == null) {
            finish()
            return
        }

        doOnBreakpoint {
            val frameProxy = this.frameProxy
            if (frameProxy != null) {
                try {
                    val stackTrace = asyncStackTraceProvider.getAsyncStackTraceSafe(frameProxy, this)
                    if (stackTrace != null && stackTrace.isNotEmpty()) {
                        print(renderAsyncStackTrace(stackTrace), ProcessOutputTypes.SYSTEM)
                    } else {
                        println("No async stack trace available", ProcessOutputTypes.SYSTEM)
                    }
                } catch (e: Throwable) {
                    val stackTrace = e.stackTraceAsString()
                    System.err.println("Exception occurred on calculating async stack traces: $stackTrace")
                    throw e
                }
            } else {
                println("FrameProxy is 'null', can't calculate async stack trace", ProcessOutputTypes.SYSTEM)
            }

            resume(this)
        }
    }

    private fun getAsyncStackTraceProvider(): CoroutineAsyncStackTraceProvider? {
        val area = Extensions.getArea(null)
        if (!area.hasExtensionPoint(ASYNC_STACKTRACE_EP_NAME)) {
            System.err.println("$ASYNC_STACKTRACE_EP_NAME extension point is not found (probably old IDE version)")
            return null
        }

        val extensionPoint = area.getExtensionPoint<Any>(ASYNC_STACKTRACE_EP_NAME)
        val provider = extensionPoint.extensions.firstIsInstanceOrNull<CoroutineAsyncStackTraceProvider>()

        if (provider == null) {
            System.err.println("Kotlin coroutine async stack trace provider is not found")
        }

        return provider
    }

    private fun Throwable.stackTraceAsString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun renderAsyncStackTrace(trace: List<StackFrameItem>) = buildString {
        appendln("Async stack trace:")
        for (item in trace) {
            append(MARGIN).appendln(item.toString())

            @Suppress("UNCHECKED_CAST")
            val variablesField = item.javaClass.declaredFields
                .first { !Modifier.isStatic(it.modifiers) && it.type == List::class.java }

            @Suppress("UNCHECKED_CAST")
            val variables = variablesField.getSafe(item) as? List<JavaValue>

            if (variables != null) {
                for (variable in variables) {
                    val descriptor = variable.descriptor
                    val name = descriptor.calcValueName()
                    val value = descriptor.calcValue(evaluationContext)

                    append(MARGIN).append(MARGIN).append(name).append(" = ").appendln(value)
                }
            }
        }
    }
}