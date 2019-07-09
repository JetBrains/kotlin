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

package org.jetbrains.kotlin.codegen.intrinsics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.Callable;
import org.jetbrains.kotlin.codegen.CallableMethod;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;

public abstract class IntrinsicMethod {
    @NotNull
    public Callable toCallable(
            @NotNull FunctionDescriptor fd,
            boolean isSuper,
            @NotNull ResolvedCall resolvedCall,
            @NotNull ExpressionCodegen codegen
    ) {
        return toCallable(codegen.getState().getTypeMapper().mapToCallableMethod(fd, false), isSuper, resolvedCall);
    }

    public boolean isApplicableToOverload(@NotNull CallableMemberDescriptor descriptor) {
        return true;
    }

    @NotNull
    protected Callable toCallable(@NotNull CallableMethod method, boolean isSuper, @NotNull ResolvedCall resolvedCall) {
        return toCallable(method, isSuper);
    }

    @NotNull
    protected Callable toCallable(@NotNull CallableMethod method, boolean isSuperCall) {
        return toCallable(method);
    }

    @NotNull
    protected Callable toCallable(@NotNull CallableMethod method) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Type nullOrObject(Type type) {
        return nullOr(type, AsmTypes.OBJECT_TYPE);
    }

    public Type nullOr(Type type, Type newType) {
        return type == null ? null : newType;
    }
}
