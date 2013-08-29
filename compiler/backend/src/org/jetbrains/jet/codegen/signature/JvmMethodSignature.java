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

package org.jetbrains.jet.codegen.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;

import java.util.ArrayList;
import java.util.List;

public class JvmMethodSignature {

    @NotNull
    private final Method asmMethod;
    /**
     * Null when we don't care about type parameters
     */
    private final String genericsSignature;
    @NotNull
    private final List<JvmMethodParameterSignature> kotlinParameterTypes;

    protected JvmMethodSignature(
            @NotNull Method asmMethod,
            @Nullable String genericsSignature,
            @NotNull List<JvmMethodParameterSignature> kotlinParameterTypes
    ) {
        this.asmMethod = asmMethod;
        this.genericsSignature = genericsSignature;
        this.kotlinParameterTypes = kotlinParameterTypes;
    }

    @NotNull
    public Method getAsmMethod() {
        return asmMethod;
    }

    public String getGenericsSignature() {
        return genericsSignature;
    }

    @NotNull
    public List<JvmMethodParameterSignature> getKotlinParameterTypes() {
        return kotlinParameterTypes;
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

    @Override
    public String toString() {
        return asmMethod.toString();
    }
}
