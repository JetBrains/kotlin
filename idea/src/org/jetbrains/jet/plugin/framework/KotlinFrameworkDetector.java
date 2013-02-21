package org.jetbrains.jet.plugin.framework;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.Alarm;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.util.ApplicationUtils;
import org.jetbrains.jet.plugin.versions.KotlinLibrariesNotificationProvider;
import org.jetbrains.jet.plugin.versions.KotlinRuntimeLibraryUtil;
import org.jetbrains.jet.utils.KotlinPathsFromHomeDir;
import org.jetbrains.jet.utils.PathUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class KotlinFrameworkDetector {
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JS_MODULE = Key.create("IS_KOTLIN_JS_MODULE");
    private static final Key<CachedValue<Boolean>> IS_KOTLIN_JAVA_MODULE = Key.create("IS_KOTLIN_JAVA_MODULE");

    private KotlinFrameworkDetector() {
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

    private static Boolean checkModuleHasJavaScriptLibrary(final Module module) {
        final Ref<Boolean> found = Ref.create(Boolean.FALSE);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ModuleRootManager.getInstance(module).orderEntries().librariesOnly().forEachLibrary(new Processor<Library>() {
                    @Override
                    public boolean process(Library library) {
                        String libraryName = library.getName();
                        if (libraryName != null && libraryName.contains(PathUtil.JS_LIB_JAR_NAME)) {
                            found.set(Boolean.TRUE);
                            return false;
                        }
                        return true;
                    }
                });
            }
        });

        return found.get();
    }

    public static boolean isJsModule(@NotNull final Module module) {
        CachedValue<Boolean> result = module.getUserData(IS_KOTLIN_JS_MODULE);
        if (result == null) {
            result = CachedValuesManager.getManager(module.getProject()).createCachedValue(new CachedValueProvider<Boolean>() {
                @Override
                public Result<Boolean> compute() {
                    return Result.create(checkModuleHasJavaScriptLibrary(module),
                                         ProjectRootModificationTracker.getInstance(module.getProject()));
                }
            }, false);

            module.putUserData(IS_KOTLIN_JS_MODULE, result);
        }

        return result.getValue();
    }

    public static Pair<List<String>, String> getLibLocationAndTargetForProject(Module module) {
        return Pair.<List<String>, String>create(new ArrayList<String>(), "ecma3");
    }
}
