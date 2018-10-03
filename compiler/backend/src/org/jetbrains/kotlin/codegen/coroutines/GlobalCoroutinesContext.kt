/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR

class GlobalCoroutinesContext(private val diagnostics: DiagnosticSink) {
    private var monitorsDepth = 0

    private val inlineLambdaInsideMonitorSourceArgumentIndexes = arrayListOf<Set<Int>>()

    fun pushArgumentIndexes(indexes: Set<Int>) {
        inlineLambdaInsideMonitorSourceArgumentIndexes.add(indexes)
    }

    fun popArgumentIndexes() {
        inlineLambdaInsideMonitorSourceArgumentIndexes.pop()
    }

    private fun enterMonitor() {
        monitorsDepth++
    }

    fun enterMonitorIfNeeded(index: Int?) {
        if (index == null) return
        if (inlineLambdaInsideMonitorSourceArgumentIndexes.peek()?.contains(index) != true) return
        enterMonitor()
    }

    private fun exitMonitor() {
        assert(monitorsDepth > 0) {
            "exitMonitor without corresponding enterMonitor"
        }
        monitorsDepth--
    }

    fun exitMonitorIfNeeded(index: Int?) {
        if (index == null) return
        if (inlineLambdaInsideMonitorSourceArgumentIndexes.peek()?.contains(index) != true) return
        exitMonitor()
    }

    fun checkSuspendCall(call: ResolvedCall<*>) {
        if (monitorsDepth != 0) {
            diagnostics.report(SUSPENSION_POINT_INSIDE_MONITOR.on(call.call.callElement, call.resultingDescriptor))
        }
    }
}