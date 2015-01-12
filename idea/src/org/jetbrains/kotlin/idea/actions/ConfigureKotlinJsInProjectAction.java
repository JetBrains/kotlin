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

package org.jetbrains.kotlin.idea.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.kotlin.idea.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.kotlin.idea.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.kotlin.idea.configuration.KotlinProjectConfigurator;

public class ConfigureKotlinJsInProjectAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null) {
            return;
        }
        KotlinProjectConfigurator configurator = ConfigureKotlinInProjectUtils.getConfiguratorByName(KotlinJsModuleConfigurator.NAME);

        assert configurator != null : "JsModuleConfigurator should be non-null";

        if (ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, configurator).isEmpty()) {
            Messages.showInfoMessage("All modules are configured", "Configure Project Info");
            return;
        }
        configurator.configure(project);
    }
}
