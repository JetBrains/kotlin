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
import org.jetbrains.jet.renderer.DescriptorRenderer;

/**
 * Stores information about resolved descriptor and position of that descriptor.
 * Position will be used for removing duplicates
 */
public final class JetLookupObject {
    private static final Logger LOG = Logger.getInstance("#" + JetLookupObject.class.getName());

    @Nullable
    private final DeclarationDescriptor descriptor;

    @NotNull
    private final KotlinCodeAnalyzer analyzer;

    @Nullable
    private final PsiElement psiElement;

    public JetLookupObject(@Nullable DeclarationDescriptor descriptor, @NotNull KotlinCodeAnalyzer analyzer, @Nullable PsiElement psiElement) {
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
        return super.toString() + " " + descriptor + " " + psiElement;
    }

    @Override
    public int hashCode() {
        int result = descriptor != null ? descriptor.hashCode() : 0;
        result = 31 * result + (psiElement != null ? psiElement.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        JetLookupObject lookupObject = (JetLookupObject) obj;

        if (!analyzer.equals(lookupObject.analyzer)) {
            LOG.warn("Descriptors from different resolve sessions");
            return false;
        }

        if (descriptor != null ? !descriptor.equals(lookupObject.descriptor) : lookupObject.descriptor != null) return false;
        if (psiElement != null ? !psiElement.equals(lookupObject.psiElement) : lookupObject.psiElement != null) return false;

        return true;
    }
}
