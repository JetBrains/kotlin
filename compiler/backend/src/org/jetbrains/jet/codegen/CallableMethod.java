package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import java.util.List;

/**
 * @author yole
 * @author alex.tkacman
 */
public class CallableMethod implements Callable {
    private String owner;
    private final Method signature;
    private int invokeOpcode;
    private final List<Type> valueParameterTypes;
    private ClassDescriptor thisClass = null;

    private CallableDescriptor receiverFunction = null;
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

    public Method getSignature() {
        return signature;
    }

    public int getInvokeOpcode() {
        return invokeOpcode;
    }

    public List<Type> getValueParameterTypes() {
        return valueParameterTypes;
    }

    public void setNeedsReceiver(@Nullable CallableDescriptor receiverClass) {
        this.receiverFunction = receiverClass;
    }

    public void setNeedsThis(@Nullable ClassDescriptor receiverClass) {
        this.thisClass = receiverClass;
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
        return receiverFunction != null;
    }
}
