/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.kmp.parser.KtNodeTypes


fun Int.isExpression(): Boolean = when (this) {
    // Stub-based element types that are not `KtExpression`
    KtNodeTypes.CLASS_BODY_ID,
    KtNodeTypes.COMPANION_BLOCK_ID,
    KtNodeTypes.INITIALIZER_LIST_ID,
    KtNodeTypes.VALUE_PARAMETER_LIST_ID,
    KtNodeTypes.CONTEXT_PARAMETER_LIST_ID,
    KtNodeTypes.CONTEXT_RECEIVER_ID,
    KtNodeTypes.TYPE_PARAMETER_LIST_ID,
    KtNodeTypes.TYPE_CONSTRAINT_LIST_ID,
    KtNodeTypes.TYPE_CONSTRAINT_ID,
    KtNodeTypes.SUPER_TYPE_LIST_ID,
    KtNodeTypes.DELEGATED_SUPER_TYPE_ENTRY_ID,
    KtNodeTypes.SUPER_TYPE_CALL_ENTRY_ID,
    KtNodeTypes.SUPER_TYPE_ENTRY_ID,
    KtNodeTypes.MODIFIER_LIST_ID,
    KtNodeTypes.ANNOTATION_ID,
    KtNodeTypes.ANNOTATION_ENTRY_ID,
    KtNodeTypes.ANNOTATION_TARGET_ID,
    KtNodeTypes.TYPE_REFERENCE_ID,
    KtNodeTypes.USER_TYPE_ID,
    KtNodeTypes.DYNAMIC_TYPE_ID,
    KtNodeTypes.FUNCTION_TYPE_ID,
    KtNodeTypes.FUNCTION_TYPE_RECEIVER_ID,
    KtNodeTypes.NULLABLE_TYPE_ID,
    KtNodeTypes.INTERSECTION_TYPE_ID,
    KtNodeTypes.TYPE_PROJECTION_ID,
    KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY_ID,
    KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY_ID,
    KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY_ID,
    KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY_ID,
    KtNodeTypes.STRING_INTERPOLATION_PREFIX_ID,
    KtNodeTypes.TYPE_ARGUMENT_LIST_ID,
    KtNodeTypes.VALUE_ARGUMENT_LIST_ID,
    KtNodeTypes.VALUE_ARGUMENT_ID,
    KtNodeTypes.CONTRACT_EFFECT_LIST_ID,
    KtNodeTypes.CONTRACT_EFFECT_ID,
    KtNodeTypes.LAMBDA_ARGUMENT_ID,
    KtNodeTypes.VALUE_ARGUMENT_NAME_ID,
    KtNodeTypes.PACKAGE_DIRECTIVE_ID,
    KtNodeTypes.FILE_ANNOTATION_LIST_ID,
    KtNodeTypes.IMPORT_LIST_ID,
    KtNodeTypes.IMPORT_DIRECTIVE_ID,
    KtNodeTypes.IMPORT_ALIAS_ID,
        -> false

    // All stub-based element types that are not `KtExpression` are listed above
    in KtNodeTypes.FILE_ID..KtNodeTypes.BLOCK_CODE_FRAGMENT_ID,
        -> true

    else -> false
}
