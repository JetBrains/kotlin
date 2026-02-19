/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.List;

/**
 * Represents a list of file-level annotations at the beginning of a Kotlin file.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 *    @file:JvmName("Utils")
 *    @file:Suppress("UNUSED")
 * // ^__________________^
 * package com.example
 * }</pre>
 */
public class KtFileAnnotationList extends KtElementImplStub<KotlinPlaceHolderStub<KtFileAnnotationList>> implements
                                                                                                            KtAnnotationsContainer {

    public KtFileAnnotationList(@NotNull ASTNode node) {
        super(node);
    }

    public KtFileAnnotationList(@NotNull KotlinPlaceHolderStub<KtFileAnnotationList> stub) {
        super(stub, KtStubBasedElementTypes.FILE_ANNOTATION_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitFileAnnotationList(this, data);
    }

    @Override
    @NotNull
    public List<KtAnnotation> getAnnotations() {
        return getStubOrPsiChildrenAsList(KtStubBasedElementTypes.ANNOTATION);
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        return KtPsiUtilKt.collectAnnotationEntriesFromStubOrPsi(this);
    }
}
