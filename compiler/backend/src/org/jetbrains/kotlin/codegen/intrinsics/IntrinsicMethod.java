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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.Callable;
import org.jetbrains.kotlin.codegen.CallableMethod;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType;

public abstract class IntrinsicMethod {

    @NotNull
    public Callable toCallable(@NotNull FunctionDescriptor fd, boolean isSuper, @NotNull ResolvedCall resolvedCall, @NotNull ExpressionCodegen codegen) {
        return toCallable(codegen.getState(), fd, codegen.getContext(), isSuper, resolvedCall);
    }

    @NotNull
    public Callable toCallable(@NotNull GenerationState state, @NotNull FunctionDescriptor fd, @NotNull CodegenContext<?> context, boolean isSuper, @NotNull
            ResolvedCall resolvedCall) {
        return toCallable(state, fd, context, isSuper);
    }

    @NotNull
    public Callable toCallable(@NotNull GenerationState state, @NotNull FunctionDescriptor fd, @NotNull CodegenContext<?> context, boolean isSuper) {
        return toCallable(state.getTypeMapper().mapToCallableMethod(fd, false, context), isSuper);
    }

    @NotNull
    public Callable toCallable(@NotNull CallableMethod method, boolean isSuperCall) {
        //assert !isSuper;
        return toCallable(method);
    }

    @NotNull
    public Callable toCallable(@NotNull CallableMethod method) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public List<Type> transformTypes(List<Type> types) {
        return Lists.transform(types, new Function<Type, Type>() {
            @Override
            public Type apply(Type input) {
                return numberFunctionOperandType(input);
            }
        });
    }

    public Type nullOrObject(Type type) {
        return nullOr(type, AsmTypes.OBJECT_TYPE);
    }

    public Type nullOr(Type type, Type newType) {
        return type == null ? null : newType;
    }

}
