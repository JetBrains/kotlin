/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeProjectionStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeProjectionStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeProjectionStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtTypeProjectionElementType extends KtStubElementType<KotlinTypeProjectionStubImpl, KtTypeProjection> {
    public KtTypeProjectionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeProjection.class, KotlinTypeProjectionStub.class);
    }

    @Override
    public StubElementFactory<KotlinTypeProjectionStubImpl, KtTypeProjection> getStubFactory() {
        return KotlinTypeProjectionStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinTypeProjectionStubImpl> getStubSerializer() {
        return KotlinTypeProjectionStubSerializer.INSTANCE;
    }
}
