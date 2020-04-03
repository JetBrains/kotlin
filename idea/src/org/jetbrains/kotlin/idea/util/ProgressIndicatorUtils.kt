/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.util.ExceptionUtil
import org.jetbrains.annotations.Nls
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Copied from [com.intellij.openapi.progress.util.ProgressIndicatorUtils]
 */
object ProgressIndicatorUtils {
    @JvmStatic
    fun <T> underModalProgress(
        project: Project,
        @Nls progressTitle: String,
        computable: () -> T
    ): T = com.intellij.openapi.actionSystem.ex.ActionUtil.underModalProgress(project, progressTitle, computable)

    fun <T> runUnderDisposeAwareIndicator(
        parent: Disposable,
        computable: () -> T
    ): T = BackgroundTaskUtil.runUnderDisposeAwareIndicator(parent, Computable { computable() })

    @JvmStatic
    fun <T> awaitWithCheckCanceled(future: Future<T>): T {
        val indicator = ProgressManager.getInstance().progressIndicator
        while (true) {
            checkCancelledEvenWithPCEDisabled(indicator)
            try {
                return future[10, TimeUnit.MILLISECONDS]
            } catch (ignore: TimeoutException) {
            } catch (e: Throwable) {
                val cause = e.cause
                if (cause is CancellationException) {
                    throw ProcessCanceledException(cause)
                } else {
                    ExceptionUtil.rethrowUnchecked(e)
                    throw RuntimeException(e)
                }
            }
        }
    }

    private fun checkCancelledEvenWithPCEDisabled(indicator: ProgressIndicator?) =
        indicator?.let {
            if (it.isCanceled) {
                it.checkCanceled() // maybe it'll throw with some useful additional information
                throw ProcessCanceledException()
            }
        }
}