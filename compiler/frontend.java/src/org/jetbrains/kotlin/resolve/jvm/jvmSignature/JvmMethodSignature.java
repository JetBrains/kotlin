/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.jvmSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.List;

public class JvmMethodSignature {
    private final Method asmMethod;
    private final List<JvmMethodParameterSignature> valueParameters;

    public JvmMethodSignature(
            @NotNull Method asmMethod,
            @NotNull List<JvmMethodParameterSignature> valueParameters
    ) {
        this.asmMethod = asmMethod;
        this.valueParameters = valueParameters;
    }

    @NotNull
    public Method getAsmMethod() {
        return asmMethod;
    }


    @NotNull
    public List<JvmMethodParameterSignature> getValueParameters() {
        return valueParameters;
    }

    @NotNull
    public Type getReturnType() {
        return asmMethod.getReturnType();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JvmMethodSignature)) return false;

        JvmMethodSignature that = (JvmMethodSignature) o;

        return asmMethod.equals(that.asmMethod) &&
               valueParameters.equals(that.valueParameters);
    }

    @Override
    public int hashCode() {
        int result = asmMethod.hashCode();
        result = 31 * result + valueParameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return asmMethod.toString();
    }
}
