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
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;

public class JetClassAnnotation extends PsiAnnotationWithAbiVersion {
    private static final JetClassAnnotation NULL_ANNOTATION = new JetClassAnnotation(null);
    static {
        NULL_ANNOTATION.checkInitialized();
    }

    private String signature;

    private JetClassAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @Override
    protected void initialize() {
        super.initialize();
        signature = getStringAttribute(JvmStdlibNames.JET_CLASS_SIGNATURE, "");
    }

    @NotNull
    public String signature() {
        checkInitialized();
        return signature;
    }

    public int kind() {
        return flags() & JvmStdlibNames.FLAG_CLASS_KIND_MASK;
    }

    @NotNull
    public static JetClassAnnotation get(PsiClass psiClass) {
        PsiAnnotation annotation = JavaAnnotationResolver.findOwnAnnotation(psiClass,
                                                                                  JvmStdlibNames.JET_CLASS.getFqName().getFqName());
        return annotation != null ? new JetClassAnnotation(annotation) : NULL_ANNOTATION;
    }
}
