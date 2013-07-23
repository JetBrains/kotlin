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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;

public class KotlinSignatureAnnotation {
    private static final KotlinSignatureAnnotation NULL_ANNOTATION = new KotlinSignatureAnnotation(null);

    static {
        NULL_ANNOTATION.computeSignature();
    }

    @Nullable
    private final PsiAnnotation psiAnnotation;
    private String signature;

    private KotlinSignatureAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        this.psiAnnotation = psiAnnotation;
    }

    @NotNull
    public static KotlinSignatureAnnotation get(@NotNull PsiMember member) {
        PsiAnnotation annotation = JavaAnnotationResolver.
                findAnnotationWithExternal(member, JvmAnnotationNames.KOTLIN_SIGNATURE.getFqName().asString());
        return annotation != null ? new KotlinSignatureAnnotation(annotation) : NULL_ANNOTATION;
    }

    @NotNull
    private String computeSignature() {
        if (psiAnnotation != null) {
            PsiAnnotationMemberValue attribute = psiAnnotation.findAttributeValue(JvmAnnotationNames.KOTLIN_SIGNATURE_VALUE_FIELD_NAME);
            if (attribute instanceof PsiLiteralExpression) {
                Object value = ((PsiLiteralExpression) attribute).getValue();
                if (value instanceof String) {
                    return StringUtil.unescapeStringCharacters((String) value);
                }
            }
        }

        return "";
    }

    @NotNull
    public String signature() {
        if (signature == null) {
            signature = computeSignature();
        }
        return signature;
    }
}
