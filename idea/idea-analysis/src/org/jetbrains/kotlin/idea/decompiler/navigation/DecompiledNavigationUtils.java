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

package org.jetbrains.kotlin.idea.decompiler.navigation;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.idea.caches.resolve.JsProjectDetector;
import org.jetbrains.kotlin.idea.decompiler.DecompilerPackage;
import org.jetbrains.kotlin.idea.decompiler.KotlinClsFileBase;
import org.jetbrains.kotlin.idea.stubindex.JetSourceFilterScope;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder;
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.getFqName;

public final class DecompiledNavigationUtils {

    @Nullable
    public static JetDeclaration getDeclarationFromDecompiledClassFile(
            @NotNull Project project,
            @NotNull DeclarationDescriptor referencedDescriptor
    ) {
        VirtualFile virtualFile;

        if (JsProjectDetector.isJsProject(project) && NavigationPackage.getKotlinJavascriptLibrary(referencedDescriptor, project) != null) {
            JsMetaFileVirtualFileHolder system = JsMetaFileVirtualFileHolder.getInstance(project);
            virtualFile = system.getFile(referencedDescriptor);
            if (virtualFile == null) return null;
        }
        else {
            virtualFile = findVirtualFileContainingDescriptor(project, referencedDescriptor);
            if (virtualFile == null || !DecompilerPackage.isKotlinCompiledFile(virtualFile)) return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
        if (!(psiFile instanceof KotlinClsFileBase)) {
            return null;
        }

        return ((KotlinClsFileBase) psiFile).getDeclarationForDescriptor(referencedDescriptor);
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
        GlobalSearchScope scopeToSearchIn = JetSourceFilterScope.kotlinSourceAndClassFiles(GlobalSearchScope.allScope(project), project);
        VirtualFileFinder fileFinder = VirtualFileFinderFactory.SERVICE.getInstance(project).create(scopeToSearchIn);
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
            FqNameUnsafe fqNameUnsafe = getFqName(containerDescriptor);
            return fqNameUnsafe.isSafe() ? fqNameUnsafe.toSafe() : null;
        }
        return null;
    }

    private DecompiledNavigationUtils() {
    }
}
