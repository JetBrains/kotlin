/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.kdoc.psi.api.KDoc;
import org.jetbrains.kotlin.psi.findDocComment.FindDocCommentKt;

import java.util.concurrent.atomic.AtomicLong;

public abstract class KtDeclarationStub<T extends StubElement<?>> extends KtModifierListOwnerStub<T> implements KtDeclaration {
    private final AtomicLong modificationStamp = new AtomicLong();

    public KtDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modificationStamp.getAndIncrement();
    }

    public long getModificationStamp() {
        return modificationStamp.get();
    }

    @Nullable
    @Override
    public KDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }

    @Override
    public PsiElement getOriginalElement() {
        KotlinDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(KotlinDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getOriginalElement(this) : this;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        KotlinDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(KotlinDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getNavigationElement(this) : this;
    }
}
