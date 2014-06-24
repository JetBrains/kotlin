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
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.analyzer.AnalyzerFacade;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.MainFunctionDetector;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeProvider;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;

import java.util.Collections;
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
        Module module = location.getModule();
        if (module == null) {
            return null;
        }

        if (ProjectStructureUtil.isJsKotlinModule(module)) {
            return null;
        }

        JetFile file = getStartClassFile(location);
        if (file == null || !JetPluginUtil.isInSource(file, true)) {
            return null;
        }

        mySourceElement = file;

        FqName startClassFQName = PackageClassUtils.getPackageClassFqName(file.getPackageFqName());

        return createConfigurationByQName(module, configurationContext, startClassFQName);
    }

    @Nullable
    private static JetFile getStartClassFile(@NotNull Location location) {
        PsiFile psiFile = location.getPsiElement().getContainingFile();
        if (psiFile instanceof JetFile) {
            JetFile jetFile = (JetFile) psiFile;
            AnalyzerFacade facade = AnalyzerFacadeProvider.getAnalyzerFacadeForFile(jetFile);
            ResolveSession resolveSession =
                    facade.createSetup(jetFile.getProject(), Collections.<JetFile>emptyList(), GlobalSearchScope.fileScope(jetFile))
                            .getLazyResolveSession();
            MainFunctionDetector mainFunctionDetector = new MainFunctionDetector(resolveSession);
            if (mainFunctionDetector.hasMain(jetFile.getDeclarations())) {
                return jetFile;
            }
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
        JetFile file = getStartClassFile(location);
        if (file == null) {
            return null;
        }

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
