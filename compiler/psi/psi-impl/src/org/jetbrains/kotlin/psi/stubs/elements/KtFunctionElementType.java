/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinFunctionStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinFunctionStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtFunctionElementType extends KtStubElementType<KotlinFunctionStubImpl, KtNamedFunction> {

    public KtFunctionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtNamedFunction.class, KotlinFunctionStub.class);
    }

    @Override
    public StubElementFactory<KotlinFunctionStubImpl, KtNamedFunction> getStubFactory() {
        return KotlinFunctionStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinFunctionStubImpl> getStubSerializer() {
        return KotlinFunctionStubSerializer.INSTANCE;
    }
}
