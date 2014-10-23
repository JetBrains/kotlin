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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.framework.JSLibraryStdDescription;
import org.jetbrains.jet.plugin.framework.ui.CreateLibraryDialogWithModules;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.k2js.JavaScript;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class KotlinJsModuleConfigurator extends KotlinWithLibraryConfigurator {
    public static final String NAME = JavaScript.LOWER_NAME;

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return JavaScript.FULL_NAME;
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        return ProjectStructureUtil.isJsKotlinModule(module);
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
    public String getSourcesJarName() {
        return PathUtil.JS_LIB_SRC_JAR_NAME;
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
    public File getExistedSourcesJarFile() {
        return getExistedJarFile();
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
                    JSLibraryStdDescription.DIALOG_TITLE,
                    JSLibraryStdDescription.DIALOG_CAPTION);

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

    KotlinJsModuleConfigurator() {
    }
}
