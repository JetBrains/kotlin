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

package org.jetbrains.jet.plugin.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetClassBody;
import org.jetbrains.jet.lang.psi.JetFunction;

/**
 * @author yole
 */
public class KotlinReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<PsiReference> consumer) {
        PsiElement element = queryParameters.getElementToSearch();
        if (element instanceof JetClass) {
            String className = ((JetClass) element).getName();
            if (className != null) {
                queryParameters.getOptimizer().searchWord(className, queryParameters.getScope(),
                                                          true, JetLightClass.wrapDelegate((JetClass) element));
            }
        }
        else if (element instanceof JetFunction) {
            final JetFunction function = (JetFunction) element;
            final String name = function.getName();
            if (function.getParent() instanceof JetClassBody && name != null) {
                final PsiMethod method = JetLightClass.wrapMethod(function);
                if (method != null) {
                    queryParameters.getOptimizer().searchWord(name, queryParameters.getScope(),
                                                              true, JetLightClass.wrapMethod((JetFunction) element));
                }
            }

        }
    }
}
