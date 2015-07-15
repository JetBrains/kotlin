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

import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.Call;
import org.jetbrains.kotlin.psi.JetCallExpression;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue.NO_RECEIVER;

public class FunctionReferenceGenerationStrategy extends FunctionGenerationStrategy.CodegenBased<FunctionDescriptor> {
    private final ResolvedCall<?> resolvedCall;
    private final FunctionDescriptor referencedFunction;

    public FunctionReferenceGenerationStrategy(
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ResolvedCall<?> resolvedCall
    ) {
        super(state, functionDescriptor);
        this.resolvedCall = resolvedCall;
        this.referencedFunction = (FunctionDescriptor) resolvedCall.getResultingDescriptor();
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

        JetCallExpression fakeExpression = constructFakeFunctionCall();
        final List<? extends ValueArgument> fakeArguments = fakeExpression.getValueArguments();

        final ReceiverValue dispatchReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getDispatchReceiverParameter());
        final ReceiverValue extensionReceiver = computeAndSaveReceiver(signature, codegen, referencedFunction.getExtensionReceiverParameter());
        computeAndSaveArguments(fakeArguments, codegen);

        ResolvedCall<CallableDescriptor> fakeResolvedCall = new DelegatingResolvedCall<CallableDescriptor>(resolvedCall) {
            @NotNull
            @Override
            public ReceiverValue getExtensionReceiver() {
                return extensionReceiver;
            }

            @NotNull
            @Override
            public ReceiverValue getDispatchReceiver() {
                return dispatchReceiver;
            }

            @NotNull
            @Override
            public List<ResolvedValueArgument> getValueArgumentsByIndex() {
                List<ResolvedValueArgument> result = new ArrayList<ResolvedValueArgument>(fakeArguments.size());
                for (ValueArgument argument : fakeArguments) {
                    result.add(new ExpressionValueArgument(argument));
                }
                return result;
            }
        };

        StackValue result;
        Type returnType = codegen.getReturnType();
        if (referencedFunction instanceof ConstructorDescriptor) {
            if (returnType.getSort() == Type.ARRAY) {
                //noinspection ConstantConditions
                result = codegen.generateNewArray(fakeExpression, referencedFunction.getReturnType());
            }
            else {
                result = codegen.generateConstructorCall(fakeResolvedCall, returnType);
            }
        }
        else {
            Call call = CallMaker.makeCall(fakeExpression, NO_RECEIVER, null, fakeExpression, fakeArguments);
            result = codegen.invokeFunction(call, fakeResolvedCall, StackValue.none());
        }

        InstructionAdapter v = codegen.v;
        result.put(returnType, v);
        v.areturn(returnType);
    }

    @NotNull
    private JetCallExpression constructFakeFunctionCall() {
        StringBuilder fakeFunctionCall = new StringBuilder("callableReferenceFakeCall(");
        for (Iterator<ValueParameterDescriptor> iterator = referencedFunction.getValueParameters().iterator(); iterator.hasNext(); ) {
            ValueParameterDescriptor descriptor = iterator.next();
            fakeFunctionCall.append("p").append(descriptor.getIndex());
            if (iterator.hasNext()) {
                fakeFunctionCall.append(", ");
            }
        }
        fakeFunctionCall.append(")");
        return (JetCallExpression) JetPsiFactory(state.getProject()).createExpression(fakeFunctionCall.toString());
    }

    private void computeAndSaveArguments(@NotNull List<? extends ValueArgument> fakeArguments, @NotNull ExpressionCodegen codegen) {
        int receivers = (referencedFunction.getDispatchReceiverParameter() != null ? 1 : 0) +
                        (referencedFunction.getExtensionReceiverParameter() != null ? 1 : 0);

        List<ValueParameterDescriptor> parameters = KotlinPackage.drop(callableDescriptor.getValueParameters(), receivers);
        for (int i = 0; i < parameters.size(); i++) {
            ValueParameterDescriptor parameter = parameters.get(i);
            ValueArgument fakeArgument = fakeArguments.get(i);

            Type type = state.getTypeMapper().mapType(parameter);
            int localIndex = codegen.myFrameMap.getIndex(parameter);
            codegen.tempVariables.put(fakeArgument.getArgumentExpression(), StackValue.local(localIndex, type));
        }
    }

    @NotNull
    private ReceiverValue computeAndSaveReceiver(
            @NotNull JvmMethodSignature signature,
            @NotNull ExpressionCodegen codegen,
            @Nullable ReceiverParameterDescriptor receiver
    ) {
        if (receiver == null) return NO_RECEIVER;

        JetExpression receiverExpression = JetPsiFactory(state.getProject()).createExpression("callableReferenceFakeReceiver");
        codegen.tempVariables.put(receiverExpression, receiverParameterStackValue(signature));
        return new ExpressionReceiver(receiverExpression, receiver.getType());
    }

    @NotNull
    private static StackValue.Local receiverParameterStackValue(@NotNull JvmMethodSignature signature) {
        // 0 is this (the callable reference class), 1 is the invoke() method's first parameter
        return StackValue.local(1, signature.getAsmMethod().getArgumentTypes()[0]);
    }
}
