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

import com.intellij.execution.*;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.InheritorChooser;
import com.intellij.execution.junit2.info.MethodLocation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.theoryinpractice.testng.configuration.TestNGConfiguration;
import com.theoryinpractice.testng.configuration.TestNGConfigurationProducer;
import com.theoryinpractice.testng.util.TestNGUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.platform.jvm.JvmPlatformKt;

import java.util.List;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

public class KotlinTestNgConfigurationProducer extends TestNGConfigurationProducer {
    @Override
    public boolean shouldReplace(ConfigurationFromContext self, ConfigurationFromContext other) {
        return other.isProducedBy(TestNGConfigurationProducer.class);
    }

    @Override
    public boolean isConfigurationFromContext(TestNGConfiguration configuration, ConfigurationContext context) {
        if (isMultipleElementsSelected(context)) {
            return false;
        }
        final RunConfiguration predefinedConfiguration = context.getOriginalConfiguration(getConfigurationType());
        final Location contextLocation = context.getLocation();
        if (contextLocation == null) {
            return false;
        }
        Location location = JavaExecutionUtil.stepIntoSingleClass(contextLocation);
        if (location == null) {
            return false;
        }
        final PsiElement element = location.getPsiElement();

        RunnerAndConfigurationSettings template =
                RunManager.getInstance(location.getProject()).getConfigurationTemplate(getConfigurationFactory());
        final Module predefinedModule = ((TestNGConfiguration) template.getConfiguration()).getConfigurationModule().getModule();
        final String vmParameters =
                predefinedConfiguration instanceof CommonJavaRunConfigurationParameters
                ? ((CommonJavaRunConfigurationParameters) predefinedConfiguration).getVMParameters()
                : null;
        if (vmParameters != null && !Comparing.strEqual(vmParameters, configuration.getVMParameters())) return false;
        if (differentParamSet(configuration, contextLocation)) return false;

        KtNamedDeclaration declarationToRun = getDeclarationToRun(element);
        if (declarationToRun == null) return false;
        PsiNamedElement lightElement = CollectionsKt.firstOrNull(LightClassUtilsKt.toLightElements(declarationToRun));
        if (lightElement != null && configuration.isConfiguredByElement(lightElement)) {
            final Module configurationModule = configuration.getConfigurationModule().getModule();
            if (Comparing.equal(location.getModule(), configurationModule)) return true;
            if (Comparing.equal(predefinedModule, configurationModule)) return true;
        }

        return false;
    }

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

        if (!ProjectRootsUtil.isInProjectOrLibSource(leaf, false)) {
            return false;
        }

        if (!(leaf.getContainingFile() instanceof KtFile)) {
            return false;
        }

        KtFile ktFile = (KtFile) leaf.getContainingFile();

        if (!JvmPlatformKt.isJvm(TargetPlatformDetector.getPlatform(ktFile))) {
            return false;
        }

        Pair<PsiClass, PsiMethod> classAndMethod = getTestClassAndMethod(leaf);
        if (classAndMethod == null) return false;

        PsiClass testClass = classAndMethod.getFirst();
        if (testClass == null) return false;
        PsiMethod testMethod = classAndMethod.getSecond();

        return configure(configuration, location, context, project, testClass, testMethod);
    }

    @Override
    public void onFirstRun(ConfigurationFromContext configuration, ConfigurationContext context, Runnable startRunnable) {
        KtNamedDeclaration declarationToRun = getDeclarationToRun(configuration.getSourceElement());
        final PsiNamedElement lightElement = CollectionsKt.firstOrNull(LightClassUtilsKt.toLightElements(declarationToRun));

        // Copied from TestNGInClassConfigurationProducer.onFirstRun()
        if (lightElement instanceof PsiMethod || lightElement instanceof PsiClass) {
            PsiMethod psiMethod;
            PsiClass containingClass;

            if (lightElement instanceof PsiMethod) {
                psiMethod = (PsiMethod)lightElement;
                containingClass = psiMethod.getContainingClass();
            } else {
                psiMethod = null;
                containingClass = (PsiClass)lightElement;
            }

            InheritorChooser inheritorChooser = new InheritorChooser() {
                @Override
                protected void runForClasses(List<PsiClass> classes, PsiMethod method, ConfigurationContext context, Runnable performRunnable) {
                    ((TestNGConfiguration)context.getConfiguration().getConfiguration()).bePatternConfiguration(classes, method);
                    super.runForClasses(classes, method, context, performRunnable);
                }

                @Override
                protected void runForClass(PsiClass aClass,
                        PsiMethod psiMethod,
                        ConfigurationContext context,
                        Runnable performRunnable) {
                    if (lightElement instanceof PsiMethod) {
                        Project project = psiMethod.getProject();
                        MethodLocation methodLocation = new MethodLocation(project, psiMethod, PsiLocation.fromPsiElement(aClass));
                        ((TestNGConfiguration)context.getConfiguration().getConfiguration()).setMethodConfiguration(methodLocation);
                    } else {
                        ((TestNGConfiguration)context.getConfiguration().getConfiguration()).setClassConfiguration(aClass);
                    }
                    super.runForClass(aClass, psiMethod, context, performRunnable);
                }
            };
            if (inheritorChooser.runMethodInAbstractClass(context,
                                                          startRunnable,
                                                          psiMethod,
                                                          containingClass,
                                                          new Condition<PsiClass>() {
                                                              @Override
                                                              public boolean value(PsiClass aClass) {
                                                                  return aClass.hasModifierProperty(PsiModifier.ABSTRACT) &&
                                                                         TestNGUtil.hasTest(aClass);
                                                              }
                                                          })) return;
        }

        super.onFirstRun(configuration, context, startRunnable);
    }

    @Nullable
    private static KtNamedDeclaration getDeclarationToRun(@NotNull PsiElement leaf) {
        if (!(leaf.getContainingFile() instanceof KtFile)) return null;
        KtFile jetFile = (KtFile) leaf.getContainingFile();

        KtNamedFunction function = PsiTreeUtil.getParentOfType(leaf, KtNamedFunction.class, false);
        if (function != null) return function;

        KtClass ktClass = PsiTreeUtil.getParentOfType(leaf, KtClass.class, false);
        if (ktClass != null) return ktClass;

        return getClassDeclarationInFile(jetFile);
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

    @Nullable
    static KtClass getClassDeclarationInFile(KtFile jetFile) {
        KtClass tempSingleDeclaration = null;

        for (KtDeclaration ktDeclaration : jetFile.getDeclarations()) {
            if (ktDeclaration instanceof KtClass) {
                KtClass declaration = (KtClass) ktDeclaration;

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

    @Nullable
    public static Pair<PsiClass, PsiMethod> getTestClassAndMethod(@NotNull PsiElement leaf) {
        KtNamedDeclaration declarationToRun = getDeclarationToRun(leaf);

        if (declarationToRun instanceof KtNamedFunction) {
            KtNamedFunction function = (KtNamedFunction) declarationToRun;

            @SuppressWarnings("unchecked")
            KtElement owner = PsiTreeUtil.getParentOfType(function, KtFunction.class, KtClass.class);

            if (owner instanceof KtClass) {
                PsiClass delegate = toLightClass((KtClass) owner);
                if (delegate != null) {
                    for (PsiMethod method : delegate.getMethods()) {
                        if (method.getNavigationElement() == function) {
                            if (TestNGUtil.hasTest(method)) {
                                return new Pair<>(delegate, method);
                            }
                            break;
                        }
                    }
                }
            }
        }

        if (declarationToRun instanceof KtClass) {
            PsiClass delegate = toLightClass((KtClassOrObject) declarationToRun);
            if (isTestNGClass(delegate)) {
                return new Pair<>(delegate, null);
            }
        }

        return null;
    }
}
