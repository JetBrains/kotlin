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

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.MethodFilter
import org.jetbrains.kotlin.idea.util.application.runReadAction
import com.intellij.debugger.impl.PositionUtil
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.roots.libraries.LibraryUtil
import org.jetbrains.kotlin.idea.JetJdkAndLibraryProjectDescriptor
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.util.io.FileUtil
import com.intellij.debugger.SourcePosition
import kotlin.properties.Delegates
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl

abstract class KotlinDebuggerTestBase : KotlinDebuggerTestCase() {

    protected val dp: DebugProcessImpl
        get() = getDebugProcess() ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    protected fun onBreakpoint(doOnBreakpoint: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            initContexts(it)
            it.printContext()
            it.doOnBreakpoint()
        }
    }

    protected fun SuspendContextImpl.stepInto() {
        this.stepInto(false, null)
    }

    protected var evaluationContext: EvaluationContextImpl by Delegates.notNull()
    protected var debuggerContext: DebuggerContextImpl by Delegates.notNull()

    protected fun initContexts(suspendContext: SuspendContextImpl) {
        evaluationContext = createEvaluationContext(suspendContext)
        debuggerContext = createDebuggerContext(suspendContext)
    }

    protected fun SuspendContextImpl.stepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        dp.getManagerThread()!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter))
    }

    protected fun SuspendContextImpl.printContext() {
        runReadAction {(): Unit ->
            if (this.getFrameProxy() == null) {
                return@runReadAction println("Context thread is null", ProcessOutputTypes.SYSTEM)
            }

            val sourcePosition = PositionUtil.getSourcePosition(this)
            println(renderSourcePosition(sourcePosition), ProcessOutputTypes.SYSTEM)
        }
    }

    protected fun renderSourcePosition(sourcePosition: SourcePosition?): String {
        if (sourcePosition == null) {
            return "null"
        }

        val virtualFile = sourcePosition.getFile().getVirtualFile()
        if (virtualFile == null) {
            return "VirtualFile for position is null"
        }

        val libraryEntry = LibraryUtil.findLibraryEntry(virtualFile, getProject())
        if (libraryEntry != null && (libraryEntry is JdkOrderEntry ||
                                     libraryEntry.getPresentableName() == JetJdkAndLibraryProjectDescriptor.LIBRARY_NAME)) {
            return FileUtil.getNameWithoutExtension(virtualFile.getName()) + ".!EXT!"
        }

        return virtualFile.getName() + ":" + (sourcePosition.getLine() + 1)
    }

    protected fun finish() {
        onBreakpoint {
            resume(this)
        }
    }
}
