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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.CommonProcessors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.ScalarIndexExtension;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetPluginUtil;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.kotlin.idea.configuration.KotlinJavaModuleConfigurator;
import org.jetbrains.kotlin.idea.framework.JSLibraryStdPresentationProvider;
import org.jetbrains.kotlin.idea.framework.JavaRuntimePresentationProvider;
import org.jetbrains.kotlin.idea.framework.LibraryPresentationProviderUtil;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.kotlin.load.java.AbiVersionUtil;
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
                new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer abiVersion) {
                        return !AbiVersionUtil.isAbiVersionCompatible(abiVersion);
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
                new Function1<Integer, Boolean>() {
                    @Override
                    public Boolean invoke(Integer abiVersion) {
                        return !KotlinJavascriptMetadataUtils.isAbiVersionCompatible(abiVersion);
                    }
                });
    }


    public static void addJdkAnnotations(@NotNull Sdk sdk) {
        addAnnotations(sdk, PathUtil.getKotlinPathsForIdeaPlugin().getJdkAnnotationsPath());
    }

    public static void addAndroidSdkAnnotations(@NotNull Sdk sdk) {
        addAnnotations(sdk, PathUtil.getKotlinPathsForIdeaPlugin().getAndroidSdkAnnotationsPath());
    }

    public static void removeJdkAnnotations(@NotNull Sdk sdk) {
        removeAnnotations(sdk, PathUtil.getKotlinPathsForIdeaPlugin().getJdkAnnotationsPath());
    }

    private static void addAnnotations(@NotNull Sdk sdk, @NotNull File annotationsPath) {
        modifyAnnotations(sdk, annotationsPath, true);
    }

    private static void removeAnnotations(@NotNull Sdk sdk, @NotNull File annotationsPath) {
        modifyAnnotations(sdk, annotationsPath, false);
    }

    private static void modifyAnnotations(@NotNull Sdk sdk, @NotNull File annotationsPath, boolean isAdd) {
        if (annotationsPath.exists()) {
            VirtualFile jdkAnnotationsJar = LocalFileSystem.getInstance().findFileByIoFile(annotationsPath);
            if (jdkAnnotationsJar != null) {
                SdkModificator modificator = sdk.getSdkModificator();
                VirtualFile jarRootForLocalFile = JarFileSystem.getInstance().getJarRootForLocalFile(jdkAnnotationsJar);
                if (isAdd) {
                    modificator.addRoot(jarRootForLocalFile, AnnotationOrderRootType.getInstance());
                }
                else {
                    modificator.removeRoot(jarRootForLocalFile, AnnotationOrderRootType.getInstance());
                }
                modificator.commitChanges();
            }
        }
    }

    public static boolean jdkAnnotationsArePresent(@NotNull Sdk sdk) {
        return areAnnotationsPresent(sdk, PathUtil.JDK_ANNOTATIONS_JAR);
    }

    public static boolean androidSdkAnnotationsArePresent(@NotNull Sdk sdk) {
        return areAnnotationsPresent(sdk, PathUtil.ANDROID_SDK_ANNOTATIONS_JAR);
    }

    private static boolean areAnnotationsPresent(@NotNull Sdk sdk, @NotNull final String jarFileName) {
        return ContainerUtil.exists(sdk.getRootProvider().getFiles(AnnotationOrderRootType.getInstance()),
                                    new Condition<VirtualFile>() {
                                        @Override
                                        public boolean value(VirtualFile file) {
                                            return jarFileName.equals(file.getName());
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
                KotlinJavaModuleConfigurator configurator = (KotlinJavaModuleConfigurator)
                        ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJavaModuleConfigurator.NAME);
                assert configurator != null : "Configurator with given name doesn't exists: " + KotlinJavaModuleConfigurator.NAME;

                for (Library library : libraries) {
                    if (LibraryPresentationProviderUtil.isDetected(JavaRuntimePresentationProvider.getInstance(), library)) {
                        updateJar(project, JavaRuntimePresentationProvider.getRuntimeJar(library), LibraryJarDescriptor.RUNTIME_JAR);
                        updateJar(project, JavaRuntimePresentationProvider.getReflectJar(library), LibraryJarDescriptor.REFLECT_JAR);

                        if (configurator.changeOldSourcesPathIfNeeded(library)) {
                            configurator.copySourcesToPathFromLibrary(library);
                        }
                        else {
                            updateJar(project, JavaRuntimePresentationProvider.getRuntimeSrcJar(library), LibraryJarDescriptor.RUNTIME_SRC_JAR);
                        }
                    }
                    else if (LibraryPresentationProviderUtil.isDetected(JSLibraryStdPresentationProvider.getInstance(), library)) {
                        updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibJar(library), LibraryJarDescriptor.JS_STDLIB_JAR);
                        updateJar(project, JSLibraryStdPresentationProvider.getJsStdLibSrcJar(library), LibraryJarDescriptor.JS_STDLIB_SRC_JAR);
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
        return bundledRuntimeVersion(JetPluginUtil.getPluginVersion());
    }

    @NotNull
    public static String bundledRuntimeVersion(@NotNull String pluginVersion) {
        int placeToSplit = -1;

        for (int i = 1; i < pluginVersion.length(); i++) {
            char ch = pluginVersion.charAt(i);
            if (Character.isLetter(ch) && pluginVersion.charAt(i - 1) == '.') {
                placeToSplit = i - 1;
                break;
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
                throw new IllegalArgumentException("Shouldn't be called for updating same file: " + updatedFile);
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
            @NotNull ScalarIndexExtension<Integer> index,
            @NotNull Function1<Module, Boolean> checkModule,
            @NotNull Function1<Integer, Boolean> checkAbiVersion
    ) {
        ID<Integer, Void> id = index.getName();

        Module[] modules = ModuleManager.getInstance(project).getModules();

        List<Module> modulesToCheck = KotlinPackage.filter(modules, checkModule);
        if (modulesToCheck.isEmpty()) return Collections.emptyList();

        Collection<Integer> abiVersions = collectAllKeys(id, modulesToCheck);
        Set<Integer> badAbiVersions = Sets.newHashSet(KotlinPackage.filter(abiVersions, checkAbiVersion));
        Set<VirtualFile> badRoots = Sets.newHashSet();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        for (Integer version : badAbiVersions) {
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
    private static Collection<Integer> collectAllKeys(@NotNull ID<Integer, Void> id, @NotNull List<Module> modules) {
        Set<Integer> allKeys = new HashSet<Integer>();

        for (Module module : modules) {
            GlobalSearchScope scope = GlobalSearchScope.moduleWithLibrariesScope(module);
            FileBasedIndex.getInstance().processAllKeys(id, new CommonProcessors.CollectProcessor<Integer>(allKeys), scope, null);
        }

        return allKeys;
    }
}
