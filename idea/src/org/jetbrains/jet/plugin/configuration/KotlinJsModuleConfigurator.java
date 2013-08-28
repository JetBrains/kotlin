package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.framework.JSLibraryStdDescription;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaScriptLibraryDialogWithModules;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.List;

public class KotlinJsModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = "js";

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "As JavaScript project";
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (KotlinFrameworkDetector.isJsKotlinModule(module)) {
            String pathFromLibrary = getPathToJarFromLibrary(module.getProject());
            return pathFromLibrary != null && getJarFile(pathFromLibrary).exists();
        }
        return false;
    }

    @NotNull
    @Override
    protected String getLibraryName() {
        return JSLibraryStdDescription.LIBRARY_NAME;
    }

    @NotNull
    @Override
    protected String getJarName() {
        return PathUtil.JS_LIB_JAR_NAME;
    }

    @Override
    protected void addRootsToLibrary(@NotNull Library.ModifiableModel library, @NotNull File jarFile) {
        String libraryRoot = VfsUtil.getUrlForLibraryRoot(jarFile);
        library.addRoot(libraryRoot, OrderRootType.CLASSES);
        library.addRoot(libraryRoot, OrderRootType.SOURCES);
    }

    @NotNull
    @Override
    protected String getMessageForOverrideDialog() {
        return JSLibraryStdDescription.JAVA_SCRIPT_LIBRARY_CREATION;
    }

    @NotNull
    @Override
    public File getExistedJarFile() {
        File result;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            result = PathUtil.getKotlinPathsForDistDirectory().getJsLibJarPath();
        }
        else {
            result = PathUtil.getKotlinPathsForIdeaPlugin().getJsLibJarPath();
        }
        if (!result.exists()) {
            showError("Jar file wasn't found in " + result.getPath());
        }
        return result;
    }

    @Override
    public void configure(@NotNull Project project) {
        String defaultPathToJar = getDefaultPathToJarFile(project);
        String defaultPathToJsFile = getDefaultPathToJsFile(project);

        boolean showPathToJarPanel = needToChooseJarPath(project);
        boolean showPathToJsFilePanel = needToChooseJsFilePath(project);

        List<Module> nonConfiguredModules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this);

        CreateJavaScriptLibraryDialogWithModules dialog =
                new CreateJavaScriptLibraryDialogWithModules(project, nonConfiguredModules,
                                                  defaultPathToJar, defaultPathToJsFile,
                                                  showPathToJarPanel, showPathToJsFilePanel);
        dialog.show();

        if (!dialog.isOK()) return;

        for (Module module : dialog.getModulesToConfigure()) {
            configureModuleWithLibrary(module, defaultPathToJar, dialog.getCopyLibraryIntoPath());
        }
        configureModuleWithJsFile(defaultPathToJsFile, dialog.getCopyJsIntoPath());
    }

    public static boolean isJsFilePresent(@NotNull String dir) {
        String runtimeJarFileName = dir + "/" + PathUtil.JS_LIB_JAR_NAME;
        return new File(runtimeJarFileName).exists();
    }

    @NotNull
    public File getJsFile() {
        File result;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            result = PathUtil.getKotlinPathsForDistDirectory().getJsLibJsPath();
        }
        else {
            result = PathUtil.getKotlinPathsForIdeaPlugin().getJsLibJsPath();
        }
        if (!result.exists()) {
            showError("Jar file wasn't found in " + result.getPath());
        }
        return result;
    }

    private static boolean needToChooseJsFilePath(@NotNull Project project) {
        String defaultPath = FileUIUtils.createRelativePath(project, project.getBaseDir(), "script");
        return !isJsFilePresent(defaultPath);
    }

    @NotNull
    private static String getDefaultPathToJsFile(@NotNull Project project) {
        return FileUIUtils.createRelativePath(project, project.getBaseDir(), "script");
    }

    protected void configureModuleWithJsFile(
            @NotNull String defaultPath,
            @Nullable String pathToJsFromDialog
    ) {
        boolean isJsFilePresent = isJsFilePresent(defaultPath);
        if (isJsFilePresent) return;

        if (pathToJsFromDialog != null) {
            copyFileToDir(getJsFile(), pathToJsFromDialog);
        }
    }

    KotlinJsModuleConfigurator() {
    }
}
