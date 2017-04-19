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
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.psi.KtDeclarationWithBody;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.MethodVisitor;

public abstract class FunctionGenerationStrategy {
    public abstract void generateBody(
            @NotNull MethodVisitor mv,
            @NotNull FrameMap frameMap,
            @NotNull JvmMethodSignature signature,
            @NotNull MethodContext context,
            @NotNull MemberCodegen<?> parentCodegen
    );

    public MethodVisitor wrapMethodVisitor(@NotNull MethodVisitor mv, int access, @NotNull String name, @NotNull String desc) {
        return mv;
    }

    public static class FunctionDefault extends CodegenBased {
        private final KtDeclarationWithBody declaration;

        public FunctionDefault(
                @NotNull GenerationState state,
                @NotNull KtDeclarationWithBody declaration
        ) {
            super(state);
            this.declaration = declaration;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            codegen.returnExpression(declaration.getBodyExpression());
        }
    }

    public abstract static class CodegenBased extends FunctionGenerationStrategy {
        protected final GenerationState state;

        public CodegenBased(@NotNull GenerationState state) {
            this.state = state;
        }

        @Override
        public final void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull FrameMap frameMap,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context,
                @NotNull MemberCodegen<?> parentCodegen
        ) {
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), context, state, parentCodegen);
            doGenerateBody(codegen, signature);
        }

        public abstract void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature);
    }
}
