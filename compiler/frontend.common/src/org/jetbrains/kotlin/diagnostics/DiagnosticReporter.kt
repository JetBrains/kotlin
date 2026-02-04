/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl

interface DiagnosticBaseContext {
    val languageVersionSettings: LanguageVersionSettings

    object Default : DiagnosticBaseContext {
        override val languageVersionSettings: LanguageVersionSettings
            get() = LanguageVersionSettingsImpl.DEFAULT
    }
}

interface DiagnosticContext : DiagnosticBaseContext {
    val containingFilePath: String?

    fun isDiagnosticSuppressed(diagnostic: KtDiagnostic): Boolean
}

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext)

    open fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {
    }

    abstract val hasErrors: Boolean
}
