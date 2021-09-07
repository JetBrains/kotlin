/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

class KtCollectionLiteralEntryPair(node: ASTNode) : KtCollectionLiteralEntry(node) {

    // Copied from KtBinaryExpression
    val key: KtExpression?
        get() {
            var node = colon.node.treePrev
            while (node != null) {
                val psi = node.psi
                if (psi is KtExpression) {
                    return psi
                }
                node = node.treePrev
            }
            return null
        }

    val value: KtExpression?
        get() {
            var node = colon.node.treeNext
            while (node != null) {
                val psi = node.psi
                if (psi is KtExpression) {
                    return psi
                }
                node = node.treeNext
            }
            return null
        }

    val colon: PsiElement
        get() = findChildByType(KtTokens.COLON)
            ?: throw NullPointerException("No colon was found for pair collection literal entry: " + Arrays.toString(children))
}