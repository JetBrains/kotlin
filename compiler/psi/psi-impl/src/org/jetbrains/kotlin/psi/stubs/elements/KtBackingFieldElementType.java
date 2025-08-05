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
import org.jetbrains.kotlin.psi.KtBackingField;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBackingFieldStubImpl;

import java.io.IOException;

public class KtBackingFieldElementType extends KtStubElementType<KotlinBackingFieldStubImpl, KtBackingField> {
    public KtBackingFieldElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtBackingField.class, KotlinBackingFieldStub.class);
    }

    @NotNull
    @Override
    public KotlinBackingFieldStubImpl createStub(@NotNull KtBackingField psi, StubElement parentStub) {
        return new KotlinBackingFieldStubImpl(parentStub, psi.hasInitializer());
    }

    @Override
    public void serialize(@NotNull KotlinBackingFieldStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.hasInitializer());
    }

    @NotNull
    @Override
    public KotlinBackingFieldStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean hasInitializer = dataStream.readBoolean();
        return new KotlinBackingFieldStubImpl(parentStub, hasInitializer);
    }
}
