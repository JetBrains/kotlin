/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.jvmSignature;

import org.jetbrains.annotations.NotNull;
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
