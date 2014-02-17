/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.*;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.openapi.components.PathMacroManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.SettingsEditorGroup;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetMainDetector;
import org.jetbrains.jet.plugin.stubindex.JetTopLevelFunctionsFqnNameIndex;

import java.util.*;

public class JetRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
    implements CommonJavaRunConfigurationParameters {

    public String MAIN_CLASS_NAME;
    public String VM_PARAMETERS;
    public String PROGRAM_PARAMETERS;
    public String WORKING_DIRECTORY;

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

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        SettingsEditorGroup<JetRunConfiguration> group = new SettingsEditorGroup<JetRunConfiguration>();
        group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new JetRunConfigurationEditor(getProject()));
        JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);
        group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<JetRunConfiguration>());
        return group;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        PathMacroManager.getInstance(getProject()).expandPaths(element);
        super.readExternal(element);

        JavaRunConfigurationExtensionManager.getInstance().readExternal(this, element);
        DefaultJDOMExternalizer.readExternal(this, element);

        readModule(element);
        EnvironmentVariablesComponent.readExternal(element, getEnvs());
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        super.writeExternal(element);
        JavaRunConfigurationExtensionManager.getInstance().writeExternal(this, element);
        DefaultJDOMExternalizer.writeExternal(this, element);

        writeModule(element);
        EnvironmentVariablesComponent.writeExternal(element, getEnvs());
        PathMacroManager.getInstance(getProject()).collapsePathsRecursively(element);
    }

    @Override
    public void setVMParameters(String value) {
        VM_PARAMETERS = value;
    }

    @Override
    public String getVMParameters() {
        return VM_PARAMETERS;
    }

    @Override
    public void setProgramParameters(String value) {
        PROGRAM_PARAMETERS = value;
    }

    @Override
    public String getProgramParameters() {
        return PROGRAM_PARAMETERS;
    }

    @Override
    public void setWorkingDirectory(String value) {
        WORKING_DIRECTORY = ExternalizablePath.urlValue(value);
    }

    @Override
    public String getWorkingDirectory() {
        return ExternalizablePath.localPathValue(WORKING_DIRECTORY);
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
    public void setEnvs(@NotNull Map<String, String> envs) {
        this.myEnvs = envs;
    }

    @Override
    public boolean isPassParentEnvs() {
        return PASS_PARENT_ENVS;
    }

    @Override
    public String getRunClass() {
        return MAIN_CLASS_NAME;
    }

    public void setRunClass(String value) {
        MAIN_CLASS_NAME = value;
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

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
        throws ExecutionException {
        JavaCommandLineState state = new MyJavaCommandLineState(this, executionEnvironment);
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    private class MyJavaCommandLineState extends JavaCommandLineState {

        private final JetRunConfiguration myConfiguration;

        public MyJavaCommandLineState(@NotNull JetRunConfiguration configuration, ExecutionEnvironment environment) {
            super(environment);
            myConfiguration = configuration;
        }

        @Override
        protected JavaParameters createJavaParameters() throws ExecutionException {
            JavaParameters params = new JavaParameters();
            RunConfigurationModule module = myConfiguration.getConfigurationModule();

            int classPathType = getClasspathType(module);

            String jreHome = myConfiguration.ALTERNATIVE_JRE_PATH_ENABLED ? myConfiguration.ALTERNATIVE_JRE_PATH : null;
            JavaParametersUtil.configureModule(module, params, classPathType, jreHome);
            JavaParametersUtil.configureConfiguration(params, myConfiguration);

            params.setMainClass(myConfiguration.getRunClass());

            return params;
        }

        private int getClasspathType(RunConfigurationModule configurationModule) throws CantRunException {
            Module module = configurationModule.getModule();
            if (module == null) throw CantRunException.noModuleConfigured(configurationModule.getModuleName());

            String runClass = myConfiguration.getRunClass();
            if (runClass == null) throw new CantRunException(String.format("Run class should be defined for configuration '%s'", myConfiguration.getName()));

            PsiClass psiClass = JavaExecutionUtil.findMainClass(module, runClass);
            if (psiClass == null) throw CantRunException.classNotFound(runClass, module);

            FqName packageFqName = new FqName(runClass).parent();
            JetNamedFunction mainFun = findMainFun(module, packageFqName);
            if (mainFun == null) throw new CantRunException(String.format("Top-level function 'main' not found in package '%s'", packageFqName));

            Module classModule = ModuleUtilCore.findModuleForPsiElement(mainFun);
            if (classModule == null) classModule = module;
            ModuleFileIndex fileIndex = ModuleRootManager.getInstance(classModule).getFileIndex();

            VirtualFile virtualFileForMainFun = mainFun.getContainingFile().getVirtualFile();
            if (virtualFileForMainFun == null) throw new CantRunException(String.format("Top-level function 'main' not found in package '%s'", packageFqName));

            if (fileIndex.isInSourceContent(virtualFileForMainFun)) {
                if (fileIndex.isInTestSourceContent(virtualFileForMainFun)) {
                    return JavaParameters.JDK_AND_CLASSES_AND_TESTS;
                }
                else {
                    return JavaParameters.JDK_AND_CLASSES;
                }
            }
            List<OrderEntry> entriesForFile = fileIndex.getOrderEntriesForFile(virtualFileForMainFun);
            for (OrderEntry entry : entriesForFile) {
                if (entry instanceof ExportableOrderEntry && ((ExportableOrderEntry)entry).getScope() == DependencyScope.TEST) {
                    return JavaParameters.JDK_AND_CLASSES_AND_TESTS;
                }
            }
            return JavaParameters.JDK_AND_CLASSES;
        }

        @Nullable
        private JetNamedFunction findMainFun(@NotNull Module module, @NotNull FqName packageFqName) throws CantRunException {
            String mainFunFqName = packageFqName.child(Name.identifier("main")).asString();
            Collection<JetNamedFunction> mainFunctions =
                    JetTopLevelFunctionsFqnNameIndex.getInstance().get(mainFunFqName, module.getProject(), module.getModuleScope(true));
            for (JetNamedFunction function : mainFunctions) {
                if (JetMainDetector.isMain(function)) {
                    return function;
                }
            }
            return null;
        }
    }
}
