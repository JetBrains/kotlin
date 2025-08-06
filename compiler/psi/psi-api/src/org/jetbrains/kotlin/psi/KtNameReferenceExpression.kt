/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtStubBasedElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub

class KtNameReferenceExpression : KtExpressionImplStub<KotlinNameReferenceExpressionStub>, KtSimpleNameExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinNameReferenceExpressionStub) : super(stub, KtStubBasedElementTypes.REFERENCE_EXPRESSION)

    override fun getReferencedName(): String {
        val stub = greenStub
        if (stub != null) {
            return stub.referencedName
        }
        return KtSimpleNameExpressionImpl.getReferencedNameImpl(this)
    }

    override fun getReferencedNameAsName(): Name {
        return KtSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
    }

    override fun getReferencedNameElement(): PsiElement {
        return findChildByType(NAME_REFERENCE_EXPRESSIONS) ?: this
    }

    override fun getIdentifier(): PsiElement? {
        return findChildByType(KtTokens.IDENTIFIER)
    }

    override fun getReferencedNameElementType(): IElementType {
        return KtSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    val isPlaceholder: Boolean
        get() = getIdentifier()?.text?.equals("_") == true

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD)
    }
}
