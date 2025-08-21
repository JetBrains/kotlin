/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.kotlin.psi.KtPropertyAccessor;
import org.jetbrains.kotlin.psi.stubs.KotlinPropertyAccessorStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KotlinPropertyAccessorStubImpl extends KotlinStubBaseImpl<KtPropertyAccessor> implements KotlinPropertyAccessorStub {
    private final boolean isGetter;
    private final boolean hasBody;
    private final boolean hasNoExpressionBody;
    private final boolean mayHaveContract;

    public KotlinPropertyAccessorStubImpl(
            StubElement parent,
            boolean isGetter,
            boolean hasBody,
            boolean hasNoExpressionBody,
            boolean mayHaveContract
    ) {
        super(parent, KtStubElementTypes.PROPERTY_ACCESSOR);
        this.isGetter = isGetter;
        this.hasBody = hasBody;
        this.hasNoExpressionBody = hasNoExpressionBody;
        this.mayHaveContract = mayHaveContract;
    }

    @Override
    public boolean isGetter() {
        return isGetter;
    }

    @Override
    public boolean getHasBody() {
        return hasBody;
    }

    @Override
    public boolean getHasNoExpressionBody() {
        return hasNoExpressionBody;
    }

    @Override
    public boolean getMayHaveContract() {
        return mayHaveContract;
    }
}
