/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtTypeParameter;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeParameterStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeParameterStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinTypeParameterStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinTypeParameterStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtTypeParameterElementType extends KtStubElementType<KotlinTypeParameterStubImpl, KtTypeParameter> {
    public KtTypeParameterElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtTypeParameter.class, KotlinTypeParameterStub.class);
    }

    @Override
    public StubElementFactory<KotlinTypeParameterStubImpl, KtTypeParameter> getStubFactory() {
        return KotlinTypeParameterStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinTypeParameterStubImpl> getStubSerializer() {
        return KotlinTypeParameterStubSerializer.INSTANCE;
    }
}
