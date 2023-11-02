/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.utils.addToStdlib.popLast

inline fun <T> isCallTheFirstStatement(
    root: T,
    getElementType: (T) -> IElementType,
    getChildren: (T) -> List<T>,
): Boolean {
    val stack = getChildren(root).asReversed().toMutableList()

    while (stack.isNotEmpty()) {
        val child = stack.popLast()
        when (getElementType(child)) {
            KtTokens.LBRACE, KtTokens.WHITE_SPACE, KtTokens.DOT, KtTokens.EOL_COMMENT -> {}
            KtNodeTypes.CALL_EXPRESSION -> return true
            KtNodeTypes.REFERENCE_EXPRESSION -> {}
            KtNodeTypes.DOT_QUALIFIED_EXPRESSION -> {
                stack += getChildren(child).asReversed()
            }
            else -> return false
        }
    }
    return false
}
