/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.sun.jdi.request.EventRequest
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger

class CoroutineViewDebugSessionListener(
    private val session: XDebugSession,
    private val xCoroutineView: XCoroutineView
) : XDebugSessionListener {
    val log by logger

    override fun sessionPaused() {
        log.info("XListener: sessionPaused")
        val suspendContext = session.suspendContext ?: return requestClear()
        xCoroutineView.alarm.cancel()
        renew(suspendContext)
    }

    override fun sessionResumed() {
        xCoroutineView.saveState()
        log.info("XListener: sessionResumed")
        val suspendContext = session.suspendContext ?: return requestClear()
        renew(suspendContext)
    }

    override fun sessionStopped() {
        log.info("XListener: sessionStopped")
        val suspendContext = session.suspendContext ?: return requestClear()
        renew(suspendContext)
    }

    override fun stackFrameChanged() {
        xCoroutineView.saveState()
        log.info("XListener: stackFrameChanged")
//        val suspendContext = session.suspendContext ?: return requestClear()
//        log.warn("stackFrameChanged ${session}")
//        renew(suspendContext)
    }

    override fun beforeSessionResume() {
        log.info("XListener: beforeSessionResume")
        log.warn("beforeSessionResume ${session}")
    }

    override fun settingsChanged() {
        log.info("XListener: settingsChanged")
        val suspendContext = session.suspendContext ?: return requestClear()
        log.warn("settingsChanged ${session}")
        renew(suspendContext)
    }

    fun renew(suspendContext: XSuspendContext) {
        if(suspendContext is SuspendContextImpl && suspendContext.suspendPolicy == EventRequest.SUSPEND_ALL) {
            DebuggerUIUtil.invokeLater {
                xCoroutineView.renewRoot(suspendContext)
            }
        }
    }

    private fun requestClear() {
        if (ApplicationManager.getApplication().isUnitTestMode) { // no delay in tests
            xCoroutineView.resetRoot()
        } else {
            xCoroutineView.alarm.cancelAndRequest()
        }
    }
}