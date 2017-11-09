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

package org.jetbrains.kotlin.resolve.lazy.data;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;

import java.util.Collections;
import java.util.List;

public abstract class KtClassOrObjectInfo<E extends KtClassOrObject> implements KtClassLikeInfo {
    protected final E element;

    protected KtClassOrObjectInfo(@NotNull E element) {
        this.element = element;
    }

    @Nullable
    public Name getName() {
        return element.getNameAsName();
    }

    @Override
    @NotNull
    public KtClassOrObject getCorrespondingClassOrObject() {
        return element;
    }

    @Override
    @Nullable
    public KtModifierList getModifierList() {
        return element.getModifierList();
    }

    @Override
    @NotNull
    public List<KtDeclaration> getDeclarations() {
        return element.getDeclarations();
    }

    @NotNull
    @Override
    public List<KtObjectDeclaration> getCompanionObjects() {
        KtClassBody body = element.getBody();
        if (body == null) {
            return Collections.emptyList();
        }
        return body.getAllCompanionObjects();
    }

    @NotNull
    @Override
    public PsiElement getScopeAnchor() {
        return element;
    }

    @NotNull
    @Override
    public FqName getContainingPackageFqName() {
        PsiFile file = element.getContainingFile();
        if (file instanceof KtFile) {
            KtFile jetFile = (KtFile) file;
            return jetFile.getPackageFqName();
        }
        throw new IllegalArgumentException("Not in a KtFile: " + element);
    }

    @NotNull
    @Override
    public List<KtAnnotationEntry> getDanglingAnnotations() {
        KtClassBody body = element.getBody();
        return body == null ? Collections.emptyList() : body.getDanglingAnnotations();
    }

    @NotNull
    @Override
    public List<? extends KtParameter> getPrimaryConstructorParameters() {
        return element.getPrimaryConstructorParameters();
    }

    @Override
    public String toString() {
        return "info for " + element.getText();
    }
}
