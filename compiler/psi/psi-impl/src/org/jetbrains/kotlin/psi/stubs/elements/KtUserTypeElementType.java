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
import org.jetbrains.kotlin.psi.KtUserType;
import org.jetbrains.kotlin.psi.stubs.KotlinUserTypeStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinUserTypeStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinUserTypeStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinUserTypeStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtUserTypeElementType extends KtStubElementType<KotlinUserTypeStubImpl, KtUserType> {
    public KtUserTypeElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtUserType.class, KotlinUserTypeStub.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinUserTypeStubImpl, KtUserType> getStubFactory() {
        return KotlinUserTypeStubFactory.INSTANCE;
    }

    @KtImplementationDetail
    @Override
    public StubSerializer<KotlinUserTypeStubImpl> getStubSerializer() {
        return KotlinUserTypeStubSerializer.INSTANCE;
    }
}
