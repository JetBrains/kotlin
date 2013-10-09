/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.util.Printer;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;

import java.util.List;

import static org.jetbrains.asm4.Opcodes.INVOKESPECIAL;
import static org.jetbrains.asm4.Opcodes.INVOKESTATIC;

public class CallableMethod implements Callable {
    @NotNull
    private final Type owner;
    @Nullable
    private final Type defaultImplOwner;
    @Nullable
    private final Type defaultImplParam;
    private final JvmMethodSignature signature;
    private final int invokeOpcode;
    @Nullable
    private final Type thisClass;
    @Nullable
    private final Type receiverParameterType;
    @Nullable
    private final Type generateCalleeType;

    public CallableMethod(
            @NotNull Type owner, @Nullable Type defaultImplOwner, @Nullable Type defaultImplParam,
            JvmMethodSignature signature, int invokeOpcode,
            @Nullable Type thisClass, @Nullable Type receiverParameterType, @Nullable Type generateCalleeType
    ) {
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
    public Type getOwner() {
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

    @Nullable
    public Type getThisType() {
        return thisClass;
    }

    @Nullable
    public Type getReceiverClass() {
        return receiverParameterType;
    }

    private void invoke(InstructionAdapter v) {
        v.visitMethodInsn(getInvokeOpcode(), owner.getInternalName(), getSignature().getAsmMethod().getName(),
                          getSignature().getAsmMethod().getDescriptor());
    }

    public void invokeWithNotNullAssertion(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull ResolvedCall resolvedCall
    ) {
        invokeWithoutAssertions(v);
        AsmUtil.genNotNullAssertionForMethod(v, state, resolvedCall);
    }

    public void invokeWithoutAssertions(@NotNull InstructionAdapter v) {
        invoke(v);
    }

    @Nullable
    public Type getGenerateCalleeType() {
        return generateCalleeType;
    }

    private void invokeDefault(InstructionAdapter v, int mask) {
        if (defaultImplOwner == null || defaultImplParam == null) {
            throw new IllegalStateException();
        }

        v.iconst(mask);
        String desc = getSignature().getAsmMethod().getDescriptor().replace(")", "I)");
        if ("<init>".equals(getSignature().getAsmMethod().getName())) {
            v.visitMethodInsn(INVOKESPECIAL, defaultImplOwner.getInternalName(), "<init>", desc);
        }
        else {
            if (getInvokeOpcode() != INVOKESTATIC) {
                desc = desc.replace("(", "(" + defaultImplParam.getDescriptor());
            }
            v.visitMethodInsn(INVOKESTATIC, defaultImplOwner.getInternalName(),
                              getSignature().getAsmMethod().getName() + JvmAbi.DEFAULT_PARAMS_IMPL_SUFFIX, desc);
        }
    }

    public void invokeDefaultWithNotNullAssertion(
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state,
            @NotNull ResolvedCall resolvedCall,
            int mask
    ) {
        invokeDefault(v, mask);
        AsmUtil.genNotNullAssertionForMethod(v, state, resolvedCall);
    }

    public boolean isNeedsThis() {
        return thisClass != null && generateCalleeType == null;
    }

    public Type getReturnType() {
        return signature.getAsmMethod().getReturnType();
    }

    @Override
    public String toString() {
        return Printer.OPCODES[invokeOpcode] + " " + owner + "." + signature;
    }
}
