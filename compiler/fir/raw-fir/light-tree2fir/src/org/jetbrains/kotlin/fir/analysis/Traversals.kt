/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.BasicElementTypeId
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.KtTokenId
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
            KtTokenId.LBRACE, BasicElementTypeId.WHITE_SPACE, KtTokenId.DOT, KtTokenId.EOL_COMMENT -> {}
            KtNodeTypeId.CALL_EXPRESSION -> return true
            KtNodeTypeId.REFERENCE_EXPRESSION -> {}
            KtNodeTypeId.DOT_QUALIFIED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            KtNodeTypeId.ANNOTATION_ENTRY -> {}
            KtNodeTypeId.ANNOTATED_EXPRESSION -> {
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
    val functionCall = getChildren(root).firstOrNull { getElementTokenId(it) == KtNodeTypeId.CALL_EXPRESSION } ?: return false
    val lambda = getChildren(functionCall).firstOrNull { getElementTokenId(it) == KtNodeTypeId.LAMBDA_ARGUMENT } ?: return false
    val expr = getChildren(lambda).singleOrNull() ?: return false
    return getElementTokenId(expr) == KtNodeTypeId.LABELED_EXPRESSION
}
