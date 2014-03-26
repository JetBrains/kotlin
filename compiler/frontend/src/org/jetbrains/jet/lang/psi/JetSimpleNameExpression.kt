/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.Nullable
import org.jetbrains.jet.lang.parsing.JetExpressionParsing
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lexer.JetTokens.*

public trait JetSimpleNameExpression : JetReferenceExpression {

    public fun isImportDirectiveExpression(): Boolean {
        val parent = getParent()
        if (parent == null) {
            return false
        }
        else {
            return parent is JetImportDirective || parent.getParent() is JetImportDirective
        }
    }

    public fun getReferencedName(): String {
        val text = getReferencedNameElement().getNode()!!.getText()
        return JetPsiUtil.unquoteIdentifierOrFieldReference(text)
    }

    public fun getReferencedNameAsName(): Name {
        val name = getReferencedName()
        return Name.identifierNoValidate(name)
    }

    public fun getReferencedNameElement(): PsiElement

    public fun getIdentifier(): PsiElement?

    public fun getReferencedNameElementType(): IElementType {
        return getReferencedNameElement().getNode()!!.getElementType()!!
    }
}

abstract class JetSimpleNameExpressionImpl(node: ASTNode) : JetExpressionImpl(node), JetSimpleNameExpression {
    public override fun getIdentifier(): PsiElement? {
        return findChildByType(JetTokens.IDENTIFIER)
    }

    override fun <R, D> accept(visitor: JetVisitor<R, D>, data: D?): R {
        return visitor.visitSimpleNameExpression(this, data) as R
    }

    public override fun getReferencedNameElement(): PsiElement {
        return findChildByType(REFERENCE_TOKENS) ?:
               findChildByType(JetExpressionParsing.ALL_OPERATIONS) ?:
               this
    }

    class object {
        public val REFERENCE_TOKENS: TokenSet = TokenSet.create(LABEL_IDENTIFIER, IDENTIFIER, FIELD_IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD)
    }
}
