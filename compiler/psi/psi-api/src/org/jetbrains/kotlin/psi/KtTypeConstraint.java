/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a single type constraint in a {@code where} clause.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun <T> sort(list: List<T>) where T : Comparable<T> {}
 * //                                ^_______________^
 * }</pre>
 */
public class KtTypeConstraint extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeConstraint>>
        implements KtAnnotated, KtAnnotationsContainer {
    /** A shared empty array, which can be reused to avoid unnecessary allocations. */
    public static final KtTypeConstraint[] EMPTY_ARRAY = new KtTypeConstraint[0];

    @KtImplementationDetail
    public KtTypeConstraint(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtTypeConstraint(@NotNull KotlinPlaceHolderStub<KtTypeConstraint> stub) {
        super(stub, KtNodeTypes.TYPE_CONSTRAINT);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraint(this, data);
    }

    /**
     * Returns the reference to the constrained type parameter (the part before {@code :}), or {@code null} if it is absent in
     * incomplete code.
     */
    @Nullable @IfNotParsed
    public KtSimpleNameExpression getSubjectTypeParameterName() {
        return getStubOrPsiChild(KtNodeTypes.REFERENCE_EXPRESSION, KtNameReferenceExpression.class);
    }

    /** Returns the upper bound type reference (the part after {@code :}), or {@code null} if it is absent in incomplete code. */
    @Nullable @IfNotParsed
    public KtTypeReference getBoundTypeReference() {
        return getStubOrPsiChild(KtNodeTypes.TYPE_REFERENCE, KtTypeReference.class);
    }

    @Override
    @NotNull
    public List<KtAnnotation> getAnnotations() {
        return Arrays.asList(getStubOrPsiChildren(KtNodeTypes.ANNOTATION, KtAnnotation.EMPTY_ARRAY));
    }

    @Override
    @NotNull
    public List<KtAnnotationEntry> getAnnotationEntries() {
        return KtPsiUtilKt.collectAnnotationEntriesFromStubOrPsi(this);
    }
}
