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
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator;
import org.jetbrains.kotlin.idea.configuration.RuntimeLibraryFiles;
import org.jetbrains.kotlin.idea.framework.ui.CreateLibraryDialog;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;
import org.jetbrains.kotlin.idea.util.projectStructure.ProjectStructurePackage;

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
    private final boolean useRelativePaths;

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
        useRelativePaths = project == null;
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

        Library library = ProjectStructurePackage.findLibrary(rootModel.orderEntries(), new Function1<Library, Boolean>() {
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
            deferredCopyFileRequests.performRequests(ProjectStructurePackage.getModuleDir(module), model);
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

        public void performRequests(@NotNull String relativePath, Library.ModifiableModel model) {
            for (CopyFileRequest request : copyFilesRequests) {
                String destinationPath = FileUtil.isAbsolute(request.toDir) ?
                                         request.toDir :
                                         new File(relativePath, request.toDir).getPath();

                File resultFile = configurator.copyFileToDir(request.file, destinationPath);

                if (request.replaceInLib) {
                    ProjectStructurePackage.replaceFileRoot(model, request.file, resultFile);
                }
            }
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
        KotlinWithLibraryConfigurator configurator =
                (KotlinWithLibraryConfigurator) ConfigureKotlinInProjectUtils.getConfiguratorByName(configuratorName);
        assert configurator != null : "Configurator with name " + configuratorName + " should exists";

        deferredCopyFileRequests = new DeferredCopyFileRequests(configurator);

        String defaultPathToJarFile = useRelativePaths ? DEFAULT_LIB_DIR_NAME
                                                       : FileUIUtils.createRelativePath(null, contextDirectory, DEFAULT_LIB_DIR_NAME);

        RuntimeLibraryFiles files = configurator.getExistingJarFiles();

        File libraryFile;
        File librarySrcFile;

        File stdJarInDefaultPath = files.getRuntimeDestination(defaultPathToJarFile);
        if (!useRelativePaths && stdJarInDefaultPath.exists()) {
            libraryFile = stdJarInDefaultPath;

            File sourcesJar = files.getRuntimeSourcesDestination(defaultPathToJarFile);
            if (sourcesJar.exists()) {
                librarySrcFile = sourcesJar;
            }
            else {
                librarySrcFile = files.getRuntimeSourcesJar();
                deferredCopyFileRequests.addCopyWithReplaceRequest(librarySrcFile, libraryFile.getParent());
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

            libraryFile = files.getRuntimeJar();
            librarySrcFile = files.getRuntimeSourcesJar();
        }

        return createConfiguration(libraryFile, librarySrcFile);
    }

    @NotNull
    protected NewLibraryConfiguration createConfiguration(@NotNull File libraryFile, @NotNull File librarySrcFile) {
        final String libraryFileUrl = VfsUtil.getUrlForLibraryRoot(libraryFile);
        final String libraryFileSrcUrl = VfsUtil.getUrlForLibraryRoot(librarySrcFile);

        return new NewLibraryConfiguration(libraryName, null, new LibraryVersionProperties()) {
            @Override
            public void addRoots(@NotNull LibraryEditor editor) {
                editor.addRoot(libraryFileUrl, OrderRootType.CLASSES);
                editor.addRoot(libraryFileSrcUrl, OrderRootType.SOURCES);
            }
        };
    }
}
