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
 * [analyze][org.jetbrains.kotlin.analysis.api.analyze] call.
 */
internal class KaBaseWriteActionStartedChecker(parentDisposable: Disposable) {
    private val currentAnalyzeCallDepth = ThreadLocal.withInitial { 0 }

    init {
        val listener = object : ApplicationListener {
            override fun writeActionFinished(action: Any) {
                if (currentAnalyzeCallDepth.get() > 0) {
                    throw WriteActionStartedInAnalysisContextException()
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
    }
}

private class WriteActionStartedInAnalysisContextException : IllegalStateException(
    "A write action should never be executed inside an analysis context (i.e. an `analyze` call)."
)
