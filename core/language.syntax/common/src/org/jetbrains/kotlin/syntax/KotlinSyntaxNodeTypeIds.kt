/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

public object KotlinSyntaxNodeTypeIds {
    public const val FILE: SyntaxId = KotlinSyntaxTokenIds.ERROR_SAFE_ACCESS + 1

    public const val CLASS: SyntaxId = FILE + 1
    public const val FUNCTION: SyntaxId = CLASS + 1
    public const val PROPERTY: SyntaxId = FUNCTION + 1
    public const val DESTRUCTURING_DECLARATION: SyntaxId = PROPERTY + 1
    public const val DESTRUCTURING_DECLARATION_ENTRY: SyntaxId = DESTRUCTURING_DECLARATION + 1

    public const val OBJECT_DECLARATION: SyntaxId = DESTRUCTURING_DECLARATION_ENTRY + 1
    public const val TYPEALIAS: SyntaxId = OBJECT_DECLARATION + 1
    public const val COMPANION_BLOCK: SyntaxId = TYPEALIAS + 1

    public const val ENUM_ENTRY: SyntaxId = COMPANION_BLOCK + 1
    public const val CLASS_INITIALIZER: SyntaxId = ENUM_ENTRY + 1
    public const val SCRIPT_INITIALIZER: SyntaxId = CLASS_INITIALIZER + 1
    public const val SECONDARY_CONSTRUCTOR: SyntaxId = SCRIPT_INITIALIZER + 1
    public const val PRIMARY_CONSTRUCTOR: SyntaxId = SECONDARY_CONSTRUCTOR + 1
    public const val CONTEXT_RECEIVER: SyntaxId = PRIMARY_CONSTRUCTOR + 1
    public const val CONTEXT_PARAMETER_LIST: SyntaxId = CONTEXT_RECEIVER + 1
    public const val TYPE_PARAMETER_LIST: SyntaxId = CONTEXT_PARAMETER_LIST + 1
    public const val TYPE_PARAMETER: SyntaxId = TYPE_PARAMETER_LIST + 1
    public const val SUPER_TYPE_LIST: SyntaxId = TYPE_PARAMETER + 1
    public const val DELEGATED_SUPER_TYPE_ENTRY: SyntaxId = SUPER_TYPE_LIST + 1
    public const val SUPER_TYPE_CALL_ENTRY: SyntaxId = DELEGATED_SUPER_TYPE_ENTRY + 1
    public const val SUPER_TYPE_ENTRY: SyntaxId = SUPER_TYPE_CALL_ENTRY + 1
    public const val PROPERTY_DELEGATE: SyntaxId = SUPER_TYPE_ENTRY + 1
    public const val CONSTRUCTOR_CALLEE: SyntaxId = PROPERTY_DELEGATE + 1
    public const val VALUE_PARAMETER_LIST: SyntaxId = CONSTRUCTOR_CALLEE + 1
    public const val VALUE_PARAMETER: SyntaxId = VALUE_PARAMETER_LIST + 1

    public const val CLASS_BODY: SyntaxId = VALUE_PARAMETER + 1
    public const val IMPORT_LIST: SyntaxId = CLASS_BODY + 1
    public const val FILE_ANNOTATION_LIST: SyntaxId = IMPORT_LIST + 1
    public const val IMPORT_DIRECTIVE: SyntaxId = FILE_ANNOTATION_LIST + 1
    public const val IMPORT_ALIAS: SyntaxId = IMPORT_DIRECTIVE + 1
    public const val MODIFIER_LIST: SyntaxId = IMPORT_ALIAS + 1
    public const val ANNOTATION: SyntaxId = MODIFIER_LIST + 1
    public const val ANNOTATION_ENTRY: SyntaxId = ANNOTATION + 1
    public const val ANNOTATION_TARGET: SyntaxId = ANNOTATION_ENTRY + 1

    public const val TYPE_ARGUMENT_LIST: SyntaxId = ANNOTATION_TARGET + 1
    public const val VALUE_ARGUMENT_LIST: SyntaxId = TYPE_ARGUMENT_LIST + 1
    public const val VALUE_ARGUMENT: SyntaxId = VALUE_ARGUMENT_LIST + 1
    public const val CONTRACT_EFFECT_LIST: SyntaxId = VALUE_ARGUMENT + 1
    public const val CONTRACT_EFFECT: SyntaxId = CONTRACT_EFFECT_LIST + 1
    public const val LAMBDA_ARGUMENT: SyntaxId = CONTRACT_EFFECT + 1
    public const val VALUE_ARGUMENT_NAME: SyntaxId = LAMBDA_ARGUMENT + 1
    public const val TYPE_REFERENCE: SyntaxId = VALUE_ARGUMENT_NAME + 1

    public const val USER_TYPE: SyntaxId = TYPE_REFERENCE + 1
    public const val DYNAMIC_TYPE: SyntaxId = USER_TYPE + 1
    public const val FUNCTION_TYPE: SyntaxId = DYNAMIC_TYPE + 1
    public const val FUNCTION_TYPE_RECEIVER: SyntaxId = FUNCTION_TYPE + 1
    public const val NULLABLE_TYPE: SyntaxId = FUNCTION_TYPE_RECEIVER + 1
    public const val INTERSECTION_TYPE: SyntaxId = NULLABLE_TYPE + 1
    public const val UNION_TYPE: SyntaxId = INTERSECTION_TYPE + 1
    public const val TYPE_PROJECTION: SyntaxId = UNION_TYPE + 1

    public const val PROPERTY_ACCESSOR: SyntaxId = TYPE_PROJECTION + 1
    public const val BACKING_FIELD: SyntaxId = PROPERTY_ACCESSOR + 1
    public const val INITIALIZER_LIST: SyntaxId = BACKING_FIELD + 1
    public const val TYPE_CONSTRAINT_LIST: SyntaxId = INITIALIZER_LIST + 1
    public const val TYPE_CONSTRAINT: SyntaxId = TYPE_CONSTRAINT_LIST + 1

    public const val CONSTRUCTOR_DELEGATION_CALL: SyntaxId = TYPE_CONSTRAINT + 1
    public const val CONSTRUCTOR_DELEGATION_REFERENCE: SyntaxId = CONSTRUCTOR_DELEGATION_CALL + 1

    public const val NULL: SyntaxId = CONSTRUCTOR_DELEGATION_REFERENCE + 1
    public const val BOOLEAN_CONSTANT: SyntaxId = NULL + 1
    public const val FLOAT_CONSTANT: SyntaxId = BOOLEAN_CONSTANT + 1
    public const val CHARACTER_CONSTANT: SyntaxId = FLOAT_CONSTANT + 1
    public const val INTEGER_CONSTANT: SyntaxId = CHARACTER_CONSTANT + 1

    public const val STRING_TEMPLATE: SyntaxId = INTEGER_CONSTANT + 1
    public const val LONG_STRING_TEMPLATE_ENTRY: SyntaxId = STRING_TEMPLATE + 1
    public const val SHORT_STRING_TEMPLATE_ENTRY: SyntaxId = LONG_STRING_TEMPLATE_ENTRY + 1
    public const val LITERAL_STRING_TEMPLATE_ENTRY: SyntaxId = SHORT_STRING_TEMPLATE_ENTRY + 1
    public const val ESCAPE_STRING_TEMPLATE_ENTRY: SyntaxId = LITERAL_STRING_TEMPLATE_ENTRY + 1
    public const val STRING_INTERPOLATION_PREFIX: SyntaxId = ESCAPE_STRING_TEMPLATE_ENTRY + 1

    public const val PARENTHESIZED: SyntaxId = STRING_INTERPOLATION_PREFIX + 1
    public const val RETURN: SyntaxId = PARENTHESIZED + 1
    public const val THROW: SyntaxId = RETURN + 1
    public const val CONTINUE: SyntaxId = THROW + 1
    public const val BREAK: SyntaxId = CONTINUE + 1
    public const val IF: SyntaxId = BREAK + 1
    public const val CONDITION: SyntaxId = IF + 1
    public const val THEN: SyntaxId = CONDITION + 1
    public const val ELSE: SyntaxId = THEN + 1
    public const val TRY: SyntaxId = ELSE + 1
    public const val CATCH: SyntaxId = TRY + 1
    public const val FINALLY: SyntaxId = CATCH + 1
    public const val FOR: SyntaxId = FINALLY + 1
    public const val WHILE: SyntaxId = FOR + 1
    public const val DO_WHILE: SyntaxId = WHILE + 1
    public const val LOOP_RANGE: SyntaxId = DO_WHILE + 1
    public const val BODY: SyntaxId = LOOP_RANGE + 1

    public const val BLOCK: SyntaxId = BODY + 1

    public const val LAMBDA_EXPRESSION: SyntaxId = BLOCK + 1

    public const val FUNCTION_LITERAL: SyntaxId = LAMBDA_EXPRESSION + 1
    public const val ANNOTATED_EXPRESSION: SyntaxId = FUNCTION_LITERAL + 1

    public const val REFERENCE_EXPRESSION: SyntaxId = ANNOTATED_EXPRESSION + 1
    public const val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: SyntaxId = REFERENCE_EXPRESSION + 1
    public const val OPERATION_REFERENCE: SyntaxId = ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION + 1
    public const val LABEL: SyntaxId = OPERATION_REFERENCE + 1

    public const val LABEL_QUALIFIER: SyntaxId = LABEL + 1

    public const val THIS_EXPRESSION: SyntaxId = LABEL_QUALIFIER + 1
    public const val SUPER_EXPRESSION: SyntaxId = THIS_EXPRESSION + 1
    public const val BINARY_EXPRESSION: SyntaxId = SUPER_EXPRESSION + 1
    public const val BINARY_WITH_TYPE: SyntaxId = BINARY_EXPRESSION + 1
    public const val IS_EXPRESSION: SyntaxId = BINARY_WITH_TYPE + 1
    public const val PREFIX_EXPRESSION: SyntaxId = IS_EXPRESSION + 1
    public const val POSTFIX_EXPRESSION: SyntaxId = PREFIX_EXPRESSION + 1
    public const val LABELED_EXPRESSION: SyntaxId = POSTFIX_EXPRESSION + 1
    public const val CALL_EXPRESSION: SyntaxId = LABELED_EXPRESSION + 1
    public const val ARRAY_ACCESS_EXPRESSION: SyntaxId = CALL_EXPRESSION + 1
    public const val INDICES: SyntaxId = ARRAY_ACCESS_EXPRESSION + 1
    public const val DOT_QUALIFIED_EXPRESSION: SyntaxId = INDICES + 1
    public const val CALLABLE_REFERENCE_EXPRESSION: SyntaxId = DOT_QUALIFIED_EXPRESSION + 1
    public const val CLASS_LITERAL_EXPRESSION: SyntaxId = CALLABLE_REFERENCE_EXPRESSION + 1
    public const val SAFE_ACCESS_EXPRESSION: SyntaxId = CLASS_LITERAL_EXPRESSION + 1
    public const val ERROR_SAFE_ACCESS_EXPRESSION: SyntaxId = SAFE_ACCESS_EXPRESSION + 1

    public const val OBJECT_LITERAL: SyntaxId = ERROR_SAFE_ACCESS_EXPRESSION + 1

    public const val WHEN: SyntaxId = OBJECT_LITERAL + 1
    public const val WHEN_ENTRY: SyntaxId = WHEN + 1
    public const val WHEN_ENTRY_GUARD: SyntaxId = WHEN_ENTRY + 1

    public const val WHEN_CONDITION_IN_RANGE: SyntaxId = WHEN_ENTRY_GUARD + 1
    public const val WHEN_CONDITION_IS_PATTERN: SyntaxId = WHEN_CONDITION_IN_RANGE + 1
    public const val WHEN_CONDITION_EXPRESSION: SyntaxId = WHEN_CONDITION_IS_PATTERN + 1

    public const val COLLECTION_LITERAL_EXPRESSION: SyntaxId = WHEN_CONDITION_EXPRESSION + 1

    public const val PACKAGE_DIRECTIVE: SyntaxId = COLLECTION_LITERAL_EXPRESSION + 1
    public const val SCRIPT: SyntaxId = PACKAGE_DIRECTIVE + 1

    public const val TYPE_CODE_FRAGMENT: SyntaxId = SCRIPT + 1
    public const val EXPRESSION_CODE_FRAGMENT: SyntaxId = TYPE_CODE_FRAGMENT + 1
    public const val BLOCK_CODE_FRAGMENT: SyntaxId = EXPRESSION_CODE_FRAGMENT + 1
    /**
     * Remember to update first const in [KDocSyntaxTokenIds] after adding new tokens here ^^^
     */
}
