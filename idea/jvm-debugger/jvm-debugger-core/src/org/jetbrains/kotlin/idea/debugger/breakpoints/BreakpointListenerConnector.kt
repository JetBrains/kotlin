/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener

object BreakpointListenerConnector {
    @JvmStatic
    fun subscribe(debugProcess: DebugProcessImpl, indicator: ProgressWindow, listener: XBreakpointListener<XBreakpoint<*>>) {
        debugProcess.project.messageBus.connect(indicator).subscribe(XBreakpointListener.TOPIC, listener)
    }
}