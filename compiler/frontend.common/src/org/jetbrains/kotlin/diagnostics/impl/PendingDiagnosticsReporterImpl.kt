/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.PendingDiagnosticReporter

class PendingDiagnosticsReporterImpl(private val delegate: DiagnosticReporter) : PendingDiagnosticReporter() {
    private val pendingDiagnosticsByFilePath: MutableMap<String, MutableList<KtDiagnostic>> = mutableMapOf()

    override val hasErrors: Boolean
        get() = delegate.hasErrors

    override val hasWarningsForWError: Boolean
        get() = delegate.hasWarningsForWError

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        when (val filePath = context.containingFilePath) {
            null -> delegate.report(diagnostic, context)
            else -> {
                if (context.isDiagnosticSuppressed(diagnostic)) return
                val pendingDiagnostics = pendingDiagnosticsByFilePath.getOrPut(filePath) { mutableListOf() }
                pendingDiagnostics.add(diagnostic)
            }
        }
    }

    override fun checkAndCommitReportsOn(
        element: AbstractKtSourceElement,
        context: DiagnosticContext,
        commitEverything: Boolean,
    ) {
        if (pendingDiagnosticsByFilePath.isEmpty()) return
        val pathFromContext = context.containingFilePath
        val pendingIterator = pendingDiagnosticsByFilePath.iterator()
        while (pendingIterator.hasNext()) {
            val (path, pendingList) = pendingIterator.next()
            assert(pathFromContext == null || path == pathFromContext) {
                "Pending diagnostics for file $path are commited on file $pathFromContext"
            }

            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                val diagnosticElement = (diagnostic as? KtDiagnosticWithSource)?.element
                when {
                    context.isDiagnosticSuppressed(diagnostic) -> {
                        if (diagnosticElement != null &&
                            (diagnosticElement == element ||
                                    diagnosticElement.startOffset >= element.startOffset && diagnosticElement.endOffset <= element.endOffset)
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnosticElement == element || commitEverything -> {
                        iterator.remove()
                        delegate.report(diagnostic, context)
                    }
                }
            }
            if (pendingList.isEmpty()) {
                pendingIterator.remove()
            }
        }
    }
}
