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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqName;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.getFqNameSafe;

public final class DecompiledNavigationUtils {

    private static final Logger LOG = Logger.getInstance(DecompiledNavigationUtils.class);

    @Nullable
    public static JetDeclaration findDeclarationForReference(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        JetDeclaration declarationFromDecompiledClassFile = getDeclarationFromDecompiledClassFile(project, referencedDescriptor);
        if (declarationFromDecompiledClassFile == null) {
            return null;
        }
        return JetSourceNavigationHelper.replaceBySourceDeclarationIfPresent(declarationFromDecompiledClassFile);
    }

    @Nullable
    private static JetDeclaration getDeclarationFromDecompiledClassFile(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        DeclarationDescriptor effectiveReferencedDescriptor = getEffectiveReferencedDescriptor(referencedDescriptor);
        VirtualFile virtualFile = findVirtualFileContainingDescriptor(project, effectiveReferencedDescriptor);

        if (virtualFile == null || !LibrariesPackage.isKotlinCompiledFile(virtualFile)) return null;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof JetClsFile)) {
            return null;
        }
        JetDeclaration jetDeclaration = ((JetClsFile) psiFile).getDeclarationForDescriptor(effectiveReferencedDescriptor);
        if (jetDeclaration != null) {
            return jetDeclaration;
        }
        else {
            LOG.warn("Could not find an element to navigate to for descriptor " + getFqName(effectiveReferencedDescriptor));
        }
        return null;
    }

    //TODO: should be done via some generic mechanism
    @NotNull
    private static DeclarationDescriptor getEffectiveReferencedDescriptor(@NotNull DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor) {
            return DescriptorUtils.unwrapFakeOverride((CallableMemberDescriptor) descriptor);
        }
        return descriptor;
    }


    /*
        Find virtual file which contains the declaration of descriptor we're navigating to.
     */
    @Nullable
    private static VirtualFile findVirtualFileContainingDescriptor(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        FqName containerFqName = getContainerFqName(referencedDescriptor);
        if (containerFqName == null) {
            return null;
        }
        VirtualFileFinder fileFinder = VirtualFileFinder.SERVICE.getInstance(project);
        VirtualFile virtualFile = fileFinder.findVirtualFileWithHeader(containerFqName);
        if (virtualFile == null) {
            return null;
        }
        return virtualFile;
    }

    //TODO: navigate to inner classes
    @Nullable
    private static FqName getContainerFqName(@NotNull DeclarationDescriptor referencedDescriptor) {
        ClassOrPackageFragmentDescriptor
                containerDescriptor = DescriptorUtils.getParentOfType(referencedDescriptor, ClassOrPackageFragmentDescriptor.class, false);
        if (containerDescriptor instanceof PackageFragmentDescriptor) {
            return PackageClassUtils.getPackageClassFqName(((PackageFragmentDescriptor) containerDescriptor).getFqName());
        }
        if (containerDescriptor instanceof ClassDescriptor) {
            if (containerDescriptor.getContainingDeclaration() instanceof ClassDescriptor
                || ExpressionTypingUtils.isLocal(containerDescriptor.getContainingDeclaration(), containerDescriptor)) {
                return getContainerFqName(containerDescriptor.getContainingDeclaration());
            }
            return getFqNameSafe(containerDescriptor);
        }
        return null;
    }

    private DecompiledNavigationUtils() {
    }
}
