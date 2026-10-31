/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds

@ApiStatus.Experimental
object KtNodeTypes : SyntaxElementTypesWithIds() {
    val FILE: SyntaxElementType = register(KtNodeTypeId.FILE, "kotlin.FILE")

    // Start of stub element types

    val CLASS: SyntaxElementType = register(KtNodeTypeId.CLASS, "CLASS")
    val FUN: SyntaxElementType = register(KtNodeTypeId.FUNCTION, "FUNCTION")
    val PROPERTY: SyntaxElementType = register(KtNodeTypeId.PROPERTY, "PROPERTY")
    val PROPERTY_ACCESSOR: SyntaxElementType = register(KtNodeTypeId.PROPERTY_ACCESSOR, "PROPERTY_ACCESSOR")
    val BACKING_FIELD: SyntaxElementType = register(KtNodeTypeId.BACKING_FIELD, "BACKING_FIELD")
    val TYPEALIAS: SyntaxElementType = register(KtNodeTypeId.TYPEALIAS, "TYPEALIAS")

    val ENUM_ENTRY: SyntaxElementType = register(KtNodeTypeId.ENUM_ENTRY, "ENUM_ENTRY")
    val OBJECT_DECLARATION: SyntaxElementType = register(KtNodeTypeId.OBJECT_DECLARATION, "OBJECT_DECLARATION")
    val CLASS_INITIALIZER: SyntaxElementType = register(KtNodeTypeId.CLASS_INITIALIZER, "CLASS_INITIALIZER")
    val SECONDARY_CONSTRUCTOR: SyntaxElementType = register(KtNodeTypeId.SECONDARY_CONSTRUCTOR, "SECONDARY_CONSTRUCTOR")
    val PRIMARY_CONSTRUCTOR: SyntaxElementType = register(KtNodeTypeId.PRIMARY_CONSTRUCTOR, "PRIMARY_CONSTRUCTOR")

    val VALUE_PARAMETER: SyntaxElementType = register(KtNodeTypeId.VALUE_PARAMETER, "VALUE_PARAMETER")
    val VALUE_PARAMETER_LIST: SyntaxElementType = register(KtNodeTypeId.VALUE_PARAMETER_LIST, "VALUE_PARAMETER_LIST")

    val TYPE_PARAMETER: SyntaxElementType = register(KtNodeTypeId.TYPE_PARAMETER, "TYPE_PARAMETER")
    val TYPE_PARAMETER_LIST: SyntaxElementType = register(KtNodeTypeId.TYPE_PARAMETER_LIST, "TYPE_PARAMETER_LIST")

    val ANNOTATION_ENTRY: SyntaxElementType = register(KtNodeTypeId.ANNOTATION_ENTRY, "ANNOTATION_ENTRY")
    val ANNOTATION: SyntaxElementType = register(KtNodeTypeId.ANNOTATION, "ANNOTATION")

    val ANNOTATION_TARGET: SyntaxElementType = register(KtNodeTypeId.ANNOTATION_TARGET, "ANNOTATION_TARGET")

    val CLASS_BODY: SyntaxElementType = register(KtNodeTypeId.CLASS_BODY, "CLASS_BODY")

    val COMPANION_BLOCK: SyntaxElementType = register(KtNodeTypeId.COMPANION_BLOCK, "COMPANION_BLOCK")

    val IMPORT_LIST: SyntaxElementType = register(KtNodeTypeId.IMPORT_LIST, "IMPORT_LIST")

    val FILE_ANNOTATION_LIST: SyntaxElementType = register(KtNodeTypeId.FILE_ANNOTATION_LIST, "FILE_ANNOTATION_LIST")

    val IMPORT_DIRECTIVE: SyntaxElementType = register(KtNodeTypeId.IMPORT_DIRECTIVE, "IMPORT_DIRECTIVE")

    val IMPORT_ALIAS: SyntaxElementType = register(KtNodeTypeId.IMPORT_ALIAS, "IMPORT_ALIAS")

    val PACKAGE_DIRECTIVE: SyntaxElementType = register(KtNodeTypeId.PACKAGE_DIRECTIVE, "PACKAGE_DIRECTIVE")

    val MODIFIER_LIST: SyntaxElementType = register(KtNodeTypeId.MODIFIER_LIST, "MODIFIER_LIST")

    val TYPE_CONSTRAINT_LIST: SyntaxElementType = register(KtNodeTypeId.TYPE_CONSTRAINT_LIST, "TYPE_CONSTRAINT_LIST")

    val TYPE_CONSTRAINT: SyntaxElementType = register(KtNodeTypeId.TYPE_CONSTRAINT, "TYPE_CONSTRAINT")

    val NULLABLE_TYPE: SyntaxElementType = register(KtNodeTypeId.NULLABLE_TYPE, "NULLABLE_TYPE")

    val INTERSECTION_TYPE: SyntaxElementType = register(KtNodeTypeId.INTERSECTION_TYPE, "INTERSECTION_TYPE")
    val UNION_TYPE: SyntaxElementType = register(KtNodeTypeId.UNION_TYPE, "UNION_TYPE")

    val TYPE_REFERENCE: SyntaxElementType = register(KtNodeTypeId.TYPE_REFERENCE, "TYPE_REFERENCE")

    val USER_TYPE: SyntaxElementType = register(KtNodeTypeId.USER_TYPE, "USER_TYPE")
    val DYNAMIC_TYPE: SyntaxElementType = register(KtNodeTypeId.DYNAMIC_TYPE, "DYNAMIC_TYPE")

    val FUNCTION_TYPE: SyntaxElementType = register(KtNodeTypeId.FUNCTION_TYPE, "FUNCTION_TYPE")

    val TYPE_CODE_FRAGMENT: SyntaxElementType = register(KtNodeTypeId.TYPE_CODE_FRAGMENT, "kotlin.TYPE_CODE_FRAGMENT")
    val EXPRESSION_CODE_FRAGMENT: SyntaxElementType = register(KtNodeTypeId.EXPRESSION_CODE_FRAGMENT, "kotlin.EXPRESSION_CODE_FRAGMENT")
    val BLOCK_CODE_FRAGMENT: SyntaxElementType = register(KtNodeTypeId.BLOCK_CODE_FRAGMENT, "kotlin.BLOCK_CODE_FRAGMENT")

    val TYPE_PROJECTION: SyntaxElementType = register(KtNodeTypeId.TYPE_PROJECTION, "TYPE_PROJECTION")

    val FUNCTION_TYPE_RECEIVER: SyntaxElementType = register(KtNodeTypeId.FUNCTION_TYPE_RECEIVER, "FUNCTION_TYPE_RECEIVER")

    val REFERENCE_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.REFERENCE_EXPRESSION, "REFERENCE_EXPRESSION")
    val DOT_QUALIFIED_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.DOT_QUALIFIED_EXPRESSION, "DOT_QUALIFIED_EXPRESSION")
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: SyntaxElementType =
        register(KtNodeTypeId.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION, "ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION")
    val TYPE_ARGUMENT_LIST: SyntaxElementType = register(KtNodeTypeId.TYPE_ARGUMENT_LIST, "TYPE_ARGUMENT_LIST")

    val VALUE_ARGUMENT_LIST: SyntaxElementType = register(KtNodeTypeId.VALUE_ARGUMENT_LIST, "VALUE_ARGUMENT_LIST")

    val VALUE_ARGUMENT: SyntaxElementType = register(KtNodeTypeId.VALUE_ARGUMENT, "VALUE_ARGUMENT")

    val CONTRACT_EFFECT_LIST: SyntaxElementType = register(KtNodeTypeId.CONTRACT_EFFECT_LIST, "CONTRACT_EFFECT_LIST")

    val CONTRACT_EFFECT: SyntaxElementType = register(KtNodeTypeId.CONTRACT_EFFECT, "CONTRACT_EFFECT")

    val LAMBDA_ARGUMENT: SyntaxElementType = register(KtNodeTypeId.LAMBDA_ARGUMENT, "LAMBDA_ARGUMENT")

    val VALUE_ARGUMENT_NAME: SyntaxElementType = register(KtNodeTypeId.VALUE_ARGUMENT_NAME, "VALUE_ARGUMENT_NAME")

    val SUPER_TYPE_LIST: SyntaxElementType = register(KtNodeTypeId.SUPER_TYPE_LIST, "SUPER_TYPE_LIST")

    val INITIALIZER_LIST: SyntaxElementType = register(KtNodeTypeId.INITIALIZER_LIST, "INITIALIZER_LIST")

    val DELEGATED_SUPER_TYPE_ENTRY: SyntaxElementType = register(KtNodeTypeId.DELEGATED_SUPER_TYPE_ENTRY, "DELEGATED_SUPER_TYPE_ENTRY")

    val SUPER_TYPE_CALL_ENTRY: SyntaxElementType = register(KtNodeTypeId.SUPER_TYPE_CALL_ENTRY, "SUPER_TYPE_CALL_ENTRY")
    val SUPER_TYPE_ENTRY: SyntaxElementType = register(KtNodeTypeId.SUPER_TYPE_ENTRY, "SUPER_TYPE_ENTRY")
    val CONSTRUCTOR_CALLEE: SyntaxElementType = register(KtNodeTypeId.CONSTRUCTOR_CALLEE, "CONSTRUCTOR_CALLEE")

    val CONTEXT_RECEIVER: SyntaxElementType = register(KtNodeTypeId.CONTEXT_RECEIVER, "CONTEXT_RECEIVER")
    val CONTEXT_PARAMETER_LIST: SyntaxElementType = register(KtNodeTypeId.CONTEXT_PARAMETER_LIST, "CONTEXT_PARAMETER_LIST")

    val NULL: SyntaxElementType = register(KtNodeTypeId.NULL, "NULL")
    val BOOLEAN_CONSTANT: SyntaxElementType = register(KtNodeTypeId.BOOLEAN_CONSTANT, "BOOLEAN_CONSTANT")
    val FLOAT_CONSTANT: SyntaxElementType = register(KtNodeTypeId.FLOAT_CONSTANT, "FLOAT_CONSTANT")
    val CHARACTER_CONSTANT: SyntaxElementType = register(KtNodeTypeId.CHARACTER_CONSTANT, "CHARACTER_CONSTANT")
    val INTEGER_CONSTANT: SyntaxElementType = register(KtNodeTypeId.INTEGER_CONSTANT, "INTEGER_CONSTANT")
    val CLASS_LITERAL_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.CLASS_LITERAL_EXPRESSION, "CLASS_LITERAL_EXPRESSION")
    val COLLECTION_LITERAL_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.COLLECTION_LITERAL_EXPRESSION, "COLLECTION_LITERAL_EXPRESSION")

    val STRING_TEMPLATE: SyntaxElementType = register(KtNodeTypeId.STRING_TEMPLATE, "STRING_TEMPLATE")

    val LONG_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KtNodeTypeId.LONG_STRING_TEMPLATE_ENTRY, "LONG_STRING_TEMPLATE_ENTRY")

    val SHORT_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KtNodeTypeId.SHORT_STRING_TEMPLATE_ENTRY, "SHORT_STRING_TEMPLATE_ENTRY")

    val LITERAL_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KtNodeTypeId.LITERAL_STRING_TEMPLATE_ENTRY, "LITERAL_STRING_TEMPLATE_ENTRY")

    val ESCAPE_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KtNodeTypeId.ESCAPE_STRING_TEMPLATE_ENTRY, "ESCAPE_STRING_TEMPLATE_ENTRY")

    val SCRIPT: SyntaxElementType = register(KtNodeTypeId.SCRIPT, "SCRIPT")

    val STRING_INTERPOLATION_PREFIX: SyntaxElementType = register(KtNodeTypeId.STRING_INTERPOLATION_PREFIX, "STRING_INTERPOLATION_PREFIX")

    // End of Stub element types

    val DESTRUCTURING_DECLARATION: SyntaxElementType = register(KtNodeTypeId.DESTRUCTURING_DECLARATION, "DESTRUCTURING_DECLARATION")
    val DESTRUCTURING_DECLARATION_ENTRY: SyntaxElementType =
        register(KtNodeTypeId.DESTRUCTURING_DECLARATION_ENTRY, "DESTRUCTURING_DECLARATION_ENTRY")
    val SCRIPT_INITIALIZER: SyntaxElementType = register(KtNodeTypeId.SCRIPT_INITIALIZER, "SCRIPT_INITIALIZER")
    val PROPERTY_DELEGATE: SyntaxElementType = register(KtNodeTypeId.PROPERTY_DELEGATE, "PROPERTY_DELEGATE")
    val CONSTRUCTOR_DELEGATION_CALL: SyntaxElementType = register(KtNodeTypeId.CONSTRUCTOR_DELEGATION_CALL, "CONSTRUCTOR_DELEGATION_CALL")
    val CONSTRUCTOR_DELEGATION_REFERENCE: SyntaxElementType =
        register(KtNodeTypeId.CONSTRUCTOR_DELEGATION_REFERENCE, "CONSTRUCTOR_DELEGATION_REFERENCE")

    val PARENTHESIZED: SyntaxElementType = register(KtNodeTypeId.PARENTHESIZED, "PARENTHESIZED")
    val RETURN: SyntaxElementType = register(KtNodeTypeId.RETURN, "RETURN")
    val THROW: SyntaxElementType = register(KtNodeTypeId.THROW, "THROW")
    val CONTINUE: SyntaxElementType = register(KtNodeTypeId.CONTINUE, "CONTINUE")
    val BREAK: SyntaxElementType = register(KtNodeTypeId.BREAK, "BREAK")
    val IF: SyntaxElementType = register(KtNodeTypeId.IF, "IF")
    val CONDITION: SyntaxElementType = register(KtNodeTypeId.CONDITION, "CONDITION")
    val THEN: SyntaxElementType = register(KtNodeTypeId.THEN, "THEN")
    val ELSE: SyntaxElementType = register(KtNodeTypeId.ELSE, "ELSE")
    val TRY: SyntaxElementType = register(KtNodeTypeId.TRY, "TRY")
    val CATCH: SyntaxElementType = register(KtNodeTypeId.CATCH, "CATCH")
    val FINALLY: SyntaxElementType = register(KtNodeTypeId.FINALLY, "FINALLY")
    val FOR: SyntaxElementType = register(KtNodeTypeId.FOR, "FOR")
    val WHILE: SyntaxElementType = register(KtNodeTypeId.WHILE, "WHILE")
    val DO_WHILE: SyntaxElementType = register(KtNodeTypeId.DO_WHILE, "DO_WHILE")
    val LOOP_RANGE: SyntaxElementType = register(KtNodeTypeId.LOOP_RANGE, "LOOP_RANGE")
    val BODY: SyntaxElementType = register(KtNodeTypeId.BODY, "BODY")

    val BLOCK: SyntaxElementType = register(KtNodeTypeId.BLOCK, "BLOCK")
    val LAMBDA_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.LAMBDA_EXPRESSION, "LAMBDA_EXPRESSION")
    val FUNCTION_LITERAL: SyntaxElementType = register(KtNodeTypeId.FUNCTION_LITERAL, "FUNCTION_LITERAL")
    val ANNOTATED_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.ANNOTATED_EXPRESSION, "ANNOTATED_EXPRESSION")
    val OPERATION_REFERENCE: SyntaxElementType = register(KtNodeTypeId.OPERATION_REFERENCE, "OPERATION_REFERENCE")
    val LABEL: SyntaxElementType = register(KtNodeTypeId.LABEL, "LABEL")
    val LABEL_QUALIFIER: SyntaxElementType = register(KtNodeTypeId.LABEL_QUALIFIER, "LABEL_QUALIFIER")

    val THIS_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.THIS_EXPRESSION, "THIS_EXPRESSION")
    val SUPER_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.SUPER_EXPRESSION, "SUPER_EXPRESSION")
    val BINARY_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.BINARY_EXPRESSION, "BINARY_EXPRESSION")
    val BINARY_WITH_TYPE: SyntaxElementType = register(KtNodeTypeId.BINARY_WITH_TYPE, "BINARY_WITH_TYPE")
    val IS_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.IS_EXPRESSION, "IS_EXPRESSION")
    val PREFIX_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.PREFIX_EXPRESSION, "PREFIX_EXPRESSION")
    val POSTFIX_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.POSTFIX_EXPRESSION, "POSTFIX_EXPRESSION")
    val LABELED_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.LABELED_EXPRESSION, "LABELED_EXPRESSION")
    val CALL_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.CALL_EXPRESSION, "CALL_EXPRESSION")
    val ARRAY_ACCESS_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.ARRAY_ACCESS_EXPRESSION, "ARRAY_ACCESS_EXPRESSION")
    val INDICES: SyntaxElementType = register(KtNodeTypeId.INDICES, "INDICES")
    val CALLABLE_REFERENCE_EXPRESSION: SyntaxElementType =
        register(KtNodeTypeId.CALLABLE_REFERENCE_EXPRESSION, "CALLABLE_REFERENCE_EXPRESSION")
    val SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.SAFE_ACCESS_EXPRESSION, "SAFE_ACCESS_EXPRESSION")
    val ERROR_SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.ERROR_SAFE_ACCESS_EXPRESSION, "ERROR_SAFE_ACCESS_EXPRESSION")

    val OBJECT_LITERAL: SyntaxElementType = register(KtNodeTypeId.OBJECT_LITERAL, "OBJECT_LITERAL")
    val WHEN: SyntaxElementType = register(KtNodeTypeId.WHEN, "WHEN")
    val WHEN_ENTRY: SyntaxElementType = register(KtNodeTypeId.WHEN_ENTRY, "WHEN_ENTRY")
    val WHEN_ENTRY_GUARD: SyntaxElementType = register(KtNodeTypeId.WHEN_ENTRY_GUARD, "WHEN_ENTRY_GUARD")
    val WHEN_CONDITION_IN_RANGE: SyntaxElementType = register(KtNodeTypeId.WHEN_CONDITION_IN_RANGE, "WHEN_CONDITION_IN_RANGE")
    val WHEN_CONDITION_IS_PATTERN: SyntaxElementType = register(KtNodeTypeId.WHEN_CONDITION_IS_PATTERN, "WHEN_CONDITION_IS_PATTERN")
    val WHEN_CONDITION_EXPRESSION: SyntaxElementType = register(KtNodeTypeId.WHEN_CONDITION_EXPRESSION, "WHEN_CONDITION_WITH_EXPRESSION")
}
