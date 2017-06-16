/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.ActionMethodFilter
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.RequestHint
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.psi.PsiFile
import com.intellij.util.Range
import com.intellij.xdebugger.impl.XSourcePositionImpl
import com.sun.jdi.Location
import com.sun.jdi.request.EventRequest
import org.jetbrains.kotlin.idea.debugger.isOnSuspendReturnOrReenter
import org.jetbrains.kotlin.idea.debugger.suspendFunctionFirstLineLocation
import org.jetbrains.kotlin.idea.util.application.runReadAction

class KotlinSuspendCallStepOverFilter(
        private val line: Int,
        private val file: PsiFile,
        private val ignoreBreakpoints: Boolean) : MethodFilter, ActionMethodFilter {
    override fun getCallingExpressionLines(): Range<Int>? = Range(line, line)

    override fun locationMatches(process: DebugProcessImpl, location: Location?): Boolean {
        return location != null && isOnSuspendReturnOrReenter(location)
    }

    override fun onReached(context: SuspendContextImpl, hint: RequestHint): Int {
        val location = context.frameProxy?.location() ?: return RequestHint.STOP
        val suspendStartLineNumber = suspendFunctionFirstLineLocation(location) ?: return RequestHint.STOP

        val debugProcess = context.debugProcess
        val breakpointManager = DebuggerManagerEx.getInstanceEx(debugProcess.project).breakpointManager
        breakpointManager.applyThreadFilter(debugProcess, null)

        createRunToCursorBreakpoint(context, suspendStartLineNumber - 1, file, ignoreBreakpoints)
        return RequestHint.RESUME
    }
}

private fun createRunToCursorBreakpoint(context: SuspendContextImpl, line: Int, file: PsiFile, ignoreBreakpoints: Boolean) {
    val position = XSourcePositionImpl.create(file.virtualFile, line) ?: return
    val process = context.debugProcess
    process.showStatusText(DebuggerBundle.message("status.run.to.cursor"))
    process.cancelRunToCursorBreakpoint()

    if (ignoreBreakpoints) {
        DebuggerManagerEx.getInstanceEx(process.project).breakpointManager.disableBreakpoints(process)
    }

    val runToCursorBreakpoint =
            runReadAction {
                DebuggerManagerEx.getInstanceEx(process.project).breakpointManager.addRunToCursorBreakpoint(position, ignoreBreakpoints)
            } ?:
            return

    runToCursorBreakpoint.suspendPolicy = when {
        context.suspendPolicy == EventRequest.SUSPEND_EVENT_THREAD -> DebuggerSettings.SUSPEND_THREAD
        else -> DebuggerSettings.SUSPEND_ALL
    }

    runToCursorBreakpoint.createRequest(process)
    process.setRunToCursorBreakpoint(runToCursorBreakpoint)
}