/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import org.jetbrains.annotations.NonNls

class CoroutineDebuggerContentInfo {
    companion object {
        val COROUTINE_THREADS_CONTENT = "CoroutineThreadsContent"
        val XCOROUTINE_THREADS_CONTENT = "XCoroutineThreadsContent"
        val XCOROUTINE_POPUP_ACTION_GROUP = "Kotlin.XDebugger.Actions"
    }
}

class CoroutineDebuggerActions {
    companion object {
        @NonNls
        val COROUTINE_PANEL_POPUP: String = "Debugger.CoroutinesPanelPopup"
    }
}