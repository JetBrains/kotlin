/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic

abstract class BaseDiagnosticsCollector : DiagnosticReporter() {
    abstract val diagnostics: List<KtDiagnostic>
    abstract val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
    abstract val hasErrors: Boolean

    abstract val rawReport: (Boolean, String) -> Unit
}
