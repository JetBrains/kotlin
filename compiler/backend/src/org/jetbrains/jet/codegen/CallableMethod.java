package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.types.JetType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

/**
 * @author yole
 * @author alex.tkacman
 */
public class CallableMethod implements Callable {
    private String owner;
    private final JvmMethodSignature signature;
    private int invokeOpcode;
    private ClassDescriptor thisClass = null;

    private CallableDescriptor receiverFunction = null;
    private Type generateCalleeType = null;

    public CallableMethod(String owner, JvmMethodSignature signature, int invokeOpcode) {
        this.owner = owner;
        this.signature = signature;
        this.invokeOpcode = invokeOpcode;
    }

    public String getOwner() {
        return owner;
    }

    public JvmMethodSignature getSignature() {
        return signature;
    }

    public int getInvokeOpcode() {
        return invokeOpcode;
    }

    public List<Type> getValueParameterTypes() {
        return signature.getValueParameterTypes();
    }

    public void setNeedsReceiver(@Nullable CallableDescriptor receiverClass) {
        this.receiverFunction = receiverClass;
    }

    public JetType getThisType() {
        return thisClass.getDefaultType();
    }

    public JetType getReceiverClass() {
        return receiverFunction.getReceiverParameter().getType();
    }

    public void setNeedsThis(@Nullable ClassDescriptor receiverClass) {
        this.thisClass = receiverClass;
    }

    void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), getOwner(), getSignature().getAsmMethod().getName(), getSignature().getAsmMethod().getDescriptor());
    }

    public void requestGenerateCallee(Type objectType) {
        generateCalleeType = objectType;
    }

    public Type getGenerateCalleeType() {
        return generateCalleeType;
    }

    public void invokeWithDefault(InstructionAdapter v, int mask) {
        v.iconst(mask);
        String desc = getSignature().getAsmMethod().getDescriptor().replace(")", "I)");
        if("<init>".equals(getSignature().getAsmMethod().getName())) {
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, getOwner(), "<init>", desc);
        }
        else {
            if(getInvokeOpcode() != Opcodes.INVOKESTATIC)
                desc = desc.replace("(", "(L" + getOwner() + ";");
            v.visitMethodInsn(Opcodes.INVOKESTATIC, getInvokeOpcode() == Opcodes.INVOKEINTERFACE ? getOwner() + JvmAbi.TRAIT_IMPL_SUFFIX
                                                                                                 : getOwner(), getSignature().getAsmMethod().getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc);
        }
    }

    public boolean isNeedsThis() {
        return thisClass != null;
    }

    public boolean isNeedsReceiver() {
        return receiverFunction != null;
    }
}
