/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.utils.addToStdlib.popLast

inline fun <T> isCallTheFirstStatement(
    root: T,
    getElementTokenId: (T) -> Int,
    getChildren: (T) -> List<T>,
): Boolean {
    val stack = getChildren(root).asReversed().toMutableList()

    while (stack.isNotEmpty()) {
        val child = stack.popLast()
        when (getElementTokenId(child)) {
            KtTokens.LBRACE_ID, KtTokens.WHITE_SPACE_ID, KtTokens.DOT_ID, KtTokens.EOL_COMMENT_ID -> {}
            KtNodeTypes.CALL_EXPRESSION_ID -> return true
            KtNodeTypes.REFERENCE_EXPRESSION_ID -> {}
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION_ID -> {
                stack += getChildren(child).asReversed()
            }
            KtNodeTypes.ANNOTATION_ENTRY_ID -> {}
            KtNodeTypes.ANNOTATED_EXPRESSION_ID -> {
                stack += getChildren(child).asReversed()
            }
            else -> return false
        }
    }
    return false
}

inline fun <T> firstFunctionCallInBlockHasLambdaArgumentWithLabel(
    root: T,
    getElementTokenId: (T) -> Int,
    getChildren: (T) -> List<T>,
): Boolean {
    val functionCall = getChildren(root).firstOrNull { getElementTokenId(it) == KtNodeTypes.CALL_EXPRESSION_ID } ?: return false
    val lambda = getChildren(functionCall).firstOrNull { getElementTokenId(it) == KtNodeTypes.LAMBDA_ARGUMENT_ID } ?: return false
    val expr = getChildren(lambda).singleOrNull() ?: return false
    return getElementTokenId(expr) == KtNodeTypes.LABELED_EXPRESSION_ID
}
