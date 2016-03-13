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
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;

import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.anonymousClassForCallable;

public class ClosureContext extends ClassContext {
    private final FunctionDescriptor functionDescriptor;

    public ClosureContext(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable CodegenContext parentContext,
            @NotNull LocalLookup localLookup
    ) {
        super(typeMapper, anonymousClassForCallable(typeMapper.getBindingContext(), functionDescriptor),
              OwnerKind.IMPLEMENTATION, parentContext, localLookup);

        this.functionDescriptor = functionDescriptor;
    }

    @NotNull
    public FunctionDescriptor getFunctionDescriptor() {
        return functionDescriptor;
    }

    @Override
    public String toString() {
        return "Closure: " + getContextDescriptor();
    }
}
