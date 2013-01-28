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
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetTypeParameterList;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterListStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetTypeParameterListStubImpl;

import java.io.IOException;

public class JetTypeParameterListElementType extends JetStubElementType<PsiJetTypeParameterListStub, JetTypeParameterList> {
    public JetTypeParameterListElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetTypeParameterList createPsiFromAst(@NotNull ASTNode node) {
        return new JetTypeParameterList(node);
    }

    @Override
    public JetTypeParameterList createPsi(@NotNull PsiJetTypeParameterListStub stub) {
        return new JetTypeParameterList(stub, JetStubElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    public PsiJetTypeParameterListStub createStub(@NotNull JetTypeParameterList psi, StubElement parentStub) {
        return new PsiJetTypeParameterListStubImpl(JetStubElementTypes.TYPE_PARAMETER_LIST, parentStub);
    }

    @Override
    public void serialize(PsiJetTypeParameterListStub stub, StubOutputStream dataStream) throws IOException {
        // Do nothing
    }

    @Override
    public PsiJetTypeParameterListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new PsiJetTypeParameterListStubImpl(JetStubElementTypes.TYPE_PARAMETER_LIST, parentStub);
    }

    @Override
    public void indexStub(PsiJetTypeParameterListStub stub, IndexSink sink) {
        // No index
    }
}

