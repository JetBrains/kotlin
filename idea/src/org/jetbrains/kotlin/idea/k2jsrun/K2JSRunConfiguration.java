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

package org.jetbrains.kotlin.idea.k2jsrun;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

@SuppressWarnings("deprecation")
public final class K2JSRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
        implements RunConfigurationWithSuppressedDefaultRunAction {

    @NotNull
    private K2JSConfigurationSettings settings = new K2JSConfigurationSettings(getProject());

    public K2JSRunConfiguration(String name, RunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
        super(name, runConfigurationModule, factory);
        runConfigurationModule.init();
    }


    @Override
    public Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @Override
    public GlobalSearchScope getSearchScope() {
        return SearchScopeProvider.createSearchScope(getModules());
    }

    @Override
    protected ModuleBasedConfiguration createInstance() {
        return new K2JSRunConfiguration(getName(), getConfigurationModule(), getFactory());
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new K2JSRunConfigurationEditor(getProject());
    }


    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
            throws ExecutionException {
        return new MyProfileState();
    }

    private static final class MyProfileState implements RunProfileState {
        @Override
        public ExecutionResult execute(Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
            return new DefaultExecutionResult();
        }
    }

    @NotNull
    public K2JSConfigurationSettings settings() {
        return settings;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
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
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        element.addContent(XmlSerializer.serialize(settings()));
    }
}
