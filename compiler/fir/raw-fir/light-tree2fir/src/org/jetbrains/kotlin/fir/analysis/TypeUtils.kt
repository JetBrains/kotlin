/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.KtNodeTypeId


fun Int.isExpression(): Boolean = when (this) {
    // Stub-based element types that are not `KtExpression`
    KtNodeTypeId.CLASS_BODY,
    KtNodeTypeId.COMPANION_BLOCK,
    KtNodeTypeId.INITIALIZER_LIST,
    KtNodeTypeId.VALUE_PARAMETER_LIST,
    KtNodeTypeId.CONTEXT_PARAMETER_LIST,
    KtNodeTypeId.CONTEXT_RECEIVER,
    KtNodeTypeId.TYPE_PARAMETER_LIST,
    KtNodeTypeId.TYPE_CONSTRAINT_LIST,
    KtNodeTypeId.TYPE_CONSTRAINT,
    KtNodeTypeId.SUPER_TYPE_LIST,
    KtNodeTypeId.DELEGATED_SUPER_TYPE_ENTRY,
    KtNodeTypeId.SUPER_TYPE_CALL_ENTRY,
    KtNodeTypeId.SUPER_TYPE_ENTRY,
    KtNodeTypeId.MODIFIER_LIST,
    KtNodeTypeId.ANNOTATION,
    KtNodeTypeId.ANNOTATION_ENTRY,
    KtNodeTypeId.ANNOTATION_TARGET,
    KtNodeTypeId.TYPE_REFERENCE,
    KtNodeTypeId.USER_TYPE,
    KtNodeTypeId.DYNAMIC_TYPE,
    KtNodeTypeId.FUNCTION_TYPE,
    KtNodeTypeId.FUNCTION_TYPE_RECEIVER,
    KtNodeTypeId.NULLABLE_TYPE,
    KtNodeTypeId.INTERSECTION_TYPE,
    KtNodeTypeId.TYPE_PROJECTION,
    KtNodeTypeId.LONG_STRING_TEMPLATE_ENTRY,
    KtNodeTypeId.SHORT_STRING_TEMPLATE_ENTRY,
    KtNodeTypeId.LITERAL_STRING_TEMPLATE_ENTRY,
    KtNodeTypeId.ESCAPE_STRING_TEMPLATE_ENTRY,
    KtNodeTypeId.STRING_INTERPOLATION_PREFIX,
    KtNodeTypeId.TYPE_ARGUMENT_LIST,
    KtNodeTypeId.VALUE_ARGUMENT_LIST,
    KtNodeTypeId.VALUE_ARGUMENT,
    KtNodeTypeId.CONTRACT_EFFECT_LIST,
    KtNodeTypeId.CONTRACT_EFFECT,
    KtNodeTypeId.LAMBDA_ARGUMENT,
    KtNodeTypeId.VALUE_ARGUMENT_NAME,
    KtNodeTypeId.PACKAGE_DIRECTIVE,
    KtNodeTypeId.FILE_ANNOTATION_LIST,
    KtNodeTypeId.IMPORT_LIST,
    KtNodeTypeId.IMPORT_DIRECTIVE,
    KtNodeTypeId.IMPORT_ALIAS,
        -> false

    else -> {
        // All stub-based element types that are not `KtExpression` are listed above
        this in KtNodeTypeId.FILE..KtNodeTypeId.BLOCK_CODE_FRAGMENT
    }
}
