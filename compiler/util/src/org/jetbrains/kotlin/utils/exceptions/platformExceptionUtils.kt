/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils.exceptions

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.IndexNotReadyException

/**
 * Some exceptions that originate from Intellij Platform should never be logged or handled and must always be rethrown.
 *
 * Examples of such exceptions include [ProcessCanceledException] and [IndexNotReadyException].
 */
fun shouldIjPlatformExceptionBeRethrown(exception: Throwable): Boolean = when (exception) {
    is ControlFlowException -> true
    is IndexNotReadyException -> true
    else -> false
}

/**
 * Some exceptions that originate from Intellij Platform should never be logged or handled and must always be rethrown.
 *
 * Examples of such exceptions include [ProcessCanceledException] and [IndexNotReadyException].
 */
fun rethrowIntellijPlatformExceptionIfNeeded(exception: Throwable) {
    if (shouldIjPlatformExceptionBeRethrown(exception)) {
        throw exception
    }
}
