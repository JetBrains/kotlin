/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator.diagnostics.model

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Severity
import kotlin.reflect.KType

sealed class DiagnosticData {
    abstract val containingObjectName: String
    abstract val name: String
    abstract val psiType: KType
    abstract val parameters: List<DiagnosticParameter>
    abstract val positioningStrategy: PositioningStrategy
}

data class RegularDiagnosticData(
    override val containingObjectName: String,
    val severity: Severity,
    override val name: String,
    override val psiType: KType,
    override val parameters: List<DiagnosticParameter>,
    override val positioningStrategy: PositioningStrategy,
    val isSuppressible: Boolean,
) : DiagnosticData()

data class DeprecationDiagnosticData(
    override val containingObjectName: String,
    val featureForError: LanguageFeature,
    override val name: String,
    override val psiType: KType,
    override val parameters: List<DiagnosticParameter>,
    override val positioningStrategy: PositioningStrategy,
) : DiagnosticData()

data class DiagnosticParameter(
    val name: String,
    val type: KType
)

enum class PositioningStrategy(private val strategy: String? = null) {
    DEFAULT,
    VAL_OR_VAR_NODE,
    SECONDARY_CONSTRUCTOR_DELEGATION_CALL,
    DECLARATION_NAME,
    DECLARATION_NAME_ONLY,
    DECLARATION_SIGNATURE,
    DECLARATION_SIGNATURE_OR_DEFAULT,
    VISIBILITY_MODIFIER,
    MODALITY_MODIFIER,
    OPERATOR,
    PARAMETER_DEFAULT_VALUE,
    PARAMETERS_WITH_DEFAULT_VALUE,
    PARAMETER_VARARG_MODIFIER,
    DECLARATION_RETURN_TYPE,
    OVERRIDE_MODIFIER,
    DOT_BY_QUALIFIED,
    OPEN_MODIFIER,
    WHEN_EXPRESSION,
    IF_EXPRESSION,
    ELSE_ENTRY,
    VARIANCE_MODIFIER,
    LATEINIT_MODIFIER,
    INLINE_OR_VALUE_MODIFIER,
    INNER_MODIFIER,
    SUSPEND_MODIFIER,
    SELECTOR_BY_QUALIFIED,
    REFERENCE_BY_QUALIFIED,
    REFERENCED_NAME_BY_QUALIFIED,
    PRIVATE_MODIFIER,
    COMPANION_OBJECT,
    CONST_MODIFIER,
    ARRAY_ACCESS,
    SAFE_ACCESS,
    AS_TYPE,
    USELESS_ELVIS,
    NAME_OF_NAMED_ARGUMENT,
    VALUE_ARGUMENTS,
    VALUE_ARGUMENTS_LIST,
    SUPERTYPES_LIST,
    RETURN_WITH_LABEL,
    PROPERTY_INITIALIZER,
    WHOLE_ELEMENT,
    LONG_LITERAL_SUFFIX,
    REIFIED_MODIFIER,
    TYPE_PARAMETERS_LIST,
    FUN_MODIFIER,
    FUN_INTERFACE,
    NAME_IDENTIFIER,
    QUESTION_MARK_BY_TYPE,
    ANNOTATION_USE_SITE,
    IMPORT_LAST_NAME,
    IMPORT_LAST_BUT_ONE_NAME,
    DATA_MODIFIER,
    SPREAD_OPERATOR,
    DECLARATION_WITH_BODY,
    NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT,
    INCOMPATIBLE_DECLARATION,
    ACTUAL_DECLARATION_NAME,
    UNREACHABLE_CODE,
    INLINE_PARAMETER_MODIFIER,
    ABSTRACT_MODIFIER,
    LABEL,
    COMMAS,
    OPERATOR_MODIFIER,
    INFIX_MODIFIER,
    NON_FINAL_MODIFIER_OR_NAME,
    ENUM_MODIFIER,
    FIELD_KEYWORD,
    TAILREC_MODIFIER,
    EXTERNAL_MODIFIER,
    PROPERTY_DELEGATE,
    IMPORT_ALIAS,
    DECLARATION_START_TO_NAME,
    REDUNDANT_NULLABLE,
    INLINE_FUN_MODIFIER,
    CALL_ELEMENT_WITH_DOT,
    EXPECT_ACTUAL_MODIFIER,
    TYPEALIAS_TYPE_REFERENCE,
    SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC,
    ;

    val expressionToCreate get() = "SourceElementPositioningStrategies.${strategy ?: name}"

    companion object {
        const val importToAdd = "org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies"
    }
}


fun DiagnosticData.hasDefaultPositioningStrategy(): Boolean =
    positioningStrategy == PositioningStrategy.DEFAULT
