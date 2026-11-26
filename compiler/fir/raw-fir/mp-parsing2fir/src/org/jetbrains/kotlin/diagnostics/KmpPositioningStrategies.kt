/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

/**
 * Stubs of the KMP parse tree implementations of all [PositioningStrategyKinds],
 * in the same order as in `LightTreePositioningStrategies` to simplify porting.
 *
 * TODO: implement the strategies based on LightTreePositioningStrategies, KT-91322
 */
object KmpPositioningStrategies : PositioningStrategyRegistry<KmpPositioningStrategy>() {
    init {
        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DEFAULT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SYNTAX_ERROR)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SUPERTYPES_LIST)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VAL_OR_VAR_NODE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.COMPANION_OBJECT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SECONDARY_CONSTRUCTOR_DELEGATION_CALL)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_RETURN_TYPE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_START_TO_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_NAME_ONLY)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ACTUAL_DECLARATION_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.CONTEXT_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_SIGNATURE_OR_DEFAULT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VARIABLE_INITIALIZER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VISIBILITY_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.MODALITY_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ABSTRACT_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OPEN_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OVERRIDE_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PRIVATE_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.LATEINIT_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VARIANCE_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.CONST_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.FUN_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SUSPEND_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.INLINE_OR_VALUE_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VALUE_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.INNER_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DATA_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OPERATOR_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.INFIX_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ENUM_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TAILREC_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.EXTERNAL_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.EXPECT_ACTUAL_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OBJECT_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.FIELD_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PROPERTY_DELEGATE_BY_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.INLINE_PARAMETER_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.INLINE_FUN_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OPERATOR)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.REFERENCE_OR_OPERATOR)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PARAMETER_DEFAULT_VALUE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PARAMETERS_WITH_DEFAULT_VALUE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PARAMETER_VARARG_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.NAME_OF_NAMED_ARGUMENT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.VALUE_ARGUMENTS_LIST)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DOT_BY_QUALIFIED)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SELECTOR_BY_QUALIFIED)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.FUN_INTERFACE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.REFERENCE_BY_QUALIFIED)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.REFERENCED_NAME_BY_QUALIFIED)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.RECEIVER_OF_DOT_QUALIFIED)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.WHEN_EXPRESSION)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.WHEN_GUARD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.IF_EXPRESSION)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TRY_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ELSE_ENTRY)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ARRAY_ACCESS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SAFE_ACCESS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.AS_TYPE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.USELESS_ELVIS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.USELESS_ELVIS_LEFT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.RETURN_WITH_LABEL)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.WHOLE_ELEMENT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.LONG_LITERAL_SUFFIX)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.REIFIED_MODIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TYPE_PARAMETERS_LIST)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.FUNCTION_TYPE_RECEIVER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.NAME_IDENTIFIER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.REDUNDANT_NULLABLE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.QUESTION_MARK_BY_TYPE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.ANNOTATION_USE_SITE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.IMPORT_LAST_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.IMPORT_LAST_BUT_ONE_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.IMPORT_ALIAS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SPREAD_OPERATOR)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DECLARATION_WITH_BODY)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.UNREACHABLE_CODE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.NOT_SUPPORTED_IN_INLINE_MOST_RELEVANT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.LABEL)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.COMMAS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.NON_FINAL_MODIFIER_OR_NAME)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DELEGATED_SUPERTYPE_BY_KEYWORD)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.CALL_ELEMENT_WITH_DOT)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TYPEALIAS_TYPE_REFERENCE)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS_DIAGNOSTIC)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_SELF)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.PACKAGE_DIRECTIVE_NAME_EXPRESSION)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.OUTERMOST_PARENTHESES_IN_ASSIGNMENT_LHS)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DESTRUCTURING_DECLARATION_ENTRIES)

        KmpPositioningStrategy().registerAs(PositioningStrategyKinds.DEPRECATION)
    }
}
