/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.*

class DeduplicatingDiagnosticReporter(private val delegate: DiagnosticReporter) : DiagnosticReporter() {
    override val hasErrors: Boolean get() = delegate.hasErrors
    override val hasWarningsForWError: Boolean get() = delegate.hasWarningsForWError

    private val reported = mutableSetOf<Triple<String?, AbstractKtSourceElement, KtDiagnosticFactoryN>>()

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        when (diagnostic) {
            null -> {}
            is KtDiagnosticWithoutSource -> delegate.report(diagnostic, context)
            is KtDiagnosticWithSource -> {
                if (reported.add(Triple(context.containingFilePath, diagnostic.element, diagnostic.factory))) {
                    delegate.report(diagnostic, context)
                }
            }
        }
    }
}

fun DiagnosticReporter.deduplicating(): DeduplicatingDiagnosticReporter {
    if (this is DeduplicatingDiagnosticReporter) return this
    return DeduplicatingDiagnosticReporter(this)
}
