/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import kotlin.SubclassOptInRequired;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * Base implementation of {@link KtTypeParameterListOwner} backed directly by the AST tree, for declarations that do not yet have a
 * stub-based representation.
 *
 * <p>This is an internal implementation base class of the Kotlin PSI.
 *
 * @deprecated a transitional base class kept only until all type-parameter-list owners get stubs; prefer the stub-capable
 * {@link KtTypeParameterListOwnerStub}.
 */
// TODO: Remove when all implementations of JetTypeParameterListOwner get stubs
@Deprecated
@SubclassOptInRequired(markerClass = KtImplementationDetail.class)
abstract class KtTypeParameterListOwnerNotStubbed extends KtNamedDeclarationNotStubbed implements KtTypeParameterListOwner {
    public KtTypeParameterListOwnerNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public KtTypeParameterList getTypeParameterList() {
        return (KtTypeParameterList) findChildByType(KtNodeTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    @Nullable
    public KtTypeConstraintList getTypeConstraintList() {
        return (KtTypeConstraintList) findChildByType(KtNodeTypes.TYPE_CONSTRAINT_LIST);
    }

    @Override
    @NotNull
    public List<KtTypeConstraint> getTypeConstraints() {
        KtTypeConstraintList typeConstraintList = getTypeConstraintList();
        if (typeConstraintList == null) {
            return Collections.emptyList();
        }
        return typeConstraintList.getConstraints();
    }

    @Override
    @NotNull
    public List<KtTypeParameter> getTypeParameters() {
        KtTypeParameterList list = getTypeParameterList();
        if (list == null) return Collections.emptyList();

        return list.getParameters();
    }
}
