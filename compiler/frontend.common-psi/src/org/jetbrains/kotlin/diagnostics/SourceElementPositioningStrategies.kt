/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

object SourceElementPositioningStrategies {
    val DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DEFAULT,
        PsiPositioningStrategies.DEFAULT
    )

    val SYNTAX_ERROR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DEFAULT,
        PsiPositioningStrategies.SYNTAX_ERROR
    )

    val VAL_OR_VAR_NODE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VAL_OR_VAR_NODE,
        PsiPositioningStrategies.VAL_OR_VAR_NODE
    )

    val FUN_INTERFACE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_INTERFACE,
        PsiPositioningStrategies.FUN_INTERFACE
    )

    val COMPANION_OBJECT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.COMPANION_OBJECT,
        PsiPositioningStrategies.COMPANION_OBJECT
    )

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL,
        PsiPositioningStrategies.SECONDARY_CONSTRUCTOR_DELEGATION_CALL
    )

    val DECLARATION_RETURN_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_RETURN_TYPE,
        PsiPositioningStrategies.DECLARATION_RETURN_TYPE
    )

    val CONTEXT_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CONTEXT_KEYWORD,
        PsiPositioningStrategies.CONTEXT_KEYWORD
    )

    val DECLARATION_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_NAME,
        PsiPositioningStrategies.DECLARATION_NAME
    )

    val DECLARATION_NAME_ONLY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_NAME_ONLY,
        PsiPositioningStrategies.DECLARATION_NAME_ONLY
    )

    val DECLARATION_SIGNATURE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE,
        PsiPositioningStrategies.DECLARATION_SIGNATURE
    )

    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS,
        PsiPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS
    )

    val DECLARATION_SIGNATURE_OR_DEFAULT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT,
        PsiPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT
    )

    val VISIBILITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VISIBILITY_MODIFIER,
        PsiPositioningStrategies.VISIBILITY_MODIFIER
    )

    val MODALITY_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.MODALITY_MODIFIER,
        PsiPositioningStrategies.MODALITY_MODIFIER
    )

    val ABSTRACT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ABSTRACT_MODIFIER,
        PsiPositioningStrategies.ABSTRACT_MODIFIER
    )

    val OPEN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPEN_MODIFIER,
        PsiPositioningStrategies.OPEN_MODIFIER
    )

    val OVERRIDE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OVERRIDE_MODIFIER,
        PsiPositioningStrategies.OVERRIDE_MODIFIER
    )

    val PRIVATE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PRIVATE_MODIFIER,
        PsiPositioningStrategies.PRIVATE_MODIFIER
    )

    val LATEINIT_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LATEINIT_MODIFIER,
        PsiPositioningStrategies.LATEINIT_MODIFIER
    )

    val VARIANCE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VARIANCE_MODIFIER,
        PsiPositioningStrategies.VARIANCE_MODIFIER
    )

    val CONST_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CONST_MODIFIER,
        PsiPositioningStrategies.CONST_MODIFIER
    )

    val INLINE_OR_VALUE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_OR_VALUE_MODIFIER,
        PsiPositioningStrategies.INLINE_OR_VALUE_MODIFIER
    )

    val VALUE_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_MODIFIER,
        PsiPositioningStrategies.VALUE_MODIFIER
    )

    val INNER_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INNER_MODIFIER,
        PsiPositioningStrategies.INNER_MODIFIER
    )

    val FUN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUN_MODIFIER,
        PsiPositioningStrategies.FUN_MODIFIER
    )

    val SUSPEND_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUSPEND_MODIFIER,
        PsiPositioningStrategies.SUSPEND_MODIFIER
    )

    val DATA_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DATA_MODIFIER,
        PsiPositioningStrategies.DATA_MODIFIER
    )

    val EXPECT_ACTUAL_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.EXPECT_ACTUAL_MODIFIER,
        PsiPositioningStrategies.EXPECT_ACTUAL_MODIFIER
    )

    val OBJECT_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OBJECT_KEYWORD,
        PsiPositioningStrategies.OBJECT_KEYWORD
    )

    val OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR,
        PsiPositioningStrategies.OPERATOR
    )

    val REFERENCE_OR_OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCE_OR_OPERATOR,
        PsiPositioningStrategies.REFERENCE_OR_OPERATOR
    )

    val PARAMETER_DEFAULT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_DEFAULT_VALUE,
        PsiPositioningStrategies.PARAMETER_DEFAULT_VALUE
    )

    val PARAMETERS_WITH_DEFAULT_VALUE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE,
        PsiPositioningStrategies.PARAMETERS_WITH_DEFAULT_VALUE
    )

    val PARAMETER_VARARG_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PARAMETER_VARARG_MODIFIER,
        PsiPositioningStrategies.PARAMETER_VARARG_MODIFIER
    )

    val NAME_OF_NAMED_ARGUMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_OF_NAMED_ARGUMENT,
        PsiPositioningStrategies.NAME_OF_NAMED_ARGUMENT
    )

    val VALUE_ARGUMENTS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VALUE_ARGUMENTS_LIST,
        PsiPositioningStrategies.VALUE_ARGUMENTS_LIST
    )

    val SUPERTYPES_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUPERTYPES_LIST,
        PsiPositioningStrategies.SUPERTYPES_LIST
    )

    val DOT_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DOT_BY_QUALIFIED,
        PsiPositioningStrategies.DOT_BY_QUALIFIED
    )

    val SELECTOR_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SELECTOR_BY_QUALIFIED,
        PsiPositioningStrategies.SELECTOR_BY_QUALIFIED
    )

    val REFERENCE_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCE_BY_QUALIFIED,
        PsiPositioningStrategies.REFERENCE_BY_QUALIFIED
    )

    val REFERENCED_NAME_BY_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REFERENCED_NAME_BY_QUALIFIED,
        PsiPositioningStrategies.REFERENCED_NAME_BY_QUALIFIED
    )

    /**
     * NB! The whole receiver including parentheses is marked, which is not true if we use the FIR source of the receiver
     * element with [DEFAULT] positioning.
     */
    val RECEIVER_OF_DOT_QUALIFIED = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.RECEIVER_OF_DOT_QUALIFIED,
        PsiPositioningStrategies.RECEIVER_OF_DOT_QUALIFIED
    )

    val DEPRECATION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DEPRECATION,
        PsiPositioningStrategies.DEPRECATION
    )

    val WHEN_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHEN_EXPRESSION,
        PsiPositioningStrategies.WHEN_EXPRESSION
    )

    val WHEN_GUARD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHEN_GUARD,
        PsiPositioningStrategies.WHEN_GUARD
    )

    val IF_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IF_EXPRESSION,
        PsiPositioningStrategies.IF_EXPRESSION
    )

    val ELSE_ENTRY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ELSE_ENTRY,
        PsiPositioningStrategies.ELSE_ENTRY
    )

    val ARRAY_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ARRAY_ACCESS,
        PsiPositioningStrategies.ARRAY_ACCESS
    )

    val SAFE_ACCESS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SAFE_ACCESS,
        PsiPositioningStrategies.SAFE_ACCESS
    )

    val AS_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.AS_TYPE,
        PsiPositioningStrategies.AS_TYPE
    )

    val USELESS_ELVIS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.USELESS_ELVIS,
        PsiPositioningStrategies.USELESS_ELVIS
    )

    val USELESS_ELVIS_LEFT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.USELESS_ELVIS_LEFT,
        PsiPositioningStrategies.USELESS_ELVIS_LEFT
    )

    val RETURN_WITH_LABEL = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.RETURN_WITH_LABEL,
        PsiPositioningStrategies.RETURN_WITH_LABEL
    )

    val VARIABLE_INITIALIZER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.VARIABLE_INITIALIZER,
        PsiPositioningStrategies.VARIABLE_INITIALIZER
    )

    val WHOLE_ELEMENT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.WHOLE_ELEMENT,
        PsiPositioningStrategies.WHOLE_ELEMENT
    )

    val LONG_LITERAL_SUFFIX = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.LONG_LITERAL_SUFFIX,
        PsiPositioningStrategies.LONG_LITERAL_SUFFIX
    )

    val REIFIED_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REIFIED_MODIFIER,
        PsiPositioningStrategies.REIFIED_MODIFIER
    )

    val TYPE_PARAMETERS_LIST = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPE_PARAMETERS_LIST,
        PsiPositioningStrategies.TYPE_PARAMETERS_LIST
    )

    val FUNCTION_TYPE_RECEIVER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FUNCTION_TYPE_RECEIVER,
        PsiPositioningStrategies.FUNCTION_TYPE_RECEIVER
    )

    val NAME_IDENTIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NAME_IDENTIFIER,
        PsiPositioningStrategies.NAME_IDENTIFIER
    )

    val REDUNDANT_NULLABLE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.REDUNDANT_NULLABLE,
        PsiPositioningStrategies.REDUNDANT_NULLABLE
    )

    val QUESTION_MARK_BY_TYPE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.QUESTION_MARK_BY_TYPE,
        PsiPositioningStrategies.QUESTION_MARK_BY_TYPE
    )

    val ANNOTATION_USE_SITE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ANNOTATION_USE_SITE,
        PsiPositioningStrategies.ANNOTATION_USE_SITE
    )

    val IMPORT_LAST_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_LAST_NAME,
        PsiPositioningStrategies.IMPORT_LAST_NAME
    )

    val IMPORT_LAST_BUT_ONE_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_LAST_BUT_ONE_NAME,
        PsiPositioningStrategies.IMPORT_LAST_BUT_ONE_NAME,
    )

    val SPREAD_OPERATOR = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SPREAD_OPERATOR,
        PsiPositioningStrategies.SPREAD_OPERATOR
    )

    val DECLARATION_WITH_BODY = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_WITH_BODY,
        PsiPositioningStrategies.DECLARATION_WITH_BODY
    )
    val COMMAS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.COMMAS,
        PsiPositioningStrategies.COMMAS
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
        PsiPositioningStrategies.LABEL
    )

    val NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT,
        PsiPositioningStrategies.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT
    )

    val INLINE_PARAMETER_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_PARAMETER_MODIFIER,
        PsiPositioningStrategies.INLINE_PARAMETER_MODIFIER
    )

    val INLINE_FUN_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INLINE_FUN_MODIFIER,
        PsiPositioningStrategies.INLINE_FUN_MODIFIER
    )

    val OPERATOR_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OPERATOR_MODIFIER,
        PsiPositioningStrategies.OPERATOR_MODIFIER
    )

    val INFIX_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.INFIX_MODIFIER,
        PsiPositioningStrategies.INFIX_MODIFIER
    )

    val NON_FINAL_MODIFIER_OR_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.NON_FINAL_MODIFIER_OR_NAME,
        PsiPositioningStrategies.NON_FINAL_MODIFIER_OR_NAME
    )

    val ENUM_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.ENUM_MODIFIER,
        PsiPositioningStrategies.ENUM_MODIFIER
    )

    val FIELD_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.FIELD_KEYWORD,
        PsiPositioningStrategies.FIELD_KEYWORD
    )

    val TAILREC_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TAILREC_MODIFIER,
        PsiPositioningStrategies.TAILREC_MODIFIER
    )

    val EXTERNAL_MODIFIER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.EXTERNAL_MODIFIER,
        PsiPositioningStrategies.EXTERNAL_MODIFIER
    )

    val PROPERTY_DELEGATE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PROPERTY_DELEGATE,
        PsiPositioningStrategies.PROPERTY_DELEGATE
    )

    val IMPORT_ALIAS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.IMPORT_ALIAS,
        PsiPositioningStrategies.IMPORT_ALIAS
    )

    val DECLARATION_START_TO_NAME = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DECLARATION_START_TO_NAME,
        PsiPositioningStrategies.DECLARATION_START_TO_NAME
    )

    val DELEGATED_SUPERTYPE_BY_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DELEGATED_SUPERTYPE_BY_KEYWORD,
        PsiPositioningStrategies.DELEGATED_SUPERTYPE_BY_KEYWORD
    )

    val PROPERTY_DELEGATE_BY_KEYWORD = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD,
        PsiPositioningStrategies.PROPERTY_DELEGATE_BY_KEYWORD
    )

    val CALL_ELEMENT_WITH_DOT = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.CALL_ELEMENT_WITH_DOT,
        PsiPositioningStrategies.CALL_ELEMENT_WITH_DOT
    )

    val TYPEALIAS_TYPE_REFERENCE = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPEALIAS_TYPE_REFERENCE,
        PsiPositioningStrategies.TYPEALIAS_TYPE_REFERENCE,
    )

    val SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC,
        PsiPositioningStrategies.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC,
    )

    val TYPE_ARGUMENT_LIST_OR_SELF = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPE_ARGUMENT_LIST_OR_SELF,
        PsiPositioningStrategies.TYPE_ARGUMENT_LIST_OR_SELF,
    )

    val TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER,
        PsiPositioningStrategies.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER,
    )

    val PACKAGE_DIRECTIVE_NAME_EXPRESSION = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.PACKAGE_DIRECTIVE_NAME_EXPRESSION,
        PsiPositioningStrategies.PACKAGE_DIRECTIVE_NAME_EXPRESSION,
    )

    val OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS,
        PsiPositioningStrategies.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS,
    )

    val DESTRUCTURING_DECLARATION_ENTRIES = SourceElementPositioningStrategy(
        LightTreePositioningStrategies.DESTRUCTURING_DECLARATION_ENTRIES,
        PsiPositioningStrategies.DESTRUCTURING_DECLARATION_ENTRIES,
    )
}
