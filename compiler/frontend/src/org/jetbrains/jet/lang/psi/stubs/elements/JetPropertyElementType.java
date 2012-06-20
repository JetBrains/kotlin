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
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetTypeReference;
import org.jetbrains.jet.lang.psi.stubs.PsiJetPropertyStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetPropertyStubImpl;

import java.io.IOException;

/**
 * @author Nikolay Krasko
 */
public class JetPropertyElementType extends JetStubElementType<PsiJetPropertyStub, JetProperty> {
    public JetPropertyElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetProperty createPsiFromAst(@NotNull ASTNode node) {
        return new JetProperty(node);
    }

    @Override
    public PsiJetPropertyStub createStub(LighterAST tree, LighterASTNode node, StubElement parentStub) {
        return null;
    }

    @Override
    public JetProperty createPsi(@NotNull PsiJetPropertyStub stub) {
        return new JetProperty(stub, JetStubElementTypes.PROPERTY);
    }

    @Override
    public PsiJetPropertyStub createStub(@NotNull JetProperty psi, StubElement parentStub) {
        JetTypeReference typeRef = psi.getPropertyTypeRef();
        JetExpression expression = psi.getInitializer();

        return new PsiJetPropertyStubImpl(JetStubElementTypes.PROPERTY, parentStub,
            psi.getName(), psi.isVar(), psi.isLocal(),
            typeRef != null ? typeRef.getText() : null,
            expression != null ? expression.getText() : null);
    }

    @Override
    public void serialize(PsiJetPropertyStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.isVar());
        dataStream.writeBoolean(stub.isLocal());
        dataStream.writeName(stub.getTypeText());
        dataStream.writeName(stub.getInferenceBodyText());
    }

    @Override
    public PsiJetPropertyStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        boolean isVar = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        StringRef typeText = dataStream.readName();
        StringRef inferenceBodyText = dataStream.readName();

        return new PsiJetPropertyStubImpl(JetStubElementTypes.PROPERTY, parentStub,
                                          name, isVar, isLocal, typeText, inferenceBodyText);
    }

    @Override
    public void indexStub(PsiJetPropertyStub stub, IndexSink sink) {
        // No index
    }
}
