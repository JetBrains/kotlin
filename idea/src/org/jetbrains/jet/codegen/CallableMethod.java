package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 */
public class CallableMethod {
    private String owner;
    private final Method signature;
    private final int invokeOpcode;
    private final List<Type> valueParameterTypes;
    private boolean acceptsTypeArguments = false;
    private boolean needsReceiverOnStack = false;
    private ClassDescriptor receiverClass = null;

    public CallableMethod(String owner, Method signature, int invokeOpcode, List<Type> valueParameterTypes) {
        this.owner = owner;
        this.signature = signature;
        this.invokeOpcode = invokeOpcode;
        this.valueParameterTypes = valueParameterTypes;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Method getSignature() {
        return signature;
    }

    public int getInvokeOpcode() {
        return invokeOpcode;
    }

    public List<Type> getValueParameterTypes() {
        return valueParameterTypes;
    }

    public boolean acceptsTypeArguments() {
        return acceptsTypeArguments;
    }

    public void setAcceptsTypeArguments(boolean acceptsTypeArguments) {
        this.acceptsTypeArguments = acceptsTypeArguments;
    }

    public void setNeedsReceiver(@Nullable ClassDescriptor receiverClass) {
        needsReceiverOnStack = true;
        this.receiverClass = receiverClass;
    }

    public boolean needsReceiverOnStack() {
        return needsReceiverOnStack;
    }

    public ClassDescriptor getReceiverClass() {
        return receiverClass;
    }

    void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), getOwner(), getSignature().getName(), getSignature().getDescriptor());
    }
}
