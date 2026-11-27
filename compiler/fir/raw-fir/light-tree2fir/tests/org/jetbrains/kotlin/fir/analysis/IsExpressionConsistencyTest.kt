/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.kotlin.ElementTypeUtils
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.syntax.syntaxId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Checks that both `isExpression` implementations match the PSI on every node type and token:
 * an element type is an expression iff its PSI element is a [KtExpression].
 * The implementations are [ElementTypeUtils.isExpression] for light tree nodes and the tree-agnostic `SyntaxId.isExpression()`.
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
                val name = "${holder.simpleName}.${field.name}"
                val expected = createPsi(elementType) is KtExpression
                assertEquals(expected, with(ElementTypeUtils) { LightNode(elementType).isExpression() }) { "ElementTypeUtils: $name" }
                assertEquals(expected, elementType.syntaxId().isExpression()) { "SyntaxId: $name" }
            }
        }
    }

    private fun createPsi(elementType: IElementType): PsiElement? = when (elementType) {
        is KtNodeType -> elementType.createPsi(CompositeElement(elementType))
        // File roots (the Kotlin file and the code fragments) are `KtFile`
        is IFileElementType -> null
        // `BLOCK`, `LAMBDA_EXPRESSION` and KDoc, whose nodes are PSI elements themselves
        is ILazyParseableElementType -> elementType.createNode("") as? PsiElement
        // Tokens are leaves without a specific PSI element
        else -> null
    }
}
