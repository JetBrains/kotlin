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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public abstract class JetModuleTypeManager {
    public static JetModuleTypeManager getInstance() {
        return ServiceManager.getService(JetModuleTypeManager.class);
    }

    public abstract boolean isKtFileInGradleProjectInWrongFolder(@NotNull PsiElement element);
    public abstract boolean isKtFileInGradleProjectInWrongFolder(@NotNull VirtualFile virtualFile, @NotNull Project project);
    public abstract boolean isAndroidGradleModule(@NotNull Module module);
    public boolean isGradleModule(@NotNull Module module) {
        return ModuleTypeCacheManager.OBJECT$.geInstance(module.getProject()).isGradleModule(module);
    }
}
