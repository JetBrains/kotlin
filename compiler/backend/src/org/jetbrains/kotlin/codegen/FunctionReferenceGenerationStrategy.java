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

package org.jetbrains.kotlin.codegen;

import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.calls.model.DelegatingResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.*;

public class FunctionReferenceGenerationStrategy extends FunctionGenerationStrategy.CodegenBased {
    private final ResolvedCall<?> resolvedCall;
    private final FunctionDescriptor referencedFunction;
    private final FunctionDescriptor functionDescriptor;
    private final Type receiverType; // non-null for bound references
    private final StackValue receiverValue;
    private final boolean isInliningStrategy;

    public FunctionReferenceGenerationStrategy(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ResolvedCall<?> resolvedCall,
            @Nullable Type receiverType,
            @Nullable StackValue receiverValue,
            boolean isInliningStrategy
    ) {
        super(state);
        this.resolvedCall = resolvedCall;
        this.referencedFunction = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
        this.functionDescriptor = functionDescriptor;
        this.receiverType = receiverType;
        this.receiverValue = receiverValue;
        this.isInliningStrategy = isInliningStrategy;
        assert receiverType != null || receiverValue == null
                : "A receiver value is provided for unbound function reference. Either this is a bound reference and you forgot " +
                  "to pass receiverType, or you accidentally passed some receiverValue for a reference without receiver";
    }

    @Override
    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
        /*
         Here we need to put the arguments from our locals to the stack and invoke the referenced method. Since invocation
         of methods is highly dependent on expressions, we create a fake call expression. Then we create a new instance of
         ExpressionCodegen and, in order for it to generate code correctly, we save to its 'tempVariables' field every
         argument of our fake expression, pointing it to the corresponding index in our locals. This way generation of
         every argument boils down to calling LOAD with the corresponding index
         */

        KtCallExpression fakeExpression = CodegenUtil.constructFakeFunctionCall(state.getProject(), referencedFunction);
        final List<? extends ValueArgument> fakeArguments = fakeExpression.getValueArguments();

        final ReceiverValue dispatchReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getDispatchReceiverParameter());
        final ReceiverValue extensionReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getExtensionReceiverParameter());
        computeAndSaveArguments(fakeArguments, codegen);

        ResolvedCall<CallableDescriptor> fakeResolvedCall = new DelegatingResolvedCall<CallableDescriptor>(resolvedCall) {

            private final Map<ValueParameterDescriptor, ResolvedValueArgument> argumentMap;
            {
                argumentMap = new LinkedHashMap<ValueParameterDescriptor, ResolvedValueArgument>(fakeArguments.size());
                int index = 0;
                List<ValueParameterDescriptor> parameters = functionDescriptor.getValueParameters();
                for (ValueArgument argument : fakeArguments) {
                    argumentMap.put(parameters.get(index), new ExpressionValueArgument(argument));
                    index++;
                }
            }

            @Nullable
            @Override
            public ReceiverValue getExtensionReceiver() {
                return extensionReceiver;
            }

            @Nullable
            @Override
            public ReceiverValue getDispatchReceiver() {
                return dispatchReceiver;
            }

            @NotNull
            @Override
            public List<ResolvedValueArgument> getValueArgumentsByIndex() {
                return new ArrayList<ResolvedValueArgument>(argumentMap.values());
            }

            @NotNull
            @Override
            public Map<ValueParameterDescriptor, ResolvedValueArgument> getValueArguments() {
                return argumentMap;
            }
        };

        StackValue result;
        Type returnType = codegen.getReturnType();
        if (referencedFunction instanceof ConstructorDescriptor) {
            if (returnType.getSort() == Type.ARRAY) {
                //noinspection ConstantConditions
                result = codegen.generateNewArray(fakeExpression, referencedFunction.getReturnType(), fakeResolvedCall);
            }
            else {
                result = codegen.generateConstructorCall(fakeResolvedCall, returnType);
            }
        }
        else {
            Call call = CallMaker.makeCall(fakeExpression, null, null, fakeExpression, fakeArguments);
            result = codegen.invokeFunction(call, fakeResolvedCall, StackValue.none());
        }

        InstructionAdapter v = codegen.v;
        result.put(returnType, v);
        v.areturn(returnType);
    }

    private void computeAndSaveArguments(@NotNull List<? extends ValueArgument> fakeArguments, @NotNull ExpressionCodegen codegen) {
        int receivers = (referencedFunction.getDispatchReceiverParameter() != null ? 1 : 0) +
                        (referencedFunction.getExtensionReceiverParameter() != null ? 1 : 0) -
                        (receiverType != null ? 1 : 0);

        List<ValueParameterDescriptor> parameters = CollectionsKt.drop(functionDescriptor.getValueParameters(), receivers);
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameter = parameters.get(i);
            ValueArgument fakeArgument = fakeArguments.get(i);

            Type type = state.getTypeMapper().mapType(parameter);
            int localIndex = codegen.myFrameMap.getIndex(parameter);
            codegen.tempVariables.put(fakeArgument.getArgumentExpression(), StackValue.local(localIndex, type));
        }
    }

    @Nullable
    private ReceiverValue computeAndSaveReceiver(
            @NotNull JvmMethodSignature signature,
            @NotNull ExpressionCodegen codegen,
            @Nullable ReceiverParameterDescriptor receiver
    ) {
        if (receiver == null) return null;

        KtExpression receiverExpression = KtPsiFactoryKt.KtPsiFactory(state.getProject()).createExpression("callableReferenceFakeReceiver");
        codegen.tempVariables.put(receiverExpression, receiverParameterStackValue(signature, codegen));
        return ExpressionReceiver.Companion.create(receiverExpression, receiver.getType(), BindingContext.EMPTY);
    }

    @NotNull
    private StackValue receiverParameterStackValue(@NotNull JvmMethodSignature signature, @NotNull ExpressionCodegen codegen) {
        if (receiverValue != null) return receiverValue;

        if (receiverType != null) {
            ClassDescriptor classDescriptor = (ClassDescriptor) codegen.getContext().getParentContext().getContextDescriptor();
            Type asmType = codegen.getState().getTypeMapper().mapClass(classDescriptor);
            return CallableReferenceUtilKt.capturedBoundReferenceReceiver(asmType, receiverType, isInliningStrategy);
        }

        // 0 is this (the callable reference class), 1 is the invoke() method's first parameter
        return StackValue.local(1, signature.getAsmMethod().getArgumentTypes()[0]);
    }
}
