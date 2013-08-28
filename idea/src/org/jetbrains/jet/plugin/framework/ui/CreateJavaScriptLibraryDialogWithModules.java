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

package org.jetbrains.jet.plugin.framework.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.framework.JSLibraryCreateOptions;

import java.awt.*;
import java.util.List;

public class CreateJavaScriptLibraryDialogWithModules extends CreateJavaScriptLibraryDialogBase implements JSLibraryCreateOptions {

    private final ChooseModulePanel chooseModulePanel;

    public CreateJavaScriptLibraryDialogWithModules(
            @NotNull Project project,
            @NotNull List<Module> modules,
            @NotNull String defaultPathToJar,
            @NotNull String defaultPathToJsFile,
            boolean showPathToJarPanel,
            boolean showPathToJsFilePanel
    ) {
        super(project, defaultPathToJar, defaultPathToJsFile, showPathToJarPanel, showPathToJsFilePanel);

        chooseModulePanel = new ChooseModulePanel(project, modules);
        chooseModulesPanelPlace.add(chooseModulePanel.getContentPane(), BorderLayout.CENTER);

        if (showPathToJarPanel || showPathToJsFilePanel) {
            chooseModulePanel.showSeparator();
        }

        updateComponents();
    }

    public List<Module> getModulesToConfigure() {
        return chooseModulePanel.getModulesToConfigure();
    }
}
