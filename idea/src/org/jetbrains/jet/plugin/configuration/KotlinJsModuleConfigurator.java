/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.plugin.framework.JSLibraryStdDescription;
import org.jetbrains.jet.plugin.framework.ui.CreateJavaScriptLibraryDialogWithModules;
import org.jetbrains.jet.plugin.framework.ui.FileUIUtils;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
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
        return "JavaScript";
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (ProjectStructureUtil.isJsKotlinModule(module)) {
            String pathFromLibrary = getPathFromLibrary(module.getProject(), OrderRootType.CLASSES);
            return pathFromLibrary != null && getFileInDir(getJarName(), pathFromLibrary).exists();
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
    public String getJarName() {
        return PathUtil.JS_LIB_JAR_NAME;
    }

    @NotNull
    @Override
    protected String getSourcesJarName() {
        return PathUtil.JS_LIB_JAR_NAME;
    }

    @NotNull
    @Override
    protected String getMessageForOverrideDialog() {
        return JSLibraryStdDescription.JAVA_SCRIPT_LIBRARY_CREATION;
    }

    @NotNull
    @Override
    public File getExistedJarFile() {
        return assertFileExists(PathUtil.getKotlinPathsForIdeaPlugin().getJsLibJarPath());
    }

    @Override
    protected File getExistedSourcesJarFile() {
        return getExistedJarFile();
    }

    @Override
    public void configure(@NotNull Project project) {
        String defaultPathToJar = getDefaultPathToJarFile(project);
        String defaultPathToJsFile = getDefaultPathToJsFile(project);

        boolean showPathToJarPanel = needToChooseJarPath(project);
        boolean showPathToJsFilePanel = needToChooseJsFilePath(project);

        List<Module> nonConfiguredModules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this);

        if (nonConfiguredModules.size() > 1 || showPathToJarPanel || showPathToJsFilePanel) {
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
        else {
            for (Module module : nonConfiguredModules) {
                configureModuleWithLibrary(module, defaultPathToJar, null);
            }
            configureModuleWithJsFile(defaultPathToJsFile, null);
        }
    }

    public static boolean isJsFilePresent(@NotNull String dir) {
        return new File(dir + "/" + PathUtil.JS_LIB_JAR_NAME).exists();
    }

    @NotNull
    public File getJsFile() {
        return assertFileExists(PathUtil.getKotlinPathsForIdeaPlugin().getJsLibJsPath());
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
