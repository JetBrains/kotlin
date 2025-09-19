/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.List;

public class KtTypeConstraint extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeConstraint>>
        implements KtAnnotated, KtAnnotationsContainer {
    public KtTypeConstraint(@NotNull ASTNode node) {
        super(node);
    }

    public KtTypeConstraint(@NotNull KotlinPlaceHolderStub<KtTypeConstraint> stub) {
        super(stub, KtStubBasedElementTypes.TYPE_CONSTRAINT);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraint(this, data);
    }

    @Nullable @IfNotParsed
    @SuppressWarnings("deprecation")
    public KtSimpleNameExpression getSubjectTypeParameterName() {
        return getStubOrPsiChild(KtStubBasedElementTypes.REFERENCE_EXPRESSION);
    }

    @Nullable @IfNotParsed
    @SuppressWarnings("deprecation")
    public KtTypeReference getBoundTypeReference() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_REFERENCE);
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
