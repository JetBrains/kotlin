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

package org.jetbrains.jet.plugin.framework;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.PathUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;
import org.jetbrains.k2js.config.EcmaVersion;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class KotlinFrameworkDetector {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");

    private KotlinFrameworkDetector() {
    }

    public static boolean isJsKotlinModule(@NotNull JetFile file) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        return module != null && isJsKotlinModule(module);
    }

    public static boolean isJavaKotlinModule(@NotNull Module module) {
        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
        return KotlinRuntimeLibraryUtil.getKotlinRuntimeMarkerClass(scope) != null;
    }

    public static boolean isJsKotlinModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JS_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    return Result.create(getJSStandardLibrary(module) != null, ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JS_MODULE, result);
        }

        return result.getValue();
    }

    @NotNull
    public static Pair<List<String>, String> getLibLocationAndTargetForProject(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (isJsKotlinModule(module)) {
                return getLibLocationAndTargetForProject(module);
            }
        }

        return Pair.empty();
    }

    public static Pair<List<String>, String> getLibLocationAndTargetForProject(final Module module) {
        final Set<String> pathsToJSLib = Sets.newHashSet();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (LibraryPresentationProviderUtil.isDetected(JsHeaderLibraryPresentationProvider.getInstance(), library)) {
                            for (VirtualFile file : library.getRootProvider().getFiles(OrderRootType.SOURCES)) {
                                pathsToJSLib.add(PathUtil.getLocalPath(file));
                            }
                        }

                        return true;
                    }
                });
            }
        });

        return Pair.<List<String>, String>create(Lists.newArrayList(pathsToJSLib), EcmaVersion.defaultVersion().toString());
    }


    @Nullable
    private static Library getJSStandardLibrary(final Module module) {
        final Ref<Library> jsLibrary = Ref.create();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (JSLibraryStdPresentationProvider.getInstance().detect(Arrays.asList(library.getFiles(OrderRootType.CLASSES))) != null) {
                            jsLibrary.set(library);
                            return false;
                        }

                        return true;
                    }
                });
            }
        });

        return jsLibrary.get();
    }
}
