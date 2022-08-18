/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.project.IndexNotReadyException

public fun shouldIjPlatformExceptionBeRethrown(exception: Throwable): Boolean = when (exception) {
    is ControlFlowException -> true
    is IndexNotReadyException -> true
    else -> false
}