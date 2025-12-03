/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithSource
import org.jetbrains.kotlin.diagnostics.Severity

class PendingDiagnosticsCollectorWithSuppress : BaseDiagnosticsCollector() {
    private val pendingDiagnosticsByFilePath: MutableMap<String, MutableList<KtDiagnostic>> = mutableMapOf()
    private val _diagnosticsByFilePath: MutableMap<String?, MutableList<KtDiagnostic>> = mutableMapOf()
    override val diagnostics: List<KtDiagnostic>
        get() = _diagnosticsByFilePath.flatMap { it.value }
    override val diagnosticsByFilePath: Map<String?, List<KtDiagnostic>>
        get() = _diagnosticsByFilePath

    override var hasErrors = false
        private set

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && !context.isDiagnosticSuppressed(diagnostic)) {
            when (val filePath = context.containingFilePath) {
                null -> {
                    val diagnostics = _diagnosticsByFilePath.getOrPut(key = null) { mutableListOf() }
                    diagnostics.add(diagnostic)
                    updateHasErrors(diagnostic)
                }
                else -> {
                    val pendingDiagnostics = pendingDiagnosticsByFilePath.getOrPut(filePath) { mutableListOf() }
                    pendingDiagnostics.add(diagnostic)
                }
            }
        }
    }

    override fun checkAndCommitReportsOn(
        element: AbstractKtSourceElement,
        context: DiagnosticContext?
    ) {
        if (pendingDiagnosticsByFilePath.isEmpty()) return
        val commitEverything = context == null
        val pendingIterator = pendingDiagnosticsByFilePath.iterator()
        while (pendingIterator.hasNext()) {
            val (path, pendingList) = pendingIterator.next()
            val committedList = _diagnosticsByFilePath.getOrPut(path) { mutableListOf() }
            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                val diagnosticElement = (diagnostic as? KtDiagnosticWithSource)?.element
                when {
                    context?.isDiagnosticSuppressed(diagnostic) == true -> {
                        if (diagnosticElement != null &&
                            (diagnosticElement == element ||
                                    diagnosticElement.startOffset >= element.startOffset && diagnosticElement.endOffset <= element.endOffset)
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnosticElement == element || commitEverything -> {
                        iterator.remove()
                        committedList += diagnostic
                        updateHasErrors(diagnostic)
                    }
                }
            }
            if (pendingList.isEmpty()) {
                pendingIterator.remove()
            }
        }
    }

    private fun updateHasErrors(diagnostic: KtDiagnostic) {
        hasErrors = hasErrors || diagnostic.severity == Severity.ERROR
    }
}
