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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.JetLightClass;
import org.jetbrains.jet.lang.psi.JetClass;

/**
 * @author yole
 */
public class KotlinDefinitionsSearcher extends QueryExecutorBase<PsiElement, PsiElement> {
    @Override
    public void processQuery(@NotNull PsiElement queryParameters, @NotNull final Processor<PsiElement> consumer) {
        if (queryParameters instanceof JetClass) {
            final JetLightClass psiClass = JetLightClass.wrapDelegate((JetClass) queryParameters);
            final Query<PsiClass> query = ClassInheritorsSearch.search(psiClass, true);
            query.forEach(new Processor<PsiClass>() {
              @Override
              public boolean process(final PsiClass pyClass) {
                return consumer.process(pyClass);
              }
            });
        }
    }
}
