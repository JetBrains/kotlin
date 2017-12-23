/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExternalizablePath;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaRunConfigurationModule;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RefactoringListenerProvider;

/**
 * @deprecated Will be dropped in 1.2.20. Use KotlinRunConfiguration instead.
 */
@SuppressWarnings("DeprecatedIsStillUsed")
@Deprecated
public abstract class JetRunConfiguration extends ModuleBasedConfiguration<JavaRunConfigurationModule>
        implements CommonJavaRunConfigurationParameters, RefactoringListenerProvider {
    public String MAIN_CLASS_NAME;
    public String WORKING_DIRECTORY;

    public JetRunConfiguration(String name, JavaRunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
        super(name, runConfigurationModule, factory);
    }

    @Override
    public void setWorkingDirectory(String value) {
        WORKING_DIRECTORY = ExternalizablePath.urlValue(value);
    }

    @Override
    public String getWorkingDirectory() {
        return ExternalizablePath.localPathValue(WORKING_DIRECTORY);
    }
}