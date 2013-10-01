package org.jetbrains.jet.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.jet.plugin.configuration.KotlinProjectConfigurator;

public class ConfigureKotlinJsInProjectAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = PlatformDataKeys.PROJECT.getData(e.getDataContext());
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
