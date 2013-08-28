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

package org.jetbrains.jet.plugin.references;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.plugin.libraries.DecompiledNavigationUtils;

import java.util.Collection;
import java.util.List;

public class JetReferenceUtil {
    private JetReferenceUtil() {}

    public static void findPsiElements(
            @NotNull Project project,
            @NotNull BindingContext context,
            @NotNull List<ResolveResult> results,
            @Nullable DeclarationDescriptor resultingDescriptor
    ) {
        if (resultingDescriptor == null) return;

        DeclarationDescriptor original = resultingDescriptor.getOriginal();

        List<PsiElement> declarations = BindingContextUtils.descriptorToDeclarations(context, original);
        for (PsiElement declaration : declarations) {
            results.add(new PsiElementResolveResult(declaration, true));
        }

        JetDeclaration declarationInDecompiledFile =
                DecompiledNavigationUtils.findDeclarationForReference(project, original);
        if (declarationInDecompiledFile != null) {
            results.add(new PsiElementResolveResult(declarationInDecompiledFile));
        }

        Collection<PsiElement> builtInSymbols =
                project.getComponent(BuiltInsReferenceResolver.class).resolveBuiltInSymbol(original);

        for (PsiElement symbol : builtInSymbols) {
            results.add(new PsiElementResolveResult(symbol));
        }
    }
}
