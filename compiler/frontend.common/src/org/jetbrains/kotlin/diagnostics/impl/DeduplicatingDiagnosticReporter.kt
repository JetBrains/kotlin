/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.impl

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnostic

class DeduplicatingDiagnosticReporter(private val inner: DiagnosticReporter) : DiagnosticReporter() {

    private val reported = mutableSetOf<Triple<String?, AbstractKtSourceElement, AbstractKtDiagnosticFactory>>()

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic != null && reported.add(Triple(context.containingFilePath, diagnostic.element, diagnostic.factory))) {
            inner.report(diagnostic, context)
        }
    }
}

fun DiagnosticReporter.deduplicating(): DeduplicatingDiagnosticReporter = DeduplicatingDiagnosticReporter(this)