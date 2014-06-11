/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.search.MethodTextOccurrenceProcessor;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

public class KotlinLightMethodTextOccurrenceProcessor extends MethodTextOccurrenceProcessor {
    public KotlinLightMethodTextOccurrenceProcessor(@NotNull PsiClass aClass, boolean strictSignatureSearch, PsiMethod... methods) {
        super(aClass, strictSignatureSearch, methods);
    }

    @Override
    protected boolean processInexactReference(PsiReference ref, PsiElement refElement, PsiMethod method, Processor<PsiReference> consumer) {
        if (refElement instanceof JetNamedFunction) {
            PsiMethod lightMethod = LightClassUtil.getLightClassMethod((JetNamedFunction) refElement);
            if (lightMethod != null) return super.processInexactReference(ref, lightMethod, method, consumer);
        }

        return true;
    }
}
