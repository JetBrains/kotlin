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
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;

/**
 * @author Stepan Koltsov
 */
public class JetMethodAnnotation extends PsiAnnotationWrapper {

    public JetMethodAnnotation(@Nullable PsiAnnotation psiAnnotation) {
        super(psiAnnotation);
    }
    
    private int kind;
    private boolean kindInitialized;
    public int kind() {
        if (!kindInitialized) {
            kind = getIntAttribute(JvmStdlibNames.JET_METHOD_KIND_FIELD, JvmStdlibNames.JET_METHOD_KIND_DEFAULT);
            kindInitialized = true;
        }
        return kind;
    }

    private String typeParameters;
    @NotNull
    public String typeParameters() {
        if (typeParameters == null) {
            typeParameters = getStringAttribute(JvmStdlibNames.JET_METHOD_TYPE_PARAMETERS_FIELD, "");
        }
        return typeParameters;
    }

    private String returnType;
    @NotNull
    public String returnType() {
        if (returnType == null) {
            returnType = getStringAttribute(JvmStdlibNames.JET_METHOD_RETURN_TYPE_FIELD, "");
        }
        return returnType;
    }

    private boolean returnTypeNullable;
    private boolean returnTypeNullableInitialized;
    @NotNull
    public boolean returnTypeNullable() {
        if (!returnTypeNullableInitialized) {
            returnTypeNullable = getBooleanAttribute(JvmStdlibNames.JET_METHOD_NULLABLE_RETURN_TYPE_FIELD, false);
            returnTypeNullableInitialized = true;
        }
        return returnTypeNullable;
    }

    private String propertyType;
    @NotNull
    public String propertyType() {
        if (propertyType == null) {
            propertyType = getStringAttribute(JvmStdlibNames.JET_METHOD_PROPERTY_TYPE_FIELD, "");
        }
        return propertyType;
    }

    public static JetMethodAnnotation get(PsiMethod psiMethod) {
        return new JetMethodAnnotation(psiMethod.getModifierList().findAnnotation(JvmStdlibNames.JET_METHOD.getFqName()));
    }
}
