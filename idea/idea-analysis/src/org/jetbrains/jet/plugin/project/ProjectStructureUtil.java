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
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.PathUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.framework.JsHeaderLibraryDetectionUtil;
import org.jetbrains.jet.plugin.framework.JsLibraryStdDetectionUtil;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryCoreUtil;

import java.util.*;

public class ProjectStructureUtil {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");
    private static final Key<CachedValue<Boolean>> HAS_KOTLIN_JVM_MODULES = Key.create("HAS_KOTLIN_JVM_MODULES");
    private static final Key<CachedValue<Boolean>> IS_DEPEND_ON_JVM_KOTLIN = Key.create("KOTLIN_IS_DEPEND_ON_JVM_KOTLIN");

    private ProjectStructureUtil() {
    }

    public static boolean isJsKotlinModule(@NotNull JetFile file) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        return module != null && isJsKotlinModule(module);
    }

    public static boolean isJavaKotlinModule(@NotNull Module module) {
        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(
                hasKotlinFilesOnlyInTests(module));
        return KotlinRuntimeLibraryCoreUtil.getKotlinRuntimeMarkerClass(scope) != null;
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

    public static boolean hasJvmKotlinModules(@NotNull final Project project) {
        CachedValue<Boolean> result = project.getUserData(HAS_KOTLIN_JVM_MODULES);
        if (result == null) {
            result = CachedValuesManager.getManager(project).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    boolean hasJvmKotlinModules = false;

                    for (Module module : ModuleManager.getInstance(project).getModules()) {
                        if (isJavaKotlinModule(module)) {
                            hasJvmKotlinModules = true;
                            break;
                        }
                    }

                    return Result.create(hasJvmKotlinModules, ProjectRootModificationTracker.getInstance(project));
                }
            }, false);

            project.putUserData(HAS_KOTLIN_JVM_MODULES, result);
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
                        boolean detected = JsHeaderLibraryDetectionUtil.isJsHeaderLibraryDetected(Arrays.asList(
                                library.getFiles(OrderRootType.CLASSES)));

                        if (detected) {
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
                        List<VirtualFile> classes = Arrays.asList(library.getFiles(OrderRootType.CLASSES));

                        boolean detected = JsLibraryStdDetectionUtil.getJsLibraryStdVersion(classes) != null;

                        if (detected) {
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

    public static boolean hasKotlinFilesInSources(@NotNull Module module) {
        return FileTypeIndex.containsFileOfType(JetFileType.INSTANCE, module.getModuleScope(false));
    }

    public static boolean hasKotlinFilesOnlyInTests(@NotNull Module module) {
        return !hasKotlinFilesInSources(module) && FileTypeIndex.containsFileOfType(JetFileType.INSTANCE, module.getModuleScope(true));
    }
}
