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
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPropertyStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinPropertyStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinPropertyStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtPropertyElementType extends KtStubElementType<KotlinPropertyStubImpl, KtProperty> {
    public KtPropertyElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtProperty.class, KotlinPropertyStub.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinPropertyStubImpl, KtProperty> getStubFactory() {
        return KotlinPropertyStubFactory.INSTANCE;
    }

    @KtImplementationDetail
    @Override
    public StubSerializer<KotlinPropertyStubImpl> getStubSerializer() {
        return KotlinPropertyStubSerializer.INSTANCE;
    }
}
