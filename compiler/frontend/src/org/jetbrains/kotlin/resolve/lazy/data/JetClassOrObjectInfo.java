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
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.name.FqName;

import java.util.Collections;
import java.util.List;

public abstract class JetClassOrObjectInfo<E extends JetClassOrObject> implements JetClassLikeInfo {
    protected final E element;

    protected JetClassOrObjectInfo(@NotNull E element) {
        this.element = element;
    }

    @Nullable
    public Name getName() {
        return element.getNameAsName();
    }

    @Override
    public JetClassOrObject getCorrespondingClassOrObject() {
        return element;
    }

    @Override
    @Nullable
    public JetModifierList getModifierList() {
        return element.getModifierList();
    }

    @Override
    @NotNull
    public List<JetDeclaration> getDeclarations() {
        return element.getDeclarations();
    }

    @NotNull
    @Override
    public List<JetObjectDeclaration> getDefaultObjects() {
        JetClassBody body = element.getBody();
        if (body == null) {
            return Collections.emptyList();
        }
        return body.getAllDefaultObjects();
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
        if (file instanceof JetFile) {
            JetFile jetFile = (JetFile) file;
            return jetFile.getPackageFqName();
        }
        throw new IllegalArgumentException("Not in a JetFile: " + element);
    }

    @NotNull
    @Override
    public List<JetAnnotationEntry> getDanglingAnnotations() {
        JetClassBody body = element.getBody();
        return body == null ? Collections.<JetAnnotationEntry>emptyList() : body.getDanglingAnnotations();
    }

    @Override
    public String toString() {
        return "info for " + element.getText();
    }
}
