/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.SyntaxId

fun SyntaxId.isExpression(): Boolean = when (this) {
    // Stub-based element types that are not `KtExpression`
    KotlinSyntaxNodeTypeIds.CLASS_BODY,
    KotlinSyntaxNodeTypeIds.COMPANION_BLOCK,
    KotlinSyntaxNodeTypeIds.INITIALIZER_LIST,
    KotlinSyntaxNodeTypeIds.VALUE_PARAMETER_LIST,
    KotlinSyntaxNodeTypeIds.CONTEXT_PARAMETER_LIST,
    KotlinSyntaxNodeTypeIds.CONTEXT_RECEIVER,
    KotlinSyntaxNodeTypeIds.TYPE_PARAMETER_LIST,
    KotlinSyntaxNodeTypeIds.TYPE_CONSTRAINT_LIST,
    KotlinSyntaxNodeTypeIds.TYPE_CONSTRAINT,
    KotlinSyntaxNodeTypeIds.SUPER_TYPE_LIST,
    KotlinSyntaxNodeTypeIds.DELEGATED_SUPER_TYPE_ENTRY,
    KotlinSyntaxNodeTypeIds.SUPER_TYPE_CALL_ENTRY,
    KotlinSyntaxNodeTypeIds.SUPER_TYPE_ENTRY,
    KotlinSyntaxNodeTypeIds.MODIFIER_LIST,
    KotlinSyntaxNodeTypeIds.ANNOTATION,
    KotlinSyntaxNodeTypeIds.ANNOTATION_ENTRY,
    KotlinSyntaxNodeTypeIds.ANNOTATION_TARGET,
    KotlinSyntaxNodeTypeIds.TYPE_REFERENCE,
    KotlinSyntaxNodeTypeIds.USER_TYPE,
    KotlinSyntaxNodeTypeIds.DYNAMIC_TYPE,
    KotlinSyntaxNodeTypeIds.FUNCTION_TYPE,
    KotlinSyntaxNodeTypeIds.FUNCTION_TYPE_RECEIVER,
    KotlinSyntaxNodeTypeIds.NULLABLE_TYPE,
    KotlinSyntaxNodeTypeIds.INTERSECTION_TYPE,
    KotlinSyntaxNodeTypeIds.TYPE_PROJECTION,
    KotlinSyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY,
    KotlinSyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY,
    KotlinSyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY,
    KotlinSyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY,
    KotlinSyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX,
    KotlinSyntaxNodeTypeIds.TYPE_ARGUMENT_LIST,
    KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT_LIST,
    KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT,
    KotlinSyntaxNodeTypeIds.CONTRACT_EFFECT_LIST,
    KotlinSyntaxNodeTypeIds.CONTRACT_EFFECT,
    KotlinSyntaxNodeTypeIds.LAMBDA_ARGUMENT,
    KotlinSyntaxNodeTypeIds.VALUE_ARGUMENT_NAME,
    KotlinSyntaxNodeTypeIds.PACKAGE_DIRECTIVE,
    KotlinSyntaxNodeTypeIds.FILE_ANNOTATION_LIST,
    KotlinSyntaxNodeTypeIds.IMPORT_LIST,
    KotlinSyntaxNodeTypeIds.IMPORT_DIRECTIVE,
    KotlinSyntaxNodeTypeIds.IMPORT_ALIAS,
        -> false

    else -> {
        // All stub-based element types that are not `KtExpression` are listed above
        this in KotlinSyntaxNodeTypeIds.FILE..KotlinSyntaxNodeTypeIds.BLOCK_CODE_FRAGMENT
    }
}
