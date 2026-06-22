/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.stubs.KotlinImportDirectiveStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinImportDirectiveStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinImportDirectiveStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinImportDirectiveStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtImportDirectiveElementType extends KtStubElementType<KotlinImportDirectiveStubImpl, KtImportDirective> {
    public KtImportDirectiveElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtImportDirective.class, KotlinImportDirectiveStub.class);
    }

    @Override
    public StubElementFactory<KotlinImportDirectiveStubImpl, KtImportDirective> getStubFactory() {
        return KotlinImportDirectiveStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinImportDirectiveStubImpl> getStubSerializer() {
        return KotlinImportDirectiveStubSerializer.INSTANCE;
    }
}
