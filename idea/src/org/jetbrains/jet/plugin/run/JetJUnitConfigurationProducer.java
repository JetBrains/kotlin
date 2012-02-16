/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * @author max
 */
package org.jetbrains.jet.plugin.run;

import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;

public class JetJUnitConfigurationProducer extends RuntimeConfigurationProducer {
    private JetElement myElement;

    public JetJUnitConfigurationProducer() {
        super(JUnitConfigurationType.getInstance());
    }

    @Override
    public PsiElement getSourceElement() {
        return myElement;
    }

    @Override
    protected RunnerAndConfigurationSettings createConfigurationByElement(Location location, ConfigurationContext context) {

        PsiElement leaf = location.getPsiElement();
        JetClass jetClass = PsiTreeUtil.getParentOfType(leaf, JetClass.class);
        if (jetClass != null) {
            myElement = jetClass;
            PsiClass delegate = new JetLightClass(jetClass.getManager(), (JetFile) jetClass.getContainingFile(), JetPsiUtil.getFQName(jetClass));

            RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(jetClass.getProject(), context);
            final JUnitConfiguration configuration = (JUnitConfiguration)settings.getConfiguration();

            final Module originalModule = configuration.getConfigurationModule().getModule();
            configuration.beClassConfiguration(delegate);
            configuration.restoreOriginalModule(originalModule);
            JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);
            return settings;

        }

        return null;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
