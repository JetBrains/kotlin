/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.checkers.diagnostics.TextDiagnostic

interface DiagnosticDiffCallbacks {
    fun missingDiagnostic(diagnostic: TextDiagnostic, expectedStart: Int, expectedEnd: Int)

    fun wrongParametersDiagnostic(expectedDiagnostic: TextDiagnostic, actualDiagnostic: TextDiagnostic, start: Int, end: Int)

    fun unexpectedDiagnostic(diagnostic: TextDiagnostic, actualStart: Int, actualEnd: Int)
}