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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.load.java.structure.JavaElement;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaSourceFactoryOwner;

public abstract class JavaElementImpl<Psi extends PsiElement> implements JavaElement, JavaSourceFactoryOwner {
    protected final JavaElementPsiSource<Psi> psiElementSource;

    @Override
    @NotNull
    public JavaElementSourceFactory getSourceFactory() {
        return psiElementSource.getFactory();
    }

    protected JavaElementImpl(@NotNull JavaElementPsiSource<Psi> psiElementSource) {
        this.psiElementSource = psiElementSource;
    }

    @NotNull
    public Psi getPsi() {
        return psiElementSource.getPsi();
    }

    @Override
    public int hashCode() {
        return getPsi().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof JavaElementImpl && getPsi().equals(((JavaElementImpl) obj).getPsi());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + psiElementSource;
    }
}
