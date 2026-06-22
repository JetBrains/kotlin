/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPlaceHolderStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtPlaceHolderStubElementType<T extends KtElementImplStub<? extends StubElement<?>>> extends
                                                                                                 KtStubElementType<KotlinPlaceHolderStubImpl<T>, T> {
    private final KotlinPlaceHolderStubFactory<T> stubFactory = new KotlinPlaceHolderStubFactory<>(this);
    private final KotlinPlaceHolderStubSerializer<T> stubSerializer = new KotlinPlaceHolderStubSerializer<>(this);

    public KtPlaceHolderStubElementType(@NotNull @NonNls String debugName, @NotNull Class<T> psiClass) {
        super(debugName, psiClass, KotlinPlaceHolderStub.class);
    }

    @Override
    public StubElementFactory<KotlinPlaceHolderStubImpl<T>, T> getStubFactory() {
        return stubFactory;
    }

    @Override
    public StubSerializer<KotlinPlaceHolderStubImpl<T>> getStubSerializer() {
        return stubSerializer;
    }
}
