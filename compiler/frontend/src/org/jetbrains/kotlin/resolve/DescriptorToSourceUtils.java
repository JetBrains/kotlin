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

package org.jetbrains.kotlin.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.source.SourcePackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;

public final class DescriptorToSourceUtils {
    private static void collectEffectiveReferencedDescriptors(@NotNull List<DeclarationDescriptor> result, @NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            CallableMemberDescriptor.Kind kind = ((CallableMemberDescriptor) descriptor).getKind();
            if (kind != DECLARATION && kind != SYNTHESIZED) {
                for (DeclarationDescriptor overridden: ((CallableMemberDescriptor) descriptor).getOverriddenDescriptors()) {
                    collectEffectiveReferencedDescriptors(result, overridden.getOriginal());
                }
                return;
            }
        }
        result.add(descriptor);
    }

    @NotNull
    public static Collection<DeclarationDescriptor> getEffectiveReferencedDescriptors(@NotNull DeclarationDescriptor descriptor) {
        List<DeclarationDescriptor> result = new ArrayList<DeclarationDescriptor>();
        collectEffectiveReferencedDescriptors(result, descriptor.getOriginal());
        return result;
    }

    @Nullable
    public static PsiElement getSourceFromDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (!(descriptor instanceof DeclarationDescriptorWithSource)) {
            return null;
        }
        return SourcePackage.getPsi(((DeclarationDescriptorWithSource) descriptor).getSource());
    }

    // NOTE this is also used by KDoc
    @Nullable
    public static PsiElement descriptorToDeclaration(@NotNull DeclarationDescriptor descriptor) {
        for (DeclarationDescriptor declarationDescriptor : getEffectiveReferencedDescriptors(descriptor.getOriginal())) {
            PsiElement source = getSourceFromDescriptor(declarationDescriptor);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    @Nullable
    public static PsiElement classDescriptorToDeclaration(@NotNull ClassDescriptor clazz) {
        return getSourceFromDescriptor(clazz);
    }

    @Nullable
    public static JetFile getContainingFile(@NotNull DeclarationDescriptor declarationDescriptor) {
        // declarationDescriptor may describe a synthesized element which doesn't have PSI
        // To workaround that, we find a top-level parent (which is inside a PackageFragmentDescriptor), which is guaranteed to have PSI
        DeclarationDescriptor descriptor = findTopLevelParent(declarationDescriptor);
        if (descriptor == null) return null;

        PsiElement declaration = descriptorToDeclaration(descriptor);
        if (declaration == null) return null;

        PsiFile containingFile = declaration.getContainingFile();
        if (!(containingFile instanceof JetFile)) return null;
        return (JetFile) containingFile;
    }

    @Nullable
    private static DeclarationDescriptor findTopLevelParent(@NotNull DeclarationDescriptor declarationDescriptor) {
        DeclarationDescriptor descriptor = declarationDescriptor;
        if (declarationDescriptor instanceof PropertyAccessorDescriptor) {
            descriptor = ((PropertyAccessorDescriptor) descriptor).getCorrespondingProperty();
        }
        while (!(descriptor == null || DescriptorUtils.isTopLevelDeclaration(descriptor))) {
            descriptor = descriptor.getContainingDeclaration();
        }
        return descriptor;
    }

    private DescriptorToSourceUtils() {}
}
