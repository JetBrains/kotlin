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
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub;
import org.jetbrains.kotlin.psi.stubs.StubUtils;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl;
import org.jetbrains.kotlin.psi.stubs.impl.Utils;

import java.io.IOException;
import java.util.List;

public class KtObjectElementType extends KtStubElementType<KotlinObjectStub, KtObjectDeclaration> {
    public KtObjectElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtObjectDeclaration.class, KotlinObjectStub.class);
    }

    @NotNull
    @Override
    public KotlinObjectStub createStub(@NotNull KtObjectDeclaration psi, StubElement parentStub) {
        String name = psi.getName();
        FqName fqName = KtPsiUtilKt.safeFqNameForLazyResolve(psi);
        List<String> superNames = KtPsiUtilKt.getSuperNames(psi);
        return new KotlinObjectStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(name), fqName, psi.getClassId(), Utils.INSTANCE.wrapStrings(superNames),
                psi.isTopLevel(), psi.isCompanion(), psi.isLocal(), psi.isObjectLiteral()
        );
    }

    @Override
    public void serialize(@NotNull KotlinObjectStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());

        FqName fqName = stub.getFqName();
        dataStream.writeName(fqName != null ? fqName.toString() : null);

        StubUtils.serializeClassId(dataStream, stub.getClassId());

        dataStream.writeBoolean(stub.isTopLevel());
        dataStream.writeBoolean(stub.isCompanion());
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

        ClassId classId = StubUtils.deserializeClassId(dataStream);

        boolean isTopLevel = dataStream.readBoolean();
        boolean isCompanion = dataStream.readBoolean();
        boolean isLocal = dataStream.readBoolean();
        boolean isObjectLiteral = dataStream.readBoolean();

        int superCount = dataStream.readVarInt();
        StringRef[] superNames = StringRef.createArray(superCount);
        for (int i = 0; i < superCount; i++) {
            superNames[i] = dataStream.readName();
        }

        return new KotlinObjectStubImpl(
                (StubElement<?>) parentStub, name, fqName, classId, superNames, isTopLevel, isCompanion, isLocal, isObjectLiteral
        );
    }

    @Override
    public void indexStub(@NotNull KotlinObjectStub stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexObject(stub, sink);
    }
}
