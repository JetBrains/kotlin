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
import org.jetbrains.asm4.Type;

public class JvmMethodParameterSignature {
    @NotNull
    private final Type asmType;
    @NotNull
    private final JvmMethodParameterKind kind;

    public JvmMethodParameterSignature(@NotNull Type asmType, @NotNull JvmMethodParameterKind kind) {
        this.asmType = asmType;
        this.kind = kind;
    }

    @NotNull
    public Type getAsmType() {
        return asmType;
    }

    @NotNull
    public JvmMethodParameterKind getKind() {
        return kind;
    }
}
