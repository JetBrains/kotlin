/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class KtNameReferenceExpression : KtExpressionImplStub<KotlinNameReferenceExpressionStub>, KtSimpleNameExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: KotlinNameReferenceExpressionStub) : super(stub, KtStubElementTypes.REFERENCE_EXPRESSION)

    override fun getReferencedName(): String {
        val stub = stub
        if (stub != null) {
            return stub.getReferencedName()
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

    companion object {
        private val NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD)
    }
}
