/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.stubs.impl;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtImplementationDetail;
import org.jetbrains.kotlin.psi.KtProjectionKind;
import org.jetbrains.kotlin.psi.KtTypeProjection;
import org.jetbrains.kotlin.psi.stubs.KotlinTypeProjectionStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

public class KotlinTypeProjectionStubImpl extends KotlinStubBaseImpl<KtTypeProjection> implements KotlinTypeProjectionStub {

    private final int projectionKindOrdinal;

    public KotlinTypeProjectionStubImpl(@Nullable StubElement<?> parent, int projectionKindOrdinal) {
        super(parent, KtStubElementTypes.TYPE_PROJECTION);
        this.projectionKindOrdinal = projectionKindOrdinal;
    }

    @NotNull
    @Override
    public KtProjectionKind getProjectionKind() {
        return KtProjectionKind.values()[projectionKindOrdinal];
    }

    @Override
    @NotNull
    @KtImplementationDetail
    public KotlinTypeProjectionStubImpl copyInto(@Nullable StubElement<?> newParent) {
        return new KotlinTypeProjectionStubImpl(
                newParent,
                projectionKindOrdinal
        );
    }
}
