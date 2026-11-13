/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds

@ApiStatus.Experimental
object KtNodeTypes : SyntaxElementTypesWithIds() {
    val FILE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FILE, "kotlin.FILE")

    // Start of stub element types

    val CLASS: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CLASS, "CLASS")
    val FUN: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FUNCTION, "FUNCTION")
    val PROPERTY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PROPERTY, "PROPERTY")
    val PROPERTY_ACCESSOR: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PROPERTY_ACCESSOR, "PROPERTY_ACCESSOR")
    val BACKING_FIELD: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BACKING_FIELD, "BACKING_FIELD")
    val TYPEALIAS: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPEALIAS, "TYPEALIAS")

    val ENUM_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ENUM_ENTRY, "ENUM_ENTRY")
    val OBJECT_DECLARATION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.OBJECT_DECLARATION, "OBJECT_DECLARATION")
    val CLASS_INITIALIZER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CLASS_INITIALIZER, "CLASS_INITIALIZER")
    val SECONDARY_CONSTRUCTOR: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SECONDARY_CONSTRUCTOR, "SECONDARY_CONSTRUCTOR")
    val PRIMARY_CONSTRUCTOR: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PRIMARY_CONSTRUCTOR, "PRIMARY_CONSTRUCTOR")

    val VALUE_PARAMETER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.VALUE_PARAMETER, "VALUE_PARAMETER")
    val VALUE_PARAMETER_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.VALUE_PARAMETER_LIST, "VALUE_PARAMETER_LIST")

    val TYPE_PARAMETER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_PARAMETER, "TYPE_PARAMETER")
    val TYPE_PARAMETER_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_PARAMETER_LIST, "TYPE_PARAMETER_LIST")

    val ANNOTATION_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ANNOTATION_ENTRY, "ANNOTATION_ENTRY")
    val ANNOTATION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ANNOTATION, "ANNOTATION")

    val ANNOTATION_TARGET: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ANNOTATION_TARGET, "ANNOTATION_TARGET")

    val CLASS_BODY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CLASS_BODY, "CLASS_BODY")

    val COMPANION_BLOCK: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.COMPANION_BLOCK, "COMPANION_BLOCK")

    val IMPORT_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.IMPORT_LIST, "IMPORT_LIST")

    val FILE_ANNOTATION_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FILE_ANNOTATION_LIST, "FILE_ANNOTATION_LIST")

    val IMPORT_DIRECTIVE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.IMPORT_DIRECTIVE, "IMPORT_DIRECTIVE")

    val IMPORT_ALIAS: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.IMPORT_ALIAS, "IMPORT_ALIAS")

    val PACKAGE_DIRECTIVE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PACKAGE_DIRECTIVE, "PACKAGE_DIRECTIVE")

    val MODIFIER_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.MODIFIER_LIST, "MODIFIER_LIST")

    val TYPE_CONSTRAINT_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_CONSTRAINT_LIST, "TYPE_CONSTRAINT_LIST")

    val TYPE_CONSTRAINT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_CONSTRAINT, "TYPE_CONSTRAINT")

    val NULLABLE_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.NULLABLE_TYPE, "NULLABLE_TYPE")

    val INTERSECTION_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.INTERSECTION_TYPE, "INTERSECTION_TYPE")
    val UNION_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.UNION_TYPE, "UNION_TYPE")

    val TYPE_REFERENCE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_REFERENCE, "TYPE_REFERENCE")

    val USER_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.USER_TYPE, "USER_TYPE")
    val DYNAMIC_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.DYNAMIC_TYPE, "DYNAMIC_TYPE")

    val FUNCTION_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FUNCTION_TYPE, "FUNCTION_TYPE")

    val TYPE_CODE_FRAGMENT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_CODE_FRAGMENT, "kotlin.TYPE_CODE_FRAGMENT")
    val EXPRESSION_CODE_FRAGMENT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.EXPRESSION_CODE_FRAGMENT, "kotlin.EXPRESSION_CODE_FRAGMENT")
    val BLOCK_CODE_FRAGMENT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BLOCK_CODE_FRAGMENT, "kotlin.BLOCK_CODE_FRAGMENT")

    val TYPE_PROJECTION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_PROJECTION, "TYPE_PROJECTION")

    val FUNCTION_TYPE_RECEIVER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FUNCTION_TYPE_RECEIVER, "FUNCTION_TYPE_RECEIVER")

    val REFERENCE_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION, "REFERENCE_EXPRESSION")
    val DOT_QUALIFIED_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION, "DOT_QUALIFIED_EXPRESSION")
    val ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION: SyntaxElementType =
        register(KotlinSyntaxNodeTypeIds.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION, "ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION")
    val TYPE_ARGUMENT_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TYPE_ARGUMENT_LIST, "TYPE_ARGUMENT_LIST")

    val VALUE_ARGUMENT_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT_LIST, "VALUE_ARGUMENT_LIST")

    val VALUE_ARGUMENT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT, "VALUE_ARGUMENT")

    val CONTRACT_EFFECT_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONTRACT_EFFECT_LIST, "CONTRACT_EFFECT_LIST")

    val CONTRACT_EFFECT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONTRACT_EFFECT, "CONTRACT_EFFECT")

    val LAMBDA_ARGUMENT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LAMBDA_ARGUMENT, "LAMBDA_ARGUMENT")

    val VALUE_ARGUMENT_NAME: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT_NAME, "VALUE_ARGUMENT_NAME")

    val SUPER_TYPE_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SUPER_TYPE_LIST, "SUPER_TYPE_LIST")

    val INITIALIZER_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.INITIALIZER_LIST, "INITIALIZER_LIST")

    val DELEGATED_SUPER_TYPE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.DELEGATED_SUPER_TYPE_ENTRY, "DELEGATED_SUPER_TYPE_ENTRY")

    val SUPER_TYPE_CALL_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SUPER_TYPE_CALL_ENTRY, "SUPER_TYPE_CALL_ENTRY")
    val SUPER_TYPE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SUPER_TYPE_ENTRY, "SUPER_TYPE_ENTRY")
    val CONSTRUCTOR_CALLEE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONSTRUCTOR_CALLEE, "CONSTRUCTOR_CALLEE")

    val CONTEXT_RECEIVER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONTEXT_RECEIVER, "CONTEXT_RECEIVER")
    val CONTEXT_PARAMETER_LIST: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONTEXT_PARAMETER_LIST, "CONTEXT_PARAMETER_LIST")

    val NULL: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.NULL, "NULL")
    val BOOLEAN_CONSTANT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BOOLEAN_CONSTANT, "BOOLEAN_CONSTANT")
    val FLOAT_CONSTANT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FLOAT_CONSTANT, "FLOAT_CONSTANT")
    val CHARACTER_CONSTANT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CHARACTER_CONSTANT, "CHARACTER_CONSTANT")
    val INTEGER_CONSTANT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.INTEGER_CONSTANT, "INTEGER_CONSTANT")
    val CLASS_LITERAL_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CLASS_LITERAL_EXPRESSION, "CLASS_LITERAL_EXPRESSION")
    val COLLECTION_LITERAL_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.COLLECTION_LITERAL_EXPRESSION, "COLLECTION_LITERAL_EXPRESSION")

    val STRING_TEMPLATE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.STRING_TEMPLATE, "STRING_TEMPLATE")

    val LONG_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY, "LONG_STRING_TEMPLATE_ENTRY")

    val SHORT_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY, "SHORT_STRING_TEMPLATE_ENTRY")

    val LITERAL_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY, "LITERAL_STRING_TEMPLATE_ENTRY")

    val ESCAPE_STRING_TEMPLATE_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY, "ESCAPE_STRING_TEMPLATE_ENTRY")

    val SCRIPT: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SCRIPT, "SCRIPT")

    val STRING_INTERPOLATION_PREFIX: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX, "STRING_INTERPOLATION_PREFIX")

    // End of Stub element types

    val DESTRUCTURING_DECLARATION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.DESTRUCTURING_DECLARATION, "DESTRUCTURING_DECLARATION")
    val DESTRUCTURING_DECLARATION_ENTRY: SyntaxElementType =
        register(KotlinSyntaxNodeTypeIds.DESTRUCTURING_DECLARATION_ENTRY, "DESTRUCTURING_DECLARATION_ENTRY")
    val SCRIPT_INITIALIZER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SCRIPT_INITIALIZER, "SCRIPT_INITIALIZER")
    val PROPERTY_DELEGATE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PROPERTY_DELEGATE, "PROPERTY_DELEGATE")
    val CONSTRUCTOR_DELEGATION_CALL: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONSTRUCTOR_DELEGATION_CALL, "CONSTRUCTOR_DELEGATION_CALL")
    val CONSTRUCTOR_DELEGATION_REFERENCE: SyntaxElementType =
        register(KotlinSyntaxNodeTypeIds.CONSTRUCTOR_DELEGATION_REFERENCE, "CONSTRUCTOR_DELEGATION_REFERENCE")

    val PARENTHESIZED: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PARENTHESIZED, "PARENTHESIZED")
    val RETURN: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.RETURN, "RETURN")
    val THROW: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.THROW, "THROW")
    val CONTINUE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONTINUE, "CONTINUE")
    val BREAK: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BREAK, "BREAK")
    val IF: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.IF, "IF")
    val CONDITION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CONDITION, "CONDITION")
    val THEN: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.THEN, "THEN")
    val ELSE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ELSE, "ELSE")
    val TRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.TRY, "TRY")
    val CATCH: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CATCH, "CATCH")
    val FINALLY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FINALLY, "FINALLY")
    val FOR: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FOR, "FOR")
    val WHILE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHILE, "WHILE")
    val DO_WHILE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.DO_WHILE, "DO_WHILE")
    val LOOP_RANGE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LOOP_RANGE, "LOOP_RANGE")
    val BODY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BODY, "BODY")

    val BLOCK: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BLOCK, "BLOCK", KotlinBlockLazyParser)
    val LAMBDA_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LAMBDA_EXPRESSION, "LAMBDA_EXPRESSION", KotlinLambdaLazyParser)
    val FUNCTION_LITERAL: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.FUNCTION_LITERAL, "FUNCTION_LITERAL")
    val ANNOTATED_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION, "ANNOTATED_EXPRESSION")
    val OPERATION_REFERENCE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.OPERATION_REFERENCE, "OPERATION_REFERENCE")
    val LABEL: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LABEL, "LABEL")
    val LABEL_QUALIFIER: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LABEL_QUALIFIER, "LABEL_QUALIFIER")

    val THIS_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.THIS_EXPRESSION, "THIS_EXPRESSION")
    val SUPER_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SUPER_EXPRESSION, "SUPER_EXPRESSION")
    val BINARY_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BINARY_EXPRESSION, "BINARY_EXPRESSION")
    val BINARY_WITH_TYPE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.BINARY_WITH_TYPE, "BINARY_WITH_TYPE")
    val IS_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.IS_EXPRESSION, "IS_EXPRESSION")
    val PREFIX_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.PREFIX_EXPRESSION, "PREFIX_EXPRESSION")
    val POSTFIX_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.POSTFIX_EXPRESSION, "POSTFIX_EXPRESSION")
    val LABELED_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION, "LABELED_EXPRESSION")
    val CALL_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.CALL_EXPRESSION, "CALL_EXPRESSION")
    val ARRAY_ACCESS_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ARRAY_ACCESS_EXPRESSION, "ARRAY_ACCESS_EXPRESSION")
    val INDICES: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.INDICES, "INDICES")
    val CALLABLE_REFERENCE_EXPRESSION: SyntaxElementType =
        register(KotlinSyntaxNodeTypeIds.CALLABLE_REFERENCE_EXPRESSION, "CALLABLE_REFERENCE_EXPRESSION")
    val SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.SAFE_ACCESS_EXPRESSION, "SAFE_ACCESS_EXPRESSION")
    val ERROR_SAFE_ACCESS_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.ERROR_SAFE_ACCESS_EXPRESSION, "ERROR_SAFE_ACCESS_EXPRESSION")

    val OBJECT_LITERAL: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.OBJECT_LITERAL, "OBJECT_LITERAL")
    val WHEN: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN, "WHEN")
    val WHEN_ENTRY: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN_ENTRY, "WHEN_ENTRY")
    val WHEN_ENTRY_GUARD: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN_ENTRY_GUARD, "WHEN_ENTRY_GUARD")
    val WHEN_CONDITION_IN_RANGE: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN_CONDITION_IN_RANGE, "WHEN_CONDITION_IN_RANGE")
    val WHEN_CONDITION_IS_PATTERN: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN_CONDITION_IS_PATTERN, "WHEN_CONDITION_IS_PATTERN")
    val WHEN_CONDITION_EXPRESSION: SyntaxElementType = register(KotlinSyntaxNodeTypeIds.WHEN_CONDITION_EXPRESSION, "WHEN_CONDITION_WITH_EXPRESSION")
}
