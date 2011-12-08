package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.commons.Method;

/**
 * @author Stepan Koltsov
 */
public class JvmMethodSignature {
    
    private final Method asmMethod;
    /** Null when we don't care about type parameters */
    private final String genericsSignature;

    public JvmMethodSignature(@NotNull Method asmMethod, @Nullable String genericsSignature) {
        this.asmMethod = asmMethod;
        this.genericsSignature = genericsSignature;
    }

    public Method getAsmMethod() {
        return asmMethod;
    }

    public String getGenericsSignature() {
        return genericsSignature;
    }
}
