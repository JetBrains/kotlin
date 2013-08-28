package org.jetbrains.jet.plugin.configuration;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;

import java.io.File;

import static com.intellij.notification.Notifications.Bus;

public abstract class KotlinWithLibraryConfigurator implements KotlinProjectConfigurator {

    @NotNull
    protected abstract String getLibraryName();

    @NotNull
    protected abstract String getJarName();

    @NotNull
    protected abstract String getMessageForOverrideDialog();

    @NotNull
    protected abstract File getExistedJarFile();

    protected abstract void addRootsToLibrary(@NotNull Library.ModifiableModel library, @NotNull File jarFile);

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return true;
    }

    public boolean isJarPresent(@NotNull String dir) {
        return getJarInDir(dir).exists();
    }

    public File getJarInDir(@NotNull String dir) {
        String runtimeJarFileName = dir + "/" + getJarName();
        return new File(runtimeJarFileName);
    }

    protected void configureModuleWithLibrary(
            @NotNull Module module,
            @NotNull LibraryState libraryState,
            @NotNull FileState jarState,
            @Nullable String dirWithJar
    ) {
        Project project = module.getProject();

        switch (libraryState) {
            case LIBRARY:
                switch (jarState) {
                    case EXISTS: {
                        break;
                    }
                    case COPY: {
                        String pathToJarFromLibrary = getPathToJarFromLibrary(project);
                        if (pathToJarFromLibrary != null) {
                            copyJarToDir(pathToJarFromLibrary);
                        }
                        else {
                            showError("Cannot copy jar to root for " + getLibraryName() + " library");
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
                        assert dirWithJar !=
                               null : "Jar file exists, so dirWithJar must be non-null: should be default path to lib directory";
                        addJarToExistedLibrary(project, getJarFile(dirWithJar));
                        break;
                    }
                    case COPY: {
                        assert dirWithJar != null : "Path to copy should be visible in configuration dialog and must be non-null";
                        File file = copyJarToDir(dirWithJar);
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
                        assert dirWithJar !=
                               null : "Jar file exists, so dirWithJar must be non-null: should be default path to lib directory";
                        addJarToNewLibrary(project, getJarFile(dirWithJar));
                        break;
                    }
                    case COPY: {
                        assert dirWithJar != null : "Path to copy should be visible in configuration dialog and must be non-null";
                        File file = copyJarToDir(dirWithJar);
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

    private void addLibraryToModuleIfNeeded(Module module) {
        if (getKotlinLibrary(module) == null) {
            Library library = getKotlinLibrary(module.getProject());
            assert library != null : "Kotlin project library should exists";
            ModuleRootModificationUtil.addDependency(module, library);
            showInfoNotification(library.getName() + " library was added to module " + module.getName());
        }
    }

    @Nullable
    protected String getPathToJarFromLibrary(Project project) {
        Library library = getKotlinLibrary(project);
        if (library == null) return null;

        String[] libraryFiles = library.getUrls(OrderRootType.CLASSES);
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

    @NotNull
    protected File getJarFile(@NotNull String dirToJar) {
        return new File(dirToJar + "/" + getJarName());
    }

    protected void configureModuleWithLibrary(
            @NotNull Module module,
            @NotNull String defaultPath,
            @Nullable String pathFromDialog
    ) {
        Project project = module.getProject();

        FileState runtimeState = getJarState(project, defaultPath, pathFromDialog);
        LibraryState libraryState = getLibraryState(project);
        String dirToCopyJar = isJarPresent(defaultPath) ? defaultPath : pathFromDialog;

        configureModuleWithLibrary(module, libraryState, runtimeState, dirToCopyJar);
    }

    private void addJarToExistedLibrary(@NotNull Project project, @NotNull File jarFile) {
        Library library = getKotlinLibrary(project);
        assert library != null : "Kotlin library should present, instead createNewLibrary should be invoked";
        final Library.ModifiableModel model = library.getModifiableModel();
        addRootsToLibrary(model, jarFile);
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
                addRootsToLibrary(model, jarFile);
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
    private Library getKotlinLibrary(@NotNull Module module) {
        final Ref<Library> result = Ref.create(null);
        OrderEnumerator.orderEntries(module).forEachLibrary(new Processor<Library>() {
            @Override
            public boolean process(Library library) {
                if (isKotlinLibrary(library)) {
                    result.set(library);
                    return false;
                }
                return true;
            }
        });
        return result.get();
    }

    @Nullable
    private Library getKotlinLibrary(@NotNull Project project) {
        LibrariesContainer librariesContainer = LibrariesContainerFactory.createContainer(project);
        for (Library library : librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.PROJECT)) {
            if (isKotlinLibrary(library)) {
                return library;
            }
        }
        for (Library library : librariesContainer.getLibraries(LibrariesContainer.LibraryLevel.GLOBAL)) {
            if (isKotlinLibrary(library)) {
                return library;
            }
        }
        return null;
    }

    private boolean isKotlinLibrary(@NotNull Library library) {
        return getLibraryName().equals(library.getName());
    }

    public File copyJarToDir(@NotNull String toDir) {
        return copyFileToDir(getExistedJarFile(), toDir);
    }

    public File copyFileToDir(@NotNull File file, @NotNull String toDir) {
        File copy = FileUIUtils.copyWithOverwriteDialog(getMessageForOverrideDialog(), toDir, file);
        if (copy != null) {
            showInfoNotification(file.getName() + " was copied to " + toDir);
        }
        return copy;
    }

    private static void showInfoNotification(@NotNull String message) {
        Bus.notify(new Notification("Configure Kotlin", "Configure Kotlin", message, NotificationType.INFORMATION));
    }

    protected boolean needToChooseJarPath(@NotNull Project project) {
        String defaultPath = getDefaultPathToJarFile(project);
        return !isProjectLibraryPresent(project) && !isJarPresent(defaultPath);
    }

    protected static String getDefaultPathToJarFile(@NotNull Project project) {
        return FileUIUtils.createRelativePath(project, project.getBaseDir(), "lib");
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
            @NotNull String defaultPath,
            @Nullable String pathFromDialog
    ) {
        String pathFormLibrary = getPathToJarFromLibrary(project);
        if (isJarPresent(defaultPath) ||
                (pathFromDialog != null && isJarPresent(pathFromDialog)) ||
                (pathFormLibrary != null && getJarFile(pathFormLibrary).exists())) {
            return FileState.EXISTS;
        }
        else if (pathFromDialog == null) {
            return FileState.DO_NOT_COPY;
        }
        else {
            return FileState.COPY;
        }
    }

    KotlinWithLibraryConfigurator() {
    }
}
