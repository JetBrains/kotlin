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
import org.jetbrains.kotlin.psi.KtPropertyAccessor;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyAccessorStubImpl;

import java.io.IOException;

public class KtPropertyAccessorElementType extends KtStubElementType<KotlinPropertyAccessorStubImpl, KtPropertyAccessor> {
    public KtPropertyAccessorElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtPropertyAccessor.class, KotlinPropertyAccessorStub.class);
    }

    @NotNull
    @Override
    public KotlinPropertyAccessorStubImpl createStub(@NotNull KtPropertyAccessor psi, StubElement parentStub) {
        return new KotlinPropertyAccessorStubImpl(
                parentStub,
                psi.isGetter(),
                psi.hasBody(),
                psi.hasBlockBody(),
                KtPsiUtilKt.isLegacyContractPresentPsiCheck(psi)
        );
    }

    @Override
    public void serialize(@NotNull KotlinPropertyAccessorStubImpl stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeBoolean(stub.isGetter());
        dataStream.writeBoolean(stub.hasBody());
        dataStream.writeBoolean(stub.hasNoExpressionBody());
        dataStream.writeBoolean(stub.mayHaveContract());
    }

    @NotNull
    @Override
    public KotlinPropertyAccessorStubImpl deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        boolean isGetter = dataStream.readBoolean();
        boolean hasBody = dataStream.readBoolean();
        boolean hasNoExpressionBody = dataStream.readBoolean();
        boolean mayHaveContract = dataStream.readBoolean();
        return new KotlinPropertyAccessorStubImpl(parentStub, isGetter, hasBody, hasNoExpressionBody, mayHaveContract);
    }
}
