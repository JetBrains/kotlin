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

import com.intellij.diagnostic.logging.LogConfigurationPanel;
import com.intellij.execution.*;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.JavaParametersUtil;
import com.intellij.execution.util.ProgramParametersUtil;
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KotlinLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.KotlinLightClassForPackage;
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetDeclarationContainer;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.*;

public class JetRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule>
    implements CommonJavaRunConfigurationParameters, RefactoringListenerProvider {

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
    public void checkConfiguration() throws RuntimeConfigurationException {
        JavaParametersUtil.checkAlternativeJRE(this);
        ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), getConfigurationModule().getModule());
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
        throws ExecutionException {
        JavaCommandLineState state = new MyJavaCommandLineState(this, executionEnvironment);
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject()));
        return state;
    }

    @Nullable
    @Override
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        if (element instanceof JetDeclarationContainer) {
            FqName name = KotlinRunConfigurationProducer.Companion.getStartClassFqName((JetDeclarationContainer) element);
            if (name != null && name.asString().equals(MAIN_CLASS_NAME)) {
                return new RefactoringElementAdapter() {
                    @Override
                    public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                        updateMainClassName(newElement);
                    }

                    @Override
                    protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                        updateMainClassName(newElement);
                    }
                };
            }
        }
        return null;
    }

    private void updateMainClassName(PsiElement element) {
        JetDeclarationContainer container = KotlinRunConfigurationProducer.Companion.getEntryPointContainer(element);
        FqName name = KotlinRunConfigurationProducer.Companion.getStartClassFqName(container);
        if (name != null) {
            MAIN_CLASS_NAME = name.asString();
        }
    }

    private static class MyJavaCommandLineState extends JavaCommandLineState {

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

            JetNamedFunction mainFun = findMainFun(module, psiClass);
            if (mainFun == null) throw new CantRunException(noFunctionFoundMessage(psiClass));

            Module classModule = ModuleUtilCore.findModuleForPsiElement(mainFun);
            if (classModule == null) classModule = module;

            VirtualFile virtualFileForMainFun = mainFun.getContainingFile().getVirtualFile();
            if (virtualFileForMainFun == null) throw new CantRunException(noFunctionFoundMessage(psiClass));

            ModuleFileIndex fileIndex = ModuleRootManager.getInstance(classModule).getFileIndex();
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

        @NotNull
        private String noFunctionFoundMessage(@NotNull PsiClass psiClass) {
            //noinspection ConstantConditions
            FqName classFqName = new FqName(psiClass.getQualifiedName());
            if (psiClass instanceof KotlinLightClassForExplicitDeclaration) {
                return String.format("Function 'main' not found in class '%s'", classFqName);
            }
            return String.format("Top-level function 'main' not found in package '%s'", classFqName.parent());
        }

        @NotNull
        private Collection<JetNamedFunction> getMainFunCandidates(@NotNull Module module, @NotNull PsiClass psiClass) {
            if (psiClass instanceof KotlinLightClassForPackage) {
                String qualifiedName = psiClass.getQualifiedName();
                if (qualifiedName == null) return Collections.emptyList();
                FqName mainFunFqName = new FqName(qualifiedName).parent().child(Name.identifier("main"));
                return JetTopLevelFunctionFqnNameIndex.getInstance().get(
                        mainFunFqName.asString(), module.getProject(), module.getModuleRuntimeScope(true)
                );
            }
            return KotlinPackage.filterNotNull(
                    KotlinPackage.map(
                            psiClass.findMethodsByName("main", false),
                            new Function1<PsiMethod, JetNamedFunction>() {
                                @Override
                                public JetNamedFunction invoke(PsiMethod method) {
                                    if (!(method instanceof KotlinLightMethod)) return null;

                                    JetDeclaration declaration = ((KotlinLightMethod) method).getOrigin();
                                    return declaration instanceof JetNamedFunction ? (JetNamedFunction) declaration : null;
                                }
                            }
                    )
            );
        }

        @Nullable
        private JetNamedFunction findMainFun(@NotNull Module module, @NotNull PsiClass psiClass) throws CantRunException {
            for (JetNamedFunction function : getMainFunCandidates(module, psiClass)) {
                BindingContext bindingContext = ResolvePackage.analyze(function, BodyResolveMode.FULL);
                MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(bindingContext);
                if (mainFunctionDetector.isMain(function)) return function;
            }
            return null;
        }
    }
}
