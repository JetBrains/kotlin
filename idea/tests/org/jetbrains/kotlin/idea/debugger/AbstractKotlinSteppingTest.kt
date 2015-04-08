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

import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.kotlin.idea.debugger.KotlinSmartStepIntoHandler.KotlinMethodSmartStepTarget
import org.jetbrains.kotlin.idea.debugger.KotlinSmartStepIntoHandler.KotlinBasicStepMethodFilter
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.engine.BasicStepMethodFilter
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils.*
import com.intellij.openapi.util.io.FileUtil
import java.io.File
import kotlin.properties.Delegates
import com.intellij.debugger.settings.DebuggerSettings

public abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestBase() {
    protected fun doStepIntoTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))

        configureSettings(fileText)

        createDebugProcess(path)
        val count = findStringWithPrefixes(fileText, "// STEP_INTO: ")?.toInt() ?: 1

        for (i in 1..count) {
            onBreakpoint { stepInto() }
        }

        finish()
    }

    protected fun doSmartStepIntoTest(path: String) {
        createDebugProcess(path)
        onBreakpoint { smartStepInto() }
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
