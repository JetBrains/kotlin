/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinDotNameReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtDotNameReferenceExpression : KtExpressionImplStub<KotlinDotNameReferenceExpressionStub>, KtSimpleNameExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinDotNameReferenceExpressionStub) : super(stub, KtStubElementTypes.DOT_REFERENCE_EXPRESSION)

    override fun getReferencedName(): String {
        val stub = greenStub
        if (stub != null) {
            return stub.getReferencedName()
        }
        return KtSimpleNameExpressionImpl.getReferencedNameImpl(this)
    }

    override fun getReferencedNameAsName(): Name {
        return KtSimpleNameExpressionImpl.getReferencedNameAsNameImpl(this)
    }

    override fun getReferencedNameElement(): PsiElement {
        return findChildByType(IDENTIFIER) ?: this
    }

    override fun getIdentifier(): PsiElement? {
        return findChildByType(IDENTIFIER)
    }

    override fun getReferencedNameElementType(): IElementType {
        return KtSimpleNameExpressionImpl.getReferencedNameElementTypeImpl(this)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDotNameReferenceExpression(this, data)
    }
}
