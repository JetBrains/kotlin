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
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;

public class JetValueParameterAnnotation extends PsiAnnotationWrapper {
    private static final JetValueParameterAnnotation NULL_ANNOTATION = new JetValueParameterAnnotation(null);
    static {
        NULL_ANNOTATION.checkInitialized();
    }

    private String name;
    private String type;
    private boolean receiver;
    private boolean hasDefaultValue;
    private boolean vararg;

    private JetValueParameterAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }

    @Override
    protected void initialize() {
        name = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD, "");
        type = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD, "");
        receiver = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD, false);
        hasDefaultValue = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD, false);
        vararg = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_VARARG, false);
    }

    @NotNull
    public String name() {
        checkInitialized();
        return name;
    }

    @NotNull
    public String type() {
        checkInitialized();
        return type;
    }

    public boolean receiver() {
        checkInitialized();
        return receiver;
    }
    
    public boolean hasDefaultValue() {
        checkInitialized();
        return hasDefaultValue;
    }

    public boolean vararg() {
        checkInitialized();
        return vararg;
    }

    public static JetValueParameterAnnotation get(PsiParameter psiParameter) {
        final PsiAnnotation annotation =
                JavaAnnotationResolver.findOwnAnnotation(psiParameter, JvmStdlibNames.JET_VALUE_PARAMETER.getFqName().getFqName());
        return annotation != null ? new JetValueParameterAnnotation(annotation) : NULL_ANNOTATION;
    }
}
