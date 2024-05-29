/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.Severity.*

fun Severity.toAnalysisApiSeverity(): KaSeverity {
    return when (this) {
        ERROR -> KaSeverity.ERROR
        WARNING -> KaSeverity.WARNING
        INFO -> KaSeverity.INFO
    }
}