/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.k2jsrun;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.plugin.JetIcons;

/**
 * @author Pavel Talanov
 */
public final class K2JSRunConfigurationType extends ConfigurationTypeBase {
    public static K2JSRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(K2JSRunConfigurationType.class);
    }

    public K2JSRunConfigurationType() {
        super("K2JSConfigurationType", "K2JS", "Kotlin to Javascript configuration", JetIcons.LAUNCH);
        addFactory(new K2JSConfigurationFactory());
    }

    private class K2JSConfigurationFactory extends ConfigurationFactory {
        protected K2JSConfigurationFactory() {
            super(K2JSRunConfigurationType.this);
        }

        @Override
        public RunConfiguration createTemplateConfiguration(Project project) {
            return new K2JSRunConfiguration("Kotlin to JavaScript", project, this);
        }
    }
}