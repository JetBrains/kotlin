/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.JvmCodegenUtil;
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

public class MethodContext extends CodegenContext<CallableMemberDescriptor> {
    private Label methodStartLabel;
    private Label methodEndLabel;

    // Note: in case of code inside property accessors, functionDescriptor will be that accessor,
    // but CodegenContext#contextDescriptor will be the corresponding property
    private final FunctionDescriptor functionDescriptor;
    private final boolean isDefaultFunctionContext;

    protected MethodContext(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind contextKind,
            @NotNull CodegenContext parentContext,
            @Nullable MutableClosure closure,
            boolean isDefaultFunctionContext
    ) {
        super(JvmCodegenUtil.getDirectMember(functionDescriptor), contextKind, parentContext, closure,
              parentContext.hasThisDescriptor() ? parentContext.getThisDescriptor() : null, null);
        this.functionDescriptor = functionDescriptor;
        this.isDefaultFunctionContext = isDefaultFunctionContext;
    }

    @NotNull
    @Override
    public CodegenContext getParentContext() {
        //noinspection ConstantConditions
        return super.getParentContext();
    }

    public StackValue getReceiverExpression(KotlinTypeMapper typeMapper) {
        assert getCallableDescriptorWithReceiver() != null;
        @SuppressWarnings("ConstantConditions")
        KotlinType kotlinType = getCallableDescriptorWithReceiver().getExtensionReceiverParameter().getType();
        Type asmType = typeMapper.mapType(kotlinType);
        return StackValue.local(AsmUtil.getReceiverIndex(this, getContextDescriptor()), asmType, kotlinType);
    }

    @Override
    public StackValue lookupInContext(DeclarationDescriptor d, @Nullable StackValue result, GenerationState state, boolean ignoreNoOuter) {
        if (d instanceof SyntheticFieldDescriptor) {
            SyntheticFieldDescriptor fieldDescriptor = (SyntheticFieldDescriptor) d;
            d = fieldDescriptor.getPropertyDescriptor();
        }
        if (getContextDescriptor() == d) {
            return result != null ? result : StackValue.LOCAL_0;
        }

        return getParentContext().lookupInContext(d, result, state, ignoreNoOuter);
    }

    @Nullable
    public StackValue generateReceiver(@NotNull CallableDescriptor descriptor, @NotNull GenerationState state, boolean ignoreNoOuter) {
        // When generating bytecode of some suspend function, we replace the original descriptor with one that reflects how it should look on JVM.
        // But when we looking for receiver parameter in resolved call, it still references the initial function, so we unwrap it here
        // before comparison.
        if (CoroutineCodegenUtilKt.unwrapInitialDescriptorForSuspendFunction(getCallableDescriptorWithReceiver()) == descriptor) {
            return getReceiverExpression(state.getTypeMapper());
        }
        ReceiverParameterDescriptor parameter = descriptor.getExtensionReceiverParameter();
        return lookupInContext(parameter, StackValue.LOCAL_0, state, ignoreNoOuter);
    }

    @Override
    public StackValue getOuterExpression(StackValue prefix, boolean ignoreNoOuter) {
        return getParentContext().getOuterExpression(prefix, false);
    }

    @Nullable
    public Label getMethodStartLabel() {
        return methodStartLabel;
    }

    public void setMethodStartLabel(@NotNull Label methodStartLabel) {
        this.methodStartLabel = methodStartLabel;
    }

    @Nullable
    public Label getMethodEndLabel() {
        return methodEndLabel;
    }

    public void setMethodEndLabel(@NotNull Label methodEndLabel) {
        this.methodEndLabel = methodEndLabel;
    }

    @Override
    public String toString() {
        return "Method: " + getContextDescriptor();
    }

    public boolean isInlineMethodContext() {
        return InlineUtil.isInline(getFunctionDescriptor());
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    public boolean isDefaultFunctionContext() {
        return isDefaultFunctionContext;
    }
}
