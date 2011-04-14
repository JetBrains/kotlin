package org.jetbrains.jet.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author yole
 */
public class JetRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule> {
    public String MAIN_CLASS_NAME;

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

    public void readExternal(final Element element) throws InvalidDataException {
      PathMacroManager.getInstance(getProject()).expandPaths(element);
      super.readExternal(element);
      RunConfigurationExtension.readSettings(this, element);
      DefaultJDOMExternalizer.readExternal(this, element);
      readModule(element);
    }

    public void writeExternal(final Element element) throws WriteExternalException {
      super.writeExternal(element);
      RunConfigurationExtension.writeSettings(this, element);
      DefaultJDOMExternalizer.writeExternal(this, element);
      writeModule(element);
      PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
    }
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        final JavaCommandLineState state = new MyJavaCommandLineState(executionEnvironment);
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    private class MyJavaCommandLineState extends JavaCommandLineState {
        protected MyJavaCommandLineState(@NotNull ExecutionEnvironment environment) {
            super(environment);
        }

        @Override
        protected JavaParameters createJavaParameters() throws ExecutionException {
            final JavaParameters params = new JavaParameters();
            final int classPathType = JavaParametersUtil.getClasspathType(getConfigurationModule(), MAIN_CLASS_NAME, false);
            JavaParametersUtil.configureModule(getConfigurationModule(), params, classPathType, null);
            params.setMainClass(MAIN_CLASS_NAME);
            return params;
        }
    }
}
