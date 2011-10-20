package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 */
public class CallableMethod implements Callable {
    private String owner;
    private final Method signature;
    private final int invokeOpcode;
    private final List<Type> valueParameterTypes;
    private boolean acceptsTypeArguments = false;
    private boolean ownerFromCall = false;
    private DeclarationDescriptor thisClass = null;
    private DeclarationDescriptor receiverClass = null;
    private Type generateCalleeType = null;

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

    public void setNeedsReceiver(@Nullable DeclarationDescriptor receiverClass) {
        this.receiverClass = receiverClass;
    }

    public void setNeedsThis(@Nullable DeclarationDescriptor receiverClass) {
        this.thisClass = receiverClass;
    }

    public boolean isOwnerFromCall() {
        return ownerFromCall;
    }

    public void setOwnerFromCall(boolean ownerFromCall) {
        this.ownerFromCall = ownerFromCall;
    }

    void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), getOwner(), getSignature().getName(), getSignature().getDescriptor());
    }

    public void requestGenerateCallee(Type objectType) {
        generateCalleeType = objectType;
    }

    public Type getGenerateCalleeType() {
        return generateCalleeType;
    }

    public void invokeWithDefault(InstructionAdapter v, int mask) {
        v.iconst(mask);
        String desc = getSignature().getDescriptor().replace(")", "I)");
        if(getInvokeOpcode() != Opcodes.INVOKESTATIC)
            desc = desc.replace("(", "(L" + getOwner() + ";");
        v.visitMethodInsn(Opcodes.INVOKESTATIC, getInvokeOpcode() == Opcodes.INVOKEINTERFACE ? getOwner() + "$$TImpl" : getOwner(), getSignature().getName() + "$default", desc);
    }

    public boolean isNeedsThis() {
        return thisClass != null;
    }

    public boolean isNeedsReceiver() {
        return receiverClass != null;
    }

    public DeclarationDescriptor getThisClass() {
        return thisClass;
    }

}
