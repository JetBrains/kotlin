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
import org.jetbrains.org.objectweb.asm.Type;

public final class JvmMethodParameterSignature {
    private final Type asmType;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JvmMethodParameterSignature)) return false;

        JvmMethodParameterSignature that = (JvmMethodParameterSignature) o;
        return asmType.equals(that.asmType) && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return 31 * asmType.hashCode() + kind.hashCode();
    }

    @Override
    public String toString() {
        return kind + " " + asmType;
    }
}
