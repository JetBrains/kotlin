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

package org.jetbrains.kotlin.idea.codeInsight;

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.psi.JetElement;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.types.JetType;

public class JetTypeDeclarationProvider implements TypeDeclarationProvider {
    @Override
    public PsiElement[] getSymbolTypeDeclarations(PsiElement symbol) {
        if (symbol instanceof JetElement && symbol.getContainingFile() instanceof JetFile) {
            BindingContext bindingContext = ResolvePackage.analyze((JetElement)symbol);
            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, symbol);
            if (descriptor instanceof CallableDescriptor) {
                JetType type = ((CallableDescriptor) descriptor).getReturnType();
                if (type != null) {
                    ClassifierDescriptor classifierDescriptor = type.getConstructor().getDeclarationDescriptor();
                    if (classifierDescriptor != null) {
                        PsiElement typeElement = DescriptorToSourceUtils.descriptorToDeclaration(classifierDescriptor);
                        if (typeElement != null) {
                            return new PsiElement[] {typeElement};
                        }
                    }
                }
            }
        }
        return new PsiElement[0];
    }
}
