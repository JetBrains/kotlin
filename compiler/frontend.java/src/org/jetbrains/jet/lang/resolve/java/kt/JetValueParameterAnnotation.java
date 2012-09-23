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
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 * @author alex.tkachman
 */
public class JetValueParameterAnnotation extends PsiAnnotationWrapper {
    
    public JetValueParameterAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    private String name;
    private String type;
    private boolean nullable;
    private boolean receiver;
    private boolean hasDefaultValue;

    @Override
    protected void initialize() {
        name = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD, "");
        type = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD, "");
        nullable = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NULLABLE_FIELD, false);
        receiver = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD, false);
        hasDefaultValue = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD, false);
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

    public boolean nullable() {
        checkInitialized();
        return nullable;
    }

    public boolean receiver() {
        checkInitialized();
        return receiver;
    }
    
    public boolean hasDefaultValue() {
        checkInitialized();
        return hasDefaultValue;
    }
    
    public static JetValueParameterAnnotation get(PsiParameter psiParameter) {
        return new JetValueParameterAnnotation(
                JavaDescriptorResolver.findAnnotation(psiParameter, JvmStdlibNames.JET_VALUE_PARAMETER.getFqName().getFqName()));
    }
}
