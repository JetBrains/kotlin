package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.framework.JavaRuntimeLibraryDescription;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaLibraryDialogWithModules;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.List;

public class KotlinJavaModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = "java";

    @Override
    public boolean isConfigured(@NotNull Module module) {
        return KotlinFrameworkDetector.isJavaKotlinModule(module);
    }

    @NotNull
    @Override
    protected String getLibraryName() {
        return JavaRuntimeLibraryDescription.LIBRARY_NAME;
    }

    @NotNull
    @Override
    protected String getJarName() {
        return PathUtil.KOTLIN_JAVA_RUNTIME_JAR;
    }

    @Override
    protected void addRootsToLibrary(@NotNull Library.ModifiableModel library, @NotNull File jarFile) {
        String libraryRoot = VfsUtil.getUrlForLibraryRoot(jarFile);
        library.addRoot(libraryRoot, OrderRootType.CLASSES);
        library.addRoot(libraryRoot + "src", OrderRootType.SOURCES);
    }

    @NotNull
    @Override
    protected String getMessageForOverrideDialog() {
        return JavaRuntimeLibraryDescription.JAVA_RUNTIME_LIBRARY_CREATION;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "Java";
    }

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void configure(@NotNull Project project) {
        String defaultPath = getDefaultPathToJarFile(project);
        boolean showPathPanelForJava = needToChooseJarPath(project);

        List<Module> nonConfiguredModules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this);

        if (nonConfiguredModules.size() > 1 || showPathPanelForJava) {
            CreateJavaLibraryDialogWithModules dialog =
                    new CreateJavaLibraryDialogWithModules(project, nonConfiguredModules, defaultPath, showPathPanelForJava);
            dialog.show();
            if (!dialog.isOK()) return;
            for (Module module : dialog.getModulesToConfigure()) {
                configureModuleWithLibrary(module, defaultPath, dialog.getCopyIntoPath());
            }
        }
        else {
            for (Module module : nonConfiguredModules) {
                configureModuleWithLibrary(module, defaultPath, null);
            }
        }
    }

    @Override
    @NotNull
    public File getExistedJarFile() {
        File result;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            result = PathUtil.getKotlinPathsForDistDirectory().getRuntimePath();
        }
        else {
            result = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
        }
        if (!result.exists()) {
            showError("Jar file wasn't found in " + result.getPath());
        }
        return result;
    }

    KotlinJavaModuleConfigurator() {
    }
}
