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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Evgeny Gerashchenko
 * @since 6/1/12
 */
public class KotlinSignatureAnnotation extends PsiAnnotationWrapper {
    public static final KotlinSignatureAnnotation NULL_ANNOTATION = new KotlinSignatureAnnotation(null);
    private String signature;

    public KotlinSignatureAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @Override
    protected void initialize() {
        signature = StringUtil.unescapeStringCharacters(getStringAttribute(JvmStdlibNames.KOTLIN_SIGNATURE_VALUE_METHOD, ""));
    }

    public String signature() {
        checkInitialized();
        return signature;
    }

    @NotNull
    public static KotlinSignatureAnnotation get(PsiMethod psiMethod) {
        final PsiAnnotation annotation =
                JavaDescriptorResolver.findAnnotation(psiMethod, JvmStdlibNames.KOTLIN_SIGNATURE.getFqName().getFqName());
        return annotation != null ? new KotlinSignatureAnnotation(annotation) : NULL_ANNOTATION;
    }
}
