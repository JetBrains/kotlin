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

import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.Location;
import com.intellij.execution.PsiLocation;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.junit.JUnitConfiguration;
import com.intellij.execution.junit.JUnitConfigurationType;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.execution.junit.RuntimeConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.ProjectRootsUtil;

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

        if (!ProjectRootsUtil.isInSource(leaf, true)) {
            return null;
        }

        if (!(leaf.getContainingFile() instanceof JetFile)) {
            return null;
        }

        JetFile jetFile = (JetFile) leaf.getContainingFile();

        JetNamedFunction function = PsiTreeUtil.getParentOfType(leaf, JetNamedFunction.class, false);
        if (function != null) {
            myElement = function;

            @SuppressWarnings("unchecked")
            JetElement owner = PsiTreeUtil.getParentOfType(function, JetFunction.class, JetClass.class);

            if (owner instanceof JetClass) {
                PsiClass delegate = LightClassUtil.getPsiClass((JetClass) owner);
                if (delegate != null) {
                    for (PsiMethod method : delegate.getMethods()) {
                        if (method.getNavigationElement() == function) {
                            Location<PsiMethod> methodLocation = PsiLocation.fromPsiElement(method);
                            if (JUnitUtil.isTestMethod(methodLocation, false)) {
                                RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(context.getProject(), context);
                                JUnitConfiguration configuration = (JUnitConfiguration) settings.getConfiguration();

                                Module originalModule = configuration.getConfigurationModule().getModule();
                                configuration.beMethodConfiguration(methodLocation);
                                configuration.restoreOriginalModule(originalModule);
                                JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);

                                return settings;
                            }
                            break;
                        }
                    }
                }
            }
        }

        JetClass jetClass = PsiTreeUtil.getParentOfType(leaf, JetClass.class, false);

        if (jetClass == null) {
            jetClass = getClassDeclarationInFile(jetFile);
        }

        if (jetClass != null) {
            myElement = jetClass;
            PsiClass delegate = LightClassUtil.getPsiClass(jetClass);

            if (delegate != null && JUnitUtil.isTestClass(delegate)) {
                RunnerAndConfigurationSettings settings = cloneTemplateConfiguration(context.getProject(), context);
                JUnitConfiguration configuration = (JUnitConfiguration) settings.getConfiguration();

                Module originalModule = configuration.getConfigurationModule().getModule();
                configuration.beClassConfiguration(delegate);
                configuration.restoreOriginalModule(originalModule);
                JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);

                return settings;
            }
        }

        return null;
    }

    @Nullable
    static JetClass getClassDeclarationInFile(JetFile jetFile) {
        JetClass tempSingleDeclaration = null;

        for (JetDeclaration jetDeclaration : jetFile.getDeclarations()) {
            if (jetDeclaration instanceof JetClass) {
                JetClass declaration = (JetClass) jetDeclaration;

                if (tempSingleDeclaration == null) {
                    tempSingleDeclaration = declaration;
                }
                else {
                    // There are several class declarations in file
                    return null;
                }
            }
        }

        return tempSingleDeclaration;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
