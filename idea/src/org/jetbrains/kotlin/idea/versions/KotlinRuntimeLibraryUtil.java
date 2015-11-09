/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.versions;

import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import kotlin.ArraysKt;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider;
import org.jetbrains.kotlin.idea.framework.LibraryPresentationProviderUtil;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.kotlin.load.java.AbiVersionUtil;
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion;
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils;
import org.jetbrains.kotlin.utils.KotlinPaths;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.intellij.util.PathUtil.getLocalFile;
import static com.intellij.util.PathUtil.getLocalPath;
import static org.jetbrains.kotlin.idea.versions.OutdatedKotlinRuntimeNotification.showRuntimeJarNotFoundDialog;

public class KotlinRuntimeLibraryUtil {
    private KotlinRuntimeLibraryUtil() {
    }

    @NotNull
    public static Collection<VirtualFile> getLibraryRootsWithAbiIncompatibleKotlinClasses(@NotNull Project project) {
        return getLibraryRootsWithAbiIncompatibleVersion(
                project, KotlinAbiVersionIndex.INSTANCE$,
                new Function1<Module, Boolean>() {
                    @Override
                    public Boolean invoke(@Nullable Module module) {
                        return module != null && ProjectStructureUtil.isJavaKotlinModule(module);
                    }
                },
                new Function1<BinaryVersion, Boolean>() {
                    @Override
                    public Boolean invoke(@NotNull BinaryVersion version) {
                        return !AbiVersionUtil.isAbiVersionCompatible(version);
                    }
                });
    }

    @NotNull
    public static Collection<VirtualFile> getLibraryRootsWithAbiIncompatibleForKotlinJs(@NotNull Project project) {
        return getLibraryRootsWithAbiIncompatibleVersion(
                project, KotlinJavaScriptAbiVersionIndex.INSTANCE$,
                new Function1<Module, Boolean>() {
                    @Override
                    public Boolean invoke(@Nullable Module module) {
                        return module != null && ProjectStructureUtil.isJsKotlinModule(module);
                    }
                },
                new Function1<BinaryVersion, Boolean>() {
                    @Override
                    public Boolean invoke(@NotNull BinaryVersion version) {
                        // TODO: support major.minor.patch version in JS metadata
                        return !KotlinJavascriptMetadataUtils.isAbiVersionCompatible(version.getMinor());
                    }
                });
    }


    public static void updateLibraries(
            @NotNull final Project project,
            @NotNull final Collection<Library> libraries
    ) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                KotlinJavaModuleConfigurator kJvmConfigurator = (KotlinJavaModuleConfigurator)
                        ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJavaModuleConfigurator.NAME);
                assert kJvmConfigurator != null : "Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME;

                KotlinJsModuleConfigurator kJsConfigurator = (KotlinJsModuleConfigurator)
                        ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJsModuleConfigurator.NAME);
                assert kJsConfigurator != null : "Configurator with given name doesn't exists: " + KotlinJsModuleConfigurator.NAME;

                for (Library library : libraries) {
                    if (LibraryPresentationProviderUtil.isDetected(JavaRuntimePresentationProvider.getInstance(), library)) {
                        updateJar(project, JavaRuntimePresentationProvider.getRuntimeJar(library), LibraryJarDescriptor.RUNTIME_JAR);
                        updateJar(project, JavaRuntimePresentationProvider.getReflectJar(library), LibraryJarDescriptor.REFLECT_JAR);

                        if (kJvmConfigurator.changeOldSourcesPathIfNeeded(project, library)) {
                            kJvmConfigurator.copySourcesToPathFromLibrary(project, library);
                        }
                        else {
                            updateJar(project, JavaRuntimePresentationProvider.getRuntimeSrcJar(library), LibraryJarDescriptor.RUNTIME_SRC_JAR);
                        }
                    }
                    else if (LibraryPresentationProviderUtil.isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                        updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibJar(library), LibraryJarDescriptor.JS_STDLIB_JAR);

                        if (kJsConfigurator.changeOldSourcesPathIfNeeded(project, library)) {
                            kJsConfigurator.copySourcesToPathFromLibrary(project, library);
                        }
                        else {
                            updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibSrcJar(library), LibraryJarDescriptor.JS_STDLIB_SRC_JAR);
                        }
                    }
                }
            }
        });
    }

    private static void updateJar(
            @NotNull Project project,
            @Nullable VirtualFile fileToReplace,
            @NotNull LibraryJarDescriptor libraryJarDescriptor
    ) {
        if (fileToReplace == null && !libraryJarDescriptor.shouldExist) {
            return;
        }

        KotlinPaths paths = PathUtil.getKotlinPathsForIdeaPlugin();
        File jarPath;
        switch (libraryJarDescriptor) {
            case RUNTIME_JAR: jarPath = paths.getRuntimePath(); break;
            case REFLECT_JAR: jarPath = paths.getReflectPath(); break;
            case RUNTIME_SRC_JAR: jarPath = paths.getRuntimeSourcesPath(); break;
            case JS_STDLIB_JAR: jarPath = paths.getJsStdLibJarPath(); break;
            case JS_STDLIB_SRC_JAR: jarPath = paths.getJsStdLibSrcJarPath(); break;
            default: jarPath = null; break;
        }

        if (!jarPath.exists()) {
            showRuntimeJarNotFoundDialog(project, libraryJarDescriptor.jarName).run();
            return;
        }

        VirtualFile localJar = getLocalJar(fileToReplace);
        assert localJar != null;

        replaceFile(jarPath, localJar);
    }

    @NotNull
    public static Collection<Library> findKotlinLibraries(@NotNull Project project) {
        Set<Library> libraries = Sets.newHashSet();

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

            for (OrderEntry entry : moduleRootManager.getOrderEntries()) {
                if (entry instanceof LibraryOrderEntry) {
                    LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;
                    Library library = libraryOrderEntry.getLibrary();

                    if (library == null) {
                        continue;
                    }

                    libraries.add(library);

                    // TODO: search js libraries as well
                }
            }
        }

        return libraries;
    }

    private enum LibraryJarDescriptor {
        RUNTIME_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_JAR, true),
        REFLECT_JAR(PathUtil.KOTLIN_JAVA_REFLECT_JAR, false),
        RUNTIME_SRC_JAR(PathUtil.KOTLIN_JAVA_RUNTIME_SRC_JAR, false),
        JS_STDLIB_JAR(PathUtil.JS_LIB_JAR_NAME, true),
        JS_STDLIB_SRC_JAR(PathUtil.JS_LIB_SRC_JAR_NAME, false);

        public final String jarName;
        private final boolean shouldExist;

        LibraryJarDescriptor(@NotNull String jarName, boolean shouldExist) {
            this.jarName = jarName;
            this.shouldExist = shouldExist;
        }
    }

    @NotNull
    public static String bundledRuntimeVersion() {
        return bundledRuntimeVersion(KotlinPluginUtil.getPluginVersion());
    }

    @NotNull
    public static String bundledRuntimeVersion(@NotNull String pluginVersion) {
        int placeToSplit = -1;

        int ideaPatternIndex = StringUtil.indexOf(pluginVersion, "Idea");
        if (ideaPatternIndex >= 2 && Character.isDigit(pluginVersion.charAt(ideaPatternIndex - 2))) {
            placeToSplit = ideaPatternIndex - 1;
        }

        int ijPatternIndex = StringUtil.indexOf(pluginVersion, "IJ");
        if (ijPatternIndex >= 2 && Character.isDigit(pluginVersion.charAt(ijPatternIndex - 2))) {
            placeToSplit = ijPatternIndex - 1;
        }

        if (placeToSplit == -1) {
            for (int i = 1; i < pluginVersion.length(); i++) {
                char ch = pluginVersion.charAt(i);
                if (Character.isLetter(ch) && pluginVersion.charAt(i - 1) == '.') {
                    placeToSplit = i - 1;
                    break;
                }
            }
        }

        return placeToSplit != - 1 ? pluginVersion.substring(0, placeToSplit) : pluginVersion;
    }

    @Nullable
    public static VirtualFile getLocalJar(@Nullable VirtualFile kotlinRuntimeJar) {
        if (kotlinRuntimeJar == null) return null;

        VirtualFile localJarFile = JarFileSystem.getInstance().getVirtualFileForJar(kotlinRuntimeJar);
        if (localJarFile != null) {
            return localJarFile;
        }
        return kotlinRuntimeJar;
    }

    static void replaceFile(File updatedFile, VirtualFile replacedJarFile) {
        try {
            VirtualFile replacedFile = getLocalFile(replacedJarFile);

            String localPath = getLocalPath(replacedFile);
            assert localPath != null : "Should be called for replacing valid root file: " + replacedJarFile;

            File libraryJarPath = new File(localPath);

            if (FileUtil.filesEqual(updatedFile, libraryJarPath)) {
                return;
            }

            FileUtil.copy(updatedFile, libraryJarPath);
            replacedFile.refresh(false, true);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    private static Collection<VirtualFile> getLibraryRootsWithAbiIncompatibleVersion(
            @NotNull Project project,
            @NotNull ScalarIndexExtension<BinaryVersion> index,
            @NotNull Function1<Module, Boolean> checkModule,
            @NotNull Function1<BinaryVersion, Boolean> checkVersion
    ) {
        ID<BinaryVersion, Void> id = index.getName();

        Module[] modules = ModuleManager.getInstance(project).getModules();

        List<Module> modulesToCheck = ArraysKt.filter(modules, checkModule);
        if (modulesToCheck.isEmpty()) return Collections.emptyList();

        Collection<BinaryVersion> versions = collectAllKeys(id, modulesToCheck);
        Set<BinaryVersion> badVersions = Sets.newHashSet(CollectionsKt.filter(versions, checkVersion));
        Set<VirtualFile> badRoots = Sets.newHashSet();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        for (BinaryVersion version : badVersions) {
            Collection<VirtualFile> indexedFiles = FileBasedIndex.getInstance().getContainingFiles(
                    id, version, ProjectScope.getLibrariesScope(project));

            for (VirtualFile indexedFile : indexedFiles) {
                VirtualFile libraryRoot = fileIndex.getClassRootForFile(indexedFile);
                assert libraryRoot != null : "Only library roots were requested, " +
                                             "and only class files should be indexed with KotlinAbiVersionIndex key. " +
                                             "File: " + indexedFile.getPath();
                badRoots.add(getLocalFile(libraryRoot));
            }
        }

        return badRoots;
    }

    @NotNull
    private static <T> Collection<T> collectAllKeys(@NotNull ID<T, Void> id, @NotNull List<Module> modules) {
        Set<T> allKeys = new HashSet<T>();

        for (Module module : modules) {
            GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(module);
            FileBasedIndex.getInstance().processAllKeys(id, new CommonProcessors.CollectProcessor<T>(allKeys), scope, null);
        }

        return allKeys;
    }
}
