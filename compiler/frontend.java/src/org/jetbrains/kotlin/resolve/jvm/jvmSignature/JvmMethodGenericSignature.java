/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import java.util.Objects;

public class JvmMethodGenericSignature extends JvmMethodSignature {
    private final String genericsSignature;

    public JvmMethodGenericSignature(
            @NotNull Method asmMethod,
            @NotNull List<JvmMethodParameterSignature> valueParameters,
            @Nullable String genericsSignature
    ) {
        super(asmMethod, valueParameters);
        this.genericsSignature = genericsSignature;
    }

    @Nullable
    public String getGenericsSignature() {
        return genericsSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JvmMethodGenericSignature)) return false;

        JvmMethodGenericSignature that = (JvmMethodGenericSignature) o;

        return super.equals(that) && Objects.equals(genericsSignature, that.genericsSignature);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (genericsSignature != null ? genericsSignature.hashCode() : 0);
        return result;
    }
}
