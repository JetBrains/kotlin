/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.KotlinStubElement;

public class KotlinPlaceHolderStubImpl<T extends KtElementImplStub<? extends StubElement<?>>> extends KotlinStubBaseImpl<T>
        implements KotlinPlaceHolderStub<T> {
    public KotlinPlaceHolderStubImpl(StubElement<?> parent, IElementType elementType) {
        super(parent, elementType);
    }

    @Override
    @KtImplementationDetail
    public @NotNull KotlinPlaceHolderStubImpl<T> copyInto(@Nullable StubElement<?> newParent) {
        return new KotlinPlaceHolderStubImpl<>(newParent, getElementType());
    }


    @Override
    public boolean isEquivalentTo(@NotNull KotlinStubElement<?> other) {
        if (other.getClass() != this.getClass()) return false;
        return this.getElementType() == other.getElementType();
    }
}
