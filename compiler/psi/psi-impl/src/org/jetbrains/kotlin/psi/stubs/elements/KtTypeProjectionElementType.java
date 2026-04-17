/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

public class KtTypeProjectionElementType extends KtStubElementType<KotlinTypeProjectionStubImpl, KtTypeProjection> {
    public KtTypeProjectionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeProjection.class, KotlinTypeProjectionStub.class);
    }

    @NotNull
    @Override
    public KotlinTypeProjectionStubImpl createStub(@NotNull KtTypeProjection psi, StubElement parentStub) {
        return new KotlinTypeProjectionStubImpl(parentStub, psi.getProjectionKind().ordinal());
    }

    @Override
    public void serialize(@NotNull KotlinTypeProjectionStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeVarInt(stub.getProjectionKind().ordinal());
    }

    @NotNull
    @Override
    public KotlinTypeProjectionStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        int projectionKindOrdinal = dataStream.readVarInt();
        return new KotlinTypeProjectionStubImpl(parentStub, projectionKindOrdinal);
    }
}
