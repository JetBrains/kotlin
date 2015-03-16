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

package org.jetbrains.kotlin.idea.search.ideaExtensions;

import com.intellij.codeInsight.navigation.MethodImplementationsSearch;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import com.intellij.util.QueryExecutor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.KotlinLightMethod;
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.psi.*;

public class KotlinDefinitionsSearcher implements QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
    @Override
    public boolean execute(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters, @NotNull Processor<PsiElement> consumer) {
        PsiElement element = queryParameters.getElement();
        SearchScope scope = queryParameters.getScope();

        if (element instanceof JetClass) {
            return processClassImplementations((JetClass) element, consumer);
        }

        if (element instanceof JetNamedFunction || element instanceof JetSecondaryConstructor) {
            return processFunctionImplementations((JetFunction) element, scope, consumer);
        }

        if (element instanceof JetProperty) {
            return processPropertyImplementations((JetProperty) element, scope, consumer);
        }

        if (element instanceof JetParameter) {
            JetParameter parameter = (JetParameter) element;
            if (JetPsiUtil.getClassIfParameterIsProperty(parameter) != null) {
                return processPropertyImplementations((JetParameter) element, scope, consumer);
            }
        }

        return true;
     }

    private static boolean processClassImplementations(final JetClass klass, Processor<PsiElement> consumer) {
        PsiClass psiClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
            @Override
            public PsiClass compute() {
                return LightClassUtil.getPsiClass(klass);
            }
        });
        if (psiClass != null) {
            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, true), consumer);
        }
        return true;
    }

    private static boolean processFunctionImplementations(final JetFunction function, SearchScope scope, Processor<PsiElement> consumer) {
        PsiMethod psiMethod = ApplicationManager.getApplication().runReadAction(new Computable<PsiMethod>() {
            @Override
            public PsiMethod compute() {
                return LightClassUtil.getLightClassMethod(function);
            }
        });

        if (psiMethod != null) {
            ContainerUtil.process(MethodImplementationsSearch.getMethodImplementations(psiMethod, scope), consumer);
        }
        return true;
    }

    private static boolean processPropertyImplementations(@NotNull final JetParameter parameter, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        LightClassUtil.PropertyAccessorsPsiMethods accessorsPsiMethods = ApplicationManager.getApplication().runReadAction(
                new Computable<LightClassUtil.PropertyAccessorsPsiMethods>() {
                    @Override
                    public LightClassUtil.PropertyAccessorsPsiMethods compute() {
                        return LightClassUtil.getLightClassPropertyMethods(parameter);
                    }
                });

        return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer);
    }

    private static boolean processPropertyImplementations(@NotNull final JetProperty property, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        LightClassUtil.PropertyAccessorsPsiMethods accessorsPsiMethods = ApplicationManager.getApplication().runReadAction(
                new Computable<LightClassUtil.PropertyAccessorsPsiMethods>() {
                    @Override
                    public LightClassUtil.PropertyAccessorsPsiMethods compute() {
                        return LightClassUtil.getLightClassPropertyMethods(property);
                    }
                });

        return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer);
    }

    public static boolean processPropertyImplementationsMethods(LightClassUtil.PropertyAccessorsPsiMethods accessors, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        for (PsiMethod method : accessors) {
            PsiMethod[] implementations = MethodImplementationsSearch.getMethodImplementations(method, scope);
            for (PsiMethod implementation : implementations) {
                PsiElement mirrorElement = implementation instanceof KotlinLightMethod
                                           ? ((KotlinLightMethod) implementation).getOrigin() : null;
                if (mirrorElement instanceof JetProperty || mirrorElement instanceof JetParameter) {
                    if (!consumer.process(mirrorElement)) {
                        return false;
                    }
                }
                else if (mirrorElement instanceof JetPropertyAccessor && mirrorElement.getParent() instanceof JetProperty) {
                    if (!consumer.process(mirrorElement.getParent())) {
                        return false;
                    }
                }
                else {
                    if (!consumer.process(implementation)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
