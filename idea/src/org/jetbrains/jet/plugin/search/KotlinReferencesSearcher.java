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

package org.jetbrains.jet.plugin.search;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

public class KotlinReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<PsiReference> consumer) {
        final PsiElement element = queryParameters.getElementToSearch();
        if (element instanceof JetClass) {
            String className = ((JetClass) element).getName();
            if (className != null) {
                PsiClass lightClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
                    @Override
                    public PsiClass compute() {
                        return LightClassUtil.createLightClass((JetClass) element);
                    }
                });
                if (lightClass != null) {
                    queryParameters.getOptimizer().searchWord(className, queryParameters.getEffectiveSearchScope(), true, lightClass);
                }
            }
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
                if (method != null) {
                    queryParameters.getOptimizer().searchWord(name, queryParameters.getEffectiveSearchScope(), true, method);
                }
            }
        }
    }
}
