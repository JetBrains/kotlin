/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.progress.experimental

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicatorProvider

class CompilationCanceledException : ProcessCanceledException()

interface CompilationCanceledStatus {
    suspend fun checkCanceled(): Unit
}

object ProgressIndicatorAndCompilationCanceledStatus {
    private var canceledStatus: CompilationCanceledStatus? = null

    @JvmStatic
    @Synchronized
    fun setCompilationCanceledStatus(newCanceledStatus: CompilationCanceledStatus?): Unit {
        canceledStatus = newCanceledStatus
    }

    @JvmStatic
    suspend fun checkCanceled(): Unit {
        ProgressIndicatorProvider.checkCanceled()
        canceledStatus?.checkCanceled()
    }
}