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

package org.jetbrains.jet.plugin.search.ideaExtensions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.AsJavaPackage;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod;
import org.jetbrains.jet.plugin.JetPluginUtil;

public class KotlinReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    public static void processJetClassOrObject(
            final @NotNull JetClassOrObject element, @NotNull ReferencesSearch.SearchParameters queryParameters
    ) {
        String className = element.getName();
        if (className != null) {
            PsiClass lightClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
                @Override
                public PsiClass compute() {
                    return LightClassUtil.getPsiClass(element);
                }
            });
            if (lightClass != null) {
                queryParameters.getOptimizer().searchWord(className, queryParameters.getEffectiveSearchScope(), true, lightClass);
            }
        }
    }

    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<PsiReference> consumer) {
        PsiElement element = queryParameters.getElementToSearch();

        PsiElement unwrappedElement = AsJavaPackage.getUnwrapped(element);
        if (unwrappedElement == null
            || !JetPluginUtil.isInSource(unwrappedElement)
            || JetPluginUtil.isKtFileInGradleProjectInWrongFolder(unwrappedElement)) return;

        if (element instanceof JetClassOrObject) {
            processJetClassOrObject((JetClassOrObject) element, queryParameters);
        }
        else if (element instanceof JetNamedFunction) {
            final JetNamedFunction function = (JetNamedFunction) element;
            String name = function.getName();
            if (name != null) {
                PsiMethod method = ApplicationManager.getApplication().runReadAction(new Computable<PsiMethod>() {
                    @Override
                    public PsiMethod compute() {
                        return LightClassUtil.getLightClassMethod(function);
                    }
                });
                searchNamedElement(queryParameters, method);
            }
        }
        else if (element instanceof JetProperty) {
            final JetProperty property = (JetProperty) element;
            LightClassUtil.PropertyAccessorsPsiMethods propertyMethods =
                    ApplicationManager.getApplication().runReadAction(new Computable<LightClassUtil.PropertyAccessorsPsiMethods>() {
                        @Override
                        public LightClassUtil.PropertyAccessorsPsiMethods compute() {
                            return LightClassUtil.getLightClassPropertyMethods(property);
                        }
                    });

            searchNamedElement(queryParameters, propertyMethods.getGetter());
            searchNamedElement(queryParameters, propertyMethods.getSetter());
        }
        else if (element instanceof KotlinLightMethod) {
            JetDeclaration declaration = ((KotlinLightMethod) element).getOrigin();
            if (declaration instanceof JetProperty
                || (declaration instanceof JetParameter && ((JetParameter) declaration).getValOrVarNode() != null)) {
                searchNamedElement(queryParameters, (PsiNamedElement) declaration);
            }
            else if (declaration instanceof JetPropertyAccessor) {
                searchNamedElement(queryParameters, PsiTreeUtil.getParentOfType(declaration, JetProperty.class));
            }
        }
    }

    private static void searchNamedElement(ReferencesSearch.SearchParameters queryParameters, PsiNamedElement element) {
        String name = element != null ? element.getName() : null;
        if (name != null) {
            queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, element);
        }
    }
}
