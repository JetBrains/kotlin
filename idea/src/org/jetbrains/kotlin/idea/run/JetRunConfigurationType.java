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

package org.jetbrains.kotlin.idea.run;

import com.intellij.execution.configurations.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinIcons;
import org.jetbrains.kotlin.idea.KotlinLanguage;

public class JetRunConfigurationType extends ConfigurationTypeBase {
    public static JetRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(JetRunConfigurationType.class);
    }

    public JetRunConfigurationType() {
        super("JetRunConfigurationType", KotlinLanguage.NAME, KotlinLanguage.NAME, KotlinIcons.SMALL_LOGO_13);
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
