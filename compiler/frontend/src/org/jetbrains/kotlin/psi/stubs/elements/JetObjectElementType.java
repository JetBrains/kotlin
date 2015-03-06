/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.Utils;
import org.jetbrains.kotlin.resolve.lazy.ResolveSessionUtils;

import java.io.IOException;
import java.util.List;

public class JetObjectElementType extends JetStubElementType<KotlinObjectStub, JetObjectDeclaration> {
    public JetObjectElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetObjectDeclaration.class, KotlinObjectStub.class);
    }

    @Override
    public KotlinObjectStub createStub(@NotNull JetObjectDeclaration psi, StubElement parentStub) {
        String name = psi.getName();
        FqName fqName = ResolveSessionUtils.safeFqNameForLazyResolve(psi);
        List<String> superNames = PsiUtilPackage.getSuperNames(psi);
        return new KotlinObjectStubImpl(parentStub, StringRef.fromString(name), fqName, Utils.INSTANCE$.wrapStrings(superNames),
                                        psi.isTopLevel(), psi.isDefault(), psi.isLocal(), psi.isObjectLiteral());
    }

    @Override
    public void serialize(@NotNull KotlinObjectStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.toString() : null);

        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.isDefault());
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
    public KotlinObjectStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        StringRef fqNameStr = dataStream.readName();
        FqName fqName = fqNameStr != null ? new FqName(fqNameStr.toString()) : null;

        boolean isTopLevel = dataStream.readBoolean();
        boolean isDefault = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isObjectLiteral = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new KotlinObjectStubImpl(parentStub, name, fqName, superNames, isTopLevel, isDefault, isLocal, isObjectLiteral);
    }

    @Override
    public void indexStub(@NotNull KotlinObjectStub stub, @NotNull IndexSink sink) {
        StubIndexServiceFactory.getInstance().indexObject(stub, sink);
    }
}
