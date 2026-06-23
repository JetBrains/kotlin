/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.KtObjectDeclaration;
import org.jetbrains.kotlin.psi.stubs.KotlinObjectStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinObjectStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinObjectStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtObjectElementType extends KtStubElementType<KotlinObjectStubImpl, KtObjectDeclaration> {
    public KtObjectElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtObjectDeclaration.class, KotlinObjectStub.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinObjectStubImpl, KtObjectDeclaration> getStubFactory() {
        return KotlinObjectStubFactory.INSTANCE;
    }

    @KtImplementationDetail
    @Override
    public StubSerializer<KotlinObjectStubImpl> getStubSerializer() {
        return KotlinObjectStubSerializer.INSTANCE;
    }
}
