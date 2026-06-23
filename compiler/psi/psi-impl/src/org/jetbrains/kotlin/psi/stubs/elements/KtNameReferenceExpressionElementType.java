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
import org.jetbrains.kotlin.psi.KtNameReferenceExpression;
import org.jetbrains.kotlin.psi.stubs.KotlinNameReferenceExpressionStub;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinNameReferenceExpressionStubFactory;
import org.jetbrains.kotlin.psi.stubs.factory.KotlinNameReferenceExpressionStubSerializer;
import org.jetbrains.kotlin.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl;

@SuppressWarnings("UnstableApiUsage") // KT-78356: the platform stub-decoupling API is still @ApiStatus.Experimental
public class KtNameReferenceExpressionElementType
        extends KtStubElementType<KotlinNameReferenceExpressionStubImpl, KtNameReferenceExpression> {
    public KtNameReferenceExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, KtNameReferenceExpression.class, KotlinNameReferenceExpressionStub.class);
    }

    @KtImplementationDetail
    @Override
    public StubElementFactory<KotlinNameReferenceExpressionStubImpl, KtNameReferenceExpression> getStubFactory() {
        return KotlinNameReferenceExpressionStubFactory.INSTANCE;
    }

    @KtImplementationDetail
    @Override
    public StubSerializer<KotlinNameReferenceExpressionStubImpl> getStubSerializer() {
        return KotlinNameReferenceExpressionStubSerializer.INSTANCE;
    }
}
