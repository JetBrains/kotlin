/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryN
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithoutSource

class DeduplicatingDiagnosticReporter(private val inner: DiagnosticReporter) : DiagnosticReporter() {

    override val hasErrors: Boolean get() = inner.hasErrors

    private val reported = mutableSetOf<Triple<String?, AbstractKtSourceElement, KtDiagnosticFactoryN>>()

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        when (diagnostic) {
            null -> {}
            is KtDiagnosticWithoutSource -> inner.report(diagnostic, context)
            is KtDiagnosticWithSource -> {
                if (reported.add(Triple(context.containingFilePath, diagnostic.element, diagnostic.factory))) {
                    inner.report(diagnostic, context)
                }
            }
        }
    }

    override fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {
        inner.checkAndCommitReportsOn(element, context)
    }
}

fun DiagnosticReporter.deduplicating(): DeduplicatingDiagnosticReporter = DeduplicatingDiagnosticReporter(this)
