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

package org.jetbrains.jet.plugin.completion;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer;

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for sorting
 */
public final class DeclarationLookupObject {
    private static final Logger LOG = Logger.getInstance("#" + DeclarationLookupObject.class.getName());

    @Nullable
    private final DeclarationDescriptor descriptor;

    @NotNull
    private final KotlinCodeAnalyzer analyzer;

    @Nullable
    private final PsiElement psiElement;

    public DeclarationLookupObject(
            @Nullable DeclarationDescriptor descriptor,
            @NotNull KotlinCodeAnalyzer analyzer,
            @Nullable PsiElement psiElement
    ) {
        this.descriptor = descriptor;
        this.analyzer = analyzer;
        this.psiElement = psiElement;
    }

    @Nullable
    public DeclarationDescriptor getDescriptor() {
        return descriptor;
    }

    @Nullable
    public PsiElement getPsiElement() {
        return psiElement;
    }

    @Override
    public String toString() {
        return super.toString() + " " + descriptor;
    }

    @Override
    public int hashCode() {
        return descriptor != null ? descriptor.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DeclarationLookupObject lookupObject = (DeclarationLookupObject) obj;

        if (!analyzer.equals(lookupObject.analyzer)) {
            LOG.warn("Descriptors from different resolve sessions");
            return false;
        }

        return descriptor != null ? descriptor.equals(lookupObject.descriptor) : lookupObject.descriptor == null;
    }
}
