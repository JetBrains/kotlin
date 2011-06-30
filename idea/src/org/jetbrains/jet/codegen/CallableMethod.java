package org.jetbrains.jet.codegen;

import org.objectweb.asm.commons.Method;

/**
 * @author yole
 */
public class CallableMethod {
    private final String owner;
    private final Method descriptor;
    private final int invokeOpcode;

    public CallableMethod(String owner, Method descriptor, int invokeOpcode) {
        this.owner = owner;
        this.descriptor = descriptor;
        this.invokeOpcode = invokeOpcode;
    }

    public String getOwner() {
        return owner;
    }

    public Method getDescriptor() {
        return descriptor;
    }

    public int getInvokeOpcode() {
        return invokeOpcode;
    }
}
