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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.references.BuiltInsReferenceResolver;

import java.util.Collection;

public final class DescriptorToDeclarationUtil {
    private DescriptorToDeclarationUtil() {
    }

    public static PsiElement getDeclaration(JetFile file, DeclarationDescriptor descriptor, BindingContext bindingContext) {
        return getDeclaration(file.getProject(), descriptor, bindingContext);
    }

    public static PsiElement getDeclaration(Project project, DeclarationDescriptor descriptor, BindingContext bindingContext) {
        Collection<PsiElement> elements = BindingContextUtils.descriptorToDeclarations(bindingContext, descriptor);

        if (elements.isEmpty()) {
            BuiltInsReferenceResolver libraryReferenceResolver =
                    project.getComponent(BuiltInsReferenceResolver.class);
            elements = libraryReferenceResolver.resolveStandardLibrarySymbol(descriptor);
        }

        if (!elements.isEmpty()) {
            return elements.iterator().next();
        }

        return null;
    }
}
