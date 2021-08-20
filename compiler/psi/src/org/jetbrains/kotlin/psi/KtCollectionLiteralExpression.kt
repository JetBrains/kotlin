/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.getTrailingCommaByClosingElement

open class KtCollectionLiteralExpression(node: ASTNode) : KtExpressionImpl(node), KtReferenceExpression {
    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitCollectionLiteralExpression(this, data)
    }

    val literalKind: KtCollectionLiteralKind
        get() {
            val entries = getInnerEntries()
            if (entries.all { it is KtCollectionLiteralEntrySingle }) {
                return KtCollectionLiteralKind.LIST
            }
            if (entries.all { it is KtCollectionLiteralEntryPair }) {
                return KtCollectionLiteralKind.MAP
            }
            error("The literal type cannot be determined")
        }

    val leftBracket: PsiElement?
        get() = findChildByType(KtTokens.LBRACKET)

    val rightBracket: PsiElement?
        get() = findChildByType(KtTokens.RBRACKET)

    val trailingComma: PsiElement?
        get() = getTrailingCommaByClosingElement(rightBracket)

    fun getInnerExpressions(): List<KtExpression> {
        require(literalKind == KtCollectionLiteralKind.LIST)
        return getInnerEntries().map { (it as KtCollectionLiteralEntrySingle).expression }
    }

    fun getInnerEntries(): List<KtCollectionLiteralEntry> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtCollectionLiteralEntry::class.java)
    }
}

enum class KtCollectionLiteralKind {
    LIST, MAP
}