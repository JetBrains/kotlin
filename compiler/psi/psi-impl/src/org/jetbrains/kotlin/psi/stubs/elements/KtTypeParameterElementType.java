/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl;

import java.io.IOException;

public class KtTypeParameterElementType extends KtStubElementType<KotlinTypeParameterStubImpl, KtTypeParameter> {
    public KtTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeParameter.class, KotlinTypeParameterStub.class);
    }

    @NotNull
    @Override
    public KotlinTypeParameterStubImpl createStub(@NotNull KtTypeParameter psi, StubElement parentStub) {
        return new KotlinTypeParameterStubImpl(
                (StubElement<?>) parentStub, StringRef.fromString(psi.getName())
        );
    }

    @Override
    public void serialize(@NotNull KotlinTypeParameterStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getName());
    }

    @NotNull
    @Override
    public KotlinTypeParameterStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        StringRef name = dataStream.readName();
        return new KotlinTypeParameterStubImpl((StubElement<?>) parentStub, name);
    }
}
