/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KtNodeTypes : SyntaxElementTypesWithIds() {
    val CLASS: SyntaxElementType = KtStubElementTypes.CLASS
    val FUN: SyntaxElementType = KtStubElementTypes.FUNCTION
    val PROPERTY: SyntaxElementType = KtStubElementTypes.PROPERTY

    val OBJECT_DECLARATION: SyntaxElementType = KtStubElementTypes.OBJECT_DECLARATION
    val TYPEALIAS: SyntaxElementType = KtStubElementTypes.TYPEALIAS

    val ENUM_ENTRY: SyntaxElementType = KtStubElementTypes.ENUM_ENTRY
    val CLASS_INITIALIZER: SyntaxElementType = KtStubElementTypes.CLASS_INITIALIZER
    val SECONDARY_CONSTRUCTOR: SyntaxElementType = KtStubElementTypes.SECONDARY_CONSTRUCTOR
    val PRIMARY_CONSTRUCTOR: SyntaxElementType = KtStubElementTypes.PRIMARY_CONSTRUCTOR
    val CONTEXT_RECEIVER: SyntaxElementType = KtStubElementTypes.CONTEXT_RECEIVER
    val CONTEXT_RECEIVER_LIST: SyntaxElementType = KtStubElementTypes.CONTEXT_RECEIVER_LIST

    val TYPE_PARAMETER_LIST: SyntaxElementType = KtStubElementTypes.TYPE_PARAMETER_LIST
    val TYPE_PARAMETER: SyntaxElementType = KtStubElementTypes.TYPE_PARAMETER
    val SUPER_TYPE_LIST: SyntaxElementType = KtStubElementTypes.SUPER_TYPE_LIST
    val DELEGATED_SUPER_TYPE_ENTRY: SyntaxElementType = KtStubElementTypes.DELEGATED_SUPER_TYPE_ENTRY
    val SUPER_TYPE_CALL_ENTRY: SyntaxElementType = KtStubElementTypes.SUPER_TYPE_CALL_ENTRY
    val SUPER_TYPE_ENTRY: SyntaxElementType = KtStubElementTypes.SUPER_TYPE_ENTRY
    val CONSTRUCTOR_CALLEE: SyntaxElementType = KtStubElementTypes.CONSTRUCTOR_CALLEE
    val VALUE_PARAMETER_LIST: SyntaxElementType = KtStubElementTypes.VALUE_PARAMETER_LIST
    val VALUE_PARAMETER: SyntaxElementType = KtStubElementTypes.VALUE_PARAMETER

    val CLASS_BODY: SyntaxElementType = KtStubElementTypes.CLASS_BODY
    val IMPORT_LIST: SyntaxElementType = KtStubElementTypes.IMPORT_LIST
    val FILE_ANNOTATION_LIST: SyntaxElementType = KtStubElementTypes.FILE_ANNOTATION_LIST
    val IMPORT_DIRECTIVE: SyntaxElementType = KtStubElementTypes.IMPORT_DIRECTIVE
    val IMPORT_ALIAS: SyntaxElementType = KtStubElementTypes.IMPORT_ALIAS
    val MODIFIER_LIST: SyntaxElementType = KtStubElementTypes.MODIFIER_LIST
    val ANNOTATION: SyntaxElementType = KtStubElementTypes.ANNOTATION
    val ANNOTATION_ENTRY: SyntaxElementType = KtStubElementTypes.ANNOTATION_ENTRY
    val ANNOTATION_TARGET: SyntaxElementType = KtStubElementTypes.ANNOTATION_TARGET

    val TYPE_ARGUMENT_LIST: SyntaxElementType = KtStubElementTypes.TYPE_ARGUMENT_LIST
    val VALUE_ARGUMENT_LIST: SyntaxElementType = KtStubElementTypes.VALUE_ARGUMENT_LIST
    val VALUE_ARGUMENT: SyntaxElementType = KtStubElementTypes.VALUE_ARGUMENT
    val CONTRACT_EFFECT_LIST: SyntaxElementType = KtStubElementTypes.CONTRACT_EFFECT_LIST
    val CONTRACT_EFFECT: SyntaxElementType = KtStubElementTypes.CONTRACT_EFFECT
    val LAMBDA_ARGUMENT: SyntaxElementType = KtStubElementTypes.LAMBDA_ARGUMENT
    val VALUE_ARGUMENT_NAME: SyntaxElementType = KtStubElementTypes.VALUE_ARGUMENT_NAME
    val TYPE_REFERENCE: SyntaxElementType = KtStubElementTypes.TYPE_REFERENCE

    val USER_TYPE: SyntaxElementType = KtStubElementTypes.USER_TYPE
    val DYNAMIC_TYPE: SyntaxElementType = KtStubElementTypes.DYNAMIC_TYPE
    val FUNCTION_TYPE: SyntaxElementType = KtStubElementTypes.FUNCTION_TYPE
    val FUNCTION_TYPE_RECEIVER: SyntaxElementType = KtStubElementTypes.FUNCTION_TYPE_RECEIVER
    val NULLABLE_TYPE: SyntaxElementType = KtStubElementTypes.NULLABLE_TYPE
    val INTERSECTION_TYPE: SyntaxElementType = KtStubElementTypes.INTERSECTION_TYPE
    val TYPE_PROJECTION: SyntaxElementType = KtStubElementTypes.TYPE_PROJECTION

    val PROPERTY_ACCESSOR: SyntaxElementType = KtStubElementTypes.PROPERTY_ACCESSOR
    val BACKING_FIELD: SyntaxElementType = KtStubElementTypes.BACKING_FIELD
    val INITIALIZER_LIST: SyntaxElementType = KtStubElementTypes.INITIALIZER_LIST
    val TYPE_CONSTRAINT_LIST: SyntaxElementType = KtStubElementTypes.TYPE_CONSTRAINT_LIST
    val TYPE_CONSTRAINT: SyntaxElementType = KtStubElementTypes.TYPE_CONSTRAINT

    val NULL: SyntaxElementType = KtStubElementTypes.NULL
    val BOOLEAN_CONSTANT: SyntaxElementType = KtStubElementTypes.BOOLEAN_CONSTANT
    val FLOAT_CONSTANT: SyntaxElementType = KtStubElementTypes.FLOAT_CONSTANT
    val CHARACTER_CONSTANT: SyntaxElementType = KtStubElementTypes.CHARACTER_CONSTANT
    val INTEGER_CONSTANT: SyntaxElementType = KtStubElementTypes.INTEGER_CONSTANT

    val STRING_TEMPLATE: SyntaxElementType = KtStubElementTypes.STRING_TEMPLATE
    val LONG_STRING_TEMPLATE_ENTRY: SyntaxElementType = KtStubElementTypes.LONG_STRING_TEMPLATE_ENTRY
    val SHORT_STRING_TEMPLATE_ENTRY: SyntaxElementType = KtStubElementTypes.SHORT_STRING_TEMPLATE_ENTRY
    val LITERAL_STRING_TEMPLATE_ENTRY: SyntaxElementType = KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY
    val ESCAPE_STRING_TEMPLATE_ENTRY: SyntaxElementType = KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY
    val STRING_INTERPOLATION_PREFIX: SyntaxElementType = KtStubElementTypes.STRING_INTERPOLATION_PREFIX

    val REFERENCE_EXPRESSION: SyntaxElementType = KtStubElementTypes.REFERENCE_EXPRESSION
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: SyntaxElementType = KtStubElementTypes.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION

    val DOT_QUALIFIED_EXPRESSION: SyntaxElementType = KtStubElementTypes.DOT_QUALIFIED_EXPRESSION
    val CLASS_LITERAL_EXPRESSION: SyntaxElementType = KtStubElementTypes.CLASS_LITERAL_EXPRESSION

    val COLLECTION_LITERAL_EXPRESSION: SyntaxElementType = KtStubElementTypes.COLLECTION_LITERAL_EXPRESSION

    val PACKAGE_DIRECTIVE: SyntaxElementType = KtStubElementTypes.PACKAGE_DIRECTIVE

    val SCRIPT: SyntaxElementType = KtStubElementTypes.SCRIPT

    val TYPE_CODE_FRAGMENT: SyntaxElementType = KtStubElementTypes.TYPE_CODE_FRAGMENT
    val EXPRESSION_CODE_FRAGMENT: SyntaxElementType = KtStubElementTypes.EXPRESSION_CODE_FRAGMENT
    val BLOCK_CODE_FRAGMENT: SyntaxElementType = KtStubElementTypes.BLOCK_CODE_FRAGMENT

    const val KT_FILE_ID: Int = KtStubElementTypes.STRING_INTERPOLATION_PREFIX_ID + 1
    const val DESTRUCTURING_DECLARATION_ID: Int = KT_FILE_ID + 1
    const val DESTRUCTURING_DECLARATION_ENTRY_ID: Int = DESTRUCTURING_DECLARATION_ID + 1
    const val SCRIPT_INITIALIZER_ID: Int = DESTRUCTURING_DECLARATION_ENTRY_ID + 1
    const val PROPERTY_DELEGATE_ID: Int = SCRIPT_INITIALIZER_ID + 1
    const val CONSTRUCTOR_DELEGATION_CALL_ID: Int = PROPERTY_DELEGATE_ID + 1
    const val CONSTRUCTOR_DELEGATION_REFERENCE_ID: Int = CONSTRUCTOR_DELEGATION_CALL_ID + 1
    const val PARENTHESIZED_ID: Int = CONSTRUCTOR_DELEGATION_REFERENCE_ID + 1
    const val RETURN_ID: Int = PARENTHESIZED_ID + 1
    const val THROW_ID: Int = RETURN_ID + 1
    const val CONTINUE_ID: Int = THROW_ID + 1
    const val BREAK_ID: Int = CONTINUE_ID + 1
    const val IF_ID: Int = BREAK_ID + 1
    const val CONDITION_ID: Int = IF_ID + 1
    const val THEN_ID: Int = CONDITION_ID + 1
    const val ELSE_ID: Int = THEN_ID + 1
    const val TRY_ID: Int = ELSE_ID + 1
    const val CATCH_ID: Int = TRY_ID + 1
    const val FINALLY_ID: Int = CATCH_ID + 1
    const val FOR_ID: Int = FINALLY_ID + 1
    const val WHILE_ID: Int = FOR_ID + 1
    const val DO_WHILE_ID: Int = WHILE_ID + 1
    const val LOOP_RANGE_ID: Int = DO_WHILE_ID + 1
    const val BODY_ID: Int = LOOP_RANGE_ID + 1
    const val BLOCK_ID: Int = BODY_ID + 1
    const val LAMBDA_EXPRESSION_ID: Int = BLOCK_ID + 1
    const val FUNCTION_LITERAL_ID: Int = LAMBDA_EXPRESSION_ID + 1
    const val ANNOTATED_EXPRESSION_ID: Int = FUNCTION_LITERAL_ID + 1
    const val OPERATION_REFERENCE_ID: Int = ANNOTATED_EXPRESSION_ID + 1
    const val LABEL_ID: Int = OPERATION_REFERENCE_ID + 1
    const val LABEL_QUALIFIER_ID: Int = LABEL_ID + 1
    const val THIS_EXPRESSION_ID: Int = LABEL_QUALIFIER_ID + 1
    const val SUPER_EXPRESSION_ID: Int = THIS_EXPRESSION_ID + 1
    const val BINARY_EXPRESSION_ID: Int = SUPER_EXPRESSION_ID + 1
    const val BINARY_WITH_TYPE_ID: Int = BINARY_EXPRESSION_ID + 1
    const val IS_EXPRESSION_ID: Int = BINARY_WITH_TYPE_ID + 1
    const val PREFIX_EXPRESSION_ID: Int = IS_EXPRESSION_ID + 1
    const val POSTFIX_EXPRESSION_ID: Int = PREFIX_EXPRESSION_ID + 1
    const val LABELED_EXPRESSION_ID: Int = POSTFIX_EXPRESSION_ID + 1
    const val CALL_EXPRESSION_ID: Int = LABELED_EXPRESSION_ID + 1
    const val ARRAY_ACCESS_EXPRESSION_ID: Int = CALL_EXPRESSION_ID + 1
    const val INDICES_ID: Int = ARRAY_ACCESS_EXPRESSION_ID + 1
    const val CALLABLE_REFERENCE_EXPRESSION_ID: Int = INDICES_ID + 1
    const val SAFE_ACCESS_EXPRESSION_ID: Int = CALLABLE_REFERENCE_EXPRESSION_ID + 1
    const val OBJECT_LITERAL_ID: Int = SAFE_ACCESS_EXPRESSION_ID + 1
    const val WHEN_ID: Int = OBJECT_LITERAL_ID + 1
    const val WHEN_ENTRY_ID: Int = WHEN_ID + 1
    const val WHEN_ENTRY_GUARD_ID: Int = WHEN_ENTRY_ID + 1
    const val WHEN_CONDITION_IN_RANGE_ID: Int = WHEN_ENTRY_GUARD_ID + 1
    const val WHEN_CONDITION_IS_PATTERN_ID: Int = WHEN_CONDITION_IN_RANGE_ID + 1
    const val WHEN_CONDITION_EXPRESSION_ID: Int = WHEN_CONDITION_IS_PATTERN_ID + 1

    val KT_FILE: SyntaxElementType = register(KT_FILE_ID, "kotlin.FILE")

    val DESTRUCTURING_DECLARATION: SyntaxElementType = register(DESTRUCTURING_DECLARATION_ID, "DESTRUCTURING_DECLARATION")
    val DESTRUCTURING_DECLARATION_ENTRY: SyntaxElementType =
        register(DESTRUCTURING_DECLARATION_ENTRY_ID, "DESTRUCTURING_DECLARATION_ENTRY")
    val SCRIPT_INITIALIZER: SyntaxElementType = register(SCRIPT_INITIALIZER_ID, "SCRIPT_INITIALIZER")
    val PROPERTY_DELEGATE: SyntaxElementType = register(PROPERTY_DELEGATE_ID, "PROPERTY_DELEGATE")
    val CONSTRUCTOR_DELEGATION_CALL: SyntaxElementType = register(CONSTRUCTOR_DELEGATION_CALL_ID, "CONSTRUCTOR_DELEGATION_CALL")
    val CONSTRUCTOR_DELEGATION_REFERENCE: SyntaxElementType =
        register(CONSTRUCTOR_DELEGATION_REFERENCE_ID, "CONSTRUCTOR_DELEGATION_REFERENCE")

    val PARENTHESIZED: SyntaxElementType = register(PARENTHESIZED_ID, "PARENTHESIZED")
    val RETURN: SyntaxElementType = register(RETURN_ID, "RETURN")
    val THROW: SyntaxElementType = register(THROW_ID, "THROW")
    val CONTINUE: SyntaxElementType = register(CONTINUE_ID, "CONTINUE")
    val BREAK: SyntaxElementType = register(BREAK_ID, "BREAK")
    val IF: SyntaxElementType = register(IF_ID, "IF")
    val CONDITION: SyntaxElementType = register(CONDITION_ID, "CONDITION")
    val THEN: SyntaxElementType = register(THEN_ID, "THEN")
    val ELSE: SyntaxElementType = register(ELSE_ID, "ELSE")
    val TRY: SyntaxElementType = register(TRY_ID, "TRY")
    val CATCH: SyntaxElementType = register(CATCH_ID, "CATCH")
    val FINALLY: SyntaxElementType = register(FINALLY_ID, "FINALLY")
    val FOR: SyntaxElementType = register(FOR_ID, "FOR")
    val WHILE: SyntaxElementType = register(WHILE_ID, "WHILE")
    val DO_WHILE: SyntaxElementType = register(DO_WHILE_ID, "DO_WHILE")
    val LOOP_RANGE: SyntaxElementType = register(LOOP_RANGE_ID, "LOOP_RANGE")
    val BODY: SyntaxElementType = register(BODY_ID, "BODY")

    val BLOCK: SyntaxElementType = register(BLOCK_ID, "BLOCK")
    val LAMBDA_EXPRESSION: SyntaxElementType = register(LAMBDA_EXPRESSION_ID, "LAMBDA_EXPRESSION")
    val FUNCTION_LITERAL: SyntaxElementType = register(FUNCTION_LITERAL_ID, "FUNCTION_LITERAL")
    val ANNOTATED_EXPRESSION: SyntaxElementType = register(ANNOTATED_EXPRESSION_ID, "ANNOTATED_EXPRESSION")
    val OPERATION_REFERENCE: SyntaxElementType = register(OPERATION_REFERENCE_ID, "OPERATION_REFERENCE")
    val LABEL: SyntaxElementType = register(LABEL_ID, "LABEL")
    val LABEL_QUALIFIER: SyntaxElementType = register(LABEL_QUALIFIER_ID, "LABEL_QUALIFIER")

    val THIS_EXPRESSION: SyntaxElementType = register(THIS_EXPRESSION_ID, "THIS_EXPRESSION")
    val SUPER_EXPRESSION: SyntaxElementType = register(SUPER_EXPRESSION_ID, "SUPER_EXPRESSION")
    val BINARY_EXPRESSION: SyntaxElementType = register(BINARY_EXPRESSION_ID, "BINARY_EXPRESSION")
    val BINARY_WITH_TYPE: SyntaxElementType = register(BINARY_WITH_TYPE_ID, "BINARY_WITH_TYPE")
    val IS_EXPRESSION: SyntaxElementType = register(IS_EXPRESSION_ID, "IS_EXPRESSION")
    val PREFIX_EXPRESSION: SyntaxElementType = register(PREFIX_EXPRESSION_ID, "PREFIX_EXPRESSION")
    val POSTFIX_EXPRESSION: SyntaxElementType = register(POSTFIX_EXPRESSION_ID, "POSTFIX_EXPRESSION")
    val LABELED_EXPRESSION: SyntaxElementType = register(LABELED_EXPRESSION_ID, "LABELED_EXPRESSION")
    val CALL_EXPRESSION: SyntaxElementType = register(CALL_EXPRESSION_ID, "CALL_EXPRESSION")
    val ARRAY_ACCESS_EXPRESSION: SyntaxElementType = register(ARRAY_ACCESS_EXPRESSION_ID, "ARRAY_ACCESS_EXPRESSION")
    val INDICES: SyntaxElementType = register(INDICES_ID, "INDICES")
    val CALLABLE_REFERENCE_EXPRESSION: SyntaxElementType =
        register(CALLABLE_REFERENCE_EXPRESSION_ID, "CALLABLE_REFERENCE_EXPRESSION")
    val SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(SAFE_ACCESS_EXPRESSION_ID, "SAFE_ACCESS_EXPRESSION")

    val OBJECT_LITERAL: SyntaxElementType = register(OBJECT_LITERAL_ID, "OBJECT_LITERAL")
    val WHEN: SyntaxElementType = register(WHEN_ID, "WHEN")
    val WHEN_ENTRY: SyntaxElementType = register(WHEN_ENTRY_ID, "WHEN_ENTRY")
    val WHEN_ENTRY_GUARD: SyntaxElementType = register(WHEN_ENTRY_GUARD_ID, "WHEN_ENTRY_GUARD")
    val WHEN_CONDITION_IN_RANGE: SyntaxElementType = register(WHEN_CONDITION_IN_RANGE_ID, "WHEN_CONDITION_IN_RANGE")
    val WHEN_CONDITION_IS_PATTERN: SyntaxElementType = register(WHEN_CONDITION_IS_PATTERN_ID, "WHEN_CONDITION_IS_PATTERN")
    val WHEN_CONDITION_EXPRESSION: SyntaxElementType = register(WHEN_CONDITION_EXPRESSION_ID, "WHEN_CONDITION_WITH_EXPRESSION")
}
