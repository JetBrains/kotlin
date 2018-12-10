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

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.getSafe
import java.io.File

abstract class AbstractAsyncStackTraceTest : KotlinDebuggerTestBase() {
    private companion object {
        const val MARGIN = "    "
    }

    protected fun doTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        val asyncStackTraceProvider = AsyncStackTraceProvider.EP.extensionList
            .firstIsInstance<KotlinCoroutinesAsyncStackTraceProvider>()

        doOnBreakpoint {
            val frameProxy = this.frameProxy
            if (frameProxy != null) {
                val stackTrace = asyncStackTraceProvider.getAsyncStackTrace(frameProxy, this)
                if (stackTrace != null && stackTrace.isNotEmpty()) {
                    print(renderAsyncStackTrace(stackTrace), ProcessOutputTypes.SYSTEM)
                } else {
                    println("No async stack trace available", ProcessOutputTypes.SYSTEM)
                }
            } else {
                println("FrameProxy is 'null', can't calculate async stack trace", ProcessOutputTypes.SYSTEM)
            }

            resume(this)
        }
    }

    private fun renderAsyncStackTrace(trace: List<StackFrameItem>) = buildString {
        appendln("Async stack trace:")
        for (item in trace) {
            append(MARGIN).appendln(item.toString())

            @Suppress("UNCHECKED_CAST")
            val variables = item.javaClass.getDeclaredField("myVariables").getSafe(item) as? List<JavaValue>
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
