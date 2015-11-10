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
import com.intellij.execution.application.BaseJavaApplicationCommandLineState;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.configurations.*;
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
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPackage;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import kotlin.ArraysKt;
import kotlin.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.KtLightClassForExplicitDeclaration;
import org.jetbrains.kotlin.asJava.KtLightMethod;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtDeclarationContainer;
import org.jetbrains.kotlin.psi.KtNamedFunction;
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

        Module module = getConfigurationModule().getModule();
        if (module == null) {
            throw new RuntimeConfigurationError("Module not specified");
        }
        if (StringUtil.isEmpty(MAIN_CLASS_NAME)) {
            throw new RuntimeConfigurationError("No main class specified");
        }
        PsiClass psiClass = JavaExecutionUtil.findMainClass(module, MAIN_CLASS_NAME);
        if (psiClass == null) {
            throw new RuntimeConfigurationWarning("Class '" + MAIN_CLASS_NAME + "' not found in module " + getConfigurationModule().getModuleName());
        }
        if (findMainFun(psiClass) == null) {
            throw new RuntimeConfigurationWarning("The class " + MAIN_CLASS_NAME + " has no main method");
        }
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment)
        throws ExecutionException {
        return new MyJavaCommandLineState(this, executionEnvironment);
    }

    @Nullable
    @Override
    public RefactoringElementListener getRefactoringElementListener(PsiElement element) {
        FqName fqNameBeingRenamed;
        if (element instanceof KtDeclarationContainer) {
            fqNameBeingRenamed = KotlinRunConfigurationProducer.Companion.getStartClassFqName((KtDeclarationContainer) element);
        }
        else if (element instanceof PsiPackage) {
            fqNameBeingRenamed = new FqName(((PsiPackage) element).getQualifiedName());
        }
        else {
            fqNameBeingRenamed = null;
        }

        if (fqNameBeingRenamed == null ||
            !MAIN_CLASS_NAME.equals(fqNameBeingRenamed.asString()) && !MAIN_CLASS_NAME.startsWith(fqNameBeingRenamed.asString() + ".")) {
            return null;
        }

        if (element instanceof KtDeclarationContainer) {
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

        final String nameSuffix = MAIN_CLASS_NAME.substring(fqNameBeingRenamed.toString().length());
        return new RefactoringElementAdapter() {
            @Override
            protected void elementRenamedOrMoved(@NotNull PsiElement newElement) {
                updateMainClassNameWithSuffix(newElement, nameSuffix);
            }

            @Override
            public void undoElementMovedOrRenamed(@NotNull PsiElement newElement, @NotNull String oldQualifiedName) {
                updateMainClassNameWithSuffix(newElement, nameSuffix);
            }
        };
    }

    private void updateMainClassName(PsiElement element) {
        KtDeclarationContainer container = KotlinRunConfigurationProducer.Companion.getEntryPointContainer(element);
        FqName name = KotlinRunConfigurationProducer.Companion.getStartClassFqName(container);
        if (name != null) {
            MAIN_CLASS_NAME = name.asString();
        }
    }

    private void updateMainClassNameWithSuffix(PsiElement element, String suffix) {
        if (element instanceof PsiPackage) {
            MAIN_CLASS_NAME = ((PsiPackage) element).getQualifiedName() + suffix;
        }
    }

    @Override
    public String suggestedName() {
        if (StringUtil.isEmpty(MAIN_CLASS_NAME)) {
            return null;
        }
        return MAIN_CLASS_NAME;
    }

    @NotNull
    private static Collection<KtNamedFunction> getMainFunCandidates(@NotNull PsiClass psiClass) {
        return CollectionsKt.filterNotNull(
                ArraysKt.map(
                        psiClass.getAllMethods(),
                        new Function1<PsiMethod, KtNamedFunction>() {
                            @Override
                            public KtNamedFunction invoke(PsiMethod method) {
                                if (!(method instanceof KtLightMethod)) return null;

                                KtDeclaration declaration = ((KtLightMethod) method).getOrigin();
                                return declaration instanceof KtNamedFunction ? (KtNamedFunction) declaration : null;
                            }
                        }
                )
        );
    }

    @Nullable
    private static KtNamedFunction findMainFun(@NotNull PsiClass psiClass) {
        for (KtNamedFunction function : getMainFunCandidates(psiClass)) {
            BindingContext bindingContext = ResolutionUtils.analyze(function, BodyResolveMode.FULL);
            MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(bindingContext);
            if (mainFunctionDetector.isMain(function)) return function;
        }
        return null;
    }

    private static class MyJavaCommandLineState extends BaseJavaApplicationCommandLineState<JetRunConfiguration> {
        public MyJavaCommandLineState(@NotNull JetRunConfiguration configuration, ExecutionEnvironment environment) {
            super(environment, configuration);
        }

        @Override
        protected JavaParameters createJavaParameters() throws ExecutionException {
            JavaParameters params = new JavaParameters();
            RunConfigurationModule module = myConfiguration.getConfigurationModule();

            int classPathType = getClasspathType(module);

            String jreHome = myConfiguration.ALTERNATIVE_JRE_PATH_ENABLED ? myConfiguration.ALTERNATIVE_JRE_PATH : null;
            JavaParametersUtil.configureModule(module, params, classPathType, jreHome);
            setupJavaParameters(params);

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

            KtNamedFunction mainFun = findMainFun(psiClass);
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
            if (psiClass instanceof KtLightClassForExplicitDeclaration) {
                return String.format("Function 'main' not found in class '%s'", classFqName);
            }
            return String.format("Top-level function 'main' not found in package '%s'", classFqName.parent());
        }
    }
}
