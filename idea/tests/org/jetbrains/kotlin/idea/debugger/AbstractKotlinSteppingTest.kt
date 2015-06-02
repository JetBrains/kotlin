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

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.engine.BasicStepMethodFilter
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.idea.debugger.KotlinSmartStepIntoHandler.KotlinBasicStepMethodFilter
import org.jetbrains.kotlin.idea.debugger.KotlinSmartStepIntoHandler.KotlinMethodSmartStepTarget
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils.getPrefixedInt
import java.io.File

public abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestBase() {
    protected fun doStepIntoTest(path: String) {
        doTest(path, "STEP_INTO")
    }

    protected fun doStepOutTest(path: String) {
        doTest(path, "STEP_OUT")
    }

    protected fun doSmartStepIntoTest(path: String) {
        doTest(path, "SMART_STEP_INTO")
    }

    protected fun doCustomTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))
        configureSettings(fileText)
        createAdditionalBreakpoints(fileText)
        createDebugProcess(path)

        fun repeat(indexPrefix: String, f: SuspendContextImpl.() -> Unit) {
            for (i in 1..(getPrefixedInt(fileText, indexPrefix) ?: 1)) {
                doOnBreakpoint(f)
            }
        }

        File(path).readLines().forEach {
            when {
                it.startsWith("// STEP_INTO") -> repeat("// STEP_INTO: ") { stepInto() }
                it.startsWith("// STEP_OUT") -> repeat("// STEP_OUT: ") { stepOut() }
                it.startsWith("// SMART_STEP_INTO") -> repeat("// SMART_STEP_INTO: ") { smartStepInto() }
                it.startsWith("// RESUME") -> repeat("// RESUME: ") { resume(this) }
            }
        }

        finish()
    }

    private fun doTest(path: String, command: String) {
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)

        createDebugProcess(path)

        for (i in 1..(getPrefixedInt(fileText, "// $command: ") ?: 1)) {
            doOnBreakpoint {
                when(command) {
                    "STEP_INTO" -> stepInto()
                    "STEP_OUT" -> stepOut()
                    "SMART_STEP_INTO" -> smartStepInto()
                }
            }
        }

        finish()
    }

    private fun SuspendContextImpl.smartStepInto() {
        this.smartStepInto(false)
    }

    private fun SuspendContextImpl.smartStepInto(ignoreFilters: Boolean) {
        createSmartStepIntoFilters().forEach {
            dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, it))
        }
    }

    private fun createSmartStepIntoFilters(): List<BasicStepMethodFilter> {
        val breakpointManager = DebuggerManagerEx.getInstanceEx(getProject())?.getBreakpointManager()
        val breakpoint = breakpointManager?.getBreakpoints()?.first { it is LineBreakpoint }

        val line = (breakpoint as LineBreakpoint).getLineIndex()

        return runReadAction {
            val containingFile = breakpoint.getPsiFile()
            if (containingFile == null) throw AssertionError("Couldn't find file for breakpoint at the line $line")

            val position = MockSourcePosition(_file = containingFile, _line = line)

            val stepTargets = KotlinSmartStepIntoHandler().findSmartStepTargets(position)

            stepTargets.filterIsInstance<MethodSmartStepTarget>().map {
                stepTarget ->
                when (stepTarget) {
                    is KotlinMethodSmartStepTarget -> KotlinBasicStepMethodFilter(stepTarget as KotlinMethodSmartStepTarget)
                    else -> BasicStepMethodFilter(stepTarget.getMethod(), stepTarget.getCallingExpressionLines())
                }
            }
        }
    }
}
