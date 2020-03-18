/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

abstract class DiagnosticReporter {
    abstract fun report(diagnostic: ConeDiagnostic?)
}

class SimpleDiagnosticReporter : DiagnosticReporter() {
    val diagnostics: MutableList<ConeDiagnostic> = mutableListOf()

    override fun report(diagnostic: ConeDiagnostic?) {
        if (diagnostic == null) return
        diagnostics += diagnostic
    }
}