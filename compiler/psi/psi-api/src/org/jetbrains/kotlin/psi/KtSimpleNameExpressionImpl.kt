/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name

/**
 * Base implementation of [KtSimpleNameExpression] shared by its stub-based and AST-based variants.
 *
 * This is an internal implementation base class of the Kotlin PSI, not intended for direct use or subclassing outside of the PSI
 * implementation. Use [KtSimpleNameExpression] instead.
 */
abstract class KtSimpleNameExpressionImpl : KtExpressionImpl, KtSimpleNameExpression {
    @KtImplementationDetail
    constructor(node: ASTNode) : super(node)

    override fun getIdentifier(): PsiElement? = findChildByType(KtTokens.IDENTIFIER)

    override fun getReferencedNameElementType() = getReferencedNameElementTypeImpl(this)

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    override fun getReferencedNameAsName() = getReferencedNameAsNameImpl(this)

    override fun getReferencedName() = getReferencedNameImpl(this)

    //NOTE: an unfortunate way to share an implementation between stubbed and not stubbed tree
    companion object {
        fun getReferencedNameElementTypeImpl(expression: KtSimpleNameExpression): IElementType {
            return expression.getReferencedNameElement().node!!.elementType
        }

        fun getReferencedNameAsNameImpl(expresssion: KtSimpleNameExpression): Name {
            val name = expresssion.getReferencedName()
            return Name.identifier(name)
        }

        fun getReferencedNameImpl(expression: KtSimpleNameExpression): String {
            val text = expression.getReferencedNameElement().node!!.text
            return KtPsiUtil.unquoteIdentifierOrFieldReference(text)
        }
    }
}
