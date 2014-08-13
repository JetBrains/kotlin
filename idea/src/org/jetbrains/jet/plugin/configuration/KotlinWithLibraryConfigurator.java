/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;

import java.io.File;

import static org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils.showInfoNotification;

public abstract class KotlinWithLibraryConfigurator implements KotlinProjectConfigurator {
    public static final String DEFAULT_LIBRARY_DIR = "lib";

    @NotNull
    protected abstract String getLibraryName();

    @NotNull
    protected abstract String getJarName();

    @NotNull
    protected abstract String getSourcesJarName();

    @NotNull
    protected abstract String getMessageForOverrideDialog();

    @NotNull
    protected abstract File getExistedJarFile();

    protected abstract File getExistedSourcesJarFile();

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return !JetPluginUtil.isAndroidGradleModule(module) &&
               !JetPluginUtil.isMavenModule(module) &&
               !JetPluginUtil.isGradleModule(module);
    }

    protected void configureModuleWithLibrary(
            @NotNull Module module,
            @NotNull String defaultPath,
            @Nullable String pathFromDialog
    ) {
        Project project = module.getProject();

        LibraryState libraryState = getLibraryState(project);
        String dirToCopyJar = getPathToCopyFileTo(project, OrderRootType.CLASSES, defaultPath, pathFromDialog);
        FileState runtimeState = getJarState(project, getJarName(), OrderRootType.CLASSES, dirToCopyJar, pathFromDialog == null);

        configureModuleWithLibraryClasses(module, libraryState, runtimeState, dirToCopyJar);

        Library library = getKotlinLibrary(project);
        assert library != null : "Kotlin library should exists when adding sources root";
        String dirToCopySourcesJar = getPathToCopyFileTo(project, OrderRootType.SOURCES, defaultPath, pathFromDialog);
        FileState sourcesState = getJarState(project, getSourcesJarName(), OrderRootType.SOURCES, dirToCopySourcesJar,
                                             pathFromDialog == null);

        configureModuleWithLibrarySources(library, sourcesState, dirToCopySourcesJar);
    }

    protected void configureModuleWithLibraryClasses(
            @NotNull Module module,
            @NotNull LibraryState libraryState,
            @NotNull FileState jarState,
            @NotNull String dirToCopyJarTo
    ) {
        Project project = module.getProject();

        switch (libraryState) {
            case LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        break;
                    }
                    case COPY: {
                        copyJarToDir(dirToCopyJarTo);
                        break;
                    }
                    case DO_NOT_COPY: {
                        throw new IllegalStateException(
                                "Kotlin library exists, so path to copy should be hidden in configuration dialog and jar should be copied using path in library table");
                    }
                }
                break;
            case NON_CONFIGURED_LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        addJarToExistedLibrary(project, getFileInDir(getJarName(), dirToCopyJarTo));
                        break;
                    }
                    case COPY: {
                        File file = copyJarToDir(dirToCopyJarTo);
                        addJarToExistedLibrary(project, file);
                        break;
                    }
                    case DO_NOT_COPY: {
                        addJarToExistedLibrary(project, getExistedJarFile());
                        break;
                    }
                }
                break;
            case NEW_LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        addJarToNewLibrary(project, getFileInDir(getJarName(), dirToCopyJarTo));
                        break;
                    }
                    case COPY: {
                        File file = copyJarToDir(dirToCopyJarTo);
                        addJarToNewLibrary(project, file);
                        break;
                    }
                    case DO_NOT_COPY: {
                        addJarToNewLibrary(project, getExistedJarFile());
                        break;
                    }
                }
                break;
        }
        addLibraryToModuleIfNeeded(module);
    }

    protected void configureModuleWithLibrarySources(
            @NotNull Library library,
            @NotNull FileState jarState,
            @Nullable String dirToCopyJarTo
    ) {
        switch (jarState) {
            case EXISTS: {
                if (dirToCopyJarTo != null) {
                    addSourcesToLibraryIfNeeded(library, getFileInDir(getSourcesJarName(), dirToCopyJarTo));
                }
                break;
            }
            case COPY: {
                assert dirToCopyJarTo != null : "Path to copy should be non-null";
                File file = copyFileToDir(getExistedSourcesJarFile(), dirToCopyJarTo);
                addSourcesToLibraryIfNeeded(library, file);
                break;
            }
            case DO_NOT_COPY: {
                addSourcesToLibraryIfNeeded(library, getExistedSourcesJarFile());
                break;
            }
        }
    }

    @NotNull
    public static File getFileInDir(@NotNull String fileName, @NotNull String dirToJar) {
        return new File(dirToJar + "/" + fileName);
    }

    @Nullable
    public Library getKotlinLibrary(@NotNull Project project) {
        LibrariesContainer librariesContainer = LibrariesContainerFactory.createContainer(project);
        for (Library library : librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.PROJECT)) {
            if (isKotlinLibrary(project, library)) {
                return library;
            }
        }
        for (Library library : librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.GLOBAL)) {
            if (isKotlinLibrary(project, library)) {
                return library;
            }
        }
        return null;
    }

    public File copyFileToDir(@NotNull File file, @NotNull String toDir) {
        File copy = FileUIUtils.copyWithOverwriteDialog(getMessageForOverrideDialog(), toDir, file);
        if (copy != null) {
            showInfoNotification(file.getName() + " was copied to " + toDir);
        }
        return copy;
    }

    @Nullable
    protected String getPathFromLibrary(@NotNull Project project, @NotNull OrderRootType type) {
        return getPathFromLibrary(getKotlinLibrary(project), type);
    }

    @Nullable
    protected static String getPathFromLibrary(@Nullable Library library, @NotNull OrderRootType type) {
        if (library == null) return null;

        String[] libraryFiles = library.getUrls(type);
        if (libraryFiles.length < 1) return null;

        String pathToJarInLib = VfsUtilCore.urlToPath(libraryFiles[0]);
        String parentDir = VfsUtil.getParentDir(VfsUtil.getParentDir(pathToJarInLib));
        if (parentDir == null) return null;

        File parentDirFile = new File(parentDir);
        if (!parentDirFile.exists() && !parentDirFile.mkdirs()) {
            return null;
        }
        return parentDir;
    }

    protected static boolean addSourcesToLibraryIfNeeded(@NotNull Library library, @NotNull File file) {
        String[] librarySourceRoots = library.getUrls(OrderRootType.SOURCES);
        String librarySourceRoot = VfsUtil.getUrlForLibraryRoot(file);
        for (String sourceRoot : librarySourceRoots) {
            if (sourceRoot.equals(librarySourceRoot)) return false;
        }

        final Library.ModifiableModel model = library.getModifiableModel();
        model.addRoot(librarySourceRoot, OrderRootType.SOURCES);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
            }
        });

        showInfoNotification("Source root '" + librarySourceRoot + "' was added to " + library.getName() + " library");
        return true;
    }

    private void addLibraryToModuleIfNeeded(Module module) {
        DependencyScope expectedDependencyScope = getDependencyScope(module);
        Library kotlinLibrary = getKotlinLibrary(module);
        if (kotlinLibrary == null) {
            Library library = getKotlinLibrary(module.getProject());
            assert library != null : "Kotlin project library should exists";

            ModuleRootModificationUtil.addDependency(module, library, expectedDependencyScope, false);
            showInfoNotification(library.getName() + " library was added to module " + module.getName());
        }
        else {
            LibraryOrderEntry libraryEntry = findLibraryOrderEntry(ModuleRootManager.getInstance(module).getOrderEntries(), kotlinLibrary);
            if (libraryEntry != null) {
                DependencyScope libraryDependencyScope = libraryEntry.getScope();
                if (!expectedDependencyScope.equals(libraryDependencyScope)) {
                    libraryEntry.setScope(expectedDependencyScope);

                    showInfoNotification(kotlinLibrary.getName() + " library scope has changed from " + libraryDependencyScope +
                                         " to " + expectedDependencyScope + " for module " + module.getName());
                }
            }
        }
    }

    @Nullable
    private static LibraryOrderEntry findLibraryOrderEntry(@NotNull OrderEntry[] orderEntries, @NotNull Library library) {
        for (OrderEntry orderEntry : orderEntries) {
            if (orderEntry instanceof LibraryOrderEntry && library.equals(((LibraryOrderEntry)orderEntry).getLibrary())) {
                return (LibraryOrderEntry)orderEntry;
            }
        }

        return null;
    }

    @NotNull
    private static DependencyScope getDependencyScope(@NotNull Module module) {
        if (ProjectStructureUtil.hasKotlinFilesOnlyInTests(module)) {
            return DependencyScope.TEST;
        }
        return DependencyScope.COMPILE;
    }

    private void addJarToExistedLibrary(@NotNull Project project, @NotNull File jarFile) {
        Library library = getKotlinLibrary(project);
        assert library != null : "Kotlin library should present, instead createNewLibrary should be invoked";

        final Library.ModifiableModel model = library.getModifiableModel();
        model.addRoot(VfsUtil.getUrlForLibraryRoot(jarFile), OrderRootType.CLASSES);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
            }
        });

        showInfoNotification(library.getName() + " library was configured");
    }

    private void addJarToNewLibrary(
            @NotNull Project project,
            @NotNull final File jarFile
    ) {
        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        final Ref<Library> library = new Ref<Library>();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                library.set(table.createLibrary(getLibraryName()));
                Library.ModifiableModel model = library.get().getModifiableModel();
                model.addRoot(VfsUtil.getUrlForLibraryRoot(jarFile), OrderRootType.CLASSES);
                model.commit();
            }
        });

        showInfoNotification(library.get().getName() + " library was created");
    }

    private boolean isProjectLibraryWithoutPathsPresent(@NotNull Project project) {
        Library library = getKotlinLibrary(project);
        return library != null && library.getUrls(OrderRootType.CLASSES).length == 0;
    }

    private boolean isProjectLibraryPresent(@NotNull Project project) {
        Library library = getKotlinLibrary(project);
        return library != null && library.getUrls(OrderRootType.CLASSES).length > 0;
    }

    @Nullable
    private Library getKotlinLibrary(@NotNull final Module module) {
        final Ref<Library> result = Ref.create(null);
        OrderEnumerator.orderEntries(module).forEachLibrary(new Processor<Library>() {
            @Override
            public boolean process(Library library) {
                if (isKotlinLibrary(module.getProject(), library)) {
                    result.set(library);
                    return false;
                }
                return true;
            }
        });
        return result.get();
    }

    protected boolean isKotlinLibrary(@NotNull Project project, @NotNull Library library) {
        if (getLibraryName().equals(library.getName())) {
            return true;
        }

        for (VirtualFile root : library.getFiles(OrderRootType.CLASSES)) {
            if (root.getName().equals(getJarName())) {
                return true;
            }
        }

        return false;
    }

    private File copyJarToDir(@NotNull String toDir) {
        return copyFileToDir(getExistedJarFile(), toDir);
    }

    protected boolean needToChooseJarPath(@NotNull Project project) {
        String defaultPath = getDefaultPathToJarFile(project);
        return !isProjectLibraryPresent(project) && !getFileInDir(getJarName(), defaultPath).exists();
    }

    protected String getDefaultPathToJarFile(@NotNull Project project) {
        return FileUIUtils.createRelativePath(project, project.getBaseDir(), DEFAULT_LIBRARY_DIR);
    }

    protected void showError(@NotNull String message) {
        Messages.showErrorDialog(message, getMessageForOverrideDialog());
    }

    protected static enum FileState {
        EXISTS,
        COPY,
        DO_NOT_COPY
    }

    protected static enum LibraryState {
        LIBRARY,
        NON_CONFIGURED_LIBRARY,
        NEW_LIBRARY,
    }

    @NotNull
    protected LibraryState getLibraryState(@NotNull Project project) {
        if (isProjectLibraryPresent(project)) {
            return LibraryState.LIBRARY;
        }
        else if (isProjectLibraryWithoutPathsPresent(project)) {
            return LibraryState.NON_CONFIGURED_LIBRARY;
        }
        return LibraryState.NEW_LIBRARY;
    }

    @NotNull
    protected FileState getJarState(
            @NotNull Project project,
            @NotNull String jarName,
            @NotNull OrderRootType jarType,
            @NotNull String copyPath,
            boolean useBundled
    ) {
        String pathFromLibrary = getPathFromLibrary(project, jarType);
        if (getFileInDir(jarName, copyPath).exists()) {
           return FileState.EXISTS;
        }
        else if (pathFromLibrary != null) {
            return FileState.COPY;
        }
        else if (useBundled) {
            return FileState.DO_NOT_COPY;
        }
        else {
            return FileState.COPY;
        }
    }

    @NotNull
    private String getPathToCopyFileTo(
            @NotNull Project project,
            @NotNull OrderRootType jarType,
            @NotNull String defaultDir,
            @Nullable String pathFromDialog
    ) {
        if (pathFromDialog != null) {
            return pathFromDialog;
        }
        String pathFromLibrary = getPathFromLibrary(project, jarType);
        if (pathFromLibrary != null) {
            return pathFromLibrary;
        }
        return defaultDir;
    }

    protected File assertFileExists(@NotNull File file) {
        if (!file.exists()) {
            showError("Couldn't find file: " + file.getPath());
        }
        return file;
    }

    KotlinWithLibraryConfigurator() {
    }
}
