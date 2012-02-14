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

import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetMainDetector;

/**
 * @author yole
 */
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
        final Module module = location.getModule();
        if (module == null) {
            return null;
        }

        PsiFile psiFile = location.getPsiElement().getContainingFile();
        if (psiFile instanceof JetFile) {
            JetFile jetFile = (JetFile) psiFile;
            if (JetMainDetector.hasMain(jetFile.getDeclarations())) {
                mySourceElement = jetFile;
                String fqName = JetPsiUtil.getFQName(jetFile);
                String className = fqName.length() == 0 ? JvmAbi.PACKAGE_CLASS : fqName + "." + JvmAbi.PACKAGE_CLASS;
                return createConfigurationByQName(module, configurationContext, className);
            }
        }
        return null;
    }

    private RunnerAndConfigurationSettings createConfigurationByQName(
            @NotNull Module module,
            ConfigurationContext context,
            @NotNull String fqName
    ) {
        RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(module.getProject(), context);
        JetRunConfiguration configuration = (JetRunConfiguration) settings.getConfiguration();
        configuration.setModule(module);
        configuration.setName(StringUtil.trimEnd(fqName, "." + JvmAbi.PACKAGE_CLASS));
        configuration.MAIN_CLASS_NAME = fqName;
        return settings;
    }

    @Override
    public int compareTo(Object o) {
        return PREFERED;
    }
}
