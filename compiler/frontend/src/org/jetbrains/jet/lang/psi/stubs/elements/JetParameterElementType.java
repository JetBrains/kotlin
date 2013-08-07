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
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.psi.stubs.PsiJetParameterStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetParameterStubImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;

public class JetParameterElementType extends JetStubElementType<PsiJetParameterStub, JetParameter> {
    public JetParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetParameter createPsiFromAst(@NotNull ASTNode node) {
        return new JetParameter(node);
    }

    @Override
    public JetParameter createPsi(@NotNull PsiJetParameterStub stub) {
        return new JetParameter(stub);
    }

    @Override
    public PsiJetParameterStub createStub(@NotNull JetParameter psi, StubElement parentStub) {
        JetTypeReference typeReference = psi.getTypeReference();
        JetExpression defaultValue = psi.getDefaultValue();

        return new PsiJetParameterStubImpl(parentStub, psi.getFqName(),
                psi.getName(), psi.isMutable(), psi.isVarArg(),
                typeReference != null ? typeReference.getText() : null,
                defaultValue != null ? defaultValue.getText() : null);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        if (!super.shouldCreateStub(node)) {
            return false;
        }
        PsiElement psi = node.getPsi();
        return psi instanceof JetParameter && !((JetParameter) psi).isLoopParameter();
    }

    @Override
    public void serialize(@NotNull PsiJetParameterStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isMutable());
        dataStream.writeBoolean(stub.isVarArg());
        dataStream.writeName(stub.getTypeText());
        dataStream.writeName(stub.getDefaultValueText());
        FqName name = stub.getFqName();
        dataStream.writeName(name != null ? name.asString() : null);
    }

    @NotNull
    @Override
    public PsiJetParameterStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isMutable = dataStream.readBoolean();
        boolean isVarArg = dataStream.readBoolean();
        StringRef typeText = dataStream.readName();
        StringRef defaultValueText = dataStream.readName();
        StringRef fqNameAsString = dataStream.readName();
        FqName fqName = fqNameAsString != null ? new FqName(fqNameAsString.toString()) : null;

         return new PsiJetParameterStubImpl(parentStub, fqName, name, isMutable, isVarArg,
                                           typeText, defaultValueText);
    }

    @Override
    public void indexStub(@NotNull PsiJetParameterStub stub, @NotNull IndexSink sink) {
        // No index
    }
}
