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

package org.jetbrains.kotlin.idea.framework.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;

import java.awt.*;
import java.util.Collection;
import java.util.List;

public class CreateLibraryDialogWithModules extends CreateLibraryDialogBase {

    private final ChooseModulePanel chooseModulePanel;

    public CreateLibraryDialogWithModules(
            @NotNull Project project,
            @NotNull KotlinProjectConfigurator configurator,
            @NotNull String defaultPath,
            boolean showPathPanel,
            @NotNull String title,
            @NotNull String libraryCaption,
            @NotNull Collection<Module> excludeModules
    ) {
        super(project, defaultPath, title, libraryCaption);

        chooseModulePanel = new ChooseModulePanel(project, configurator, excludeModules);
        chooseModulesPanelPlace.add(chooseModulePanel.getContentPane(), BorderLayout.CENTER);

        chooseLibraryPathPlace.setVisible(showPathPanel);
        modulesSeparator.setVisible(showPathPanel);

        updateComponents();
    }

    public List<Module> getModulesToConfigure() {
        return chooseModulePanel.getModulesToConfigure();
    }
}
