/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cfg.TailRecursionKind;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;
import org.jetbrains.kotlin.psi.ValueArgument;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.*;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.resolve.BindingContext.TAIL_RECURSION_CALL;

public class TailRecursionCodegen {

    @NotNull
    private final MethodContext context;
    @NotNull
    private final ExpressionCodegen codegen;
    @NotNull
    private final InstructionAdapter v;
    @NotNull
    private final GenerationState state;

    public TailRecursionCodegen(
            @NotNull MethodContext context,
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull GenerationState state
    ) {
        this.context = context;
        this.codegen = codegen;
        this.v = v;
        this.state = state;
    }

    public boolean isTailRecursion(@NotNull ResolvedCall<?> resolvedCall) {
        TailRecursionKind status = state.getBindingContext().get(TAIL_RECURSION_CALL, resolvedCall.getCall());
        return status != null && status.isDoGenerateTailRecursion();
    }

    public void generateTailRecursion(ResolvedCall<?> resolvedCall) {
        CallableDescriptor fd = CoroutineCodegenUtilKt.unwrapInitialDescriptorForSuspendFunction(resolvedCall.getResultingDescriptor());
        assert fd instanceof FunctionDescriptor : "Resolved call doesn't refer to the function descriptor: " + fd;
        CallableMethod callable = (CallableMethod) codegen.resolveToCallable((FunctionDescriptor) fd, false, resolvedCall);

        List<ResolvedValueArgument> arguments = resolvedCall.getValueArgumentsByIndex();
        if (arguments == null) {
            throw new IllegalStateException("Failed to arrange value arguments by index: " + fd);
        }

        if (((FunctionDescriptor) fd).isSuspend()) {
            AsmUtil.pop(v, callable.getValueParameters().get(callable.getValueParameters().size() - 1).getAsmType());
        }

        assignParameterValues(fd, callable, arguments);
        if (callable.getExtensionReceiverType() != null) {
            if (resolvedCall.getExtensionReceiver() != fd.getExtensionReceiverParameter().getValue()) {
                StackValue expression = context.getReceiverExpression(codegen.typeMapper);
                expression.store(StackValue.onStack(callable.getExtensionReceiverType()), v, true);
            }
            else {
                AsmUtil.pop(v, callable.getExtensionReceiverType());
            }
        }

        if (callable.getDispatchReceiverType() != null) {
            AsmUtil.pop(v, callable.getDispatchReceiverType());
        }

        v.goTo(context.getMethodStartLabel());
    }

    private void assignParameterValues(
            CallableDescriptor fd,
            CallableMethod callableMethod,
            List<ResolvedValueArgument> valueArguments
    ) {
        List<Type> types = callableMethod.getValueParameterTypes();
        for (ValueParameterDescriptor parameterDescriptor : Lists.reverse(fd.getValueParameters())) {
            ResolvedValueArgument arg = valueArguments.get(parameterDescriptor.getIndex());
            Type type = types.get(parameterDescriptor.getIndex());

            if (arg instanceof ExpressionValueArgument) {
                ExpressionValueArgument ev = (ExpressionValueArgument) arg;
                ValueArgument argument = ev.getValueArgument();
                KtExpression argumentExpression = argument == null ? null : argument.getArgumentExpression();

                if (argumentExpression instanceof KtSimpleNameExpression) {
                    ResolvedCall<?> resolvedCall = CallUtilKt.getResolvedCall(argumentExpression, state.getBindingContext());
                    if (resolvedCall != null && resolvedCall.getResultingDescriptor().equals(parameterDescriptor.getOriginal())) {
                        // do nothing: we shouldn't store argument to itself again
                        AsmUtil.pop(v, type);
                        continue;
                    }
                }
                //assign the parameter below
            }
            else if (arg instanceof DefaultValueArgument) {
                AsmUtil.pop(v, type);
                DefaultParameterValueLoader.DEFAULT.genValue(parameterDescriptor, codegen).put(type, v);
            }
            else if (arg instanceof VarargValueArgument) {
                // assign the parameter below
            }
            else {
                throw new UnsupportedOperationException("Unknown argument type: " + arg + " in " + fd);
            }

            store(parameterDescriptor, type);
        }
    }

    private void store(ValueParameterDescriptor parameterDescriptor, Type type) {
        int index = getParameterVariableIndex(parameterDescriptor);
        v.store(index, type);
    }

    private int getParameterVariableIndex(ValueParameterDescriptor parameterDescriptor) {
        int index = codegen.lookupLocalIndex(parameterDescriptor);
        if (index == -1) {
            // in the case of a generic function recursively calling itself, the parameters on the call site are substituted
            index = codegen.lookupLocalIndex(parameterDescriptor.getOriginal());
        }

        if (index == -1) {
            throw new IllegalStateException("Failed to obtain parameter index: " + parameterDescriptor);
        }

        return index;
    }
}
