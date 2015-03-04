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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.JetPluginUtil;
import org.jetbrains.kotlin.idea.framework.ui.CreateLibraryDialogWithModules;
import org.jetbrains.kotlin.idea.framework.ui.FileUIUtils;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils.showInfoNotification;

public abstract class KotlinWithLibraryConfigurator implements KotlinProjectConfigurator {
    public static final String DEFAULT_LIBRARY_DIR = "lib";

    @NotNull
    protected abstract String getLibraryName();

    @NotNull
    protected abstract String getMessageForOverrideDialog();

    @NotNull
    protected abstract String getDialogTitle();

    @NotNull
    protected abstract String getLibraryCaption();

    @NotNull
    public abstract RuntimeLibraryFiles getExistingJarFiles();

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return !JetPluginUtil.isAndroidGradleModule(module) &&
               !JetPluginUtil.isMavenModule(module) &&
               !JetPluginUtil.isGradleModule(module);
    }

    @Override
    public void configure(@NotNull Project project) {
        String defaultPathToJar = getDefaultPathToJarFile(project);
        boolean showPathToJarPanel = needToChooseJarPath(project);

        List<Module> nonConfiguredModules =
                !ApplicationManager.getApplication().isUnitTestMode() ?
                ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this) :
                Arrays.asList(ModuleManager.getInstance(project).getModules());

        List<Module> modulesToConfigure = nonConfiguredModules;
        String copyLibraryIntoPath = null;

        if (nonConfiguredModules.size() > 1 || showPathToJarPanel) {
            CreateLibraryDialogWithModules dialog = new CreateLibraryDialogWithModules(
                    project, nonConfiguredModules, defaultPathToJar, showPathToJarPanel,
                    getDialogTitle(),
                    getLibraryCaption());

            if (!ApplicationManager.getApplication().isUnitTestMode()) {
                dialog.show();
                if (!dialog.isOK()) return;
            }

            modulesToConfigure = dialog.getModulesToConfigure();
            copyLibraryIntoPath = dialog.getCopyIntoPath();
        }

        for (Module module : modulesToConfigure) {
            configureModuleWithLibrary(module, defaultPathToJar, copyLibraryIntoPath);
        }
    }

    protected void configureModuleWithLibrary(
            @NotNull Module module,
            @NotNull String defaultPath,
            @Nullable String pathFromDialog
    ) {
        Project project = module.getProject();

        RuntimeLibraryFiles files = getExistingJarFiles();
        LibraryState libraryState = getLibraryState(project);
        String dirToCopyJar = getPathToCopyFileTo(project, OrderRootType.CLASSES, defaultPath, pathFromDialog);
        FileState runtimeState =
                getJarState(project, files.getRuntimeDestination(dirToCopyJar), OrderRootType.CLASSES, pathFromDialog == null);

        configureModuleWithLibraryClasses(module, libraryState, runtimeState, dirToCopyJar);

        Library library = getKotlinLibrary(project);
        assert library != null : "Kotlin library should exists when adding sources root";
        String dirToCopySourcesJar = getPathToCopyFileTo(project, OrderRootType.SOURCES, defaultPath, pathFromDialog);
        FileState sourcesState = getJarState(project, files.getRuntimeSourcesDestination(dirToCopySourcesJar), OrderRootType.SOURCES,
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
        RuntimeLibraryFiles files = getExistingJarFiles();
        File runtimeJar = files.getRuntimeJar();
        File reflectJar = files.getReflectJar();

        switch (libraryState) {
            case LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        break;
                    }
                    case COPY: {
                        copyFileToDir(runtimeJar, dirToCopyJarTo);
                        if (reflectJar != null) {
                            copyFileToDir(reflectJar, dirToCopyJarTo);
                        }
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
                        addJarsToExistingLibrary(
                                project, files.getRuntimeDestination(dirToCopyJarTo), files.getReflectDestination(dirToCopyJarTo)
                        );
                        break;
                    }
                    case COPY: {
                        addJarsToExistingLibrary(
                                project, copyFileToDir(runtimeJar, dirToCopyJarTo), copyFileToDir(reflectJar, dirToCopyJarTo)
                        );
                        break;
                    }
                    case DO_NOT_COPY: {
                        addJarsToExistingLibrary(project, runtimeJar, reflectJar);
                        break;
                    }
                }
                break;
            case NEW_LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        addJarsToNewLibrary(
                                project, files.getRuntimeDestination(dirToCopyJarTo), files.getReflectDestination(dirToCopyJarTo)
                        );
                        break;
                    }
                    case COPY: {
                        addJarsToNewLibrary(project, copyFileToDir(runtimeJar, dirToCopyJarTo), copyFileToDir(reflectJar, dirToCopyJarTo));
                        break;
                    }
                    case DO_NOT_COPY: {
                        addJarsToNewLibrary(project, runtimeJar, reflectJar);
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
        RuntimeLibraryFiles files = getExistingJarFiles();
        File runtimeSourcesJar = files.getRuntimeSourcesJar();
        switch (jarState) {
            case EXISTS: {
                if (dirToCopyJarTo != null) {
                    addSourcesToLibraryIfNeeded(library, files.getRuntimeSourcesDestination(dirToCopyJarTo));
                }
                break;
            }
            case COPY: {
                assert dirToCopyJarTo != null : "Path to copy should be non-null";
                File file = copyFileToDir(runtimeSourcesJar, dirToCopyJarTo);
                addSourcesToLibraryIfNeeded(library, file);
                break;
            }
            case DO_NOT_COPY: {
                addSourcesToLibraryIfNeeded(library, runtimeSourcesJar);
                break;
            }
        }
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

    @Contract("!null, _ -> !null")
    @Nullable
    public File copyFileToDir(@Nullable File file, @NotNull String toDir) {
        if (file == null) return null;

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

    private void addJarsToExistingLibrary(@NotNull Project project, @NotNull File runtimeJar, @Nullable File reflectJar) {
        Library library = getKotlinLibrary(project);
        assert library != null : "Kotlin library should present, instead createNewLibrary should be invoked";

        final Library.ModifiableModel model = library.getModifiableModel();
        model.addRoot(VfsUtil.getUrlForLibraryRoot(runtimeJar), OrderRootType.CLASSES);
        if (reflectJar != null) {
            model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectJar), OrderRootType.CLASSES);
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                model.commit();
            }
        });

        showInfoNotification(library.getName() + " library was configured");
    }

    private void addJarsToNewLibrary(
            @NotNull Project project,
            @NotNull final File runtimeJar,
            @Nullable final File reflectJar
    ) {
        final LibraryTable table = LibraryTablesRegistrar.getInstance().getLibraryTable(project);
        final Ref<Library> library = new Ref<Library>();
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                library.set(table.createLibrary(getLibraryName()));
                Library.ModifiableModel model = library.get().getModifiableModel();
                model.addRoot(VfsUtil.getUrlForLibraryRoot(runtimeJar), OrderRootType.CLASSES);
                if (reflectJar != null) {
                    model.addRoot(VfsUtil.getUrlForLibraryRoot(reflectJar), OrderRootType.CLASSES);
                }
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

        String fileName = getExistingJarFiles().getRuntimeJar().getName();

        for (VirtualFile root : library.getFiles(OrderRootType.CLASSES)) {
            if (root.getName().equals(fileName)) {
                return true;
            }
        }

        return false;
    }

    protected boolean needToChooseJarPath(@NotNull Project project) {
        String defaultPath = getDefaultPathToJarFile(project);
        return !isProjectLibraryPresent(project) && !getExistingJarFiles().getRuntimeDestination(defaultPath).exists();
    }

    protected String getDefaultPathToJarFile(@NotNull Project project) {
        return FileUIUtils.createRelativePath(project, project.getBaseDir(), DEFAULT_LIBRARY_DIR);
    }

    protected void showError(@NotNull String message) {
        Messages.showErrorDialog(message, getMessageForOverrideDialog());
    }

    protected enum FileState {
        EXISTS,
        COPY,
        DO_NOT_COPY
    }

    protected enum LibraryState {
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
            @NotNull File targetFile,
            @NotNull OrderRootType jarType,
            boolean useBundled
    ) {
        if (targetFile.exists()) {
           return FileState.EXISTS;
        }
        else if (getPathFromLibrary(project, jarType) != null) {
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
