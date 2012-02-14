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

package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Stepan Koltsov
 */
public abstract class PsiAnnotationWrapper {

    @Nullable
    private PsiAnnotation psiAnnotation;

    protected PsiAnnotationWrapper(@Nullable PsiAnnotation psiAnnotation) {
        this.psiAnnotation = psiAnnotation;
    }

    @Nullable
    public PsiAnnotation getPsiAnnotation() {
        return psiAnnotation;
    }

    public boolean isDefined() {
        return psiAnnotation != null;
    }
    
    @NotNull
    protected String getStringAttribute(String name, String defaultValue) {
        return PsiAnnotationUtils.getStringAttribute(psiAnnotation, name, defaultValue);
    }
    
    protected boolean getBooleanAttribute(String name, boolean defaultValue) {
        return PsiAnnotationUtils.getBooleanAttribute(psiAnnotation, name, defaultValue);
    }

    protected int getIntAttribute(String name, int defaultValue) {
        return PsiAnnotationUtils.getIntAttribute(psiAnnotation, name, defaultValue);
    }

}
