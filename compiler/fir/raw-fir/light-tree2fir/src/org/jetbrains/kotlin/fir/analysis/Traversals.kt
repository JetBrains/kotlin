/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.syntax.SyntaxBasicElementTypeIds
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.SyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.SyntaxTokenIds
import org.jetbrains.kotlin.utils.addToStdlib.popLast

inline fun <T> isCallTheFirstStatement(
    root: T,
    getElementTokenId: (T) -> SyntaxId,
    getChildren: (T) -> List<T>,
): Boolean {
    val stack = getChildren(root).asReversed().toMutableList()

    while (stack.isNotEmpty()) {
        val child = stack.popLast()
        when (getElementTokenId(child)) {
            SyntaxTokenIds.LBRACE, SyntaxBasicElementTypeIds.WHITE_SPACE, SyntaxTokenIds.DOT, SyntaxTokenIds.EOL_COMMENT -> {}
            SyntaxNodeTypeIds.CALL_EXPRESSION -> return true
            SyntaxNodeTypeIds.REFERENCE_EXPRESSION -> {}
            SyntaxNodeTypeIds.DOT_QUALIFIED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            SyntaxNodeTypeIds.ANNOTATION_ENTRY -> {}
            SyntaxNodeTypeIds.ANNOTATED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            else -> return false
        }
    }
    return false
}

inline fun <T> firstFunctionCallInBlockHasLambdaArgumentWithLabel(
    root: T,
    getElementTokenId: (T) -> SyntaxId,
    getChildren: (T) -> List<T>,
): Boolean {
    val functionCall = getChildren(root).firstOrNull { getElementTokenId(it) == SyntaxNodeTypeIds.CALL_EXPRESSION } ?: return false
    val lambda = getChildren(functionCall).firstOrNull { getElementTokenId(it) == SyntaxNodeTypeIds.LAMBDA_ARGUMENT } ?: return false
    val expr = getChildren(lambda).singleOrNull() ?: return false
    return getElementTokenId(expr) == SyntaxNodeTypeIds.LABELED_EXPRESSION
}
