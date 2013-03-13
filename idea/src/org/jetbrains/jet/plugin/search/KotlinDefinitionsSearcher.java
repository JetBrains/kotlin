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
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetClass;

public class KotlinDefinitionsSearcher extends QueryExecutorBase<PsiElement, PsiElement> {
    @Override
    public void processQuery(@NotNull final PsiElement queryParameters, @NotNull final Processor<PsiElement> consumer) {
        if (queryParameters instanceof JetClass) {
            PsiClass psiClass = ApplicationManager.getApplication().runReadAction(new Computable<PsiClass>() {
                @Override
                public PsiClass compute() {
                    return LightClassUtil.createLightClass((JetClass) queryParameters);
                }
            });
            if (psiClass != null) {
                Query<PsiClass> query = ClassInheritorsSearch.search(psiClass, true);
                query.forEach(new Processor<PsiClass>() {
                    @Override
                    public boolean process(PsiClass clazz) {
                        return consumer.process(clazz);
                    }
                });
            }
        }
    }
}
