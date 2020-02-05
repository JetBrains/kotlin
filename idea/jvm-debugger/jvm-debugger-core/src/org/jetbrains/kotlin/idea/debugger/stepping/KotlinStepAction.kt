/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.MethodFilter
import com.intellij.debugger.engine.SuspendContextImpl
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinStepOverFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.LocationToken
import org.jetbrains.kotlin.idea.debugger.stepping.filter.StepOverCallerInfo

sealed class KotlinStepAction {
    object JvmStepOver : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            debugProcess.createStepOverCommand(suspendContext, ignoreBreakpoints).contextAction(suspendContext)
        }
    }

    class StepInto(private val filter: MethodFilter?) : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            debugProcess.createStepIntoCommand(suspendContext, ignoreBreakpoints, filter).contextAction(suspendContext)
        }
    }

    object StepOut : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            debugProcess.createStepOutCommand(suspendContext).contextAction(suspendContext)
        }
    }

    class KotlinStepOver(private val tokensToSkip: Set<LocationToken>, private val callerInfo: StepOverCallerInfo) : KotlinStepAction() {
        override fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean) {
            val filter = KotlinStepOverFilter(debugProcess.project, tokensToSkip, callerInfo)
            return KotlinStepActionFactory(debugProcess).createKotlinStepOverAction(filter).contextAction(suspendContext)
        }
    }

    abstract fun apply(debugProcess: DebugProcessImpl, suspendContext: SuspendContextImpl, ignoreBreakpoints: Boolean)
}