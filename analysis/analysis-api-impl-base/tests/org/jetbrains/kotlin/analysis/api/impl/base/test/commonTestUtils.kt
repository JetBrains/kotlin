/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.analyse
import org.jetbrains.kotlin.psi.KtElement

inline fun <T> runReadAction(crossinline runnable: () -> T): T {
    return ApplicationManager.getApplication().runReadAction(Computable { runnable() })
}

fun <R> executeOnPooledThreadInReadAction(action: () -> R): R =
    ApplicationManager.getApplication().executeOnPooledThread<R> { runReadAction(action) }.get()

inline fun <R> analyseOnPooledThreadInReadAction(context: KtElement, crossinline action: KtAnalysisSession.() -> R): R =
    executeOnPooledThreadInReadAction {
        analyse(context) { action() }
    }