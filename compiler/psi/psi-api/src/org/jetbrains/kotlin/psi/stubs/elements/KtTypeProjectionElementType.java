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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl;

import java.io.IOException;

public class KtTypeProjectionElementType extends KtStubElementType<KotlinTypeProjectionStub, KtTypeProjection> {
    public KtTypeProjectionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeProjection.class, KotlinTypeProjectionStub.class);
    }

    @Override
    public KotlinTypeProjectionStub createStub(@NotNull KtTypeProjection psi, StubElement parentStub) {
        return new KotlinTypeProjectionStubImpl(parentStub, psi.getProjectionKind().ordinal());
    }

    @Override
    public void serialize(@NotNull KotlinTypeProjectionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeVarInt(stub.getProjectionKind().ordinal());
    }

    @NotNull
    @Override
    public KotlinTypeProjectionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        int projectionKindOrdinal = dataStream.readVarInt();
        return new KotlinTypeProjectionStubImpl(parentStub, projectionKindOrdinal);
    }
}
