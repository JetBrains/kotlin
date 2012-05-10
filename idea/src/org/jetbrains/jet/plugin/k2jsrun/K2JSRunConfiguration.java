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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("deprecation")
public final class K2JSRunConfiguration extends RunConfigurationBase
    implements RunConfigurationWithSuppressedDefaultRunAction {

    @NotNull
    private K2JSConfigurationSettings settings = new K2JSConfigurationSettings(getProject());

    public K2JSRunConfiguration(String name, Project project, ConfigurationFactory factory) {
        super(project, factory, name);
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new K2JSRunConfigurationEditor(getProject());
    }

    @Override
    public JDOMExternalizable createRunnerSettings(ConfigurationInfoProvider provider) {
        return null;
    }

    @Override
    public SettingsEditor<JDOMExternalizable> getRunnerSettingsEditor(ProgramRunner runner) {
        return null;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
        throws ExecutionException {
        return new MyProfileState();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        //do nothing
    }

    private final class MyProfileState implements RunProfileState {
        @Override
        public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
            return new ExecutionResult() {
                @Override
                public ExecutionConsole getExecutionConsole() {
                    return null;
                }

                @Override
                public AnAction[] getActions() {
                    return AnAction.EMPTY_ARRAY;
                }

                @Override
                public ProcessHandler getProcessHandler() {
                    return null;
                }
            };
        }

        @Override
        public RunnerSettings getRunnerSettings() {
            return new RunnerSettings<JDOMExternalizable>(null, K2JSRunConfiguration.this);
        }

        @Override
        public ConfigurationPerRunnerSettings getConfigurationSettings() {
            return null;
        }
    }

    @NotNull
    public K2JSConfigurationSettings settings() {
        return settings;
    }

    @Override
    public void readExternal(final Element element) throws InvalidDataException {
        super.readExternal(element);
        Element settingsElement = element.getChild(K2JSConfigurationSettings.class.getSimpleName());
        if (settingsElement != null) {
            K2JSConfigurationSettings deserializedSettings = XmlSerializer.deserialize(settingsElement, K2JSConfigurationSettings.class);
            if (deserializedSettings != null) {
                settings = deserializedSettings;
            }
        }
    }

    @Override
    public void writeExternal(final Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.addContent(XmlSerializer.serialize(settings()));
    }
}
