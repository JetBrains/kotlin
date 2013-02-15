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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.stubs.PsiJetObjectStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetObjectStubImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

public class JetObjectElementType extends JetStubElementType<PsiJetObjectStub, JetObjectDeclaration> {
    public JetObjectElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetObjectDeclaration createPsiFromAst(@NotNull ASTNode node) {
        return new JetObjectDeclaration(node);
    }

    @Override
    public JetObjectDeclaration createPsi(@NotNull PsiJetObjectStub stub) {
        return new JetObjectDeclaration(stub);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        if (super.shouldCreateStub(node)) {
            PsiElement psiElement = node.getPsi();
            if (psiElement instanceof JetObjectDeclaration) {
                JetObjectDeclaration objectDeclaration = (JetObjectDeclaration) psiElement;
                return objectDeclaration.getName() != null || isClassObject(objectDeclaration);
            }
        }

        return false;
    }

    @Override
    public PsiJetObjectStub createStub(@NotNull JetObjectDeclaration psi, @NotNull StubElement parentStub) {
        String name = psi.getName();
        FqName fqName = psi.getFqName();
        return new PsiJetObjectStubImpl(JetStubElementTypes.OBJECT_DECLARATION, parentStub, name, fqName, psi.isTopLevel(), isClassObject(psi));
    }

    @Override
    public void serialize(PsiJetObjectStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.toString() : null);
        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.isClassObject());
    }

    @Override
    public PsiJetObjectStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef fqNameStr = dataStream.readName();
        FqName fqName = fqNameStr != null ? new FqName(fqNameStr.toString()) : null;
        boolean isTopLevel = dataStream.readBoolean();
        boolean isClassObject = dataStream.readBoolean();

        return new PsiJetObjectStubImpl(JetStubElementTypes.OBJECT_DECLARATION, parentStub, name, fqName, isTopLevel, isClassObject);
    }

    @Override
    public void indexStub(PsiJetObjectStub stub, IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexObject(stub, sink);
    }

    private static boolean isClassObject(@NotNull JetObjectDeclaration objectDeclaration) {
        return objectDeclaration.getParent() instanceof JetClassObject;
    }
}
