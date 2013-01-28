/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.lazy;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;

public class IdentitySmartPointer<E extends PsiElement> implements SmartPsiElementPointer<E> {
    private final E element;

    public IdentitySmartPointer(@NotNull E element) {
        this.element = element;
    }

    @Override
    public E getElement() {
        return element;
    }

    @Override
    public PsiFile getContainingFile() {
        return element.getContainingFile();
    }

    @NotNull
    @Override
    public Project getProject() {
        return element.getProject();
    }

    @Override
    public VirtualFile getVirtualFile() {
        return element.getContainingFile().getVirtualFile();
    }

    @Override
    public Segment getRange() {
        return element.getTextRange();
    }
}
