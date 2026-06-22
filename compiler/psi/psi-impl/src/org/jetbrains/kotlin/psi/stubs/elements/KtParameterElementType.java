/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.stubs.KotlinParameterStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinParameterStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinParameterStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinParameterStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtParameterElementType extends KtStubElementType<KotlinParameterStubImpl, KtParameter> {
    public KtParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtParameter.class, KotlinParameterStub.class);
    }

    @Override
    public StubElementFactory<KotlinParameterStubImpl, KtParameter> getStubFactory() {
        return KotlinParameterStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinParameterStubImpl> getStubSerializer() {
        return KotlinParameterStubSerializer.INSTANCE;
    }
}
