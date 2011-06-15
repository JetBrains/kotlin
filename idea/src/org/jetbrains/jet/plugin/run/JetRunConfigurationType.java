package org.jetbrains.jet.plugin.run;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

/**
 * @author yole
 */
public class JetRunConfigurationType extends ConfigurationTypeBase {
    public static JetRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(JetRunConfigurationType.class);
    }

    public JetRunConfigurationType() {
        super("JetRunConfigurationType", "Jet", "Jet", JetFileType.INSTANCE.getIcon());
        addFactory(new JetRunConfigurationFactory(this));
    }

    private static class JetRunConfigurationFactory extends ConfigurationFactory {
        protected JetRunConfigurationFactory(@NotNull ConfigurationType type) {
            super(type);
        }

        @Override
        public RunConfiguration createTemplateConfiguration(Project project) {
            return new JetRunConfiguration("", new RunConfigurationModule(project), this);
        }
    }
}