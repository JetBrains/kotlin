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

package org.jetbrains.jet.plugin;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager;

public class JetModuleTypeManagerImpl extends JetModuleTypeManager {
    @Override
    public boolean isKtFileInGradleProjectInWrongFolder(@NotNull PsiElement element) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return false;
        }
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        return isKtFileInGradleProjectInWrongFolder(virtualFile, element.getProject());
    }

    @Override
    public boolean isKtFileInGradleProjectInWrongFolder(@NotNull VirtualFile virtualFile, @NotNull Project project ) {
        Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
        if (module == null) return false;

        if (!isAndroidGradleModule(module) && !isGradleModule(module)) {
            return false;
        }

        VirtualFile sourceRootForFile = ProjectRootManager.getInstance(project).getFileIndex().getSourceRootForFile(virtualFile);
        if (sourceRootForFile != null) {
            return !sourceRootForFile.getName().equals("kotlin");
        }
        return false;
    }

    @Override
    public boolean isAndroidGradleModule(@NotNull Module module) {
        // We don't want to depend on the Android-Gradle plugin
        // See com.android.tools.idea.gradle.util.Projects.isGradleProject()
        for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
            if (facet.getName().equals("Android-Gradle")) {
                return true;
            }
        }
        return false;
    }
}
