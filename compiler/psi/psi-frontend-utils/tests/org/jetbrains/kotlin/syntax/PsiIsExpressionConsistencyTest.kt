/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.syntax

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import org.jetbrains.kotlin.KtNodeType
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Checks that [isExpression] matches the PSI on every node type and token:
 * an element type is an expression iff its PSI element is a [KtExpression].
 */
@OptIn(KtImplementationDetail::class)
class PsiIsExpressionConsistencyTest {
    @Test
    fun nodeTypesAndTokens() {
        for (holder in listOf(KtNodeTypes::class.java, KtTokens::class.java)) {
            for (field in holder.declaredFields) {
                val elementType = field.get(null) as? IElementType ?: continue
                assertEquals(createPsi(elementType) is KtExpression, elementType.syntaxId().isExpression()) {
                    "${holder.simpleName}.${field.name}"
                }
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
