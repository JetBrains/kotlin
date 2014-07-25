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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetNameReferenceExpressionStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lexer.JetTokens;

import static org.jetbrains.jet.lexer.JetTokens.*;

public class JetNameReferenceExpression extends JetExpressionImplStub<PsiJetNameReferenceExpressionStub> implements JetSimpleNameExpression {

    private static final TokenSet NAME_REFERENCE_EXPRESSIONS = TokenSet.create(IDENTIFIER, FIELD_IDENTIFIER, THIS_KEYWORD, SUPER_KEYWORD);

    public JetNameReferenceExpression(@NotNull ASTNode node) {
        super(node);
    }

    public JetNameReferenceExpression(@NotNull PsiJetNameReferenceExpressionStub stub) {
        super(stub, JetStubElementTypes.REFERENCE_EXPRESSION);
    }

    @Override
    @NotNull
    public String getReferencedName() {
        PsiJetNameReferenceExpressionStub stub = getStub();
        if (stub != null) {
            return stub.getReferencedName();
        }
        return JetSimpleNameExpressionImpl.OBJECT$.getReferencedNameImpl(this);
    }

    @Override
    @NotNull
    public Name getReferencedNameAsName() {
        return JetSimpleNameExpressionImpl.OBJECT$.getReferencedNameAsNameImpl(this);
    }

    @Override
    @NotNull
    public PsiElement getReferencedNameElement() {
        PsiElement element = findChildByType(NAME_REFERENCE_EXPRESSIONS);
        if (element == null) {
            return this;
        }
        return element;
    }

    @Override
    @Nullable
    public PsiElement getIdentifier() {
        return findChildByType(JetTokens.IDENTIFIER);
    }

    @NotNull
    @Override
    public IElementType getReferencedNameElementType() {
        return JetSimpleNameExpressionImpl.OBJECT$.getReferencedNameElementTypeImpl(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitSimpleNameExpression(this, data);
    }
}
