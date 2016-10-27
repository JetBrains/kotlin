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
import org.jetbrains.kotlin.asJava.LightClassUtil;
import org.jetbrains.kotlin.asJava.elements.KtLightMethod;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.asJava.LightClassUtilsKt.toLightClass;

public class KotlinDefinitionsSearcher implements QueryExecutor<PsiElement, DefinitionsScopedSearch.SearchParameters> {
    @Override
    public boolean execute(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters, @NotNull Processor<PsiElement> consumer) {
        PsiElement element = queryParameters.getElement();
        SearchScope scope = queryParameters.getScope();
        consumer = skipDelegatedMethodsConsumer(consumer);

        if (element instanceof KtClass) {
            return processClassImplementations((KtClass) element, consumer);
        }

        if (element instanceof KtNamedFunction || element instanceof KtSecondaryConstructor) {
            return processFunctionImplementations((KtFunction) element, scope, consumer);
        }

        if (element instanceof KtProperty) {
            return processPropertyImplementations((KtProperty) element, scope, consumer);
        }

        if (element instanceof KtParameter) {
            KtParameter parameter = (KtParameter) element;

            if (isFieldParameter(parameter)) {
                return processPropertyImplementations((KtParameter) element, scope, consumer);
            }
        }

        return true;
     }

    @NotNull
    private static Processor<PsiElement> skipDelegatedMethodsConsumer(@NotNull final Processor<PsiElement> baseConsumer) {
        return new Processor<PsiElement>() {
            @Override
            public boolean process(PsiElement element) {
                if (isDelegated(element)) {
                    return true;
                }
                return baseConsumer.process(element);
            }
        };
    }

    private static boolean isDelegated(@NotNull PsiElement element) {
        return element instanceof KtLightMethod && ((KtLightMethod) element).isDelegated();
    }

    private static boolean isFieldParameter(final KtParameter parameter) {
        return ApplicationManager.getApplication().runReadAction(
                new Computable<Boolean>() {
                    @Override
                    public Boolean compute() {
                        return KtPsiUtil.getClassIfParameterIsProperty(parameter) != null;
                    }
                });
    }

    private static boolean processClassImplementations(@NotNull final KtClass klass, Processor<PsiElement> consumer) {
        PsiClass psiClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
            @Override
            public PsiClass compute() {
                return toLightClass(klass);
            }
        });
        if (psiClass != null) {
            return ContainerUtil.process(ClassInheritorsSearch.search(psiClass, true), consumer);
        }
        return true;
    }

    private static boolean processFunctionImplementations(final KtFunction function, SearchScope scope, Processor<PsiElement> consumer) {
        PsiMethod psiMethod = ApplicationManager.getApplication().runReadAction(new Computable<PsiMethod>() {
            @Override
            public PsiMethod compute() {
                return LightClassUtil.INSTANCE.getLightClassMethod(function);
            }
        });

        if (psiMethod != null) {
            MethodImplementationsSearch.processImplementations(psiMethod, consumer, scope);
        }

        return true;
    }

    private static boolean processPropertyImplementations(@NotNull final KtParameter parameter, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        LightClassUtil.PropertyAccessorsPsiMethods accessorsPsiMethods = ApplicationManager.getApplication().runReadAction(
                new Computable<LightClassUtil.PropertyAccessorsPsiMethods>() {
                    @Override
                    public LightClassUtil.PropertyAccessorsPsiMethods compute() {
                        return LightClassUtil.INSTANCE.getLightClassPropertyMethods(parameter);
                    }
                });

        return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer);
    }

    private static boolean processPropertyImplementations(@NotNull final KtProperty property, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        LightClassUtil.PropertyAccessorsPsiMethods accessorsPsiMethods = ApplicationManager.getApplication().runReadAction(
                new Computable<LightClassUtil.PropertyAccessorsPsiMethods>() {
                    @Override
                    public LightClassUtil.PropertyAccessorsPsiMethods compute() {
                        return LightClassUtil.INSTANCE.getLightClassPropertyMethods(property);
                    }
                });

        return processPropertyImplementationsMethods(accessorsPsiMethods, scope, consumer);
    }

    public static boolean processPropertyImplementationsMethods(LightClassUtil.PropertyAccessorsPsiMethods accessors, @NotNull SearchScope scope, @NotNull Processor<PsiElement> consumer) {
        for (PsiMethod method : accessors) {
            List<PsiMethod> implementations = new ArrayList<PsiMethod>();
            MethodImplementationsSearch.getOverridingMethods(method, implementations, scope);

            for (PsiMethod implementation : implementations) {
                if (isDelegated(implementation)) continue;

                PsiElement mirrorElement = implementation instanceof KtLightMethod
                                           ? ((KtLightMethod) implementation).getKotlinOrigin() : null;
                if (mirrorElement instanceof KtProperty || mirrorElement instanceof KtParameter) {
                    if (!consumer.process(mirrorElement)) {
                        return false;
                    }
                }
                else if (mirrorElement instanceof KtPropertyAccessor && mirrorElement.getParent() instanceof KtProperty) {
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
