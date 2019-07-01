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
import org.jetbrains.kotlin.codegen.OwnerKind;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;

public class ClosureContext extends ClassContext {
    private final FunctionDescriptor functionDescriptor;
    private final FunctionDescriptor originalSuspendLambdaDescriptor;

    public ClosureContext(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable CodegenContext parentContext,
            @NotNull LocalLookup localLookup,
            // original suspend lambda descriptor
            @Nullable FunctionDescriptor originalSuspendLambdaDescriptor
    ) {
        super(typeMapper,
              getClassForCallable(typeMapper, functionDescriptor, originalSuspendLambdaDescriptor),
              OwnerKind.IMPLEMENTATION, parentContext, localLookup);

        this.functionDescriptor = functionDescriptor;
        this.originalSuspendLambdaDescriptor = originalSuspendLambdaDescriptor;
    }

    @NotNull
    private static ClassDescriptor getClassForCallable(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable FunctionDescriptor originalSuspendLambdaDescriptor
    ) {
        FunctionDescriptor callable = originalSuspendLambdaDescriptor != null ? originalSuspendLambdaDescriptor : functionDescriptor;
        ClassDescriptor classDescriptor = typeMapper.getBindingContext().get(CodegenBinding.CLASS_FOR_CALLABLE, callable);
        if (classDescriptor == null) {
            throw new IllegalStateException("Class for callable is not found: " + functionDescriptor);
        }
        return classDescriptor;
    }

    public ClosureContext(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable CodegenContext parentContext,
            @NotNull LocalLookup localLookup
    ) {
        this(typeMapper, functionDescriptor, parentContext, localLookup, null);
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    @Override
    public String toString() {
        return "Closure: " + getContextDescriptor();
    }

    @Nullable
    public FunctionDescriptor getOriginalSuspendLambdaDescriptor() {
        return originalSuspendLambdaDescriptor;
    }
}
