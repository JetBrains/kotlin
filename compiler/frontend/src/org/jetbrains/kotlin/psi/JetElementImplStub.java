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
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementType;

import java.util.Arrays;
import java.util.List;

public class JetElementImplStub<T extends StubElement> extends StubBasedPsiElementBase<T>
        implements JetElement, StubBasedPsiElement<T> {
    public JetElementImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public JetElementImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getElementType().toString();
    }

    @Override
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            accept((JetVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @NotNull
    @Override
    public JetFile getContainingJetFile() {
        PsiFile file = getContainingFile();
        assert file instanceof JetFile : "JetElement not inside JetFile: " + file + " " + file.getText();
        return (JetFile) file;
    }

    @Override
    public <D> void acceptChildren(@NotNull JetVisitor<Void, D> visitor, D data) {
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof JetElement) {
                ((JetElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitJetElement(this, data);
    }

    @Override
    public PsiReference getReference() {
        PsiReference[] references = getReferences();
        if (references.length == 1) return references[0];
        else return null;
    }

    @NotNull
    @Override
    public PsiReference[] getReferences() {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this, PsiReferenceService.Hints.NO_HINTS);
    }

    @NotNull
    protected <PsiT extends JetElementImplStub<?>, StubT extends StubElement> List<PsiT> getStubOrPsiChildrenAsList(
            @NotNull JetStubElementType<StubT, PsiT> elementType
    ) {
        return Arrays.asList(getStubOrPsiChildren(elementType, elementType.getArrayFactory()));
    }
}
