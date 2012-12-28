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
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetParameterList;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterListStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetParameterListStubImpl;

import java.io.IOException;

public class JetParameterListElementType extends JetStubElementType<PsiJetParameterListStub, JetParameterList> {
    public JetParameterListElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetParameterList createPsiFromAst(@NotNull ASTNode node) {
        return new JetParameterList(node);
    }

    @Override
    public JetParameterList createPsi(@NotNull PsiJetParameterListStub stub) {
        return new JetParameterList(stub, JetStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    public PsiJetParameterListStub createStub(@NotNull JetParameterList psi, StubElement parentStub) {
        return new PsiJetParameterListStubImpl(JetStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
    }

    @Override
    public void serialize(PsiJetParameterListStub stub, StubOutputStream dataStream) throws IOException {
        // Nothing to serialize
    }

    @Override
    public PsiJetParameterListStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new PsiJetParameterListStubImpl(JetStubElementTypes.VALUE_PARAMETER_LIST, parentStub);
    }

    @Override
    public void indexStub(PsiJetParameterListStub stub, IndexSink sink) {
        // No index
    }
}

