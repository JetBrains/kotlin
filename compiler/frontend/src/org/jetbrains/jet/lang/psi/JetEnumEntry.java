/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public class JetEnumEntry extends JetClass {
    public JetEnumEntry(@NotNull ASTNode node) {
        super(node);
    }

    public JetEnumEntry(@NotNull PsiJetClassStub stub) {
        super(stub);
    }

    @Override
    public String getName() {
        PsiJetClassStub classStub = getStub();
        if (classStub != null) {
            return classStub.getName();
        }

        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? "<Anonymous>" : nameAsDeclaration.getName();
    }

    @Override
    public PsiElement getNameIdentifier() {
        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? null : nameAsDeclaration.getNameIdentifier();
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.ENUM_ENTRY;
    }

    @Override
    public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        JetObjectDeclarationName nameAsDeclaration = getNameAsDeclaration();
        return nameAsDeclaration == null ? null : nameAsDeclaration.setName(name);
    }

    @NotNull
    @Override
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        JetInitializerList initializerList = (JetInitializerList) findChildByType(JetNodeTypes.INITIALIZER_LIST);
        if (initializerList == null) {
            return Collections.emptyList();
        }
        return initializerList.getInitializers();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitEnumEntry(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitEnumEntry(this, data);
    }
}
