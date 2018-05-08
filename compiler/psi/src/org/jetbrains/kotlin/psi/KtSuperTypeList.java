/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.stubs.KotlinPlaceHolderStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class KtSuperTypeList extends KtElementImplStub<KotlinPlaceHolderStub<KtSuperTypeList>> {
    private final AtomicLong modificationStamp = new AtomicLong();

    public KtSuperTypeList(@NotNull ASTNode node) {
        super(node);
    }

    public KtSuperTypeList(@NotNull KotlinPlaceHolderStub<KtSuperTypeList> stub) {
        super(stub, KtStubElementTypes.SUPER_TYPE_LIST);
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
        return Arrays.asList(getStubOrPsiChildren(KtStubElementTypes.SUPER_TYPE_LIST_ENTRIES, KtSuperTypeListEntry.ARRAY_FACTORY));
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
