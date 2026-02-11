/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic

/**
 * [BaseDiagnosticsCollector] is a [DiagnosticReporter] which stores all reported diagnostics inside itself.
 */
abstract class BaseDiagnosticsCollector : DiagnosticReporter() {
    abstract val diagnostics: List<KtDiagnostic>
    abstract val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>

    object DoNothing : BaseDiagnosticsCollector() {
        override val diagnostics: List<KtDiagnostic>
            get() = emptyList()
        override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
            get() = emptyMap()

        override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {}

        override val hasErrors: Boolean
            get() = false
        override val hasWarningsForWError: Boolean
            get() = false
    }
}
