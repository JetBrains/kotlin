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
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetValueParameterAnnotation extends PsiAnnotationWrapper {
    
    public JetValueParameterAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    private String name;
    @NotNull
    public String name() {
        if (name == null) {
            name = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NAME_FIELD, "");
        }
        return name;
    }
    
    private String type;
    @NotNull
    public String type() {
        if (type == null) {
            type = getStringAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_TYPE_FIELD, "");
        }
        return type;
    }

    private boolean nullable;
    private boolean nullableInitialized = false;
    public boolean nullable() {
        if (!nullableInitialized) {
            nullable = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_NULLABLE_FIELD, false);
            nullableInitialized = true;
        }
        return nullable;
    }

    private boolean receiver;
    private boolean receiverInitialized = false;
    public boolean receiver() {
        if (!receiverInitialized) {
            receiver = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_RECEIVER_FIELD, false);
            receiverInitialized = true;
        }
        return receiver;
    }
    
    private boolean hasDefaultValue;
    private boolean hasDefaultValueInitialized = false;
    public boolean hasDefaultValue() {
        if (!hasDefaultValueInitialized) {
            hasDefaultValue = getBooleanAttribute(JvmStdlibNames.JET_VALUE_PARAMETER_HAS_DEFAULT_VALUE_FIELD, false);
            hasDefaultValueInitialized = true;
        }
        return hasDefaultValue;
    }
    
    public static JetValueParameterAnnotation get(PsiParameter psiParameter) {
        return new JetValueParameterAnnotation(psiParameter.getModifierList().findAnnotation(JvmStdlibNames.JET_VALUE_PARAMETER.getFqName()));
    }
    
}
