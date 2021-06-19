/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager

@KtInternalApiMarker
class NoWriteActionInAnalyseCallChecker(parentDisposable: Disposable) {
    init {
        val listener = object : ApplicationListener {
            override fun writeActionFinished(action: Any) {
                if (currentAnalysisContextEnteringCount.get() > 0) {
                    throw WriteActionStartInsideAnalysisContextException()
                }
            }
        }
        ApplicationManager.getApplication().addApplicationListener(listener, parentDisposable)
    }

    fun beforeEnteringAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() + 1)
    }

    fun afterLeavingAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() - 1)
    }

    private val currentAnalysisContextEnteringCount = ThreadLocal.withInitial { 0 }
}

class WriteActionStartInsideAnalysisContextException : IllegalStateException(
    "write action should be never executed inside analysis context (e,g. analyse call)"
)