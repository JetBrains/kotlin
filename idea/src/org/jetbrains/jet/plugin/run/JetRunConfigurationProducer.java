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

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.NotNullFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.MainFunctionDetector;
import org.jetbrains.jet.plugin.util.ProjectRootsUtil;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.project.ResolveSessionForBodies;

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
        JetFile file = getStartClassFile(location);
        if (file == null) return null;

        mySourceElement = file;

        FqName startClassFQName = PackageClassUtils.getPackageClassFqName(file.getPackageFqName());

        Module module = location.getModule();
        assert module != null;

        return createConfigurationByQName(module, configurationContext, startClassFQName);
    }

    @Nullable
    private static JetFile getStartClassFile(@NotNull Location location) {
        Module module = location.getModule();
        if (module == null) return null;

        if (ProjectStructureUtil.isJsKotlinModule(module)) return null;

        PsiFile psiFile = location.getPsiElement().getContainingFile();
        if (!(psiFile instanceof JetFile && ProjectRootsUtil.isInProjectOrLibSource(psiFile))) return null;

        JetFile jetFile = (JetFile) psiFile;
        final ResolveSessionForBodies session = ResolvePackage.getLazyResolveSession(jetFile);
        MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(
                new NotNullFunction<JetNamedFunction, FunctionDescriptor>() {
                    @NotNull
                    @Override
                    public FunctionDescriptor fun(JetNamedFunction function) {
                        return (FunctionDescriptor) session.resolveToDescriptor(function);
                    }
                });

        return mainFunctionDetector.hasMain(jetFile.getDeclarations()) ? jetFile : null;
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
        JetFile file = getStartClassFile(location);
        if (file == null) return null;

        FqName startClassFQName = PackageClassUtils.getPackageClassFqName(file.getPackageFqName());

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
