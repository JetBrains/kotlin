/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.DebugProcessImpl
import org.jetbrains.jet.plugin.debugger.KotlinSmartStepIntoHandler.KotlinMethodSmartStepTarget
import org.jetbrains.jet.plugin.debugger.KotlinSmartStepIntoHandler.KotlinBasicStepMethodFilter
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.ui.breakpoints.LineBreakpoint
import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.engine.BasicStepMethodFilter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable

abstract class AbstractKotlinSteppingTest : KotlinDebuggerTestCase() {

    protected fun doStepIntoTest(path: String) {
        createDebugProcess(path)
        onBreakpoint { stepInto() }
        finish()
    }

    protected fun doSmartStepIntoTest(path: String) {
        createDebugProcess(path)
        onBreakpoint { smartStepInto() }
        finish()
    }

    private val dp: DebugProcessImpl
        get() = getDebugProcess() ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    private fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            it.printContext()
            it.doOnBreakpoint()
        }
    }

    private fun SuspendContextImpl.smartStepInto() {
        this.smartStepInto(false)
    }

    private fun SuspendContextImpl.stepInto() {
        this.stepInto(false, null)
    }

    private fun SuspendContextImpl.stepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter))
    }

    private fun SuspendContextImpl.smartStepInto(ignoreFilters: Boolean) {
        createSmartStepIntoFilters().forEach {
            dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, it))
        }
    }

    private fun SuspendContextImpl.printContext() {
        printContext(this)
    }

    private fun finish() {
        onBreakpoint {
            resume(this)
        }
    }

    private fun createSmartStepIntoFilters(): List<BasicStepMethodFilter> {
        val breakpointManager = DebuggerManagerEx.getInstanceEx(getProject())?.getBreakpointManager()
        val breakpoint = breakpointManager?.getBreakpoints()?.first { it is LineBreakpoint }

        val line = (breakpoint as LineBreakpoint).getLineIndex()

        return ApplicationManager.getApplication()?.runReadAction(Computable {
            val containingFile = breakpoint.getPsiFile()
            if (containingFile == null) throw AssertionError("Couldn't find file for breakpoint at the line $line")

            val position = MockSourcePosition(_file = containingFile, _line = line)

            val stepTargets = KotlinSmartStepIntoHandler().findSmartStepTargets(position)

            stepTargets.filterIsInstance(javaClass<MethodSmartStepTarget>()).map {
                stepTarget ->
                when (stepTarget) {
                    is KotlinMethodSmartStepTarget -> KotlinBasicStepMethodFilter(stepTarget as KotlinMethodSmartStepTarget)
                    else -> BasicStepMethodFilter(stepTarget.getMethod(), stepTarget.getCallingExpressionLines())
                }
            }
        })!!
    }
}
