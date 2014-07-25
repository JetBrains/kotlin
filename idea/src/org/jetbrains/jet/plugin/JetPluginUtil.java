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

package org.jetbrains.jet.plugin;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetManager;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.ModuleTypeCacheManager;

public class JetPluginUtil {
    public static boolean isInSource(@NotNull PsiElement element) {
        return isInSource(element, true);
    }

    public static boolean isInSource(@NotNull PsiElement element, boolean includeLibrarySources) {
        PsiFile containingFile = element.getContainingFile();
        if (containingFile == null) {
            return false;
        }
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) {
            return false;
        }
        ProjectFileIndex index = ProjectFileIndex.SERVICE.getInstance(element.getProject());
        return includeLibrarySources ? index.isInSource(virtualFile) : index.isInSourceContent(virtualFile);
    }

    @NotNull
    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"));
        assert plugin != null : "How can it be? Kotlin plugin is available, but its component is running. Complete nonsense.";
        return plugin.getVersion();
    }

    public static boolean isKtFileInGradleProjectInWrongFolder(@NotNull PsiElement element) {
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

    public static boolean isKtFileInGradleProjectInWrongFolder(@NotNull VirtualFile virtualFile, @NotNull Project project) {
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

    public static boolean isAndroidGradleModule(@NotNull Module module) {
        // We don't want to depend on the Android-Gradle plugin
        // See com.android.tools.idea.gradle.util.Projects.isGradleProject()
        for (Facet facet : FacetManager.getInstance(module).getAllFacets()) {
            if (facet.getName().equals("Android-Gradle")) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGradleModule(@NotNull Module module) {
        return ModuleTypeCacheManager.OBJECT$.geInstance(module.getProject()).isGradleModule(module);
    }

    public static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
    }
}
