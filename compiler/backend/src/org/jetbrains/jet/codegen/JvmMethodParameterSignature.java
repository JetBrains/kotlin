package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class JvmMethodParameterSignature {
    @NotNull
    private final Type asmType;
    @NotNull
    private final String kotlinSignature;
    @NotNull
    private final JvmMethodParameterKind kind;

    public JvmMethodParameterSignature(
            @NotNull Type asmType, @NotNull String kotlinSignature, @NotNull JvmMethodParameterKind kind) {
        this.asmType = asmType;
        this.kotlinSignature = kotlinSignature;
        this.kind = kind;
    }

    @NotNull
    public Type getAsmType() {
        return asmType;
    }

    @NotNull
    public String getKotlinSignature() {
        return kotlinSignature;
    }

    @NotNull
    public JvmMethodParameterKind getKind() {
        return kind;
    }
}
