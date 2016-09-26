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

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.DebuggerSession
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import com.sun.jdi.request.EventRequest
import com.sun.jdi.request.StepRequest

// Mass-copy-paste code for commands behaviour from com.intellij.debugger.engine.DebugProcessImpl
@SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter")
class KotlinStepActionFactory(private val debuggerProcess: DebugProcessImpl) {
    abstract class KotlinStepAction {
        abstract fun contextAction(suspendContext: SuspendContextImpl)
    }

    fun createKotlinStepOverInlineAction(smartStepFilter: KotlinMethodFilter): KotlinStepAction {
        return StepOverInlineCommand(smartStepFilter, StepRequest.STEP_LINE)
    }

    private val debuggerContext: DebuggerContextImpl get() = debuggerProcess.debuggerContext
    private val suspendManager: SuspendManager get() = debuggerProcess.suspendManager
    private val project: Project get() = debuggerProcess.project
    private val session: DebuggerSession get() = debuggerProcess.session

    // TODO: ask for better API
    private val debugProcessDispatcher: EventDispatcher<DebugProcessListener>
        // Should be safe to use reflection as field is protected and not obfuscated
        get() = getFromField("myDebugProcessDispatcher")

    // TODO: ask for better API
    private val threadBlockedMonitor: ThreadBlockedMonitor
        // FIXME: obfuscated in ULTIMATE. Absent in old AS
        get() = getFromField("myThreadBlockedMonitor")

    private fun showStatusText(message: String) {
        debuggerProcess.showStatusText(message)
    }

    // TODO: ask for better API
    private fun doStep(
            suspendContext: SuspendContextImpl,
            stepThread: ThreadReferenceProxyImpl,
            size: Int, depth: Int, hint: RequestHint) {
        // Should be safe to use reflection as field is protected and not obfuscated
        val doStepMethod = DebugProcessImpl::class.java.getDeclaredMethod(
                "doStep",
                SuspendContextImpl::class.java, ThreadReferenceProxyImpl::class.java,
                Integer.TYPE, Integer.TYPE, RequestHint::class.java)

        doStepMethod.isAccessible = true

        doStepMethod.invoke(debuggerProcess, suspendContext, stepThread, size, depth, hint)
    }

    private fun <T> getFromField(fieldName: String): T {
        val field = DebugProcessImpl::class.java.getDeclaredField(fieldName)
        field.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        return field.get(debuggerProcess) as T
    }

    private inner class StepOverInlineCommand(private val mySmartStepFilter: KotlinMethodFilter, private val myStepSize: Int) : KotlinStepAction() {
        private fun getContextThread(suspendContext: SuspendContextImpl): ThreadReferenceProxyImpl? {
            val contextThread = debuggerContext.threadProxy
            return contextThread ?: suspendContext.thread
        }

        // See: ResumeCommand.applyThreadFilter()
        private fun applyThreadFilter(suspendContext: SuspendContextImpl, thread: ThreadReferenceProxyImpl) {
            if (suspendContext.suspendPolicy == EventRequest.SUSPEND_ALL) {
                // there could be explicit resume as a result of call to voteSuspend()
                // e.g. when breakpoint was considered invalid, in that case the filter will be applied _after_
                // resuming and all breakpoints in other threads will be ignored.
                // As resume() implicitly cleares the filter, the filter must be always applied _before_ any resume() action happens
                val breakpointManager = DebuggerManagerEx.getInstanceEx(project).breakpointManager
                breakpointManager.applyThreadFilter(debuggerProcess, thread.threadReference)
            }
        }

        // See: StepCommand.resumeAction()
        private fun resumeAction(suspendContext: SuspendContextImpl, thread: ThreadReferenceProxyImpl) {
            if (suspendContext.suspendPolicy == EventRequest.SUSPEND_EVENT_THREAD || isResumeOnlyCurrentThread) {
                threadBlockedMonitor.startWatching(thread)
            }
            if (isResumeOnlyCurrentThread && suspendContext.suspendPolicy == EventRequest.SUSPEND_ALL) {
                suspendManager.resumeThread(suspendContext, thread)
            }
            else {
                suspendManager.resume(suspendContext)
            }
        }

        // See: StepIntoCommand.contextAction()
        override fun contextAction(suspendContext: SuspendContextImpl) {
            showStatusText("Stepping over inline")
            val stepThread = getContextThread(suspendContext)

            if (stepThread == null) {
                // TODO: Intellij code doesn't bother to check thread for null, so probably it's not-null actually
                debuggerProcess.createStepOverCommand(suspendContext, true).contextAction(suspendContext)
                return
            }

            val hint = KotlinStepOverInlinedLinesHint(stepThread, suspendContext, mySmartStepFilter)
            hint.isResetIgnoreFilters = !session.shouldIgnoreSteppingFilters()

            try {
                session.setIgnoreStepFiltersFlag(stepThread.frameCount())
            }
            catch (e: EvaluateException) {
                LOG.info(e)
            }

            hint.isIgnoreFilters = true
            applyThreadFilter(suspendContext, stepThread)

            doStep(suspendContext, stepThread, myStepSize, StepRequest.STEP_OVER, hint)

            showStatusText("Process resumed")
            resumeAction(suspendContext, stepThread)
            debugProcessDispatcher.multicaster.resumed(suspendContext)
        }

    }

    companion object {
        private val LOG = Logger.getInstance(KotlinStepActionFactory::class.java)

        private val isResumeOnlyCurrentThread: Boolean
            get() = DebuggerSettings.getInstance().RESUME_ONLY_CURRENT_THREAD
    }
}
