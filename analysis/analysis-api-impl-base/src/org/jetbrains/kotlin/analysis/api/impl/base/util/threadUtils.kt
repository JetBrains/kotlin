/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.analysis.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.analysis.api.tokens.hackyAllowRunningOnEdt

@HackToForceAllowRunningAnalyzeOnEDT
inline fun <R> runInPossiblyEdtThread(action: () -> R): R = when {
    !ApplicationManager.getApplication().isDispatchThread -> action()
    else -> hackyAllowRunningOnEdt(action)
}