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

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.psi.*;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType;

import java.util.Arrays;
import java.util.List;

public class KtElementImplStub<T extends StubElement<?>> extends StubBasedPsiElementBase<T>
        implements KtElement, StubBasedPsiElement<T> {
    public KtElementImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public KtElementImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return KotlinLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getElementType().toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof KtVisitor) {
            accept((KtVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @NotNull
    @Override
    public KtFile getContainingKtFile() {
        PsiFile file = getContainingFile();
        if (!(file instanceof KtFile)) {
            // KtElementImpl.copy() might be the reason for this exception
            String fileString = file.isValid() ? (" " + file.getText()) : "";
            throw new IllegalStateException("KtElement not inside KtFile: " + file + fileString +
                                            " for element " + this + " of type " + this.getClass() + " node = " + getNode());
        }
        return (KtFile) file;
    }

    @Override
    public <D> void acceptChildren(@NotNull KtVisitor<Void, D> visitor, D data) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof KtElement) {
                ((KtElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitKtElement(this, data);
    }

    @Override
    public void delete() throws IncorrectOperationException {
        KtElementUtilsKt.deleteSemicolon(this);
        super.delete();
    }

    @Override
    @SuppressWarnings("deprecation")
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        if (references.length == 1) return references[0];
        else return null;
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return KotlinReferenceProvidersService.getReferencesFromProviders(this);
    }

    @NotNull
    protected <PsiT extends KtElementImplStub<?>, StubT extends StubElement> List<PsiT> getStubOrPsiChildrenAsList(
            @NotNull KtStubElementType<StubT, PsiT> elementType
    ) {
        return Arrays.asList(getStubOrPsiChildren(elementType, elementType.getArrayFactory()));
    }

    @NotNull
    @Override
    public KtElement getPsiOrParent() {
        return this;
    }

    @Override
    public PsiElement getParent() {
        PsiElement substitute = KtPsiUtilKt.getParentSubstitute(this);
        return substitute != null ? substitute : super.getParent();
    }
}
