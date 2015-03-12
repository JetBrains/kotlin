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
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.JetTokens

public trait JetSimpleNameExpression : JetReferenceExpression {

    public fun getReferencedName(): String

    public fun getReferencedNameAsName(): Name

    public fun getReferencedNameElement(): PsiElement

    public fun getIdentifier(): PsiElement?

    public fun getReferencedNameElementType(): IElementType
}

abstract class JetSimpleNameExpressionImpl(node: ASTNode) : JetExpressionImpl(node), JetSimpleNameExpression {

    override fun getIdentifier(): PsiElement? = findChildByType(JetTokens.IDENTIFIER)

    override fun getReferencedNameElementType() = getReferencedNameElementTypeImpl()

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D): R {
        return visitor.visitSimpleNameExpression(this, data)
    }

    override fun getReferencedNameAsName() = getReferencedNameAsNameImpl()

    override fun getReferencedName() = getReferencedNameImpl()

    //NOTE: an unfortunate way to share an implementation between stubbed and not stubbed tree
    default object Helper {

        fun JetSimpleNameExpression.getReferencedNameElementTypeImpl(): IElementType {
            return this.getReferencedNameElement().getNode()!!.getElementType()!!
        }

        fun JetSimpleNameExpression.getReferencedNameAsNameImpl(): Name {
            val name = this.getReferencedName()
            return Name.identifierNoValidate(name)
        }

        fun JetSimpleNameExpression.getReferencedNameImpl(): String {
            val text = this.getReferencedNameElement().getNode()!!.getText()
            return JetPsiUtil.unquoteIdentifierOrFieldReference(text)
        }
    }
}
