/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElementFactory;
import com.intellij.psi.stubs.StubSerializer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtDeclarationModifierList;
import org.jetbrains.kotlin.psi.stubs.KotlinModifierListStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinModifierListStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinModifierListStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinModifierListStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtModifierListElementType extends KtStubElementType<KotlinModifierListStubImpl, KtDeclarationModifierList> {
    public KtModifierListElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtDeclarationModifierList.class, KotlinModifierListStub.class);
    }

    @Override
    public StubElementFactory<KotlinModifierListStubImpl, KtDeclarationModifierList> getStubFactory() {
        return KotlinModifierListStubFactory.INSTANCE;
    }

    @Override
    public StubSerializer<KotlinModifierListStubImpl> getStubSerializer() {
        return KotlinModifierListStubSerializer.INSTANCE;
    }
}
