/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtBackingField;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.stubs.KotlinBackingFieldStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinBackingFieldStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinBackingFieldStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinBackingFieldStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtBackingFieldElementType extends KtStubElementType<KotlinBackingFieldStubImpl, KtBackingField> {
    public KtBackingFieldElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtBackingField.class, KotlinBackingFieldStub.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinBackingFieldStubImpl, KtBackingField> getStubFactory() {
        return KotlinBackingFieldStubFactory.INSTANCE;
    }

    @KtImplementationDetail
    @Override
    public StubSerializer<KotlinBackingFieldStubImpl> getStubSerializer() {
        return KotlinBackingFieldStubSerializer.INSTANCE;
    }
}
