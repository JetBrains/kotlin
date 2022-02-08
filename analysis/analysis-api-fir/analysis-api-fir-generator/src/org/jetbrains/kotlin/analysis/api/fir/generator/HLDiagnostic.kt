/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.generator

import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model.DiagnosticParameter
import kotlin.reflect.KType

data class HLDiagnostic(
    val original: DiagnosticData,
    val severity: Severity?,
    val className: String,
    val implClassName: String,
    val parameters: List<HLDiagnosticParameter>,
)

data class HLDiagnosticList(val diagnostics: List<HLDiagnostic>)

data class HLDiagnosticParameter(
    val original: DiagnosticParameter,
    val name: String,
    val type: KType,
    val originalParameterName: String,
    val conversion: HLParameterConversion,
    val importsToAdd: List<String>
)
