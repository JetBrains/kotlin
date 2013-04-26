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
import org.jetbrains.asm4.commons.Method;

import java.util.List;

public class JvmPropertyAccessorSignature extends JvmMethodSignature {

    private final boolean isGetter;

    protected JvmPropertyAccessorSignature(
            @NotNull Method asmMethod,
            @Nullable String genericsSignature,
            @Nullable String kotlinTypeParameters,
            @NotNull List<JvmMethodParameterSignature> kotlinParameterTypes,
            @NotNull String kotlinReturnType,
            boolean needGenerics,
            boolean isGetter
    ) {
        super(asmMethod, genericsSignature, kotlinTypeParameters, kotlinParameterTypes,
              kotlinReturnType, needGenerics);
        this.isGetter = isGetter;
    }

    @NotNull
    public JvmMethodSignature getJvmMethodSignature() {
        return this;
    }

    @NotNull
    public String getPropertyTypeKotlinSignature() {
        return isGetter ? getKotlinReturnType() : getKotlinParameterType(getParameterCount() - 1);
    }
}
