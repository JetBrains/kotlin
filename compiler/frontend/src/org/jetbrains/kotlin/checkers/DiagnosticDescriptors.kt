/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.checkers.diagnostics.AbstractTestDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.PositionalTextDiagnostic
import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic

abstract class AbstractDiagnosticDescriptor internal constructor(val start: Int, val end: Int) {
    val textRange: TextRange
        get() = TextRange(start, end)
}

class ActualDiagnosticDescriptor internal constructor(start: Int, end: Int, val diagnostics: List<AbstractTestDiagnostic>) :
    AbstractDiagnosticDescriptor(start, end) {

    val textDiagnosticsMap: MutableMap<AbstractTestDiagnostic, TextDiagnostic>
        get() {
            val diagnosticMap = mutableMapOf<AbstractTestDiagnostic, TextDiagnostic>()
            for (diagnostic in diagnostics) {
                diagnosticMap[diagnostic] = TextDiagnostic.asTextDiagnostic(diagnostic)
            }

            return diagnosticMap
        }
}

class TextDiagnosticDescriptor internal constructor(private val positionalTextDiagnostic: PositionalTextDiagnostic) :
    AbstractDiagnosticDescriptor(positionalTextDiagnostic.start, positionalTextDiagnostic.end) {

    val textDiagnostic: TextDiagnostic
        get() = positionalTextDiagnostic.diagnostic
}