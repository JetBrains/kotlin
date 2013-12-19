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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;

import java.util.ArrayList;
import java.util.Collection;

public abstract class FunctionGenerationStrategy {

    private FrameMap frameMap;

    public abstract void generateBody(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature signature,
            @NotNull MethodContext context,
            @Nullable MemberCodegen parentCodegen
    );

    @NotNull
    protected FrameMap createFrameMap(@NotNull JetTypeMapper typeMapper, @NotNull MethodContext context) {
        return context.prepareFrame(typeMapper);
    }

    @NotNull
    public FrameMap getFrameMap(@NotNull JetTypeMapper typeMapper, @NotNull MethodContext context) {
        if (frameMap == null) {
            frameMap = createFrameMap(typeMapper, context);
        }
        return frameMap;
    }

    public static class FunctionDefault extends CodegenBased<CallableDescriptor> {
        private final JetDeclarationWithBody declaration;

        public FunctionDefault(
                @NotNull GenerationState state,
                @NotNull CallableDescriptor descriptor,
                @NotNull JetDeclarationWithBody declaration
        ) {
            super(state, descriptor);
            this.declaration = declaration;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            codegen.returnExpression(declaration.getBodyExpression());
        }
    }

    public abstract static class CodegenBased<T extends CallableDescriptor> extends FunctionGenerationStrategy {
        protected final GenerationState state;
        protected final T callableDescriptor;

        public CodegenBased(@NotNull GenerationState state, @NotNull T callableDescriptor) {
            this.state = state;
            this.callableDescriptor = callableDescriptor;
        }

        @Override
        public final void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context,
                @Nullable MemberCodegen parentCodegen
        ) {
            ExpressionCodegen codegen = new ExpressionCodegen(mv, getFrameMap(state.getTypeMapper(), context),
                                                              signature.getAsmMethod().getReturnType(), context, state, parentCodegen);
            doGenerateBody(codegen, signature);
        }

        public abstract void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature);
    }
}
