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
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
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
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.jet.plugin.framework.JsHeaderLibraryPresentationProvider;
import org.jetbrains.jet.plugin.framework.LibraryPresentationProviderUtil;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectStructureUtil {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");
    private static final Key<CachedValue<Boolean>> IS_DEPEND_ON_JVM_KOTLIN = Key.create("KOTLIN_IS_DEPEND_ON_JVM_KOTLIN");

    private ProjectStructureUtil() {
    }

    public static boolean isJsKotlinModule(@NotNull JetFile file) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        return module != null && isJsKotlinModule(module);
    }

    public static boolean isJavaKotlinModule(@NotNull Module module) {
        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(
                ConfigureKotlinInProjectUtils.hasKotlinFilesOnlyInTests(module));
        return KotlinRuntimeLibraryUtil.getKotlinRuntimeMarkerClass(scope) != null;
    }

    public static boolean isJsKotlinModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JS_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    return Result.create(getJSStandardLibrary(module) != null, ProjectRootModificationTracker
                            .getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JS_MODULE, result);
        }

        return result.getValue();
    }

    public static boolean isUsedInKotlinJavaModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_DEPEND_ON_JVM_KOTLIN);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    boolean usedInKotlinModule = false;

                    Set<Module> dependentModules = new HashSet<Module>();
                    ModuleUtilCore.collectModulesDependsOn(module, dependentModules);

                    for (Module module : dependentModules) {
                        if (isJavaKotlinModule(module)) {
                            usedInKotlinModule = true;
                            break;
                        }
                    }

                    return Result.create(usedInKotlinModule, ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_DEPEND_ON_JVM_KOTLIN, result);
        }

        return result.getValue();
    }

    @NotNull
    public static List<String> getLibLocationForProject(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (isJsKotlinModule(module)) {
                return getLibLocationForProject(module);
            }
        }

        return Collections.emptyList();
    }

    @NotNull
    public static List<String> getLibLocationForProject(@NotNull final Module module) {
        final Set<String> pathsToJSLib = Sets.newHashSet();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (LibraryPresentationProviderUtil.isDetected(JsHeaderLibraryPresentationProvider.getInstance(), library)) {
                            for (VirtualFile file : library.getRootProvider().getFiles(OrderRootType.SOURCES)) {
                                String path = PathUtil.getLocalPath(PathUtil.getLocalFile(file));
                                if (path != null) {
                                    pathsToJSLib.add(path);
                                }
                                else {
                                    assert !file.isValid() : "Path is expected to be null only for invalid file: " + file;
                                }
                            }
                        }

                        return true;
                    }
                });
            }
        });

        return Lists.newArrayList(pathsToJSLib);
    }

    @Nullable
    private static Library getJSStandardLibrary(final Module module) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Library>() {
            @Override
            public Library compute() {
                final Ref<Library> jsLibrary = Ref.create();

                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (LibraryPresentationProviderUtil.isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                            jsLibrary.set(library);
                            return false;
                        }

                        return true;
                    }
                });

                return jsLibrary.get();
            }
        });
    }
}
