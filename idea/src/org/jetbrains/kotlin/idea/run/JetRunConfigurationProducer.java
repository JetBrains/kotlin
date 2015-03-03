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

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.MainFunctionDetector;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade;
import org.jetbrains.kotlin.idea.caches.resolve.ResolvePackage;
import org.jetbrains.kotlin.idea.project.ProjectStructureUtil;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;

import java.util.List;

public class JetRunConfigurationProducer extends RuntimeConfigurationProducer implements Cloneable {
    @Nullable
    private PsiElement mySourceElement;

    public JetRunConfigurationProducer() {
        super(JetRunConfigurationType.getInstance());
    }

    @Nullable
    @Override
    public PsiElement getSourceElement() {
        return mySourceElement;
    }

    @Override
    protected RunnerAndConfigurationSettings createConfigurationByElement(@NotNull Location location, ConfigurationContext configurationContext) {
        JetDeclarationContainer container = getEntryPointContainer(location);
        if (container == null) return null;

        mySourceElement = (PsiElement) container;

        FqName startClassFQName = getStartClassFqName(container);
        if (startClassFQName == null) return null;

        Module module = location.getModule();
        assert module != null;

        return createConfigurationByQName(module, configurationContext, startClassFQName);
    }

    @Nullable
    private static FqName getStartClassFqName(@Nullable JetDeclarationContainer container) {
        if (container == null) return null;
        if (container instanceof JetFile) return PackageClassUtils.getPackageClassFqName(((JetFile) container).getPackageFqName());
        if (container instanceof JetClassOrObject) {
            JetClassOrObject classOrObject = (JetClassOrObject) container;
            if (classOrObject instanceof JetObjectDeclaration && ((JetObjectDeclaration) classOrObject).isDefault()) {
                classOrObject = PsiTreeUtil.getParentOfType(classOrObject, JetClass.class);
            }
            return classOrObject != null ? classOrObject.getFqName() : null;
        }
        throw new IllegalArgumentException("Invalid entry-point container: " + ((PsiElement) container).getText());
    }

    @Nullable
    private static JetDeclarationContainer getEntryPointContainer(@NotNull Location location) {
        if (DumbService.getInstance(location.getProject()).isDumb()) return null;

        Module module = location.getModule();
        if (module == null) return null;

        if (ProjectStructureUtil.isJsKotlinModule(module)) return null;

        PsiElement locationElement = location.getPsiElement();

        PsiFile psiFile = locationElement.getContainingFile();
        if (!(psiFile instanceof JetFile && ProjectRootsUtil.isInProjectOrLibSource(psiFile))) return null;

        JetFile jetFile = (JetFile) psiFile;
        final ResolutionFacade resolutionFacade = ResolvePackage.getResolutionFacade(jetFile);
        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(
                new NotNullFunction<JetNamedFunction, FunctionDescriptor>() {
                    @NotNull
                    @Override
                    public FunctionDescriptor fun(JetNamedFunction function) {
                        return (FunctionDescriptor) resolutionFacade.resolveToDescriptor(function);
                    }
                });

        for (JetDeclarationContainer currentElement = PsiTreeUtil.getNonStrictParentOfType(locationElement, JetClassOrObject.class,
                                                                                           JetFile.class);
             currentElement != null;
             currentElement = PsiTreeUtil.getParentOfType((PsiElement) currentElement, JetClassOrObject.class, JetFile.class)) {
            JetDeclarationContainer entryPointContainer = currentElement;
            if (entryPointContainer instanceof JetClass) {
                entryPointContainer = ((JetClass) currentElement).getDefaultObject();
            }
            if (entryPointContainer != null && mainFunctionDetector.hasMain(entryPointContainer.getDeclarations())) return entryPointContainer;
        }

        return null;
    }

    @NotNull
    private RunnerAndConfigurationSettings createConfigurationByQName(
            @NotNull Module module,
            ConfigurationContext context,
            @NotNull FqName fqName
    ) {
        RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(module.getProject(), context);
        JetRunConfiguration configuration = (JetRunConfiguration) settings.getConfiguration();
        configuration.setModule(module);
        configuration.setName(StringUtil.trimEnd(fqName.asString(), "." + PackageClassUtils.getPackageClassName(fqName)));
        configuration.setRunClass(fqName.asString());
        return settings;
    }

    @Override
    protected RunnerAndConfigurationSettings findExistingByElement(
            Location location,
            @NotNull List<RunnerAndConfigurationSettings> existingConfigurations,
            ConfigurationContext context
    ) {
        FqName startClassFQName = getStartClassFqName(getEntryPointContainer(location));
        if (startClassFQName == null) return null;

        for (RunnerAndConfigurationSettings existingConfiguration : existingConfigurations) {
            if (existingConfiguration.getType() instanceof JetRunConfigurationType) {
                JetRunConfiguration jetConfiguration = (JetRunConfiguration)existingConfiguration.getConfiguration();
                if (Comparing.equal(jetConfiguration.getRunClass(), startClassFQName.asString())) {
                    if (Comparing.equal(location.getModule(), jetConfiguration.getConfigurationModule().getModule())) {
                        return existingConfiguration;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public int compareTo(Object o) {
        return PREFERED;
    }
}
