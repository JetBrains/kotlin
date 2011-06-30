package org.jetbrains.jet.codegen;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 */
public class CallableMethod {
    private final String owner;
    private final Method descriptor;
    private final int invokeOpcode;
    private final List<Type> valueParameterTypes;

    public CallableMethod(String owner, Method descriptor, int invokeOpcode, List<Type> valueParameterTypes) {
        this.owner = owner;
        this.descriptor = descriptor;
        this.invokeOpcode = invokeOpcode;
        this.valueParameterTypes = valueParameterTypes;
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

    public List<Type> getValueParameterTypes() {
        return valueParameterTypes;
    }

    void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), getOwner(), getDescriptor().getName(), getDescriptor().getDescriptor());
    }
}
