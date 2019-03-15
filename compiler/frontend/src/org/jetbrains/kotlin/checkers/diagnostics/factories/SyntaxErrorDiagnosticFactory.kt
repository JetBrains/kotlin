/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics.factories

import org.jetbrains.kotlin.checkers.diagnostics.SyntaxErrorDiagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity

class SyntaxErrorDiagnosticFactory private constructor() : DiagnosticFactory<SyntaxErrorDiagnostic>(Severity.ERROR) {
    override fun getName(): String {
        return "SYNTAX"
    }

    companion object {
        val INSTANCE = SyntaxErrorDiagnosticFactory()
    }
}
