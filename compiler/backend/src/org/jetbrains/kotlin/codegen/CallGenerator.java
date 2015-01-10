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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;

public abstract class CallGenerator {

    static class DefaultCallGenerator extends CallGenerator {

        private final ExpressionCodegen codegen;

        public DefaultCallGenerator(ExpressionCodegen codegen) {
            this.codegen = codegen;
        }

        @Override
        public void genCallInner(
                @NotNull CallableMethod callableMethod,
                ResolvedCall<?> resolvedCall,
                boolean callDefault,
                @NotNull ExpressionCodegen codegen
        ) {
            if (!callDefault) {
                callableMethod.invokeWithNotNullAssertion(codegen.v, codegen.getState(), resolvedCall);
            }
            else {
                callableMethod.invokeDefaultWithNotNullAssertion(codegen.v, codegen.getState(), resolvedCall);
            }
        }

        @Override
        public void genCallWithoutAssertions(
                @NotNull CallableMethod method, @NotNull ExpressionCodegen codegen
        ) {
            method.invokeWithoutAssertions(codegen.v);
        }

        @Override
        public void afterParameterPut(@NotNull Type type, StackValue stackValue, @NotNull ValueParameterDescriptor valueParameterDescriptor) {

        }

        @Override
        public void putHiddenParams() {

        }

        @Override
        public void genValueAndPut(
                @NotNull ValueParameterDescriptor valueParameterDescriptor,
                @NotNull JetExpression argumentExpression,
                @NotNull Type parameterType
        ) {
            StackValue value = codegen.gen(argumentExpression);
            value.put(parameterType, codegen.v);
        }

        @Override
        public void putCapturedValueOnStack(
                @NotNull StackValue stackValue, @NotNull Type valueType, int paramIndex
        ) {
            stackValue.put(stackValue.type, codegen.v);
        }

        @Override
        public void putValueIfNeeded(
                @Nullable ValueParameterDescriptor valueParameterDescriptor, @NotNull Type parameterType, @NotNull StackValue value
        ) {
            value.put(value.type, codegen.v);
        }
    }

    public void genCall(@NotNull CallableMethod callableMethod, @Nullable ResolvedCall<?> resolvedCall, boolean callDefault, @NotNull ExpressionCodegen codegen) {
        if (resolvedCall != null) {
            JetExpression calleeExpression = resolvedCall.getCall().getCalleeExpression();
            if (calleeExpression != null) {
                codegen.markStartLineNumber(calleeExpression);
            }
        }

        genCallInner(callableMethod, resolvedCall, callDefault, codegen);
    }

    public abstract void genCallInner(@NotNull CallableMethod callableMethod, @Nullable ResolvedCall<?> resolvedCall, boolean callDefault, @NotNull ExpressionCodegen codegen);

    public abstract void genCallWithoutAssertions(@NotNull CallableMethod callableMethod, @NotNull ExpressionCodegen codegen);

    public abstract void afterParameterPut(@NotNull Type type, StackValue stackValue, @NotNull ValueParameterDescriptor valueParameterDescriptor);

    public abstract void genValueAndPut(
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull JetExpression argumentExpression,
            @NotNull Type parameterType
    );

    public abstract void putValueIfNeeded(@Nullable ValueParameterDescriptor valueParameterDescriptor, @NotNull Type parameterType, @NotNull StackValue value);

    public abstract void putCapturedValueOnStack(
            @NotNull StackValue stackValue,
            @NotNull Type valueType, int paramIndex
    );

    public abstract void putHiddenParams();
}
