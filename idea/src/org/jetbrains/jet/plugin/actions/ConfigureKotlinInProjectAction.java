package org.jetbrains.jet.plugin.actions;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.jet.plugin.configuration.ConfigureKotlinInProjectUtils;
import org.jetbrains.jet.plugin.configuration.KotlinJsModuleConfigurator;
import org.jetbrains.jet.plugin.configuration.KotlinProjectConfigurator;

import java.util.Collection;

public class ConfigureKotlinInProjectAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = CommonDataKeys.PROJECT.getData(e.getDataContext());
        if (project == null) {
            return;
        }

        if (ConfigureKotlinInProjectUtils.isProjectConfigured(project)) {
            Messages.showInfoMessage("All modules with kotlin files are configured", "Configure Project Info");
            return;
        }

        Collection<KotlinProjectConfigurator> configurators =
                Collections2.filter(ConfigureKotlinInProjectUtils.getApplicableConfigurators(project), new Predicate<KotlinProjectConfigurator>() {
                    @Override
                    public boolean apply(KotlinProjectConfigurator input) {
                        return !input.getName().equals(KotlinJsModuleConfigurator.NAME);
                    }
                });


        if (configurators.size() == 1) {
            configurators.iterator().next().configure(project);
        }
        else if (configurators.isEmpty()) {
            Messages.showErrorDialog("There aren't configurators available", "Configure Kotlin in Project");
        }
        else {
            Messages.showErrorDialog("More than one configurator are available", "Configure Kotlin in Project");
            ConfigureKotlinInProjectUtils.showConfigureKotlinNotificationIfNeeded(project);
        }
    }
}
