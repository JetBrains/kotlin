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
 * Represents a simple name reference: an unqualified reference to a declaration by its name, or an operation sign that references a
 * declaration (for example, the `+` of a binary expression).
 *
 * ### Example:
 *
 * ```kotlin
 * val length = text.length
 * //                ^____^
 * // A simple name expression referencing the 'length' property
 * ```
 *
 * @see KtNameReferenceExpression a plain identifier reference
 * @see KtOperationReferenceExpression an operation sign that references a declaration
 */
interface KtSimpleNameExpression : KtReferenceExpression {

    /**
     * Returns the referenced name as it appears in the source, with any surrounding backticks removed.
     */
    fun getReferencedName(): String

    /**
     * Returns the referenced name as a [Name].
     */
    fun getReferencedNameAsName(): Name

    /**
     * Returns the token or element that carries the referenced name (for example, the identifier or the operation sign). Never `null`, even
     * for operation references.
     */
    fun getReferencedNameElement(): PsiElement

    /**
     * Returns the identifier token of this reference, or `null` if the name is not a plain identifier (for example, an operation sign).
     */
    fun getIdentifier(): PsiElement?

    /**
     * Returns the element type of the [referenced name element][getReferencedNameElement].
     */
    fun getReferencedNameElementType(): IElementType
}

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
