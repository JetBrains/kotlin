/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName;

import java.util.Collections;
import java.util.List;

public abstract class KtTypeParameterListOwnerStub<T extends KotlinStubWithFqName<?>>
        extends KtNamedDeclarationStub<T> implements KtTypeParameterListOwner {
    public KtTypeParameterListOwnerStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtTypeParameterListOwnerStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public KtTypeParameterList getTypeParameterList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    @Nullable
    @SuppressWarnings("deprecation")
    public KtTypeConstraintList getTypeConstraintList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.TYPE_CONSTRAINT_LIST);
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

    @Nullable
    public KtContextReceiverList getContextReceiverList() {
        KtModifierList modifierList = getModifierList();
        return modifierList == null ? null : modifierList.getContextReceiverList();
    }

    /**
     * Retrieves a list of context receiver lists associated with the current element.
     * If the element does not have a modifier list, an empty list is returned.
     * <p>
     * Valid code may have only either empty or one {@link KtContextReceiverList},
     * so {@link #getContextReceiverList } is preferable.
     *
     * @return a non-null list of {@link KtContextReceiverList} defined in the associated modifier list.
     * Returns an empty list if no context receiver lists are present.
     */
    @NotNull
    public List<KtContextReceiverList> getContextReceiverLists() {
        KtModifierList modifierList = getModifierList();
        return modifierList == null ? Collections.emptyList() : modifierList.getContextReceiverLists();
    }
}
