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

package org.jetbrains.jet.lang.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetImportList;
import org.jetbrains.jet.lang.psi.stubs.PsiJetImportListStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetImportListStubImpl;

import java.io.IOException;

public class JetImportListElementType extends JetStubElementType<PsiJetImportListStub, JetImportList> {
    public JetImportListElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetImportList createPsiFromAst(@NotNull ASTNode node) {
        return new JetImportList(node);
    }

    @Override
    public JetImportList createPsi(@NotNull PsiJetImportListStub stub) {
        return new JetImportList(stub);
    }

    @Override
    public PsiJetImportListStub createStub(
            @NotNull JetImportList psi, StubElement parentStub
    ) {
        return new PsiJetImportListStubImpl(parentStub);
    }

    @Override
    public void serialize(@NotNull PsiJetImportListStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        //do nothing;
    }

    @NotNull
    @Override
    public PsiJetImportListStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new PsiJetImportListStubImpl(parentStub);
    }

    @Override
    public void indexStub(@NotNull PsiJetImportListStub stub, @NotNull IndexSink sink) {
        //do nothing
    }
}