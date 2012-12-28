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
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetTypeParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeParameterStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetTypeParameterStubImpl;
import org.jetbrains.jet.lang.types.Variance;

import java.io.IOException;

public class JetTypeParameterElementType extends JetStubElementType<PsiJetTypeParameterStub, JetTypeParameter> {
    public JetTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetTypeParameter createPsiFromAst(@NotNull ASTNode node) {
        return new JetTypeParameter(node);
    }

    @Override
    public JetTypeParameter createPsi(@NotNull PsiJetTypeParameterStub stub) {
        return new JetTypeParameter(stub, JetStubElementTypes.TYPE_PARAMETER);
    }

    @Override
    public PsiJetTypeParameterStub createStub(@NotNull JetTypeParameter psi, StubElement parentStub) {
        JetTypeReference extendsBound = psi.getExtendsBound();
        return new PsiJetTypeParameterStubImpl(JetStubElementTypes.TYPE_PARAMETER, parentStub,
                psi.getName(),
                extendsBound != null ? extendsBound.getText() : null,
                psi.getVariance() == Variance.IN_VARIANCE,
                psi.getVariance() == Variance.OUT_VARIANCE);
    }

    @Override
    public void serialize(PsiJetTypeParameterStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeName(stub.getExtendBoundTypeText());
        dataStream.writeBoolean(stub.isInVariance());
        dataStream.writeBoolean(stub.isOutVariance());
    }

    @Override
    public PsiJetTypeParameterStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef extendBoundTypeText = dataStream.readName();
        boolean isInVariance = dataStream.readBoolean();
        boolean isOutVariance = dataStream.readBoolean();

        return new PsiJetTypeParameterStubImpl(JetStubElementTypes.TYPE_PARAMETER, parentStub,
                name, extendBoundTypeText, isInVariance, isOutVariance);
    }

    @Override
    public void indexStub(PsiJetTypeParameterStub stub, IndexSink sink) {
        // No index
    }
}
