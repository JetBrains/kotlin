/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl;

import java.io.IOException;

public class KtPlaceHolderStubElementType<T extends KtElementImplStub<? extends StubElement<?>>> extends
                                                                                                 KtStubElementType<KotlinPlaceHolderStub<T>, T> {
    public KtPlaceHolderStubElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, KotlinPlaceHolderStub.class);
    }

    @Override
    public KotlinPlaceHolderStub<T> createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new KotlinPlaceHolderStubImpl<>(parentStub, this);
    }

    @Override
    public void serialize(@NotNull KotlinPlaceHolderStub<T> stub, @NotNull StubOutputStream dataStream) throws IOException {
        //do nothing
    }

    @NotNull
    @Override
    public KotlinPlaceHolderStub<T> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new KotlinPlaceHolderStubImpl<>(parentStub, this);
    }
}
