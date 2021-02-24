/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic
import org.jetbrains.kotlin.utils.SmartList

class DiagnosedRange constructor(val start: Int) {
    var end: Int = 0
    private val diagnostics = SmartList<TextDiagnostic>()

    fun getDiagnostics(): List<TextDiagnostic> {
        return diagnostics
    }

    fun addDiagnostic(diagnostic: String) {
        diagnostics.add(TextDiagnostic.parseDiagnostic(diagnostic))
    }
}
