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
        return super.toString() + " " +
               descriptor + " " +
               psiElement;
    }

    @Override
    public int hashCode() {
        // Always check with equals
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        JetLookupObject other = (JetLookupObject)obj;

        // Same descriptor - same lookup element
        if (descriptor != null && other.descriptor != null) {
            if (analyzer == other.analyzer) {
                if (descriptor.equals(other.descriptor)) {
                    return true;
                }
            }
            else {
                LOG.warn("Descriptors from different resolve sessions");

                String descriptorText = DescriptorRenderer.TEXT.render(descriptor);
                @SuppressWarnings("ConstantConditions")
                String otherDescriptorText = DescriptorRenderer.TEXT.render(other.descriptor);
                if (descriptorText.equals(otherDescriptorText)) {
                    return true;
                }
            }
        }

        if (psiElement != null && psiElement.equals(other.psiElement)) {
            LOG.warn("Different descriptors for same psi elements");
            return true;
        }

        return (descriptor == null && other.descriptor == null &&
                psiElement == null && other.psiElement == null);
    }
}
