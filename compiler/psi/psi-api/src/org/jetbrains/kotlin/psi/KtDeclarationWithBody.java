/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface KtDeclarationWithBody extends KtDeclaration {
    @Nullable
    KtExpression getBodyExpression();

    @Nullable
    PsiElement getEqualsToken();

    @Override
    @Nullable
    String getName();

    @Nullable
    default KtContractEffectList getContractDescription() {
        return null;
    }

    default boolean hasContractEffectList() {
        return getContractDescription() != null;
    }

    /**
     * Whether the declaration may have a contract.
     * </p>
     * <b>false</b> means that the declaration is definitely having no contract,
     * but <b>true</b> doesn't guarantee that the declaration has a contract.
     */
    default boolean mayHaveContract() {
        return false;
    }

    boolean hasBlockBody();

    boolean hasBody();

    boolean hasDeclaredReturnType();

    @NotNull
    List<KtParameter> getValueParameters();

    @Nullable
    default KtBlockExpression getBodyBlockExpression() {
        KtExpression bodyExpression = getBodyExpression();
        if (bodyExpression instanceof KtBlockExpression) {
            return (KtBlockExpression) bodyExpression;
        }

        return null;
    }
}

