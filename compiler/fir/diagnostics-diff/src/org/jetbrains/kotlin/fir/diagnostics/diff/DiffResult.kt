/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.diagnostics.diff

import java.io.File

class OverallResult(
    val matched: Int,
    val missing: Int,
    val unexpected: Int,
    val mismatched: Int,
    val numFiles: Int,
    val aggregateDiagnosticResults: Map<String, AggregateDiagnosticResult>,
)

class SingleDiagnosticResult(
    val expectedFile: File,
    val actualFile: File,
    val expectedRange: DiagnosedRange?,
    val actualRange: DiagnosedRange?,
)

class AggregateDiagnosticResult(
    val matched: List<SingleDiagnosticResult>,
    val missing: List<SingleDiagnosticResult>,
    val unexpected: List<SingleDiagnosticResult>,
    val mismatched: List<SingleDiagnosticResult>,
    val numFiles: Int,
)

class FileResult(
    val matched: Int,
    val missing: Int,
    val unexpected: Int,
    val mismatched: Int,
)

class DiagnosedRange(
    val diagnostic: String,
    val start: Int,
    var end: Int = -1,
    var text: String = "",
)
