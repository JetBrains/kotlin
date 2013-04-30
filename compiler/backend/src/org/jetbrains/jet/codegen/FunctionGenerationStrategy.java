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
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;

import java.util.ArrayList;
import java.util.Collection;

public abstract class FunctionGenerationStrategy<T extends CallableDescriptor> {

    private final Collection<String> localVariableNames = new ArrayList<String>();

    private FrameMap frameMap;

    public abstract void generateBody(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature signature,
            @NotNull MethodContext context
    );

    protected void addLocalVariableName(@NotNull String name) {
        localVariableNames.add(name);
    }

    @NotNull
    public Collection<String> getLocalVariableNames() {
        return localVariableNames;
    }

    @NotNull
    protected FrameMap createFrameMap(@NotNull JetTypeMapper typeMapper, @NotNull CodegenContext context) {
        return context.prepareFrame(typeMapper);
    }

    @NotNull
    public FrameMap getFrameMap(@NotNull JetTypeMapper typeMapper, @NotNull CodegenContext context) {
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
        public void doGenerateBody(ExpressionCodegen codegen, JvmMethodSignature signature) {
            codegen.returnExpression(declaration.getBodyExpression());
        }
    }

    public abstract static class CodegenBased<T extends CallableDescriptor> extends FunctionGenerationStrategy<T> {

        private final GenerationState state;

        protected final T callableDescriptor;

        public CodegenBased(@NotNull GenerationState state, T callableDescriptor) {
            this.state = state;
            this.callableDescriptor = callableDescriptor;
        }

        @Override
        public void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context
        ) {
            ExpressionCodegen codegen = initializeExpressionCodegen(signature, context, mv, signature.getAsmMethod().getReturnType());
            doGenerateBody(codegen, signature);
            generateLocalVarNames(codegen);
        }

        abstract public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature);

        @NotNull
        public ExpressionCodegen initializeExpressionCodegen(
                JvmMethodSignature signature,
                MethodContext context,
                MethodVisitor mv,
                Type returnType
        ) {
            return new ExpressionCodegen(mv, getFrameMap(state.getTypeMapper(), context), returnType, context, state);
        }

        public void generateLocalVarNames(@NotNull ExpressionCodegen codegen) {
            for (String name : codegen.getLocalVariableNamesForExpression()) {
                addLocalVariableName(name);
            }
        }
    }
}
