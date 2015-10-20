/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens

public abstract class KtDoubleColonExpression(node: ASTNode) : KtExpressionImpl(node) {
    public val typeReference: KtTypeReference?
        get() = findChildByType(KtNodeTypes.TYPE_REFERENCE)

    public val doubleColonTokenReference: PsiElement
        get() = findChildByType(KtTokens.COLONCOLON)!!

    public fun setTypeReference(typeReference: KtTypeReference) {
        val oldTypeReference = this.typeReference
        if (oldTypeReference != null) {
            oldTypeReference.replace(typeReference)
        }
        else {
            addBefore(typeReference, doubleColonTokenReference)
        }
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitDoubleColonExpression(this, data)
    }
}
