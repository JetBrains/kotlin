/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public class CallReceiver extends StackValue {
    private final StackValue dispatchReceiver;
    private final StackValue extensionReceiver;
    private final Type secondReceiverType;

    private CallReceiver(
            @NotNull StackValue dispatchReceiver,
            @NotNull StackValue extensionReceiver,
            @NotNull Type type,
            @Nullable Type secondReceiverType
    ) {
        super(type, dispatchReceiver.canHaveSideEffects() || extensionReceiver.canHaveSideEffects());
        this.dispatchReceiver = dispatchReceiver;
        this.extensionReceiver = extensionReceiver;
        this.secondReceiverType = secondReceiverType;
    }

    public StackValue withoutReceiverArgument() {
        return new CallReceiver(dispatchReceiver, none(), type, secondReceiverType);
    }

    public static StackValue generateCallReceiver(
            @NotNull ResolvedCall<?> resolvedCall,
            @NotNull ExpressionCodegen codegen,
            @Nullable Callable callableMethod,
            @Nullable ReceiverParameterDescriptor dispatchReceiverParameter,
            @NotNull StackValue dispatchReceiver,
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @NotNull StackValue extensionReceiver
    ) {
        KotlinTypeMapper typeMapper = codegen.typeMapper;
        GenerationState state = codegen.getState();

        Type type;
        Type secondReceiverType = null;
        if (extensionReceiverParameter != null) {
            type = calcExtensionReceiverType(resolvedCall, extensionReceiverParameter, typeMapper, callableMethod, state);
            if (dispatchReceiverParameter != null) {
                secondReceiverType = calcDispatchReceiverType(resolvedCall, dispatchReceiverParameter, typeMapper, callableMethod);
            }
        }
        else if (dispatchReceiverParameter != null) {
            type = calcDispatchReceiverType(resolvedCall, dispatchReceiverParameter, typeMapper, callableMethod);
        }
        else if (isLocalFunCall(callableMethod)) {
            type = callableMethod.getGenerateCalleeType();
        }
        else {
            type = Type.VOID_TYPE;
        }

        assert type != null : "Could not map receiver type for " + resolvedCall;

        return new CallReceiver(dispatchReceiver, extensionReceiver, type, secondReceiverType);
    }

    private static Type calcDispatchReceiverType(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable ReceiverParameterDescriptor dispatchReceiver,
            @NotNull KotlinTypeMapper typeMapper,
            @Nullable Callable callableMethod
    ) {
        if (dispatchReceiver == null) return null;

        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

        if (CodegenUtilKt.isJvmStaticInObjectOrClass(descriptor)) {
            return Type.VOID_TYPE;
        }

        if (callableMethod != null) {
            return callableMethod.getDispatchReceiverType();
        }

        // Extract the receiver from the resolved call, workarounding the fact that ResolvedCall#dispatchReceiver doesn't have
        // all the needed information, for example there's no way to find out whether or not a smart cast was applied to the receiver.
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container instanceof ClassDescriptor) {
            return typeMapper.mapClass((ClassDescriptor) container);
        }

        return typeMapper.mapType(dispatchReceiver);
    }

    private static Type calcExtensionReceiverType(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable ReceiverParameterDescriptor extensionReceiver,
            @NotNull KotlinTypeMapper typeMapper,
            @Nullable Callable callableMethod,
            @NotNull GenerationState state
    ) {
        if (extensionReceiver == null) return null;

        CallableDescriptor descriptor = resolvedCall.getCandidateDescriptor();

        if (descriptor instanceof PropertyDescriptor &&
            // hackaround: boxing changes behaviour of T.javaClass intrinsic
            state.getIntrinsics().getIntrinsic((PropertyDescriptor) descriptor) != JavaClassProperty.INSTANCE
        ) {
            ReceiverParameterDescriptor receiverCandidate = descriptor.getExtensionReceiverParameter();
            assert receiverCandidate != null;
            return typeMapper.mapType(receiverCandidate.getType());
        }

        return callableMethod != null ? callableMethod.getExtensionReceiverType() : typeMapper.mapType(extensionReceiver.getType());
    }

    @Override
    public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
        StackValue currentExtensionReceiver = extensionReceiver;
        boolean hasExtensionReceiver = extensionReceiver != none();
        if (extensionReceiver instanceof SafeCall) {
            currentExtensionReceiver.put(currentExtensionReceiver.type, v);
            currentExtensionReceiver = StackValue.onStack(currentExtensionReceiver.type);
        }

        Type dispatchReceiverType = secondReceiverType != null ? secondReceiverType :
                                    hasExtensionReceiver ? dispatchReceiver.type :
                                    type;
        dispatchReceiver.put(dispatchReceiverType, v);

        currentExtensionReceiver
                .moveToTopOfStack(hasExtensionReceiver ? type : currentExtensionReceiver.type, v, dispatchReceiverType.getSize());
    }

    @Override
    public void dup(@NotNull InstructionAdapter v, boolean withReceiver) {
        AsmUtil.dup(v, extensionReceiver.type, dispatchReceiver.type);
    }

    @NotNull
    public StackValue getDispatchReceiver() {
        return dispatchReceiver;
    }

    @NotNull
    public StackValue getExtensionReceiver() {
        return extensionReceiver;
    }
}
