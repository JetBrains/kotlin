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

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.configuration.JetModuleTypeManager;

public class JetPluginUtil {

    @NotNull
    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"));
        assert plugin != null : "How can it be? Kotlin plugin is available, but its component is running. Complete nonsense.";
        return plugin.getVersion();
    }

    public static boolean isKtFileInGradleProjectInWrongFolder(@NotNull PsiElement element) {
        return JetModuleTypeManager.getInstance().isKtFileInGradleProjectInWrongFolder(element);
    }

    public static boolean isKtFileInGradleProjectInWrongFolder(@NotNull VirtualFile virtualFile, @NotNull Project project) {
        return JetModuleTypeManager.getInstance().isKtFileInGradleProjectInWrongFolder(virtualFile, project);
    }

    public static boolean isAndroidGradleModule(@NotNull Module module) {
        return JetModuleTypeManager.getInstance().isAndroidGradleModule(module);
    }

    public static boolean isGradleModule(@NotNull Module module) {
        return JetModuleTypeManager.getInstance().isGradleModule(module);
    }

    public static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
    }
}
