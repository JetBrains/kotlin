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

package org.jetbrains.jet.codegen.intrinsics;

import com.intellij.psi.PsiElement;
import kotlin.Function1;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.Callable;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

public abstract class IntrinsicMethod implements Callable {
    public final StackValue generate(
            @NotNull final ExpressionCodegen codegen,
            @NotNull final Type returnType,
            @Nullable final PsiElement element,
            @Nullable final List<JetExpression> arguments,
            @Nullable final StackValue receiver
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
            @Nullable List<JetExpression> arguments,
            @Nullable StackValue receiver
    );
}
