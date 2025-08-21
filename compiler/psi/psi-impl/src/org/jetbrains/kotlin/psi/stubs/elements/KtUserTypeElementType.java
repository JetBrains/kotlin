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
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl;

import java.io.IOException;

import static org.jetbrains.kotlin.psi.stubs.elements.TypeBeanSerializationKt.*;

public class KtUserTypeElementType extends KtStubElementType<KotlinUserTypeStubImpl, KtUserType> {
    public KtUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtUserType.class, KotlinUserTypeStub.class);
    }

    @NotNull
    @Override
    public KotlinUserTypeStubImpl createStub(@NotNull KtUserType psi, StubElement parentStub) {
        return new KotlinUserTypeStubImpl((StubElement<?>) parentStub, null, null);
    }

    @Override
    public void serialize(@NotNull KotlinUserTypeStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        serializeTypeBean(dataStream, stub.getUpperBound());
        serializeTypeBean(dataStream, stub.getAbbreviatedType());
    }

    @NotNull
    @Override
    public KotlinUserTypeStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new KotlinUserTypeStubImpl((StubElement<?>) parentStub,
                                          deserializeTypeBean(dataStream),
                                          deserializeClassTypeBean(dataStream));
    }
}
