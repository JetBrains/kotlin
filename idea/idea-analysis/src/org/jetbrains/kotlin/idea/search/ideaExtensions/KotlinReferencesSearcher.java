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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.asJava.*;
import org.jetbrains.kotlin.idea.search.usagesSearch.*;
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil;
import org.jetbrains.kotlin.psi.*;

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
                searchNamedElement(queryParameters, lightClass, className);

                if (element instanceof JetObjectDeclaration && ((JetObjectDeclaration) element).isDefault()) {
                    PsiField fieldForDefaultObject = ApplicationManager.getApplication().runReadAction(new Computable<PsiField>() {
                        @Override
                        public PsiField compute() {
                            return LightClassUtil.getLightFieldForDefaultObject(element);
                        }
                    });
                    if (fieldForDefaultObject != null) {
                        searchNamedElement(queryParameters, fieldForDefaultObject);
                    }
                }
            }
        }
    }

    @Override
    public void processQuery(
            @NotNull final ReferencesSearch.SearchParameters queryParameters,
            @NotNull final Processor<PsiReference> consumer
    ) {
        PsiElement element = queryParameters.getElementToSearch();

        final PsiNamedElement unwrappedElement = AsJavaPackage.getNamedUnwrappedElement(element);
        if (unwrappedElement == null || !ProjectRootsUtil.isInProjectOrLibSource(unwrappedElement)) return;

        ApplicationManager.getApplication().runReadAction(
                new Runnable() {
                    @Override
                    public void run() {
                        KotlinPsiSearchHelper searchHelper = new KotlinPsiSearchHelper(queryParameters.getElementToSearch().getProject());
                        UsagesSearchTarget<PsiNamedElement> searchTarget = new UsagesSearchTarget<PsiNamedElement>(
                                unwrappedElement,
                                queryParameters.getEffectiveSearchScope(),
                                UsagesSearchLocation.EVERYWHERE,
                                false
                        );
                        UsagesSearchRequestItem requestItem = new UsagesSearchRequestItem(
                                searchTarget,
                                UsagesSearchPackage.getSpecialNamesToSearch(unwrappedElement),
                                UsagesSearchPackage.getIsTargetUsage()
                        );
                        searchHelper.processFilesWithText(requestItem, consumer);
                    }
                }
        );

        searchLightElements(queryParameters, element);
    }

    private static void searchLightElements(ReferencesSearch.SearchParameters queryParameters, PsiElement element) {
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
                || (declaration instanceof JetParameter && ((JetParameter) declaration).hasValOrVarNode())) {
                searchNamedElement(queryParameters, (PsiNamedElement) declaration);
            }
            else if (declaration instanceof JetPropertyAccessor) {
                searchNamedElement(queryParameters, PsiTreeUtil.getParentOfType(declaration, JetProperty.class));
            }
        }
    }

    private static void searchNamedElement(ReferencesSearch.SearchParameters queryParameters, PsiNamedElement element) {
        searchNamedElement(queryParameters, element, element != null ? element.getName() : null);
    }

    private static void searchNamedElement(ReferencesSearch.SearchParameters queryParameters, PsiNamedElement element, @Nullable String name) {
        if (name != null) {
            queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, element);
        }
    }
}
