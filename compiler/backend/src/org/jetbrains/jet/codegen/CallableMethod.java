/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
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
    @NotNull
    private final String owner;
    @NotNull
    private final String defaultImplOwner;
    @NotNull
    private final String defaultImplParam;
    private final JvmMethodSignature signature;
    private final int invokeOpcode;
    private ClassDescriptor thisClass = null;

    private CallableDescriptor receiverFunction = null;
    private Type generateCalleeType = null;

    public CallableMethod(@NotNull String owner, @NotNull String defaultImplOwner, @NotNull String defaultImplParam,
            JvmMethodSignature signature, int invokeOpcode) {
        this.owner = owner;
        this.defaultImplOwner = defaultImplOwner;
        this.defaultImplParam = defaultImplParam;
        this.signature = signature;
        this.invokeOpcode = invokeOpcode;
    }

    @NotNull
    public String getOwner() {
        return owner;
    }

    @NotNull
    public String getDefaultImplParam() {
        return defaultImplParam;
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
        v.visitMethodInsn(getInvokeOpcode(), owner, getSignature().getAsmMethod().getName(), getSignature().getAsmMethod().getDescriptor());
    }

    public void requestGenerateCallee(Type objectType) {
        generateCalleeType = objectType;
    }

    public Type getGenerateCalleeType() {
        return generateCalleeType;
    }

    public void invokeWithDefault(InstructionAdapter v, int mask) {
        if (defaultImplOwner.length() == 0 || defaultImplParam.length() == 0) {
            throw new IllegalStateException();
        }

        v.iconst(mask);
        String desc = getSignature().getAsmMethod().getDescriptor().replace(")", "I)");
        if("<init>".equals(getSignature().getAsmMethod().getName())) {
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, defaultImplOwner, "<init>", desc);
        }
        else {
            if(getInvokeOpcode() != Opcodes.INVOKESTATIC)
                desc = desc.replace("(", "(L" + defaultImplParam + ";");
            v.visitMethodInsn(Opcodes.INVOKESTATIC, defaultImplOwner,
                    getSignature().getAsmMethod().getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc);
        }
    }

    public boolean isNeedsThis() {
        return thisClass != null && generateCalleeType == null;
    }

    public boolean isNeedsReceiver() {
        return receiverFunction != null;
    }
}
