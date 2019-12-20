/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.daemon.impl.analysis.JavaModuleGraphUtil;
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
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ex.JavaSdkUtil;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.DefaultJDOMExternalizer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.listeners.RefactoringElementAdapter;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.util.PathsList;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.idea.core.FileIndexUtilsKt;
import org.jetbrains.kotlin.idea.project.PlatformKt;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtDeclarationContainer;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import java.util.*;

@SuppressWarnings("deprecation")
public class KotlinRunConfiguration extends JetRunConfiguration {
    public String VM_PARAMETERS;
    public String PROGRAM_PARAMETERS;
    public boolean ALTERNATIVE_JRE_PATH_ENABLED;
    public String ALTERNATIVE_JRE_PATH;
    public boolean PASS_PARENT_ENVS = true;

    private Map<String, String> myEnvs = new LinkedHashMap<String, String>();

    public KotlinRunConfiguration(String name, JavaRunConfigurationModule runConfigurationModule, ConfigurationFactory factory) {
        super(name, runConfigurationModule, factory);
        runConfigurationModule.init();
    }

    @Override
    public Collection<Module> getValidModules() {
        return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
    }

    @Nullable
    @Override
    public GlobalSearchScope getSearchScope() {
        return SearchScopeProvider.createSearchScope(getModules());
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        SettingsEditorGroup<KotlinRunConfiguration> group = new SettingsEditorGroup<KotlinRunConfiguration>();
        group.addEditor(ExecutionBundle.message("run.configuration.configuration.tab.title"), new KotlinRunConfigurationEditor(getProject()));
        JavaRunConfigurationExtensionManager.getInstance().appendEditors(this, group);
        group.addEditor(ExecutionBundle.message("logs.tab.title"), new LogConfigurationPanel<KotlinRunConfiguration>());
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
        String fqNameBeingRenamed;
        if (element instanceof KtDeclarationContainer) {
            fqNameBeingRenamed = KotlinRunConfigurationProducer.Companion.getStartClassFqName((KtDeclarationContainer) element);
        }
        else if (element instanceof PsiPackage) {
            fqNameBeingRenamed = ((PsiPackage) element).getQualifiedName();
        }
        else {
            fqNameBeingRenamed = null;
        }

        if (fqNameBeingRenamed == null ||
            !MAIN_CLASS_NAME.equals(fqNameBeingRenamed) && !MAIN_CLASS_NAME.startsWith(fqNameBeingRenamed + ".")) {
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
        if (container == null) return;
        String name = KotlinRunConfigurationProducer.Companion.getStartClassFqName(container);
        if (name != null) {
            MAIN_CLASS_NAME = name;
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
        List<String> parts = StringUtil.split(MAIN_CLASS_NAME, ".");
        if (parts.isEmpty()) {
            return MAIN_CLASS_NAME;
        }
        return parts.get(parts.size() - 1);
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
                                if (!method.getName().equals("main")) return null;

                                KtDeclaration declaration = ((KtLightMethod) method).getKotlinOrigin();
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
            MainFunctionDetector mainFunctionDetector =
                    new MainFunctionDetector(bindingContext, PlatformKt.getLanguageVersionSettings(function));
            if (mainFunctionDetector.isMain(function)) return function;
        }
        return null;
    }

    private static class MyJavaCommandLineState extends BaseJavaApplicationCommandLineState<KotlinRunConfiguration> {
        public MyJavaCommandLineState(@NotNull KotlinRunConfiguration configuration, ExecutionEnvironment environment) {
            super(environment, configuration);
        }

        @Override
        protected JavaParameters createJavaParameters() throws ExecutionException {
            JavaParameters params = new JavaParameters();
            JavaRunConfigurationModule module = myConfiguration.getConfigurationModule();

            int classPathType = DumbService.getInstance(module.getProject()).computeWithAlternativeResolveEnabled(
                    () -> getClasspathType(module));

            String jreHome = myConfiguration.ALTERNATIVE_JRE_PATH_ENABLED ? myConfiguration.ALTERNATIVE_JRE_PATH : null;
            JavaParametersUtil.configureModule(module, params, classPathType, jreHome);
            setupJavaParameters(params);

            params.setShortenCommandLine(null, module.getProject());
            params.setMainClass(myConfiguration.getRunClass());
            setupModulePath(params, module);

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
                if (FileIndexUtilsKt.isInTestSourceContentKotlinAware(fileIndex, virtualFileForMainFun)) {
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
            if (psiClass instanceof KtLightClassForSourceDeclaration) {
                return String.format("Function 'main' not found in class '%s'", classFqName);
            }
            return String.format("Top-level function 'main' not found in package '%s'", classFqName.parent());
        }

        private static void setupModulePath(JavaParameters params, JavaRunConfigurationModule module) {
            if (JavaSdkUtil.isJdkAtLeast(params.getJdk(), JavaSdkVersion.JDK_1_9)) {
                PsiJavaModule mainModule = DumbService.getInstance(module.getProject()).computeWithAlternativeResolveEnabled(
                        () -> JavaModuleGraphUtil.findDescriptorByElement(module.findClass(params.getMainClass())));
                if (mainModule != null) {
                    params.setModuleName(mainModule.getName());
                    PathsList classPath = params.getClassPath();
                    PathsList modulePath = params.getModulePath();

                    modulePath.addAll(classPath.getPathList());
                    classPath.clear();
                }
            }
        }
    }
}
