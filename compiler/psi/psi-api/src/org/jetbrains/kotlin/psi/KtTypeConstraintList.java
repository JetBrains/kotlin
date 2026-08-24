/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a {@code where} clause containing type constraints.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * fun <T, U> foo() where T : A, U : B {}
 * //               ^________________^
 * }</pre>
 */
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
public class KtTypeConstraintList extends KtElementImplStub<KotlinPlaceHolderStub<KtTypeConstraintList>> {
    @KtImplementationDetail
    public KtTypeConstraintList(@NotNull ASTNode node) {
        super(node);
    }

    @KtImplementationDetail
    public KtTypeConstraintList(@NotNull KotlinPlaceHolderStub<KtTypeConstraintList> stub) {
        super(stub, KtNodeTypes.TYPE_CONSTRAINT_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraintList(this, data);
    }

    /** Returns the constraints in this {@code where} clause, in source order; empty if there are none. */
    @NotNull
    public List<KtTypeConstraint> getConstraints() {
        return Arrays.asList(getStubOrPsiChildren(KtNodeTypes.TYPE_CONSTRAINT, KtTypeConstraint.EMPTY_ARRAY));
    }
}
