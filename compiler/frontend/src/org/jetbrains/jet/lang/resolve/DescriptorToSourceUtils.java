/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.source.SourcePackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind.SYNTHESIZED;

public final class DescriptorToSourceUtils {

    @Nullable
    private static PsiElement doGetDescriptorToDeclaration(@NotNull DeclarationDescriptor descriptor) {
        DeclarationDescriptor original = descriptor.getOriginal();
        if (!(original instanceof DeclarationDescriptorWithSource)) {
            return null;
        }
        return SourcePackage.getPsi(((DeclarationDescriptorWithSource) original).getSource());
    }

    // NOTE this is also used by KDoc
    @Nullable
    public static PsiElement descriptorToDeclaration(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclaration((CallableMemberDescriptor) descriptor);
        }
        else if (descriptor instanceof ClassDescriptor) {
            return classDescriptorToDeclaration((ClassDescriptor) descriptor);
        }
        else {
            return doGetDescriptorToDeclaration(descriptor);
        }
    }

    @NotNull
    public static List<PsiElement> descriptorToDeclarations(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return callableDescriptorToDeclarations((CallableMemberDescriptor) descriptor);
        }
        else {
            PsiElement psiElement = descriptorToDeclaration(descriptor);
            if (psiElement != null) {
                return Lists.newArrayList(psiElement);
            } else {
                return Lists.newArrayList();
            }
        }
    }

    @Nullable
    public static PsiElement callableDescriptorToDeclaration(@NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() == DECLARATION || callable.getKind() == SYNTHESIZED) {
            return doGetDescriptorToDeclaration(callable);
        }
        //TODO: should not use this method for fake_override and delegation
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        if (overriddenDescriptors.size() == 1) {
            return callableDescriptorToDeclaration(overriddenDescriptors.iterator().next());
        }
        return null;
    }

    @NotNull
    public static List<PsiElement> callableDescriptorToDeclarations(@NotNull CallableMemberDescriptor callable) {
        if (callable.getKind() == DECLARATION || callable.getKind() == SYNTHESIZED) {
            PsiElement psiElement = doGetDescriptorToDeclaration(callable);
            return psiElement != null ? Lists.newArrayList(psiElement) : Lists.<PsiElement>newArrayList();
        }

        List<PsiElement> r = new ArrayList<PsiElement>();
        Set<? extends CallableMemberDescriptor> overriddenDescriptors = callable.getOverriddenDescriptors();
        for (CallableMemberDescriptor overridden : overriddenDescriptors) {
            r.addAll(callableDescriptorToDeclarations(overridden));
        }
        return r;
    }

    @Nullable
    public static PsiElement classDescriptorToDeclaration(@NotNull ClassDescriptor clazz) {
        return doGetDescriptorToDeclaration(clazz);
    }

    private DescriptorToSourceUtils() {}

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
}


