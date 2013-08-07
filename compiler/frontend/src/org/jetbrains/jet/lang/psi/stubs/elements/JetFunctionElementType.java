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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFunctionStubImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

public class JetFunctionElementType extends JetStubElementType<PsiJetFunctionStub, JetNamedFunction> {

    public JetFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetNamedFunction createPsiFromAst(@NotNull ASTNode node) {
        return new JetNamedFunction(node);
    }

    @Override
    public JetNamedFunction createPsi(@NotNull PsiJetFunctionStub stub) {
        return new JetNamedFunction(stub);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        if (super.shouldCreateStub(node)) {
            PsiElement psi = node.getPsi();
            if (psi instanceof JetNamedFunction) {
                JetNamedFunction function = (JetNamedFunction) psi;
                return function.getName() != null;
            }
        }

        return false;
    }

    @Override
    public PsiJetFunctionStub createStub(@NotNull JetNamedFunction psi, @NotNull StubElement parentStub) {
        boolean isTopLevel = psi.getParent() instanceof JetFile;
        boolean isExtension = psi.getReceiverTypeRef() != null;

        FqName qualifiedName = psi.getFqName();

        return new PsiJetFunctionStubImpl(JetStubElementTypes.FUNCTION, parentStub, psi.getName(), isTopLevel, qualifiedName, isExtension);
    }

    @Override
    public void serialize(@NotNull PsiJetFunctionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.asString() : null);

        dataStream.writeBoolean(stub.isExtension());
    }

    @NotNull
    @Override
    public PsiJetFunctionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isTopLevel = dataStream.readBoolean();

        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

        boolean isExtension = dataStream.readBoolean();

        return new PsiJetFunctionStubImpl(JetStubElementTypes.FUNCTION, parentStub, name, isTopLevel, fqName, isExtension);
    }

    @Override
    public void indexStub(@NotNull PsiJetFunctionStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexFunction(stub, sink);
    }
}
