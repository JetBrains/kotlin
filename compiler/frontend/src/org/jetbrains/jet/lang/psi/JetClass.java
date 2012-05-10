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
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetClass extends JetTypeParameterListOwner
        implements JetClassOrObject, JetModifierListOwner, StubBasedPsiElement<PsiJetClassStub<?>> {

    private PsiJetClassStub stub;

    public JetClass(@NotNull ASTNode node) {
        super(node);
    }

    // TODO (stubs)
//    public JetClass(final PsiJetClassStub stub) {
//        this.stub = stub;
//    }

    @Override
    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @NotNull
    public List<JetSecondaryConstructor> getSecondaryConstructors() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getSecondaryConstructors();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitClass(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
    }

    @Nullable
    public JetParameterList getPrimaryConstructorParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getPrimaryConstructorParameters() {
        JetParameterList list = getPrimaryConstructorParameterList();
        if (list == null) return Collections.emptyList();
        return list.getParameters();
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

    @Nullable
    public JetModifierList getPrimaryConstructorModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST);
    }

    @NotNull
    public List<JetClassInitializer> getAnonymousInitializers() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getAnonymousInitializers();
    }

    public boolean hasPrimaryConstructor() {
        return getPrimaryConstructorParameterList() != null;
    }

    @Override
    public JetObjectDeclarationName getNameAsDeclaration() {
        return (JetObjectDeclarationName) findChildByType(JetNodeTypes.OBJECT_DECLARATION_NAME);
    }

    @Override
    public JetClassBody getBody() {
        return (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
    }

    @Nullable
    public JetClassObject getClassObject() {
        JetClassBody body = getBody();
        if (body == null) return null;
        return body.getClassObject();
    }

    public List<JetProperty> getProperties() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getProperties();
    }

    public boolean isTrait() {
        return findChildByType(JetTokens.TRAIT_KEYWORD) != null;
    }

    public boolean isAnnotation() {
        return hasModifier(JetTokens.ANNOTATION_KEYWORD);
    }

    @Override
    public IStubElementType getElementType() {
        // TODO (stubs)
        return JetStubElementTypes.CLASS;
    }

    @Override
    public PsiJetClassStub<?> getStub() {
        // TODO (stubs)
        return null;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        JetPsiUtil.deleteClass(this);
    }
}
