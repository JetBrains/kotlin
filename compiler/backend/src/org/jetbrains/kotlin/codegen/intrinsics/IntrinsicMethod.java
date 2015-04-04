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
import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.context.CodegenContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.numberFunctionOperandType;

public abstract class IntrinsicMethod implements Callable {
    public StackValue generate(
            @NotNull final ExpressionCodegen codegen,
            @NotNull final Type returnType,
            @Nullable final PsiElement element,
            @NotNull final List<JetExpression> arguments,
            @NotNull final StackValue receiver
    ) {
        return StackValue.operation(returnType, new Function1<InstructionAdapter, Unit>() {
            @Override
            public Unit invoke(InstructionAdapter v) {
                Type actualType = generateImpl(codegen, v, returnType, element, arguments, receiver);
                StackValue.coerce(actualType, returnType, v);
                return Unit.INSTANCE$;
            }
        });
    }

    @NotNull
    protected abstract Type generateImpl(
            @NotNull ExpressionCodegen codegen,
            @NotNull InstructionAdapter v,
            @NotNull Type returnType,
            @Nullable PsiElement element,
            @NotNull List<JetExpression> arguments,
            @NotNull StackValue receiver
    );

    public boolean supportCallable() {
        return false;
    }

    @NotNull
    public ExtendedCallable toCallable(@NotNull FunctionDescriptor fd, boolean isSuper, @NotNull ResolvedCall resolvedCall, @NotNull ExpressionCodegen codegen) {
        return toCallable(codegen.getState(), fd, codegen.getContext(), isSuper, resolvedCall);
    }

    @NotNull
    public ExtendedCallable toCallable(@NotNull GenerationState state, @NotNull FunctionDescriptor fd, @NotNull CodegenContext<?> context, boolean isSuper, @NotNull
            ResolvedCall resolvedCall) {
        return toCallable(state, fd, context, isSuper);
    }

    @NotNull
    public ExtendedCallable toCallable(@NotNull GenerationState state, @NotNull FunctionDescriptor fd, @NotNull CodegenContext<?> context, boolean isSuper) {
        return toCallable(state.getTypeMapper().mapToCallableMethod(fd, false, context), isSuper);
    }

    @NotNull
    public ExtendedCallable toCallable(@NotNull CallableMethod method, boolean isSuperCall) {
        //assert !isSuper;
        return toCallable(method);
    }

    @NotNull
    public ExtendedCallable toCallable(@NotNull CallableMethod method) {
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
