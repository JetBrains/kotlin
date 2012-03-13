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
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetClassStubImpl;
import org.jetbrains.jet.lang.resolve.FqName;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class JetClassElementType extends JetStubElementType<PsiJetClassStub, JetClass> {

    public JetClassElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public PsiJetClassStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JetClass createPsi(@NotNull PsiJetClassStub stub) {
        // return getPsiFactory(stub).createClass(stub);
        return null;
    }

    @Override
    public JetClass createPsiFromAst(@NotNull ASTNode node) {
        return new JetClass(node);
    }

    @Override
    public PsiJetClassStub createStub(@NotNull JetClass psi, StubElement parentStub) {
        FqName fqName = JetPsiUtil.getFQName(psi);
        return new PsiJetClassStubImpl(JetStubElementTypes.CLASS, parentStub, fqName != null ? fqName.getFqName() : null, psi.getName());
    }

    @Override
    public void serialize(PsiJetClassStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getQualifiedName());
    }

    @Override
    public PsiJetClassStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        final StringRef name = dataStream.readName();
        final StringRef qualifiedName = dataStream.readName();
        
        final JetClassElementType type = JetStubElementTypes.CLASS;
        final PsiJetClassStubImpl classStub = new PsiJetClassStubImpl(type, parentStub, qualifiedName, name);

        return classStub;
    }

    @Override
    public void indexStub(PsiJetClassStub stub, IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexClass(stub, sink);
    }
}
