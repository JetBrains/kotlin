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

package org.jetbrains.jet.codegen.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class JvmMethodSignature {

    @NotNull
    private final Method asmMethod;
    /** Null when we don't care about type parameters */
    private final String genericsSignature;
    private final String kotlinTypeParameter;
    @NotNull
    private final List<JvmMethodParameterSignature> kotlinParameterTypes;
    @NotNull
    private final String kotlinReturnType;

    /**
     * Generics info is generated. However it can be trivial (e.g. fields are null).
     */
    private final boolean genericsAvailable;

    public JvmMethodSignature(@NotNull Method asmMethod, @Nullable String genericsSignature,
            @Nullable String kotlinTypeParameters, @NotNull List<JvmMethodParameterSignature> kotlinParameterTypes, @NotNull String kotlinReturnType) {
        this.asmMethod = asmMethod;
        this.genericsSignature = genericsSignature;
        this.kotlinTypeParameter = kotlinTypeParameters;
        this.kotlinParameterTypes = kotlinParameterTypes;
        this.kotlinReturnType = kotlinReturnType;
        this.genericsAvailable = true;
    }
    
    public JvmMethodSignature(@NotNull Method asmMethod, @NotNull List<JvmMethodParameterSignature> kotlinParameterTypes) {
        this.asmMethod = asmMethod;
        this.genericsSignature = null;
        this.kotlinTypeParameter = null;
        this.kotlinParameterTypes = kotlinParameterTypes;
        this.kotlinReturnType = "";
        this.genericsAvailable = false;
    }
    
    public static JvmMethodSignature simple(@NotNull String methodName, @NotNull Type returnType, @NotNull List<JvmMethodParameterSignature> parameterSignatures) {
        return new JvmMethodSignature(new Method(methodName, returnType, getTypes(parameterSignatures).toArray(new Type[0])), parameterSignatures);
    }
    
    @NotNull
    private static List<Type> getTypes(@NotNull List<JvmMethodParameterSignature> signatures) {
        List<Type> r = new ArrayList<Type>(signatures.size());
        for (JvmMethodParameterSignature signature : signatures) {
            r.add(signature.getAsmType());
        }
        return r;
    }

    private void checkGenericsAvailable() {
        if (!genericsAvailable) {
            // TODO: uncomment following line and fix all broken tests
            //throw new IllegalStateException("incorrect call sequence");
        }
    }

    @NotNull
    public Method getAsmMethod() {
        return asmMethod;
    }

    public String getGenericsSignature() {
        checkGenericsAvailable();
        return genericsSignature;
    }

    public String getKotlinTypeParameter() {
        checkGenericsAvailable();
        return kotlinTypeParameter;
    }

    @Nullable
    public List<JvmMethodParameterSignature> getKotlinParameterTypes() {
        checkGenericsAvailable();
        return kotlinParameterTypes;
    }
    
    public int getParameterCount() {
        // TODO: slow
        return asmMethod.getArgumentTypes().length;
    }
    
    @NotNull
    public String getKotlinParameterType(int i) {
        checkGenericsAvailable();
        if (kotlinParameterTypes == null) {
            return "";
        }
        else {
            return kotlinParameterTypes.get(i).getKotlinSignature();
        }
    }

    @NotNull
    public String getKotlinReturnType() {
        checkGenericsAvailable();
        return kotlinReturnType;
    }
    
    public List<Type> getValueParameterTypes() {
        List<Type> r = new ArrayList<Type>(kotlinParameterTypes.size());
        for (JvmMethodParameterSignature p : kotlinParameterTypes) {
            if (p.getKind() == JvmMethodParameterKind.VALUE) {
                r.add(p.getAsmType());
            }
        }
        return r;
    }

    @NotNull
    public String getName() {
        return asmMethod.getName();
    }
}
