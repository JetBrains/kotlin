/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.intrinsics.JavaClassProperty;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public class CallReceiver extends StackValue {
    private final StackValue dispatchReceiver;
    private final StackValue extensionReceiver;
    private final Type secondReceiverType;
    private final KotlinType secondReceiverKotlinType;

    private CallReceiver(
            @NotNull StackValue dispatchReceiver,
            @NotNull StackValue extensionReceiver,
            @NotNull Type type,
            @Nullable KotlinType kotlinType,
            @Nullable Type secondReceiverType,
            @Nullable KotlinType secondReceiverKotlinType
    ) {
        super(type, kotlinType, dispatchReceiver.canHaveSideEffects() || extensionReceiver.canHaveSideEffects());
        this.dispatchReceiver = dispatchReceiver;
        this.extensionReceiver = extensionReceiver;
        this.secondReceiverType = secondReceiverType;
        this.secondReceiverKotlinType = secondReceiverKotlinType;
    }

    public StackValue withoutReceiverArgument() {
        return new CallReceiver(dispatchReceiver, none(), type, kotlinType, secondReceiverType, secondReceiverKotlinType);
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

        JvmKotlinType jvmKotlinType;
        Type secondReceiverType = null;
        KotlinType secondReceiverKotlinType = null;
        if (extensionReceiverParameter != null) {
            jvmKotlinType = calcExtensionReceiverType(
                    resolvedCall, extensionReceiverParameter, typeMapper, callableMethod, state
            );
            if (dispatchReceiverParameter != null) {
                JvmKotlinType dispatchReceiverInfo = calcDispatchReceiverType(
                        resolvedCall, dispatchReceiverParameter, typeMapper, callableMethod
                );
                secondReceiverType = dispatchReceiverInfo.getType();
                secondReceiverKotlinType = dispatchReceiverInfo.getKotlinType();
            }
        }
        else if (dispatchReceiverParameter != null) {
            jvmKotlinType = calcDispatchReceiverType(resolvedCall, dispatchReceiverParameter, typeMapper, callableMethod);
        }
        else if (isLocalFunCall(callableMethod)) {
            Type calleeType = callableMethod.getGenerateCalleeType();
            assert calleeType != null : "Could not get callee type for " + resolvedCall;

            jvmKotlinType = new JvmKotlinType(calleeType, null);
        }
        else {
            jvmKotlinType = new JvmKotlinType(Type.VOID_TYPE, null);
        }


        return new CallReceiver(
                dispatchReceiver, extensionReceiver, jvmKotlinType.getType(),
                jvmKotlinType.getKotlinType(), secondReceiverType, secondReceiverKotlinType
        );
    }

    private static JvmKotlinType calcDispatchReceiverType(
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable ReceiverParameterDescriptor dispatchReceiver,
            @NotNull KotlinTypeMapper typeMapper,
            @Nullable Callable callableMethod
    ) {
        if (dispatchReceiver == null) return null;

        CallableDescriptor descriptor = resolvedCall.getResultingDescriptor();

        if (CodegenUtilKt.isJvmStaticInObjectOrClassOrInterface(descriptor)) {
            return new JvmKotlinType(Type.VOID_TYPE, null);
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (callableMethod != null) {
            if (InlineClassesUtilsKt.isInlineClass(container)) {
                ClassDescriptor classDescriptor = (ClassDescriptor) container;
                return new JvmKotlinType(typeMapper.mapType(classDescriptor), classDescriptor.getDefaultType());
            }
            //noinspection ConstantConditions
            return new JvmKotlinType(callableMethod.getDispatchReceiverType(), callableMethod.getDispatchReceiverKotlinType());
        }

        // Extract the receiver from the resolved call, workarounding the fact that ResolvedCall#dispatchReceiver doesn't have
        // all the needed information, for example there's no way to find out whether or not a smart cast was applied to the receiver.
        if (container instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) container;
            return new JvmKotlinType(typeMapper.mapType(classDescriptor), classDescriptor.getDefaultType());
        }

        KotlinType dispatchReceiverType = dispatchReceiver.getReturnType();

        //noinspection ConstantConditions
        return new JvmKotlinType(typeMapper.mapType(dispatchReceiverType), dispatchReceiverType);
    }

    private static JvmKotlinType calcExtensionReceiverType(
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
            return new JvmKotlinType(typeMapper.mapType(receiverCandidate.getType()), receiverCandidate.getType());
        }

        return callableMethod != null ?
               new JvmKotlinType(callableMethod.getExtensionReceiverType(), callableMethod.getExtensionReceiverKotlinType()) :
               new JvmKotlinType(typeMapper.mapType(extensionReceiver.getType()), extensionReceiver.getType());
    }

    @Override
    public void putSelector(@NotNull Type type, @Nullable KotlinType kotlinType, @NotNull InstructionAdapter v) {
        StackValue currentExtensionReceiver = extensionReceiver;
        boolean hasExtensionReceiver = extensionReceiver != none();
        if (extensionReceiver instanceof SafeCall) {
            currentExtensionReceiver.put(currentExtensionReceiver.type, currentExtensionReceiver.kotlinType, v);
            currentExtensionReceiver = StackValue.onStack(currentExtensionReceiver.type, currentExtensionReceiver.kotlinType);
        }

        Type dispatchReceiverType = calcDispatchReceiver(secondReceiverType, hasExtensionReceiver, dispatchReceiver.type, type);
        KotlinType dispatchReceiverKotlinType = calcDispatchReceiver(
                secondReceiverKotlinType, hasExtensionReceiver, dispatchReceiver.kotlinType, kotlinType
        );

        dispatchReceiver.put(dispatchReceiverType, dispatchReceiverKotlinType, v);

        currentExtensionReceiver
                .moveToTopOfStack(
                        hasExtensionReceiver ? type : currentExtensionReceiver.type,
                        hasExtensionReceiver ? kotlinType : currentExtensionReceiver.kotlinType,
                        v,
                        dispatchReceiverType.getSize()
                );
    }

    private static <T> T calcDispatchReceiver(T secondType, boolean hasExtensionReceiver, T dispatchReceiverType, T defaultType) {
        if (secondType != null) {
            return secondType;
        }
        else {
            return hasExtensionReceiver ? dispatchReceiverType : defaultType;
        }
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
