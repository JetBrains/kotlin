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

package org.jetbrains.kotlin.idea.framework;

import com.google.common.collect.Lists;
import com.intellij.framework.library.LibraryVersionProperties;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.libraries.NewLibraryConfiguration;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.*;
import org.jetbrains.kotlin.idea.framework.ui.CreateLibraryDialog;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;
import org.jetbrains.kotlin.idea.util.projectStructure.ProjectStructureUtilKt;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class CustomLibraryDescriptorWithDeferredConfig extends CustomLibraryDescription {

    private static final String DEFAULT_LIB_DIR_NAME = "lib";

    private final String configuratorName;
    private final String libraryName;
    private final String dialogTitle;
    private final String modulesSeparatorCaption;
    private final LibraryKind libraryKind;
    private final Set<? extends LibraryKind> suitableLibraryKinds;
    private final VirtualFile projectBaseDir;

    private DeferredCopyFileRequests deferredCopyFileRequests;

    /**
     * @param project null when project doesn't exist yet (called from project wizard)
     */
    public CustomLibraryDescriptorWithDeferredConfig(
            @Nullable Project project,
            @NotNull String configuratorName,
            @NotNull String libraryName,
            @NotNull String dialogTitle,
            @NotNull String modulesSeparatorCaption,
            @NotNull LibraryKind libraryKind,
            @NotNull Set<? extends LibraryKind> suitableLibraryKinds
    ) {
        this.projectBaseDir = project != null ? project.getBaseDir() : null;
        this.configuratorName = configuratorName;
        this.libraryName = libraryName;
        this.dialogTitle = dialogTitle;
        this.modulesSeparatorCaption = modulesSeparatorCaption;
        this.libraryKind = libraryKind;
        this.suitableLibraryKinds = suitableLibraryKinds;
    }

    @Nullable
    public DeferredCopyFileRequests getCopyFileRequests() {
        return deferredCopyFileRequests;
    }

    @NotNull
    @Override
    public Set<? extends LibraryKind> getSuitableLibraryKinds() {
        return suitableLibraryKinds;
    }

    public void finishLibConfiguration(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
        DeferredCopyFileRequests deferredCopyFileRequests = getCopyFileRequests();
        if (deferredCopyFileRequests == null) return;

        Library library = ProjectStructureUtilKt.findLibrary(rootModel.orderEntries(), new Function1<Library, Boolean>() {
            @Override
            public Boolean invoke(@NotNull Library library) {
                LibraryPresentationManager libraryPresentationManager = LibraryPresentationManager.getInstance();
                List<VirtualFile> classFiles = Arrays.asList(library.getFiles(OrderRootType.CLASSES));

                return libraryPresentationManager.isLibraryOfKind(classFiles, libraryKind);
            }
        });

        if (library == null) {
            return;
        }

        Library.ModifiableModel model = library.getModifiableModel();
        try {
            deferredCopyFileRequests.performRequests(module.getProject(), ProjectStructureUtilKt.getModuleDir(module), model);
        }
        finally {
            model.commit();
        }
    }

    public static class DeferredCopyFileRequests {
        private final List<CopyFileRequest> copyFilesRequests = Lists.newArrayList();
        private final KotlinWithLibraryConfigurator configurator;

        public DeferredCopyFileRequests(KotlinWithLibraryConfigurator configurator) {
            this.configurator = configurator;
        }

        public void performRequests(@NotNull Project project, @NotNull String relativePath, Library.ModifiableModel model) {
            NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(project);
            for (CopyFileRequest request : copyFilesRequests) {
                String destinationPath = FileUtil.isAbsolute(request.toDir) ?
                                         request.toDir :
                                         new File(relativePath, request.toDir).getPath();

                File resultFile = configurator.copyFileToDir(request.file, destinationPath, collector);

                if (request.replaceInLib) {
                    ProjectStructureUtilKt.replaceFileRoot(model, request.file, resultFile);
                }
            }
            collector.showNotification();
        }

        public void addCopyWithReplaceRequest(@NotNull File file, @NotNull String copyIntoPath) {
            copyFilesRequests.add(new CopyFileRequest(copyIntoPath, file, true));
        }

        public static class CopyFileRequest {
            private final String toDir;
            private final File file;
            private final boolean replaceInLib;

            public CopyFileRequest(String dir, File file, boolean replaceInLib) {
                toDir = dir;
                this.file = file;
                this.replaceInLib = replaceInLib;
            }
        }
    }

    @Nullable
    @Override
    public NewLibraryConfiguration createNewLibrary(@NotNull JComponent parentComponent, @Nullable VirtualFile contextDirectory) {
        KotlinWithLibraryConfigurator configurator = getConfigurator();

        deferredCopyFileRequests = new DeferredCopyFileRequests(configurator);

        String defaultPathToJarFile = projectBaseDir == null ? DEFAULT_LIB_DIR_NAME
                                                       : FileUIUtils.createRelativePath(null, projectBaseDir, DEFAULT_LIB_DIR_NAME);

        RuntimeLibraryFiles files = configurator.getExistingJarFiles();

        File runtimeJar;
        File reflectJar;
        File runtimeSrcJar;

        File stdJarInDefaultPath = files.getRuntimeDestination(defaultPathToJarFile);
        if (projectBaseDir != null && stdJarInDefaultPath.exists()) {
            runtimeJar = stdJarInDefaultPath;

            reflectJar = files.getReflectDestination(defaultPathToJarFile);
            if (reflectJar != null && !reflectJar.exists()) {
                reflectJar = files.getReflectJar();
                assert reflectJar != null : "getReflectDestination != null, but getReflectJar == null";
                deferredCopyFileRequests.addCopyWithReplaceRequest(reflectJar, runtimeJar.getParent());
            }

            runtimeSrcJar = files.getRuntimeSourcesDestination(defaultPathToJarFile);
            if (!runtimeSrcJar.exists()) {
                runtimeSrcJar = files.getRuntimeSourcesJar();
                deferredCopyFileRequests.addCopyWithReplaceRequest(runtimeSrcJar, runtimeJar.getParent());
            }
        }
        else {
            CreateLibraryDialog dialog = new CreateLibraryDialog(defaultPathToJarFile, dialogTitle, modulesSeparatorCaption);
            dialog.show();

            if (!dialog.isOK()) return null;

            String copyIntoPath = dialog.getCopyIntoPath();
            if (copyIntoPath != null) {
                for (File file : files.getAllJars()) {
                    deferredCopyFileRequests.addCopyWithReplaceRequest(file, copyIntoPath);
                }
            }

            runtimeJar = files.getRuntimeJar();
            reflectJar = files.getReflectJar();
            runtimeSrcJar = files.getRuntimeSourcesJar();
        }

        return createConfiguration(Arrays.asList(runtimeJar, reflectJar), runtimeSrcJar);
    }

    @NotNull
    private KotlinWithLibraryConfigurator getConfigurator() {
        KotlinWithLibraryConfigurator configurator =
                (KotlinWithLibraryConfigurator) ConfigureKotlinInProjectUtilsKt.getConfiguratorByName(configuratorName);
        assert configurator != null : "Configurator with name " + configuratorName + " should exists";
        return configurator;
    }

    // Implements an API added in IDEA 16
    @Nullable
    public NewLibraryConfiguration createNewLibraryWithDefaultSettings(@Nullable VirtualFile contextDirectory) {
        RuntimeLibraryFiles files = getConfigurator().getExistingJarFiles();
        return createConfiguration(Arrays.asList(files.getRuntimeJar(), files.getReflectJar()), files.getRuntimeSourcesJar());
    }

    @NotNull
    protected NewLibraryConfiguration createConfiguration(@NotNull final List<File> libraryFiles, @NotNull final File librarySrcFile) {
        return new NewLibraryConfiguration(libraryName, null, new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                for (File libraryFile : libraryFiles) {
                    if (libraryFile != null) {
                        editor.addRoot(VfsUtil.getUrlForLibraryRoot(libraryFile), OrderRootType.CLASSES);
                    }
                }
                editor.addRoot(VfsUtil.getUrlForLibraryRoot(librarySrcFile), OrderRootType.SOURCES);
            }
        };
    }
}
