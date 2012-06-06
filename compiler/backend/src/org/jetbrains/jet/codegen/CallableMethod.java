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
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
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
    private final JvmClassName owner;
    @Nullable
    private final JvmClassName defaultImplOwner;
    @Nullable
    private final JvmClassName defaultImplParam;
    private final JvmMethodSignature signature;
    private final int invokeOpcode;
    @Nullable
    private final JvmClassName thisClass;
    @Nullable
    private final Type receiverParameterType;
    @Nullable
    private final Type generateCalleeType;

    public CallableMethod(@NotNull JvmClassName owner, @Nullable JvmClassName defaultImplOwner, @Nullable JvmClassName defaultImplParam,
            JvmMethodSignature signature, int invokeOpcode,
            @Nullable JvmClassName thisClass, @Nullable Type receiverParameterType, @Nullable Type generateCalleeType) {
        this.owner = owner;
        this.defaultImplOwner = defaultImplOwner;
        this.defaultImplParam = defaultImplParam;
        this.signature = signature;
        this.invokeOpcode = invokeOpcode;
        this.thisClass = thisClass;
        this.receiverParameterType = receiverParameterType;
        this.generateCalleeType = generateCalleeType;
    }

    @NotNull
    public JvmClassName getOwner() {
        return owner;
    }

    @NotNull
    public JvmClassName getDefaultImplParam() {
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

    public JvmClassName getThisType() {
        return thisClass;
    }

    public Type getReceiverClass() {
        return receiverParameterType;
    }

    void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), owner.getInternalName(), getSignature().getAsmMethod().getName(), getSignature().getAsmMethod().getDescriptor());
    }

    public Type getGenerateCalleeType() {
        return generateCalleeType;
    }

    public void invokeWithDefault(InstructionAdapter v, int mask) {
        if (defaultImplOwner == null || defaultImplParam == null) {
            throw new IllegalStateException();
        }

        v.iconst(mask);
        String desc = getSignature().getAsmMethod().getDescriptor().replace(")", "I)");
        if("<init>".equals(getSignature().getAsmMethod().getName())) {
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, defaultImplOwner.getInternalName(), "<init>", desc);
        }
        else {
            if(getInvokeOpcode() != Opcodes.INVOKESTATIC)
                desc = desc.replace("(", "(" + defaultImplParam.getDescriptor());
            v.visitMethodInsn(Opcodes.INVOKESTATIC, defaultImplOwner.getInternalName(),
                    getSignature().getAsmMethod().getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc);
        }
    }

    public boolean isNeedsThis() {
        return thisClass != null && generateCalleeType == null;
    }

    public boolean isNeedsReceiver() {
        return receiverParameterType != null;
    }

    public Type getReturnType() {
        return signature.getAsmMethod().getReturnType();
    }
}
