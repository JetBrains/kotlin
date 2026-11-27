/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

/**
 * Whether the node type with this ID represents an expression, i.e., its PSI element is `KtExpression`.
 * It's `false` for tokens and for the IDs that aren't Kotlin node types.
 *
 * It's the single source of truth for all parse trees.
 * Must be updated once a new expression node type is added to the parser.
 */
public fun SyntaxId.isExpression(): Boolean = when (this) {
    // Declarations are listed too because `KtDeclaration` extends `KtExpression`
    KotlinSyntaxNodeTypeIds.CLASS,
    KotlinSyntaxNodeTypeIds.FUNCTION,
    KotlinSyntaxNodeTypeIds.PROPERTY,
    KotlinSyntaxNodeTypeIds.DESTRUCTURING_DECLARATION,
    KotlinSyntaxNodeTypeIds.DESTRUCTURING_DECLARATION_ENTRY,
    KotlinSyntaxNodeTypeIds.OBJECT_DECLARATION,
    KotlinSyntaxNodeTypeIds.TYPEALIAS,
    KotlinSyntaxNodeTypeIds.ENUM_ENTRY,
    KotlinSyntaxNodeTypeIds.CLASS_INITIALIZER,
    KotlinSyntaxNodeTypeIds.SCRIPT_INITIALIZER,
    KotlinSyntaxNodeTypeIds.SECONDARY_CONSTRUCTOR,
    KotlinSyntaxNodeTypeIds.PRIMARY_CONSTRUCTOR,
    KotlinSyntaxNodeTypeIds.TYPE_PARAMETER,
    KotlinSyntaxNodeTypeIds.CONSTRUCTOR_CALLEE,
    KotlinSyntaxNodeTypeIds.VALUE_PARAMETER,
    KotlinSyntaxNodeTypeIds.PROPERTY_ACCESSOR,
    KotlinSyntaxNodeTypeIds.BACKING_FIELD,
    KotlinSyntaxNodeTypeIds.CONSTRUCTOR_DELEGATION_REFERENCE,
    KotlinSyntaxNodeTypeIds.NULL,
    KotlinSyntaxNodeTypeIds.BOOLEAN_CONSTANT,
    KotlinSyntaxNodeTypeIds.FLOAT_CONSTANT,
    KotlinSyntaxNodeTypeIds.CHARACTER_CONSTANT,
    KotlinSyntaxNodeTypeIds.INTEGER_CONSTANT,
    KotlinSyntaxNodeTypeIds.STRING_TEMPLATE,
    KotlinSyntaxNodeTypeIds.PARENTHESIZED,
    KotlinSyntaxNodeTypeIds.RETURN,
    KotlinSyntaxNodeTypeIds.THROW,
    KotlinSyntaxNodeTypeIds.CONTINUE,
    KotlinSyntaxNodeTypeIds.BREAK,
    KotlinSyntaxNodeTypeIds.IF,
    KotlinSyntaxNodeTypeIds.TRY,
    KotlinSyntaxNodeTypeIds.FOR,
    KotlinSyntaxNodeTypeIds.WHILE,
    KotlinSyntaxNodeTypeIds.DO_WHILE,
    KotlinSyntaxNodeTypeIds.BLOCK,
    KotlinSyntaxNodeTypeIds.LAMBDA_EXPRESSION,
    KotlinSyntaxNodeTypeIds.FUNCTION_LITERAL,
    KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION,
    KotlinSyntaxNodeTypeIds.REFERENCE_EXPRESSION,
    KotlinSyntaxNodeTypeIds.ENUM_ENTRY_SUPERCLASS_REFERENCE_EXPRESSION,
    KotlinSyntaxNodeTypeIds.OPERATION_REFERENCE,
    KotlinSyntaxNodeTypeIds.LABEL,
    KotlinSyntaxNodeTypeIds.THIS_EXPRESSION,
    KotlinSyntaxNodeTypeIds.SUPER_EXPRESSION,
    KotlinSyntaxNodeTypeIds.BINARY_EXPRESSION,
    KotlinSyntaxNodeTypeIds.BINARY_WITH_TYPE,
    KotlinSyntaxNodeTypeIds.IS_EXPRESSION,
    KotlinSyntaxNodeTypeIds.PREFIX_EXPRESSION,
    KotlinSyntaxNodeTypeIds.POSTFIX_EXPRESSION,
    KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION,
    KotlinSyntaxNodeTypeIds.CALL_EXPRESSION,
    KotlinSyntaxNodeTypeIds.ARRAY_ACCESS_EXPRESSION,
    KotlinSyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION,
    KotlinSyntaxNodeTypeIds.CALLABLE_REFERENCE_EXPRESSION,
    KotlinSyntaxNodeTypeIds.CLASS_LITERAL_EXPRESSION,
    KotlinSyntaxNodeTypeIds.SAFE_ACCESS_EXPRESSION,
    KotlinSyntaxNodeTypeIds.ERROR_SAFE_ACCESS_EXPRESSION,
    KotlinSyntaxNodeTypeIds.OBJECT_LITERAL,
    KotlinSyntaxNodeTypeIds.WHEN,
    KotlinSyntaxNodeTypeIds.COLLECTION_LITERAL_EXPRESSION,
    KotlinSyntaxNodeTypeIds.SCRIPT,
        -> true

    else -> false
}
