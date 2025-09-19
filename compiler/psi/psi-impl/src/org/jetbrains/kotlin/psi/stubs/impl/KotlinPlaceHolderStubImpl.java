/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

public class KotlinPlaceHolderStubImpl<T extends KtElementImplStub<? extends StubElement<?>>> extends KotlinStubBaseImpl<T>
        implements KotlinPlaceHolderStub<T> {
    public KotlinPlaceHolderStubImpl(StubElement<?> parent, IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    @SuppressWarnings("deprecation") // KT-78356
    @Override
    @KtImplementationDetail
    public @NotNull KotlinPlaceHolderStubImpl<T> copyInto(@Nullable StubElement<?> newParent) {
        return new KotlinPlaceHolderStubImpl<>(newParent, getStubType());
    }
}
