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
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetClassStubImpl;
import org.jetbrains.jet.lang.psi.stubs.impl.Utils;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.util.List;

public class JetClassElementType extends JetStubElementType<PsiJetClassStub, JetClass> {
    public JetClassElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetClass.class, PsiJetClassStub.class);
    }

    @NotNull
    @Override
    public JetClass createPsi(@NotNull PsiJetClassStub stub) {
        return !stub.isEnumEntry() ? new JetClass(stub) : new JetEnumEntry(stub);
    }

    @NotNull
    @Override
    public JetClass createPsiFromAst(@NotNull ASTNode node) {
        return node.getElementType() != JetStubElementTypes.ENUM_ENTRY ? new JetClass(node) : new JetEnumEntry(node);
    }

    @Override
    public PsiJetClassStub createStub(@NotNull JetClass psi, StubElement parentStub) {
        FqName fqName = ResolveSessionUtils.safeFqNameForLazyResolve(psi);
        boolean isEnumEntry = psi instanceof JetEnumEntry;
        List<String> superNames = PsiUtilPackage.getSuperNames(psi);
        return new PsiJetClassStubImpl(
                getStubType(isEnumEntry), parentStub, StringRef.fromString(fqName != null ? fqName.asString() : null),
                StringRef.fromString(psi.getName()), Utils.INSTANCE$.wrapStrings(superNames), psi.isTrait(), isEnumEntry,
                psi.isLocal(), psi.isTopLevel());
    }

    @Override
    public void serialize(@NotNull PsiJetClassStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName == null ? null : fqName.asString());
        dataStream.writeBoolean(stub.isTrait());
        dataStream.writeBoolean(stub.isEnumEntry());
        dataStream.writeBoolean(stub.isLocal());
        dataStream.writeBoolean(stub.isTopLevel());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }

    @NotNull
    @Override
    public PsiJetClassStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef qualifiedName = dataStream.readName();
        boolean isTrait = dataStream.readBoolean();
        boolean isEnumEntry = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isTopLevel = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new PsiJetClassStubImpl(getStubType(isEnumEntry), parentStub, qualifiedName, name, superNames,
                                       isTrait, isEnumEntry, isLocal, isTopLevel);
    }

    @Override
    public void indexStub(@NotNull PsiJetClassStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexClass(stub, sink);
    }

    private static JetClassElementType getStubType(boolean isEnumEntry) {
        return isEnumEntry ? JetStubElementTypes.ENUM_ENTRY : JetStubElementTypes.CLASS;
    }
}
