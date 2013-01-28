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

package org.jetbrains.jet.lang.resolve.java.kt;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiLiteralExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiAnnotationUtils {

    private PsiAnnotationUtils() {
    }

    @NotNull
    public static String getStringAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, @NotNull String defaultValue) {
        return getAttribute(annotation, field, defaultValue);
    }

    public static boolean getBooleanAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, boolean defaultValue) {
        return getAttribute(annotation, field, defaultValue);
    }

    public static int getIntAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, int defaultValue) {
        return getAttribute(annotation, field, defaultValue);
    }

    @NotNull
    private static <T> T getAttribute(@Nullable PsiAnnotation annotation, @NotNull String field, @NotNull T defaultValue) {
        if (annotation == null) {
            return defaultValue;
        }
        else {
            PsiAnnotationMemberValue value = annotation.findAttributeValue(field);
            if (value instanceof PsiLiteralExpression) {
                PsiLiteralExpression attributeValue = (PsiLiteralExpression) value;
                return (T) attributeValue.getValue();
            }
            else {
                return defaultValue;
            }
        }
    }

}
