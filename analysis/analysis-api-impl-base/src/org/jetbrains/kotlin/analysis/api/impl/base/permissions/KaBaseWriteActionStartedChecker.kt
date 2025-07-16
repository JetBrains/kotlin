/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.permissions

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager

/**
 * Ensures that a [WriteActionStartedInAnalysisContextException] is thrown if a write action was started *inside* an
 * [analyze][org.jetbrains.kotlin.analysis.api.analyze] call (unless the first `analyze` was already called inside a write action).
 */
internal class KaBaseWriteActionStartedChecker(parentDisposable: Disposable) {
    private val hasEncounteredIllegalWriteAction = ThreadLocal.withInitial { false }

    private val currentAnalyzeCallDepth = ThreadLocal.withInitial { 0 }

    init {
        /**
         * Since IJPL-160901, exceptions from the event dispatcher are not propagated upwards (see
         * `EventDispatcher.isEventDispatcherErrorPropagationEnabled`). Therefore, we only use the listener to track whether an illegal
         * write action was started, and throw the exception from `afterLeavingAnalysis` later.
         */
        val listener = object : ApplicationListener {
            override fun beforeWriteActionStart(action: Any) {
                // When the first `analyze` call is already inside a write action, it's legal to start another write action inside
                // `analyze`. Note that `beforeWriteActionStart` is executed *before* the write action is entered, so `isWriteAccessAllowed`
                // will only be true when there is an outer write action.
                if (ApplicationManager.getApplication().isWriteAccessAllowed) return

                if (currentAnalyzeCallDepth.get() > 0) {
                    hasEncounteredIllegalWriteAction.set(true)
                }
            }
        }
        ApplicationManager.getApplication().addApplicationListener(listener, parentDisposable)
    }

    fun beforeEnteringAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() + 1)
    }

    fun afterLeavingAnalysis() {
        currentAnalyzeCallDepth.set(currentAnalyzeCallDepth.get() - 1)

        if (hasEncounteredIllegalWriteAction.get()) {
            hasEncounteredIllegalWriteAction.remove()
            throw WriteActionStartedInAnalysisContextException()
        }
    }
}

private class WriteActionStartedInAnalysisContextException : IllegalStateException(
    "A write action should never be executed inside an analysis context (i.e. an `analyze` call)."
)
