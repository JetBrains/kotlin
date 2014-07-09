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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.plugin.libraries.DecompiledNavigationUtils;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

import java.util.Collection;
import java.util.Collections;

public final class DescriptorToDeclarationUtil {
    private DescriptorToDeclarationUtil() {
    }

    @Nullable
    public static PsiElement getDeclaration(@NotNull JetFile file, @NotNull DeclarationDescriptor descriptor) {
        return getDeclaration(file.getProject(), descriptor);
    }

    @Nullable
    public static PsiElement getDeclaration(@NotNull Project project, @NotNull DeclarationDescriptor descriptor) {
        Collection<PsiElement> elements = DescriptorToSourceUtils.descriptorToDeclarations(descriptor);
        if (elements.isEmpty()) {
            elements = findDecompiledAndBuiltInDeclarations(project, descriptor);
        }
        if (!elements.isEmpty()) {
            return elements.iterator().next();
        }
        return null;
    }

    @NotNull
    public static Collection<PsiElement> findDecompiledAndBuiltInDeclarations(
            @NotNull Project project,
            @NotNull DeclarationDescriptor descriptor
    ) {
        BuiltInsReferenceResolver libraryReferenceResolver = project.getComponent(BuiltInsReferenceResolver.class);
        Collection<PsiElement> elements = libraryReferenceResolver.resolveBuiltInSymbol(descriptor);
        if (!elements.isEmpty()) {
            return elements;
        }

        JetDeclaration decompiledDeclaration = DecompiledNavigationUtils.findDeclarationForReference(project, descriptor);
        if (decompiledDeclaration != null) {
            return Collections.<PsiElement>singleton(decompiledDeclaration);
        }
        return Collections.emptySet();
    }
}
