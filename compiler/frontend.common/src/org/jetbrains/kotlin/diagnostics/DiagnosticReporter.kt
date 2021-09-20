/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.config.LanguageVersionSettings

interface DiagnosticContext {
    fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean

    fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): DiagnosticContext

    val languageVersionSettings: LanguageVersionSettings
}

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext)
}
