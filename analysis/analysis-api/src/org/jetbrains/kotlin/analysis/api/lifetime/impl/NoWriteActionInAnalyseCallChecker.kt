/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.lifetime.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.KtAnalysisApiInternals

@KtAnalysisApiInternals
public class NoWriteActionInAnalyseCallChecker(parentDisposable: Disposable) {
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

    public fun beforeEnteringAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() + 1)
    }

    public fun afterLeavingAnalysisContext() {
        currentAnalysisContextEnteringCount.set(currentAnalysisContextEnteringCount.get() - 1)
    }

    private val currentAnalysisContextEnteringCount = ThreadLocal.withInitial { 0 }
}

public class WriteActionStartInsideAnalysisContextException : IllegalStateException(
    "write action should be never executed inside analysis context (e,g. analyse call)"
)