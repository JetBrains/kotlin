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

package org.jetbrains.jet.plugin.project;

import com.google.common.collect.Lists;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.List;

/**
 * This class has utility functions to determine whether the project (or module) is js project.
 */
public final class JsModuleDetector {
    private JsModuleDetector() {
    }

    public static boolean isJsModule(@NotNull Module module) {
        // TODO: fix
        return false;
    }

    public static boolean isJsModule(@NotNull JetFile file) {
        VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
        if (virtualFile != null) {
            Module module = ProjectRootManager.getInstance(file.getProject()).getFileIndex().getModuleForFile(virtualFile);
            if (module != null) {
                return isJsModule(module);
            }
        }

        return false;
    }

    @NotNull
    public static Pair<List<String>, String> getLibLocationAndTargetForProject(@NotNull Project project) {
        Module module = getJSModule(project);
        if (module == null) {
            return Pair.empty();
        }
        else {
            return getLibLocationAndTargetForProject(module);
        }
    }

    @NotNull
    public static Pair<List<String>, String> getLibLocationAndTargetForProject(@NotNull Module module) {
        throw new IllegalStateException("Under construction");
        //K2JSModuleComponent jsModuleComponent = K2JSModuleComponent.getInstance(module);
        //String pathToJavaScriptLibrary = jsModuleComponent.getPathToJavaScriptLibrary();
        //String basePath = ModuleRootManager.getInstance(module).getContentRoots()[0].getPath();
        //List<String> pathsToJSLib = Lists.newArrayList();
        //if (pathToJavaScriptLibrary != null) {
        //    pathsToJSLib.add(basePath + pathToJavaScriptLibrary);
        //}
        //return Pair.create(pathsToJSLib, jsModuleComponent.getEcmaVersion().toString());
    }

    @Nullable
    private static Module getJSModule(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (isJsModule(module)) {
                return module;
            }
        }
        return null;
    }
}
