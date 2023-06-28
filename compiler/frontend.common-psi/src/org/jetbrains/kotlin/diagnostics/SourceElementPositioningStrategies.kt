/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

object SourceElementPositioningStrategies {
    val DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DEFAULT,
        PositioningStrategies.DEFAULT
    ).also {
        AbstractSourceElementPositioningStrategy.setDefault(it)
    }

    val VAL_OR_VAR_NODE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VAL_OR_VAR_NODE,
        PositioningStrategies.VAL_OR_VAR_NODE
    )

    val FUN_INTERFACE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_INTERFACE,
        PositioningStrategies.FUN_INTERFACE
    )

    val COMPANION_OBJECT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.COMPANION_OBJECT,
        PositioningStrategies.COMPANION_OBJECT
    )

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL,
        PositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL
    )

    val DECLARATION_RETURN_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_RETURN_TYPE,
        PositioningStrategies.DECLARATION_RETURN_TYPE
    )

    val DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_NAME,
        PositioningStrategies.DECLARATION_NAME
    )

    val DECLARATION_NAME_ONLY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_NAME_ONLY,
        PositioningStrategies.DECLARATION_NAME_ONLY
    )

    val DECLARATION_SIGNATURE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE,
        PositioningStrategies.DECLARATION_SIGNATURE
    )

    val DECLARATION_SIGNATURE_OR_DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT,
        PositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
    )

    val VISIBILITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VISIBILITY_MODIFIER,
        PositioningStrategies.VISIBILITY_MODIFIER
    )

    val MODALITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.MODALITY_MODIFIER,
        PositioningStrategies.MODALITY_MODIFIER
    )

    val ABSTRACT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ABSTRACT_MODIFIER,
        PositioningStrategies.ABSTRACT_MODIFIER
    )

    val OPEN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPEN_MODIFIER,
        PositioningStrategies.OPEN_MODIFIER
    )

    val OVERRIDE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OVERRIDE_MODIFIER,
        PositioningStrategies.OVERRIDE_MODIFIER
    )

    val PRIVATE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PRIVATE_MODIFIER,
        PositioningStrategies.PRIVATE_MODIFIER
    )

    val LATEINIT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LATEINIT_MODIFIER,
        PositioningStrategies.LATEINIT_MODIFIER
    )

    val VARIANCE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VARIANCE_MODIFIER,
        PositioningStrategies.VARIANCE_MODIFIER
    )

    val CONST_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CONST_MODIFIER,
        PositioningStrategies.CONST_MODIFIER
    )

    val INLINE_OR_VALUE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        PositioningStrategies.INLINE_OR_VALUE_MODIFIER
    )

    val INNER_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INNER_MODIFIER,
        PositioningStrategies.INNER_MODIFIER
    )

    val FUN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_MODIFIER,
        PositioningStrategies.FUN_MODIFIER
    )

    val SUSPEND_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUSPEND_MODIFIER,
        PositioningStrategies.SUSPEND_MODIFIER
    )

    val DATA_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DATA_MODIFIER,
        PositioningStrategies.DATA_MODIFIER
    )

    val EXPECT_ACTUAL_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.EXPECT_ACTUAL_MODIFIER,
        PositioningStrategies.EXPECT_ACTUAL_MODIFIER
    )

    val OBJECT_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OBJECT_KEYWORD,
        PositioningStrategies.OBJECT_KEYWORD
    )

    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PositioningStrategies.OPERATOR
    )

    val PARAMETER_DEFAULT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_DEFAULT_VALUE,
        PositioningStrategies.PARAMETER_DEFAULT_VALUE
    )

    val PARAMETERS_WITH_DEFAULT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE,
        PositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE
    )

    val PARAMETER_VARARG_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_VARARG_MODIFIER,
        PositioningStrategies.PARAMETER_VARARG_MODIFIER
    )

    val NAME_OF_NAMED_ARGUMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_OF_NAMED_ARGUMENT,
        PositioningStrategies.NAME_OF_NAMED_ARGUMENT
    )

    val VALUE_ARGUMENTS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS,
        PositioningStrategies.VALUE_ARGUMENTS
    )

    val VALUE_ARGUMENTS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS_LIST,
        PositioningStrategies.VALUE_ARGUMENTS_LIST
    )

    val SUPERTYPES_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUPERTYPES_LIST,
        PositioningStrategies.SUPERTYPES_LIST
    )

    val DOT_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DOT_BY_QUALIFIED,
        PositioningStrategies.DOT_BY_QUALIFIED
    )

    val SELECTOR_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SELECTOR_BY_QUALIFIED,
        PositioningStrategies.SELECTOR_BY_QUALIFIED
    )

    val REFERENCE_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCE_BY_QUALIFIED,
        PositioningStrategies.REFERENCE_BY_QUALIFIED
    )

    val REFERENCED_NAME_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
        PositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

    val WHEN_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHEN_EXPRESSION,
        PositioningStrategies.WHEN_EXPRESSION
    )

    val IF_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IF_EXPRESSION,
        PositioningStrategies.IF_EXPRESSION
    )

    val ELSE_ENTRY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ELSE_ENTRY,
        PositioningStrategies.ELSE_ENTRY
    )

    val ARRAY_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ARRAY_ACCESS,
        PositioningStrategies.ARRAY_ACCESS
    )

    val SAFE_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SAFE_ACCESS,
        PositioningStrategies.SAFE_ACCESS
    )

    val AS_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.AS_TYPE,
        PositioningStrategies.AS_TYPE
    )

    val USELESS_ELVIS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.USELESS_ELVIS,
        PositioningStrategies.USELESS_ELVIS
    )

    val RETURN_WITH_LABEL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.RETURN_WITH_LABEL,
        PositioningStrategies.RETURN_WITH_LABEL
    )

    val PROPERTY_INITIALIZER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LAST_CHILD,
        PositioningStrategies.PROPERTY_INITIALIZER
    )

    val WHOLE_ELEMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHOLE_ELEMENT,
        PositioningStrategies.WHOLE_ELEMENT
    )

    val LONG_LITERAL_SUFFIX = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LONG_LITERAL_SUFFIX,
        PositioningStrategies.LONG_LITERAL_SUFFIX
    )

    val REIFIED_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REIFIED_MODIFIER,
        PositioningStrategies.REIFIED_MODIFIER
    )

    val TYPE_PARAMETERS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPE_PARAMETERS_LIST,
        PositioningStrategies.TYPE_PARAMETERS_LIST
    )

    val NAME_IDENTIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_IDENTIFIER,
        PositioningStrategies.NAME_IDENTIFIER
    )

    val REDUNDANT_NULLABLE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REDUNDANT_NULLABLE,
        PositioningStrategies.REDUNDANT_NULLABLE
    )

    val QUESTION_MARK_BY_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.QUESTION_MARK_BY_TYPE,
        PositioningStrategies.QUESTION_MARK_BY_TYPE
    )

    val ANNOTATION_USE_SITE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ANNOTATION_USE_SITE,
        PositioningStrategies.ANNOTATION_USE_SITE
    )

    val IMPORT_LAST_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_LAST_NAME,
        PositioningStrategies.IMPORT_LAST_NAME
    )

    val SPREAD_OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SPREAD_OPERATOR,
        PositioningStrategies.SPREAD_OPERATOR
    )

    val DECLARATION_WITH_BODY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_WITH_BODY,
        PositioningStrategies.DECLARATION_WITH_BODY
    )
    val COMMAS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.COMMAS,
        PositioningStrategies.COMMAS
    )

    val UNREACHABLE_CODE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.UNREACHABLE_CODE,
        PsiPositioningStrategies.UNREACHABLE_CODE
    )

    val ACTUAL_DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ACTUAL_DECLARATION_NAME,
        PsiPositioningStrategies.ACTUAL_DECLARATION_NAME
    )

    val LABEL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LABEL,
        PositioningStrategies.LABEL
    )

    // TODO
    val INCOMPATIBLE_DECLARATION = DEFAULT

    val NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT,
        PositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT
    )

    val INLINE_PARAMETER_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_PARAMETER_MODIFIER,
        PositioningStrategies.INLINE_PARAMETER_MODIFIER
    )

    val INLINE_FUN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_FUN_MODIFIER,
        PositioningStrategies.INLINE_FUN_MODIFIER
    )

    val OPERATOR_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR_MODIFIER,
        PositioningStrategies.OPERATOR_MODIFIER
    )

    val NON_FINAL_MODIFIER_OR_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NON_FINAL_MODIFIER_OR_NAME,
        PositioningStrategies.NON_FINAL_MODIFIER_OR_NAME
    )

    val ENUM_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ENUM_MODIFIER,
        PositioningStrategies.ENUM_MODIFIER
    )

    val FIELD_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FIELD_KEYWORD,
        PositioningStrategies.FIELD_KEYWORD
    )

    val TAILREC_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TAILREC_MODIFIER,
        PositioningStrategies.TAILREC_MODIFIER
    )

    val EXTERNAL_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.EXTERNAL_MODIFIER,
        PositioningStrategies.EXTERNAL_MODIFIER
    )

    val PROPERTY_DELEGATE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PROPERTY_DELEGATE,
        PositioningStrategies.PROPERTY_DELEGATE
    )

    val IMPORT_ALIAS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_ALIAS,
        PositioningStrategies.IMPORT_ALIAS
    )

    val DECLARATION_START_TO_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_START_TO_NAME,
        PositioningStrategies.DECLARATION_START_TO_NAME
    )

    val DELEGATED_SUPERTYPE_BY_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DELEGATED_SUPERTYPE_BY_KEYWORD,
        PositioningStrategies.DELEGATED_SUPERTYPE_BY_KEYWORD
    )

    val CALL_ELEMENT_WITH_DOT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CALL_ELEMENT_WITH_DOT,
        PositioningStrategies.CALL_ELEMENT_WITH_DOT
    )

    val TYPEALIAS_TYPE_REFERENCE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPEALIAS_TYPE_REFERENCE,
        PositioningStrategies.TYPEALIAS_TYPE_REFERENCE,
    )
}
