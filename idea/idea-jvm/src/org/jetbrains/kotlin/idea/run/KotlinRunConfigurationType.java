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

import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SimpleConfigurationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinIcons;
import org.jetbrains.kotlin.idea.KotlinJvmBundle;

public class KotlinRunConfigurationType extends SimpleConfigurationType {
    public static KotlinRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(KotlinRunConfigurationType.class);
    }

    public KotlinRunConfigurationType() {
        super("JetRunConfigurationType",
              KotlinJvmBundle.message("language.name.kotlin"),
              KotlinJvmBundle.message("language.name.kotlin"),
              NotNullLazyValue.createValue(() -> KotlinIcons.SMALL_LOGO));
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new KotlinRunConfiguration("", new JavaRunConfigurationModule(project, true), this);
    }
}
