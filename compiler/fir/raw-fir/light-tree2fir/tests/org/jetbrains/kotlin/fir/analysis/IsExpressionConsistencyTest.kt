/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.ElementTypeUtils
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.syntax.syntaxId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Checks that the two `isExpression` implementations agree on every node type and token:
 * [ElementTypeUtils.isExpression] for light tree nodes and the tree-agnostic `SyntaxId.isExpression()`.
 */
@OptIn(KtImplementationDetail::class)
class IsExpressionConsistencyTest {
    @Test
    fun nodeTypesAndTokens() {
        class LightNode(private val type: IElementType) : LighterASTNode {
            override fun getTokenType(): IElementType = type
            override fun getStartOffset(): Int = 0
            override fun getEndOffset(): Int = 0
        }

        for (holder in listOf(KtNodeTypes::class.java, KtTokens::class.java)) {
            for (field in holder.declaredFields) {
                val elementType = field.get(null) as? IElementType ?: continue
                assertEquals(
                    with(ElementTypeUtils) { LightNode(elementType).isExpression() },
                    elementType.syntaxId().isExpression(),
                ) { "`ElementTypeUtils.isExpression` and `SyntaxId.isExpression` differ for ${holder.simpleName}.${field.name}" }
            }
        }
    }
}
