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

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl;

import java.io.IOException;

public class KtImportDirectiveElementType extends KtStubElementType<KotlinImportDirectiveStub, KtImportDirective> {
    public KtImportDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtImportDirective.class, KotlinImportDirectiveStub.class);
    }

    @NotNull
    @Override
    public KotlinImportDirectiveStub createStub(@NotNull KtImportDirective psi, StubElement parentStub) {
        FqName importedFqName = psi.getImportedFqName();
        StringRef fqName = StringRef.fromString(importedFqName == null ? null : importedFqName.asString());
        return new KotlinImportDirectiveStubImpl((StubElement<?>) parentStub, psi.isAllUnder(), fqName, psi.isValidImport());
    }

    @Override
    public void serialize(@NotNull KotlinImportDirectiveStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isAllUnder());
        FqName importedFqName = stub.getImportedFqName();
        dataStream.writeName(importedFqName != null ? importedFqName.asString() : null);
        dataStream.writeBoolean(stub.isValid());
    }

    @NotNull
    @Override
    public KotlinImportDirectiveStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isAllUnder = dataStream.readBoolean();
        StringRef importedName = dataStream.readName();
        boolean isValid = dataStream.readBoolean();
        return new KotlinImportDirectiveStubImpl((StubElement<?>) parentStub, isAllUnder, importedName, isValid);
    }
}
