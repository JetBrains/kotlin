/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class JetObjectDeclaration extends JetNamedDeclaration implements JetClassOrObject  {
    public JetObjectDeclaration(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getName() {
        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? null : nameAsDeclaration.getName();
    }

    @Override
    public PsiElement getNameIdentifier() {
        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? null : nameAsDeclaration.getNameIdentifier();
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? null : nameAsDeclaration.setName(name);
    }

    @Nullable
    public JetObjectDeclarationName getNameAsDeclaration() {
        return (JetObjectDeclarationName) findChildByType(JetNodeTypes.OBJECT_DECLARATION_NAME);
    }

    @Override
    @Nullable
    public JetDelegationSpecifierList getDelegationSpecifierList() {
        return (JetDelegationSpecifierList) findChildByType(JetNodeTypes.DELEGATION_SPECIFIER_LIST);
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        JetDelegationSpecifierList list = getDelegationSpecifierList();
        return list != null ? list.getDelegationSpecifiers() : Collections.<JetDelegationSpecifier>emptyList();
    }

    @Override
    @NotNull
    public List<JetClassInitializer> getAnonymousInitializers() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getAnonymousInitializers();
    }

    @Override
    public boolean hasPrimaryConstructor() {
        return true;
    }

    @Override
    public JetClassBody getBody() {
        return (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitObjectDeclaration(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitObjectDeclaration(this, data);
    }

    public boolean isObjectLiteral() {
        return getNameAsDeclaration() == null;
    }

    @NotNull
    public PsiElement getObjectKeyword() {
        return findChildByType(JetTokens.OBJECT_KEYWORD);
    }     
}
