/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtSecondaryConstructor;
import org.jetbrains.kotlin.psi.stubs.KotlinConstructorStub;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinConstructorStubImpl;

public class KtSecondaryConstructorElementType extends KtConstructorElementType<KtSecondaryConstructor> {
    public KtSecondaryConstructorElementType(@NotNull String debugName) {
        super(debugName, KtSecondaryConstructor.class, KotlinConstructorStub.class);
    }

    @Override
    protected KotlinConstructorStub<KtSecondaryConstructor> newStub(
            @NotNull StubElement<?> parentStub,
            StringRef nameRef,
            boolean hasBlockBody,
            boolean hasBody
    ) {
        return new KotlinConstructorStubImpl<>(
                parentStub, KtStubElementTypes.SECONDARY_CONSTRUCTOR, nameRef, hasBlockBody, hasBody
        );
    }
}
