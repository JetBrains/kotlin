/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics

import org.jetbrains.kotlin.diagnostics.Severity
import kotlin.reflect.KType

data class DiagnosticData(
    val severity: Severity,
    val name: String,
    val sourceElementType: KType,
    val psiType: KType,
    val parameters: List<DiagnosticParameter>,
    val positioningStrategy: PositioningStrategy,
    val group: String?,
)

data class DiagnosticParameter(
    val name: String,
    val type: KType
)

enum class PositioningStrategy(val expressionToCreate: String, val import: String) {
    DEFAULT(
        "SourceElementPositioningStrategies.DEFAULT",
        positioningStrategiesImport
    ),

    VAL_OR_VAR_NODE(
        "SourceElementPositioningStrategies.VAL_OR_VAR_NODE",
        positioningStrategiesImport
    ),

    SECONDARY_CONSTRUCTOR_DELEGATION_CALL(
        "SourceElementPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL",
        positioningStrategiesImport
    ),

    DECLARATION_NAME(
        "SourceElementPositioningStrategies.DECLARATION_NAME",
        positioningStrategiesImport
    ),

    DECLARATION_SIGNATURE(
        "SourceElementPositioningStrategies.DECLARATION_SIGNATURE",
        positioningStrategiesImport
    ),

    DECLARATION_SIGNATURE_OR_DEFAULT(
        "SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT",
        positioningStrategiesImport
    ),

    VISIBILITY_MODIFIER(
        "SourceElementPositioningStrategies.VISIBILITY_MODIFIER",
        positioningStrategiesImport
    ),

    MODALITY_MODIFIER(
        "SourceElementPositioningStrategies.MODALITY_MODIFIER",
        positioningStrategiesImport
    ),

    OPERATOR(
        "SourceElementPositioningStrategies.OPERATOR",
        positioningStrategiesImport
    ),

    PARAMETER_DEFAULT_VALUE(
        "SourceElementPositioningStrategies.PARAMETER_DEFAULT_VALUE",
        positioningStrategiesImport
    ),

    PARAMETER_VARARG_MODIFIER(
        "SourceElementPositioningStrategies.PARAMETER_VARARG_MODIFIER",
        positioningStrategiesImport
    ),

    DECLARATION_RETURN_TYPE(
        "SourceElementPositioningStrategies.DECLARATION_RETURN_TYPE",
        positioningStrategiesImport
    ),

    OVERRIDE_MODIFIER(
        "SourceElementPositioningStrategies.OVERRIDE_MODIFIER",
        positioningStrategiesImport
    ),

    DOT_BY_SELECTOR(
        "SourceElementPositioningStrategies.DOT_BY_SELECTOR",
        positioningStrategiesImport
    ),

    OPEN_MODIFIER(
        "SourceElementPositioningStrategies.OPEN_MODIFIER",
        positioningStrategiesImport
    ),

    VARIANCE_MODIFIER(
        "SourceElementPositioningStrategies.VARIANCE_MODIFIER",
        positioningStrategiesImport
    ),
}

private const val positioningStrategiesImport = "org.jetbrains.kotlin.fir.analysis.diagnostics.SourceElementPositioningStrategies"


fun DiagnosticData.hasDefaultPositioningStrategy(): Boolean =
    positioningStrategy == PositioningStrategy.DEFAULT

data class DiagnosticList(
    val diagnostics: List<DiagnosticData>,
)