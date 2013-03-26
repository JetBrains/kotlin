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

package org.jetbrains.jet.codegen.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.OwnerKind;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;

import static org.jetbrains.jet.codegen.binding.CodegenBinding.CLOSURE;

class ClosureContext extends CodegenContext {
    private final ClassDescriptor classDescriptor;

    public ClosureContext(
            @NotNull JetTypeMapper typeMapper,
            @NotNull FunctionDescriptor contextDescriptor,
            @NotNull ClassDescriptor classDescriptor,
            @Nullable CodegenContext parentContext,
            @Nullable LocalLookup localLookup
    ) {
        //noinspection SuspiciousMethodCalls
        super(contextDescriptor, OwnerKind.IMPLEMENTATION, parentContext,
              typeMapper.getBindingContext().get(CLOSURE, classDescriptor), classDescriptor, localLookup);
        this.classDescriptor = classDescriptor;

        initOuterExpression(typeMapper, classDescriptor);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContextDescriptor() {
        return classDescriptor;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    @Override
    public String toString() {
        return "Closure: " + classDescriptor;
    }
}
