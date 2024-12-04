/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
