/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.xdebugger.impl.XSourcePositionImpl
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinStepOverFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.LocationToken
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverCallerInfo
import org.jetbrains.kotlin.idea.util.application.runReadAction

sealed class KotlinStepAction {
    object StepOver : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction(suspendContext)
        }
    }

    object StepOut : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            debugProcess.createStepOutCommand(suspendContext).contextAction(suspendContext)
        }
    }

    class RunToCursor(private val position: XSourcePositionImpl) : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            return runReadAction {
                debugProcess.createRunToCursorCommand(suspendContext, position, ignoreBreakpoints)
            }.contextAction(suspendContext)
        }
    }

    class StepOverInlined(private val tokensToSkip: Set<LocationToken>, private val callerInfo: StepOverCallerInfo) : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            val filter = KotlinStepOverFilter(debugProcess.project, tokensToSkip, callerInfo)
            return KotlinStepActionFactory(debugProcess).createKotlinStepOverInlineAction(filter).contextAction(suspendContext)
        }
    }

    abstract fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean)
}