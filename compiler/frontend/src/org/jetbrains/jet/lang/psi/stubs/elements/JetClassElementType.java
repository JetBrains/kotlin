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
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetEnumEntry;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetClassStubImpl;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.util.List;

public class JetClassElementType extends JetStubElementType<PsiJetClassStub, JetClass> {

    public JetClassElementType(@NotNull @NonNls String debugName) {
        super(debugName);
    }

    @Override
    public JetClass createPsi(@NotNull PsiJetClassStub stub) {
        return !stub.isEnumEntry() ? new JetClass(stub) : new JetEnumEntry(stub);
    }

    @Override
    public JetClass createPsiFromAst(@NotNull ASTNode node) {
        return node.getElementType() != JetStubElementTypes.ENUM_ENTRY ? new JetClass(node) : new JetEnumEntry(node);
    }

    @Override
    public PsiJetClassStub createStub(@NotNull JetClass psi, StubElement parentStub) {
        FqName fqName = JetPsiUtil.getFQName(psi);
        boolean isEnumEntry = psi instanceof JetEnumEntry;
        return new PsiJetClassStubImpl(getStubType(isEnumEntry), parentStub, fqName != null ? fqName.getFqName() : null, psi.getName(),
                                       psi.getSuperNames(), psi.isTrait(), psi.isEnum(), isEnumEntry, psi.isAnnotation(), psi.isInner());
    }

    @Override
    public void serialize(PsiJetClassStub stub, StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.getFqName());
        dataStream.writeBoolean(stub.isTrait());
        dataStream.writeBoolean(stub.isEnumClass());
        dataStream.writeBoolean(stub.isEnumEntry());
        dataStream.writeBoolean(stub.isAnnotation());
        dataStream.writeBoolean(stub.isInner());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }

    @Override
    public PsiJetClassStub deserialize(StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();
        boolean isTrait = dataStream.readBoolean();
        boolean isEnumClass = dataStream.readBoolean();
        boolean isEnumEntry = dataStream.readBoolean();
        boolean isAnnotation = dataStream.readBoolean();
        boolean isInner = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new PsiJetClassStubImpl(getStubType(isEnumEntry), parentStub, qualifiedName, name, superNames,
                                       isTrait, isEnumClass, isEnumEntry, isAnnotation, isInner);
    }

    @Override
    public void indexStub(PsiJetClassStub stub, IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexClass(stub, sink);
    }

    private static JetClassElementType getStubType(boolean isEnumEntry) {
        return isEnumEntry ? JetStubElementTypes.ENUM_ENTRY : JetStubElementTypes.CLASS;
    }
}
