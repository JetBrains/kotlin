/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.generator

import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticData
import org.jetbrains.kotlin.fir.checkers.generator.diagnostics.DiagnosticParameter
import kotlin.reflect.KType

data class HLDiagnostic(
    val original: DiagnosticData,
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