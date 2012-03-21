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

package org.jetbrains.jet.plugin.run;

import com.intellij.execution.*;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author yole
 */
public class JetRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
    implements CommonJavaRunConfigurationParameters {

    private JetRunConfigurationSettings settings = new JetRunConfigurationSettings();

    public boolean ALTERNATIVE_JRE_PATH_ENABLED;
    public String ALTERNATIVE_JRE_PATH;
    private Map<String, String> myEnvs = new LinkedHashMap<String, String>();
    public boolean PASS_PARENT_ENVS = true;

    public JetRunConfiguration(String name, RunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
        super(name, runConfigurationModule, factory);
        runConfigurationModule.init();
    }

    @Override
    public Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @Override
    protected ModuleBasedConfiguration createInstance() {
        return new JetRunConfiguration(getName(), getConfigurationModule(), getFactory());
    }

    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new JetRunConfigurationEditor(getProject());
    }

    @Override
    public void readExternal(final Element element) throws InvalidDataException {
        PathMacroManager.getInstance(getProject()).expandPaths(element);
        super.readExternal(element);

        JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element);
        readModule(element);
        EnvironmentVariablesComponent.readExternal(element, getEnvs());

        Element settingsElement = element.getChild(JetRunConfigurationSettings.class.getSimpleName());
        if (settingsElement != null) {
            JetRunConfigurationSettings readSettings = XmlSerializer.deserialize(settingsElement, JetRunConfigurationSettings.class);
            if (readSettings != null) {
                settings = readSettings;
            }
        }
    }

    @Override
    public void writeExternal(final Element element) throws WriteExternalException {
        super.writeExternal(element);
        JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element);

        writeModule(element);
        EnvironmentVariablesComponent.writeExternal(element, getEnvs());
        PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);

        element.addContent(XmlSerializer.serialize(settings()));
    }

    @Override
    public void setVMParameters(String value) {
        settings().setVmParameters(value);
    }

    @Override
    public String getVMParameters() {
        return settings().getVmParameters();
    }

    @Override
    public void setProgramParameters(String value) {
        settings().setProgramParameters(value);
    }

    @Override
    public String getProgramParameters() {
        return settings().getProgramParameters();
    }

    @Override
    public void setWorkingDirectory(String value) {
        settings().setWorkingDirectory(ExternalizablePath.urlValue(value));
    }

    @Override
    public String getWorkingDirectory() {
        return ExternalizablePath.localPathValue(settings().getWorkingDirectory());
    }

    @Override
    public void setPassParentEnvs(boolean passParentEnvs) {
        PASS_PARENT_ENVS = passParentEnvs;
    }

    @Override
    @NotNull
    public Map<String, String> getEnvs() {
        return myEnvs;
    }

    @Override
    public void setEnvs(@NotNull final Map<String, String> envs) {
        this.myEnvs = envs;
    }

    @Override
    public boolean isPassParentEnvs() {
        return PASS_PARENT_ENVS;
    }

    @Override
    public String getRunClass() {
        return settings().getMainClassName();
    }

    public void setRunClass(String value) {
        settings().setMainClassName(value);
    }

    @Override
    public String getPackage() {
        return null;
    }

    @Override
    public boolean isAlternativeJrePathEnabled() {
        return ALTERNATIVE_JRE_PATH_ENABLED;
    }

    @Override
    public void setAlternativeJrePathEnabled(boolean enabled) {
        ALTERNATIVE_JRE_PATH_ENABLED = enabled;
    }

    @Override
    public String getAlternativeJrePath() {
        return ALTERNATIVE_JRE_PATH;
    }

    @Override
    public void setAlternativeJrePath(String path) {
        ALTERNATIVE_JRE_PATH = path;
    }

    @NotNull
    public JetRunConfigurationSettings settings() {
        return settings;
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
        throws ExecutionException {
        final JavaCommandLineState state = new MyJavaCommandLineState(this, executionEnvironment);
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    private class MyJavaCommandLineState extends JavaCommandLineState {

        private final JetRunConfiguration myConfiguration;

        public MyJavaCommandLineState(@NotNull final JetRunConfiguration configuration,
                                      final ExecutionEnvironment environment) {
            super(environment);
            myConfiguration = configuration;
        }

        @Override
        protected JavaParameters createJavaParameters() throws ExecutionException {
            final JavaParameters params = new JavaParameters();
            final RunConfigurationModule module = myConfiguration.getConfigurationModule();

            final int classPathType = JavaParametersUtil.getClasspathType(
                    module,
                    myConfiguration.settings().getMainClassName(),
                    false);

            final String jreHome = myConfiguration.ALTERNATIVE_JRE_PATH_ENABLED ? myConfiguration.ALTERNATIVE_JRE_PATH
                                                                                : null;
            JavaParametersUtil.configureModule(module, params, classPathType, jreHome);
            JavaParametersUtil.configureConfiguration(params, myConfiguration);

            params.setMainClass(myConfiguration.settings().getMainClassName());

            return params;
        }
    }
}
