/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.syntax.SyntaxNodeTypeIds

@ApiStatus.Experimental
object KtNodeTypes : SyntaxElementTypesWithIds() {
    val FILE: SyntaxElementType = register(SyntaxNodeTypeIds.FILE, "kotlin.FILE")

    // Start of stub element types

    val CLASS: SyntaxElementType = register(SyntaxNodeTypeIds.CLASS, "CLASS")
    val FUN: SyntaxElementType = register(SyntaxNodeTypeIds.FUNCTION, "FUNCTION")
    val PROPERTY: SyntaxElementType = register(SyntaxNodeTypeIds.PROPERTY, "PROPERTY")
    val PROPERTY_ACCESSOR: SyntaxElementType = register(SyntaxNodeTypeIds.PROPERTY_ACCESSOR, "PROPERTY_ACCESSOR")
    val BACKING_FIELD: SyntaxElementType = register(SyntaxNodeTypeIds.BACKING_FIELD, "BACKING_FIELD")
    val TYPEALIAS: SyntaxElementType = register(SyntaxNodeTypeIds.TYPEALIAS, "TYPEALIAS")

    val ENUM_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.ENUM_ENTRY, "ENUM_ENTRY")
    val OBJECT_DECLARATION: SyntaxElementType = register(SyntaxNodeTypeIds.OBJECT_DECLARATION, "OBJECT_DECLARATION")
    val CLASS_INITIALIZER: SyntaxElementType = register(SyntaxNodeTypeIds.CLASS_INITIALIZER, "CLASS_INITIALIZER")
    val SECONDARY_CONSTRUCTOR: SyntaxElementType = register(SyntaxNodeTypeIds.SECONDARY_CONSTRUCTOR, "SECONDARY_CONSTRUCTOR")
    val PRIMARY_CONSTRUCTOR: SyntaxElementType = register(SyntaxNodeTypeIds.PRIMARY_CONSTRUCTOR, "PRIMARY_CONSTRUCTOR")

    val VALUE_PARAMETER: SyntaxElementType = register(SyntaxNodeTypeIds.VALUE_PARAMETER, "VALUE_PARAMETER")
    val VALUE_PARAMETER_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.VALUE_PARAMETER_LIST, "VALUE_PARAMETER_LIST")

    val TYPE_PARAMETER: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_PARAMETER, "TYPE_PARAMETER")
    val TYPE_PARAMETER_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_PARAMETER_LIST, "TYPE_PARAMETER_LIST")

    val ANNOTATION_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.ANNOTATION_ENTRY, "ANNOTATION_ENTRY")
    val ANNOTATION: SyntaxElementType = register(SyntaxNodeTypeIds.ANNOTATION, "ANNOTATION")

    val ANNOTATION_TARGET: SyntaxElementType = register(SyntaxNodeTypeIds.ANNOTATION_TARGET, "ANNOTATION_TARGET")

    val CLASS_BODY: SyntaxElementType = register(SyntaxNodeTypeIds.CLASS_BODY, "CLASS_BODY")

    val COMPANION_BLOCK: SyntaxElementType = register(SyntaxNodeTypeIds.COMPANION_BLOCK, "COMPANION_BLOCK")

    val IMPORT_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.IMPORT_LIST, "IMPORT_LIST")

    val FILE_ANNOTATION_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.FILE_ANNOTATION_LIST, "FILE_ANNOTATION_LIST")

    val IMPORT_DIRECTIVE: SyntaxElementType = register(SyntaxNodeTypeIds.IMPORT_DIRECTIVE, "IMPORT_DIRECTIVE")

    val IMPORT_ALIAS: SyntaxElementType = register(SyntaxNodeTypeIds.IMPORT_ALIAS, "IMPORT_ALIAS")

    val PACKAGE_DIRECTIVE: SyntaxElementType = register(SyntaxNodeTypeIds.PACKAGE_DIRECTIVE, "PACKAGE_DIRECTIVE")

    val MODIFIER_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.MODIFIER_LIST, "MODIFIER_LIST")

    val TYPE_CONSTRAINT_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_CONSTRAINT_LIST, "TYPE_CONSTRAINT_LIST")

    val TYPE_CONSTRAINT: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_CONSTRAINT, "TYPE_CONSTRAINT")

    val NULLABLE_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.NULLABLE_TYPE, "NULLABLE_TYPE")

    val INTERSECTION_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.INTERSECTION_TYPE, "INTERSECTION_TYPE")
    val UNION_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.UNION_TYPE, "UNION_TYPE")

    val TYPE_REFERENCE: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_REFERENCE, "TYPE_REFERENCE")

    val USER_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.USER_TYPE, "USER_TYPE")
    val DYNAMIC_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.DYNAMIC_TYPE, "DYNAMIC_TYPE")

    val FUNCTION_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.FUNCTION_TYPE, "FUNCTION_TYPE")

    val TYPE_CODE_FRAGMENT: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_CODE_FRAGMENT, "kotlin.TYPE_CODE_FRAGMENT")
    val EXPRESSION_CODE_FRAGMENT: SyntaxElementType = register(SyntaxNodeTypeIds.EXPRESSION_CODE_FRAGMENT, "kotlin.EXPRESSION_CODE_FRAGMENT")
    val BLOCK_CODE_FRAGMENT: SyntaxElementType = register(SyntaxNodeTypeIds.BLOCK_CODE_FRAGMENT, "kotlin.BLOCK_CODE_FRAGMENT")

    val TYPE_PROJECTION: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_PROJECTION, "TYPE_PROJECTION")

    val FUNCTION_TYPE_RECEIVER: SyntaxElementType = register(SyntaxNodeTypeIds.FUNCTION_TYPE_RECEIVER, "FUNCTION_TYPE_RECEIVER")

    val REFERENCE_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.REFERENCE_EXPRESSION, "REFERENCE_EXPRESSION")
    val DOT_QUALIFIED_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION, "DOT_QUALIFIED_EXPRESSION")
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: SyntaxElementType =
        register(SyntaxNodeTypeIds.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION, "ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION")
    val TYPE_ARGUMENT_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.TYPE_ARGUMENT_LIST, "TYPE_ARGUMENT_LIST")

    val VALUE_ARGUMENT_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.VALUE_ARGUMENT_LIST, "VALUE_ARGUMENT_LIST")

    val VALUE_ARGUMENT: SyntaxElementType = register(SyntaxNodeTypeIds.VALUE_ARGUMENT, "VALUE_ARGUMENT")

    val CONTRACT_EFFECT_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.CONTRACT_EFFECT_LIST, "CONTRACT_EFFECT_LIST")

    val CONTRACT_EFFECT: SyntaxElementType = register(SyntaxNodeTypeIds.CONTRACT_EFFECT, "CONTRACT_EFFECT")

    val LAMBDA_ARGUMENT: SyntaxElementType = register(SyntaxNodeTypeIds.LAMBDA_ARGUMENT, "LAMBDA_ARGUMENT")

    val VALUE_ARGUMENT_NAME: SyntaxElementType = register(SyntaxNodeTypeIds.VALUE_ARGUMENT_NAME, "VALUE_ARGUMENT_NAME")

    val SUPER_TYPE_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.SUPER_TYPE_LIST, "SUPER_TYPE_LIST")

    val INITIALIZER_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.INITIALIZER_LIST, "INITIALIZER_LIST")

    val DELEGATED_SUPER_TYPE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.DELEGATED_SUPER_TYPE_ENTRY, "DELEGATED_SUPER_TYPE_ENTRY")

    val SUPER_TYPE_CALL_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.SUPER_TYPE_CALL_ENTRY, "SUPER_TYPE_CALL_ENTRY")
    val SUPER_TYPE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.SUPER_TYPE_ENTRY, "SUPER_TYPE_ENTRY")
    val CONSTRUCTOR_CALLEE: SyntaxElementType = register(SyntaxNodeTypeIds.CONSTRUCTOR_CALLEE, "CONSTRUCTOR_CALLEE")

    val CONTEXT_RECEIVER: SyntaxElementType = register(SyntaxNodeTypeIds.CONTEXT_RECEIVER, "CONTEXT_RECEIVER")
    val CONTEXT_PARAMETER_LIST: SyntaxElementType = register(SyntaxNodeTypeIds.CONTEXT_PARAMETER_LIST, "CONTEXT_PARAMETER_LIST")

    val NULL: SyntaxElementType = register(SyntaxNodeTypeIds.NULL, "NULL")
    val BOOLEAN_CONSTANT: SyntaxElementType = register(SyntaxNodeTypeIds.BOOLEAN_CONSTANT, "BOOLEAN_CONSTANT")
    val FLOAT_CONSTANT: SyntaxElementType = register(SyntaxNodeTypeIds.FLOAT_CONSTANT, "FLOAT_CONSTANT")
    val CHARACTER_CONSTANT: SyntaxElementType = register(SyntaxNodeTypeIds.CHARACTER_CONSTANT, "CHARACTER_CONSTANT")
    val INTEGER_CONSTANT: SyntaxElementType = register(SyntaxNodeTypeIds.INTEGER_CONSTANT, "INTEGER_CONSTANT")
    val CLASS_LITERAL_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.CLASS_LITERAL_EXPRESSION, "CLASS_LITERAL_EXPRESSION")
    val COLLECTION_LITERAL_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.COLLECTION_LITERAL_EXPRESSION, "COLLECTION_LITERAL_EXPRESSION")

    val STRING_TEMPLATE: SyntaxElementType = register(SyntaxNodeTypeIds.STRING_TEMPLATE, "STRING_TEMPLATE")

    val LONG_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY, "LONG_STRING_TEMPLATE_ENTRY")

    val SHORT_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY, "SHORT_STRING_TEMPLATE_ENTRY")

    val LITERAL_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY, "LITERAL_STRING_TEMPLATE_ENTRY")

    val ESCAPE_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY, "ESCAPE_STRING_TEMPLATE_ENTRY")

    val SCRIPT: SyntaxElementType = register(SyntaxNodeTypeIds.SCRIPT, "SCRIPT")

    val STRING_INTERPOLATION_PREFIX: SyntaxElementType = register(SyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX, "STRING_INTERPOLATION_PREFIX")

    // End of Stub element types

    val DESTRUCTURING_DECLARATION: SyntaxElementType = register(SyntaxNodeTypeIds.DESTRUCTURING_DECLARATION, "DESTRUCTURING_DECLARATION")
    val DESTRUCTURING_DECLARATION_ENTRY: SyntaxElementType =
        register(SyntaxNodeTypeIds.DESTRUCTURING_DECLARATION_ENTRY, "DESTRUCTURING_DECLARATION_ENTRY")
    val SCRIPT_INITIALIZER: SyntaxElementType = register(SyntaxNodeTypeIds.SCRIPT_INITIALIZER, "SCRIPT_INITIALIZER")
    val PROPERTY_DELEGATE: SyntaxElementType = register(SyntaxNodeTypeIds.PROPERTY_DELEGATE, "PROPERTY_DELEGATE")
    val CONSTRUCTOR_DELEGATION_CALL: SyntaxElementType = register(SyntaxNodeTypeIds.CONSTRUCTOR_DELEGATION_CALL, "CONSTRUCTOR_DELEGATION_CALL")
    val CONSTRUCTOR_DELEGATION_REFERENCE: SyntaxElementType =
        register(SyntaxNodeTypeIds.CONSTRUCTOR_DELEGATION_REFERENCE, "CONSTRUCTOR_DELEGATION_REFERENCE")

    val PARENTHESIZED: SyntaxElementType = register(SyntaxNodeTypeIds.PARENTHESIZED, "PARENTHESIZED")
    val RETURN: SyntaxElementType = register(SyntaxNodeTypeIds.RETURN, "RETURN")
    val THROW: SyntaxElementType = register(SyntaxNodeTypeIds.THROW, "THROW")
    val CONTINUE: SyntaxElementType = register(SyntaxNodeTypeIds.CONTINUE, "CONTINUE")
    val BREAK: SyntaxElementType = register(SyntaxNodeTypeIds.BREAK, "BREAK")
    val IF: SyntaxElementType = register(SyntaxNodeTypeIds.IF, "IF")
    val CONDITION: SyntaxElementType = register(SyntaxNodeTypeIds.CONDITION, "CONDITION")
    val THEN: SyntaxElementType = register(SyntaxNodeTypeIds.THEN, "THEN")
    val ELSE: SyntaxElementType = register(SyntaxNodeTypeIds.ELSE, "ELSE")
    val TRY: SyntaxElementType = register(SyntaxNodeTypeIds.TRY, "TRY")
    val CATCH: SyntaxElementType = register(SyntaxNodeTypeIds.CATCH, "CATCH")
    val FINALLY: SyntaxElementType = register(SyntaxNodeTypeIds.FINALLY, "FINALLY")
    val FOR: SyntaxElementType = register(SyntaxNodeTypeIds.FOR, "FOR")
    val WHILE: SyntaxElementType = register(SyntaxNodeTypeIds.WHILE, "WHILE")
    val DO_WHILE: SyntaxElementType = register(SyntaxNodeTypeIds.DO_WHILE, "DO_WHILE")
    val LOOP_RANGE: SyntaxElementType = register(SyntaxNodeTypeIds.LOOP_RANGE, "LOOP_RANGE")
    val BODY: SyntaxElementType = register(SyntaxNodeTypeIds.BODY, "BODY")

    val BLOCK: SyntaxElementType = register(SyntaxNodeTypeIds.BLOCK, "BLOCK")
    val LAMBDA_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.LAMBDA_EXPRESSION, "LAMBDA_EXPRESSION")
    val FUNCTION_LITERAL: SyntaxElementType = register(SyntaxNodeTypeIds.FUNCTION_LITERAL, "FUNCTION_LITERAL")
    val ANNOTATED_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.ANNOTATED_EXPRESSION, "ANNOTATED_EXPRESSION")
    val OPERATION_REFERENCE: SyntaxElementType = register(SyntaxNodeTypeIds.OPERATION_REFERENCE, "OPERATION_REFERENCE")
    val LABEL: SyntaxElementType = register(SyntaxNodeTypeIds.LABEL, "LABEL")
    val LABEL_QUALIFIER: SyntaxElementType = register(SyntaxNodeTypeIds.LABEL_QUALIFIER, "LABEL_QUALIFIER")

    val THIS_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.THIS_EXPRESSION, "THIS_EXPRESSION")
    val SUPER_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.SUPER_EXPRESSION, "SUPER_EXPRESSION")
    val BINARY_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.BINARY_EXPRESSION, "BINARY_EXPRESSION")
    val BINARY_WITH_TYPE: SyntaxElementType = register(SyntaxNodeTypeIds.BINARY_WITH_TYPE, "BINARY_WITH_TYPE")
    val IS_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.IS_EXPRESSION, "IS_EXPRESSION")
    val PREFIX_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.PREFIX_EXPRESSION, "PREFIX_EXPRESSION")
    val POSTFIX_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.POSTFIX_EXPRESSION, "POSTFIX_EXPRESSION")
    val LABELED_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.LABELED_EXPRESSION, "LABELED_EXPRESSION")
    val CALL_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.CALL_EXPRESSION, "CALL_EXPRESSION")
    val ARRAY_ACCESS_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.ARRAY_ACCESS_EXPRESSION, "ARRAY_ACCESS_EXPRESSION")
    val INDICES: SyntaxElementType = register(SyntaxNodeTypeIds.INDICES, "INDICES")
    val CALLABLE_REFERENCE_EXPRESSION: SyntaxElementType =
        register(SyntaxNodeTypeIds.CALLABLE_REFERENCE_EXPRESSION, "CALLABLE_REFERENCE_EXPRESSION")
    val SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.SAFE_ACCESS_EXPRESSION, "SAFE_ACCESS_EXPRESSION")
    val ERROR_SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.ERROR_SAFE_ACCESS_EXPRESSION, "ERROR_SAFE_ACCESS_EXPRESSION")

    val OBJECT_LITERAL: SyntaxElementType = register(SyntaxNodeTypeIds.OBJECT_LITERAL, "OBJECT_LITERAL")
    val WHEN: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN, "WHEN")
    val WHEN_ENTRY: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN_ENTRY, "WHEN_ENTRY")
    val WHEN_ENTRY_GUARD: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN_ENTRY_GUARD, "WHEN_ENTRY_GUARD")
    val WHEN_CONDITION_IN_RANGE: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN_CONDITION_IN_RANGE, "WHEN_CONDITION_IN_RANGE")
    val WHEN_CONDITION_IS_PATTERN: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN_CONDITION_IS_PATTERN, "WHEN_CONDITION_IS_PATTERN")
    val WHEN_CONDITION_EXPRESSION: SyntaxElementType = register(SyntaxNodeTypeIds.WHEN_CONDITION_EXPRESSION, "WHEN_CONDITION_WITH_EXPRESSION")
}
