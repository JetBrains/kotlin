/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.syntax.SyntaxNodeTypeIds


fun Int.isExpression(): Boolean = when (this) {
    // Stub-based element types that are not `KtExpression`
    SyntaxNodeTypeIds.CLASS_BODY,
    SyntaxNodeTypeIds.COMPANION_BLOCK,
    SyntaxNodeTypeIds.INITIALIZER_LIST,
    SyntaxNodeTypeIds.VALUE_PARAMETER_LIST,
    SyntaxNodeTypeIds.CONTEXT_PARAMETER_LIST,
    SyntaxNodeTypeIds.CONTEXT_RECEIVER,
    SyntaxNodeTypeIds.TYPE_PARAMETER_LIST,
    SyntaxNodeTypeIds.TYPE_CONSTRAINT_LIST,
    SyntaxNodeTypeIds.TYPE_CONSTRAINT,
    SyntaxNodeTypeIds.SUPER_TYPE_LIST,
    SyntaxNodeTypeIds.DELEGATED_SUPER_TYPE_ENTRY,
    SyntaxNodeTypeIds.SUPER_TYPE_CALL_ENTRY,
    SyntaxNodeTypeIds.SUPER_TYPE_ENTRY,
    SyntaxNodeTypeIds.MODIFIER_LIST,
    SyntaxNodeTypeIds.ANNOTATION,
    SyntaxNodeTypeIds.ANNOTATION_ENTRY,
    SyntaxNodeTypeIds.ANNOTATION_TARGET,
    SyntaxNodeTypeIds.TYPE_REFERENCE,
    SyntaxNodeTypeIds.USER_TYPE,
    SyntaxNodeTypeIds.DYNAMIC_TYPE,
    SyntaxNodeTypeIds.FUNCTION_TYPE,
    SyntaxNodeTypeIds.FUNCTION_TYPE_RECEIVER,
    SyntaxNodeTypeIds.NULLABLE_TYPE,
    SyntaxNodeTypeIds.INTERSECTION_TYPE,
    SyntaxNodeTypeIds.TYPE_PROJECTION,
    SyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY,
    SyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY,
    SyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY,
    SyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY,
    SyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX,
    SyntaxNodeTypeIds.TYPE_ARGUMENT_LIST,
    SyntaxNodeTypeIds.VALUE_ARGUMENT_LIST,
    SyntaxNodeTypeIds.VALUE_ARGUMENT,
    SyntaxNodeTypeIds.CONTRACT_EFFECT_LIST,
    SyntaxNodeTypeIds.CONTRACT_EFFECT,
    SyntaxNodeTypeIds.LAMBDA_ARGUMENT,
    SyntaxNodeTypeIds.VALUE_ARGUMENT_NAME,
    SyntaxNodeTypeIds.PACKAGE_DIRECTIVE,
    SyntaxNodeTypeIds.FILE_ANNOTATION_LIST,
    SyntaxNodeTypeIds.IMPORT_LIST,
    SyntaxNodeTypeIds.IMPORT_DIRECTIVE,
    SyntaxNodeTypeIds.IMPORT_ALIAS,
        -> false

    else -> {
        // All stub-based element types that are not `KtExpression` are listed above
        this in SyntaxNodeTypeIds.FILE..SyntaxNodeTypeIds.BLOCK_CODE_FRAGMENT
    }
}
