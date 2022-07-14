/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtConstructor;
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub;

import java.io.IOException;

public abstract class KtConstructorElementType<T extends KtConstructor<T>>
        extends KtStubElementType<KotlinConstructorStub<T>, T> {

    public KtConstructorElementType(@NotNull @NonNls String debugName, Class<T> tClass, Class<KotlinConstructorStub> stubClass) {
        super(debugName, tClass, stubClass);
    }

    protected abstract KotlinConstructorStub<T> newStub(
            @NotNull StubElement<?> parentStub,
            StringRef nameRef,
            boolean hasBlockBody,
            boolean hasBody
    );

    @NotNull
    @Override
    public KotlinConstructorStub<T> createStub(@NotNull T psi, @NotNull StubElement<?> parentStub) {
        boolean hasBlockBody = psi.hasBlockBody();
        boolean hasBody = psi.hasBody();
        return newStub(parentStub, StringRef.fromString(psi.getName()), hasBlockBody, hasBody);
    }

    @Override
    public void serialize(@NotNull KotlinConstructorStub<T> stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getName());
        dataStream.writeBoolean(stub.hasBlockBody());
        dataStream.writeBoolean(stub.hasBody());
    }

    @NotNull
    @Override
    public KotlinConstructorStub<T> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
            throws IOException {
        StringRef name = dataStream.readName();
        boolean hasBlockBody = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();

        return newStub((StubElement<?>) parentStub, name, hasBlockBody, hasBody);
    }

    @Override
    public void indexStub(@NotNull KotlinConstructorStub<T> stub, @NotNull IndexSink sink) {
        StubIndexService.getInstance().indexConstructor(stub, sink);
    }
}
