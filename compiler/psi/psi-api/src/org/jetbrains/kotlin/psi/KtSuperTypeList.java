/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents the list of super types after the colon in a class header.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * class Foo : Bar(), Baz
 * //          ^________^
 * }</pre>
 */
public class KtSuperTypeList extends KtElementImplStub<KotlinPlaceHolderStub<KtSuperTypeList>> {
    private final AtomicLong modificationStamp = new AtomicLong();

    public KtSuperTypeList(@NotNull ASTNode node) {
        super(node);
    }

    public KtSuperTypeList(@NotNull KotlinPlaceHolderStub<KtSuperTypeList> stub) {
        super(stub, KtStubBasedElementTypes.SUPER_TYPE_LIST);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitSuperTypeList(this, data);
    }

    @NotNull
    public KtSuperTypeListEntry addEntry(@NotNull KtSuperTypeListEntry entry) {
        return EditCommaSeparatedListHelper.INSTANCE.addItem(this, getEntries(), entry);
    }

    public void removeEntry(@NotNull KtSuperTypeListEntry entry) {
        EditCommaSeparatedListHelper.INSTANCE.removeItem(entry);
        if (getEntries().isEmpty()) {
            delete();
        }
    }

    @Override
    public void delete() throws IncorrectOperationException {
        PsiElement left = PsiTreeUtil.skipSiblingsBackward(this, PsiWhiteSpace.class, PsiComment.class);
        if (left == null || left.getNode().getElementType() != KtTokens.COLON) left = this;
        getParent().deleteChildRange(left, this);
    }

    public List<KtSuperTypeListEntry> getEntries() {
        return Arrays.asList(getStubOrPsiChildren(KtTokenSets.SUPER_TYPE_LIST_ENTRIES, KtSuperTypeListEntry.ARRAY_FACTORY));
    }


    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modificationStamp.getAndIncrement();
    }

    public long getModificationStamp() {
        return modificationStamp.get();
    }
}
