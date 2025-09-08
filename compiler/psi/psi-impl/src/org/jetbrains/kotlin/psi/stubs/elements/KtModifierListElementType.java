/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtModifierList;
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl;

import java.io.IOException;

import static org.jetbrains.kotlin.psi.stubs.impl.ModifierMaskUtils.computeMaskFromModifierList;

public class KtModifierListElementType<T extends KtModifierList> extends KtStubElementType<KotlinModifierListStubImpl, T> {
    public KtModifierListElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, KotlinModifierListStub.class);
    }

    @NotNull
    @Override
    public KotlinModifierListStubImpl createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new KotlinModifierListStubImpl(parentStub, computeMaskFromModifierList(psi));
    }

    @Override
    public void serialize(@NotNull KotlinModifierListStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        DataInputOutputUtil.writeLONG(dataStream, stub.getMask());
    }

    @NotNull
    @Override
    public KotlinModifierListStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        long mask = DataInputOutputUtil.readLONG(dataStream);
        return new KotlinModifierListStubImpl(parentStub, mask);
    }
}
