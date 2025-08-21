/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface KtCallableDeclaration extends KtNamedDeclaration, KtDeclarationWithReturnType, KtTypeParameterListOwner {
    @Nullable
    KtParameterList getValueParameterList();

    @NotNull
    List<KtParameter> getValueParameters();

    @Nullable
    KtTypeReference getReceiverTypeReference();

    @NotNull
    default List<KtContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    KtTypeReference getTypeReference();

    @SuppressWarnings("unused") // used in Kotlin IDE plugin
    @Nullable
    KtTypeReference setTypeReference(@Nullable KtTypeReference typeRef);

    @Nullable
    PsiElement getColon();
}
