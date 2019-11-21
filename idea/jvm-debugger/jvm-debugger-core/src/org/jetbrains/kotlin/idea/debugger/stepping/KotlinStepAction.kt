/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.xdebugger.impl.XSourcePositionImpl
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinStepOverInlineFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverFilterData
import org.jetbrains.kotlin.idea.util.application.runReadAction

sealed class KotlinStepAction(
    val position: XSourcePositionImpl? = null,
    val stepOverInlineData: StepOverFilterData? = null
) {
    class StepOver : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) =
            debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction(suspendContext)
    }

    class StepOut : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) =
            debugProcess.createStepOutCommand(suspendContext).contextAction(suspendContext)
    }

    class RunToCursor(position: XSourcePositionImpl) : KotlinStepAction(position) {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            return runReadAction {
                debugProcess.createRunToCursorCommand(suspendContext, position!!, ignoreBreakpoints)
            }.contextAction(suspendContext)
        }
    }

    class StepOverInlined(stepOverInlineData: StepOverFilterData) : KotlinStepAction(stepOverInlineData = stepOverInlineData) {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            return KotlinStepActionFactory(debugProcess).createKotlinStepOverInlineAction(
                KotlinStepOverInlineFilter(debugProcess.project, stepOverInlineData!!)
            ).contextAction(suspendContext)
        }
    }

    abstract fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean)
}