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
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationProducer;
import com.theoryinpractice.testng.util.TestNGUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.ProjectRootsUtil;

public class JetTestNgConfigurationProducer extends TestNGConfigurationProducer {
    @Override
    protected boolean setupConfigurationFromContext(
            TestNGConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement
    ) {
        // TODO: check TestNG Pattern running first, before method/class (see TestNGInClassConfigurationProducer for logic)
        // TODO: and PsiClassOwner not handled, which is in TestNGInClassConfigurationProducer

        Location location = context.getLocation();
        if (location == null) {
            return false;
        }

        Project project = context.getProject();
        PsiElement leaf = location.getPsiElement();

        if (!ProjectRootsUtil.isInSource(leaf, true)) {
            return false;
        }

        if (!(leaf.getContainingFile() instanceof JetFile)) {
            return false;
        }

        JetFile jetFile = (JetFile) leaf.getContainingFile();

        JetNamedFunction function = PsiTreeUtil.getParentOfType(leaf, JetNamedFunction.class, false);
        if (function != null) {
            @SuppressWarnings("unchecked")
            JetElement owner = PsiTreeUtil.getParentOfType(function, JetFunction.class, JetClass.class);

            if (owner instanceof JetClass) {
                PsiClass delegate = LightClassUtil.getPsiClass((JetClass) owner);
                if (delegate != null) {
                    for (PsiMethod method : delegate.getMethods()) {
                        if (method.getNavigationElement() == function) {
                            if (TestNGUtil.hasTest(method)) {
                                return configure(configuration, location, context, project, delegate, method);
                            }
                            break;
                        }
                    }
                }
            }
        }

        JetClass jetClass = PsiTreeUtil.getParentOfType(leaf, JetClass.class, false);

        if (jetClass == null) {
            jetClass = JetJUnitConfigurationProducer.getClassDeclarationInFile(jetFile);
        }

        if (jetClass == null) {
            return false;
        }

        PsiClass delegate = LightClassUtil.getPsiClass(jetClass);
        if (!isTestNGClass(delegate)) {
            return false;
        }

        return configure(configuration, location, context, project, delegate, null);
    }

    private boolean configure(
            TestNGConfiguration configuration, Location location, ConfigurationContext context, Project project,
            @Nullable PsiClass delegate, @Nullable PsiMethod method
    ) {
        if (delegate == null) {
            return false;
        }

        setupConfigurationModule(context, configuration);
        Module originalModule = configuration.getConfigurationModule().getModule();
        configuration.setClassConfiguration(delegate);
        if (method != null) {
            configuration.setMethodConfiguration(PsiLocation.fromPsiElement(project, method));
        }
        configuration.restoreOriginalModule(originalModule);
        configuration.setName(configuration.getName());
        JavaRunConfigurationExtensionManager.getInstance().extendCreatedConfiguration(configuration, location);
        return true;
    }

    private static boolean isTestNGClass(PsiClass psiClass) {
        return psiClass != null && PsiClassUtil.isRunnableClass(psiClass, true, false) && TestNGUtil.hasTest(psiClass);
    }
}
