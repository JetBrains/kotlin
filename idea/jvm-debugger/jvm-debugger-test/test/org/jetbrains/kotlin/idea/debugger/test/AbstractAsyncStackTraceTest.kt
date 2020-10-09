/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.execution.process.ProcessOutputTypes
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CoroutineFrameBuilder
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isPreFlight
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.utils.getSafe
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.Field
import java.lang.reflect.Modifier

abstract class AbstractAsyncStackTraceTest : KotlinDescriptorTestCaseWithStepping() {
    private companion object {
        const val MARGIN = "    "
        val ASYNC_STACKTRACE_EP_NAME = AsyncStackTraceProvider.EP.name
    }

    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        doOnBreakpoint {
            val frameProxy = this.frameProxy
            if (frameProxy != null) {
                try {
                  /*  val sem = frameProxy.location().isPreFlight()
                    val coroutineInfoData = if (sem.isCoroutineFound())
                        CoroutineFrameBuilder.lookupContinuation(this, frameProxy, sem)?.coroutineInfoData
                    else
                        null
                    if (coroutineInfoData != null && coroutineInfoData.stackTrace.isNotEmpty()) {
                        print(renderAsyncStackTrace(coroutineInfoData.stackTrace), ProcessOutputTypes.SYSTEM)
                    } else {
                        println("No async stack trace available", ProcessOutputTypes.SYSTEM)
                    }*/
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

    private fun Throwable.stackTraceAsString(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun renderAsyncStackTrace(trace: List<StackFrameItem>) = buildString {
        appendLine("Async stack trace:")
        for (item in trace) {
            append(MARGIN).appendLine(item.toString())
            val declaredFields = listDeclaredFields(item.javaClass)

            @Suppress("UNCHECKED_CAST")
            val variablesField = declaredFields
                .first { !Modifier.isStatic(it.modifiers) && it.type == List::class.java }

            @Suppress("UNCHECKED_CAST")
            val variables = variablesField.getSafe(item) as? List<JavaValue>

            if (variables != null) {
                for (variable in variables) {
                    val descriptor = variable.descriptor
                    val name = descriptor.calcValueName()
                    val value = descriptor.calcValue(evaluationContext)

                    append(MARGIN).append(MARGIN).append(name).append(" = ").appendLine(value)
                }
            }
        }
    }

    private fun listDeclaredFields(cls: Class<in Any>): MutableList<Field> {
        var clazz = cls
        val declaredFields = mutableListOf<Field>()
        while (clazz != Class.forName("java.lang.Object")) {
            declaredFields.addAll(clazz.declaredFields)
            clazz = clazz.superclass
        }
        return declaredFields
    }
}