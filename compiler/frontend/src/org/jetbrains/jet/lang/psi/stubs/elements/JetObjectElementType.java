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
import org.jetbrains.jet.lang.psi.JetClassObject;
import org.jetbrains.jet.lang.psi.JetObjectDeclaration;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lang.psi.stubs.PsiJetObjectStub;
import org.jetbrains.jet.lang.psi.stubs.impl.PsiJetObjectStubImpl;
import org.jetbrains.jet.lang.psi.stubs.impl.Utils;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.io.IOException;
import java.util.List;

public class JetObjectElementType extends JetStubElementType<PsiJetObjectStub, JetObjectDeclaration> {
    public JetObjectElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetObjectDeclaration.class, PsiJetObjectStub.class);
    }

    @Override
    public PsiJetObjectStub createStub(@NotNull JetObjectDeclaration psi, StubElement parentStub) {
        String name = psi.getName();
        FqName fqName = ResolveSessionUtils.safeFqNameForLazyResolve(psi);
        List<String> superNames = PsiUtilPackage.getSuperNames(psi);
        return new PsiJetObjectStubImpl(parentStub, StringRef.fromString(name), fqName, Utils.INSTANCE$.wrapStrings(superNames),
                                        psi.isTopLevel(), isClassObject(psi), psi.isLocal(), psi.isObjectLiteral());
    }

    @Override
    public void serialize(@NotNull PsiJetObjectStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.toString() : null);

        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.isClassObject());
        dataStream.writeBoolean(stub.isLocal());
        dataStream.writeBoolean(stub.isObjectLiteral());

        List<String> superNames = stub.getSuperNames();
        dataStream.writeVarInt(superNames.size());
        for (String name : superNames) {
            dataStream.writeName(name);
        }
    }

    @NotNull
    @Override
    public PsiJetObjectStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef fqNameStr = dataStream.readName();
        FqName fqName = fqNameStr != null ? new FqName(fqNameStr.toString()) : null;

        boolean isTopLevel = dataStream.readBoolean();
        boolean isClassObject = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isObjectLiteral = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new PsiJetObjectStubImpl(parentStub, name, fqName, superNames, isTopLevel, isClassObject, isLocal, isObjectLiteral);
    }

    @Override
    public void indexStub(@NotNull PsiJetObjectStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexObject(stub, sink);
    }

    private static boolean isClassObject(@NotNull JetObjectDeclaration objectDeclaration) {
        return objectDeclaration.getParent() instanceof JetClassObject;
    }
}
