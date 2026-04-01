/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import com.intellij.util.ArrayFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtElementImplStub;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPlaceHolderStubImpl;

import java.io.IOException;
import java.util.function.Function;

public class KtPlaceHolderStubElementType<T extends KtElementImplStub<? extends StubElement<?>>> extends
                                                                                                 KtStubElementType<KotlinPlaceHolderStubImpl<T>, T> {
    @SuppressWarnings("unchecked")
    public KtPlaceHolderStubElementType(
            @NotNull @NonNls String debugName,
            @NotNull Function<ASTNode, T> psiFromAstFactory,
            @NotNull Function<? super KotlinPlaceHolderStubImpl<T>, ? extends T> psiFromStubFactory,
            @NotNull ArrayFactory<T> arrayFactory,
            boolean isExpression
    ) {
        super(debugName, psiFromAstFactory, (Function) psiFromStubFactory, arrayFactory, isExpression);
    }

    @NotNull
    @Override
    public KotlinPlaceHolderStubImpl<T> createStub(@NotNull T psi, StubElement<?> parentStub) {
        return new KotlinPlaceHolderStubImpl<>(parentStub, this);
    }

    @Override
    public void serialize(@NotNull KotlinPlaceHolderStubImpl<T> stub, @NotNull StubOutputStream dataStream) throws IOException {
        //do nothing
    }

    @NotNull
    @Override
    public KotlinPlaceHolderStubImpl<T> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new KotlinPlaceHolderStubImpl<>(parentStub, this);
    }
}
