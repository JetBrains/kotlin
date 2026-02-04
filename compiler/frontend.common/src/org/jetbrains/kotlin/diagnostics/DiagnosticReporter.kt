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

    abstract val hasErrors: Boolean
}

/**
 * No diagnostic reported with [DiagnosticReporter.report] wouldn't be commited to resulting diagnostic
 * storage until [checkAndCommitReportsOn] function for the corresponding source element would be called.
 *
 * This is required for proper work of diagnostic suppressions in the frontend environment:
 * in the frontend the checkers stage visits files and computes the suppression information for the subtree
 * upon visiting the annotated element. But in some cases the diagnostic on some element could be reported
 * before this element was visited (e.g. class checkers could report diagnostics on members).
 *
 * ```
 * class Some {
 *     @Suppress("ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS")
 *     abstract val x: Int // ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS is reported during visiting class `Some`, not property `x`
 * }
 * ```
 *
 * So to work around this situation the pending reporter just records that some diagnostics were reported during [report]
 * call. And when the visitor reaches the underlying elements, the [checkAndCommitReportsOn] is called with [context]
 * which is aware of the suppressions on the particular element.
 *
 * This whole situation is not the case for backend diagnostics reporting, as
 * [org.jetbrains.kotlin.ir.KtDiagnosticReporterWithImplicitIrBasedContext] creates the suppression mapping once for the whole
 * file, so it doesn't matter when the diagnostic is reported, the suppressions would be already computed.
 */
abstract class PendingDiagnosticReporter : DiagnosticReporter() {
    open fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {}
}
