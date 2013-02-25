package org.jetbrains.jet.plugin.framework;

import com.google.common.collect.Lists;
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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.k2js.config.EcmaVersion;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class FrameworkDetector {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JAVA_MODULE = Key.create("IS_KOTLIN_JAVA_MODULE");

    private FrameworkDetector() {
    }

    public static boolean isJsModule(@NotNull JetFile file) {
        Module module = ModuleUtilCore.findModuleForPsiElement(file);
        return module != null && isJsModule(module);
    }

    public static boolean isJavaModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JAVA_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
                    return Result.create(KotlinRuntimeLibraryUtil.getKotlinRuntimeMarkerClass(scope) != null,
                                         ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JAVA_MODULE, result);
        }

        return result.getValue();
    }

    public static boolean isJsModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JS_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    return Result.create(getStandardJavaScriptLibrary(module) != null,
                                         ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JS_MODULE, result);
        }

        return result.getValue();
    }

    @NotNull
    private static Collection<Library> getJavaScriptHeadersLibraries(final Module module) {
        final Collection<Library> headersLibraries = Lists.newArrayList();

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        if (JSHeadersPresentationProvider.getInstance().detect(
                                Arrays.asList(library.getFiles(OrderRootType.CLASSES))) != null) {
                            headersLibraries.add(library);
                        }

                        return true;
                    }
                });
            }
        });

        return headersLibraries;
    }

    @Nullable
    private static Library getStandardJavaScriptLibrary(final Module module) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Library>() {
            @Override
            public Library compute() {
                for (Library library : getJavaScriptHeadersLibraries(module)) {
                    String libraryName = library.getName();
                    if (libraryName != null && libraryName.contains(PathUtil.JS_LIB_JAR_NAME)) {
                        return library;
                    }
                }
                return null;
            }
        });
    }

    @NotNull
    public static Pair<List<String>, String> getLibLocationAndTargetForProject(@NotNull Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            if (isJsModule(module)) {
                return getLibLocationAndTargetForProject(module);
            }
        }

        return Pair.empty();
    }

    public static Pair<List<String>, String> getLibLocationAndTargetForProject(Module module) {
        Library library = getStandardJavaScriptLibrary(module);

        if (library != null) {
            List<String> pathsToJSLib = Lists.newArrayList();
            VirtualFile[] files = library.getRootProvider().getFiles(OrderRootType.SOURCES);
            for (VirtualFile file : files) {
                pathsToJSLib.add(com.intellij.util.PathUtil.getLocalPath(file));
            }

            return Pair.create(pathsToJSLib, EcmaVersion.defaultVersion().toString());
        }
        else {
            throw new IllegalStateException("Should be called for JS module only. JS library is expected to be found");
        }
    }
}
