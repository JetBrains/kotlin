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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFileStub;
import org.jetbrains.jet.lang.psi.stubs.PsiJetFunctionStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetFunctionStubImpl;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class JetFunctionElementType extends JetStubElementType<PsiJetFunctionStub, JetNamedFunction> {

    public JetFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetNamedFunction createPsiFromAst(@NotNull ASTNode node) {
        return new JetNamedFunction(node);
    }

    @Override
    public PsiJetFunctionStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JetNamedFunction createPsi(@NotNull PsiJetFunctionStub stub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public PsiJetFunctionStub createStub(@NotNull JetNamedFunction psi, StubElement parentStub) {
        final boolean isTopLevel = parentStub instanceof PsiJetFileStub;
        final boolean isExtension = psi.getReceiverTypeRef() != null;

        return new PsiJetFunctionStubImpl(
                JetStubElementTypes.FUNCTION, parentStub, psi.getName(),
                isTopLevel, isExtension);
    }

    @Override
    public void serialize(PsiJetFunctionStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.isExtension());
    }

    @Override
    public PsiJetFunctionStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        final StringRef name = dataStream.readName();
        final boolean isTopLevel = dataStream.readBoolean();
        final boolean isExtension = dataStream.readBoolean();

        return new PsiJetFunctionStubImpl(JetStubElementTypes.FUNCTION, parentStub, name, isTopLevel, isExtension);
    }

    @Override
    public void indexStub(PsiJetFunctionStub stub, IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexFunction(stub, sink);
    }
}
