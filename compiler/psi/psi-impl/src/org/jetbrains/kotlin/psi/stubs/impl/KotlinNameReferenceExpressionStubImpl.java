/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KotlinNameReferenceExpressionStubImpl extends KotlinStubBaseImpl<KtNameReferenceExpression> implements
                                                                                                       KotlinNameReferenceExpressionStub {
    @NotNull
    private final StringRef referencedName;
    private final boolean myClassRef;

    public KotlinNameReferenceExpressionStubImpl(
            @Nullable StubElement<?> parent,
            @NotNull StringRef referencedName,
            boolean myClassRef
    ) {
        super(parent, KtStubElementTypes.REFERENCE_EXPRESSION);
        this.referencedName = referencedName;
        this.myClassRef = myClassRef;
    }

    public boolean isClassRef() {
        return myClassRef;
    }

    @NotNull
    @Override
    public String getReferencedName() {
        return referencedName.getString();
    }

    @Override
    @KtImplementationDetail
    public @NotNull KotlinNameReferenceExpressionStubImpl copyInto(@Nullable StubElement<?> newParent) {
        return new KotlinNameReferenceExpressionStubImpl(
                newParent,
                referencedName,
                myClassRef
        );
    }
}
