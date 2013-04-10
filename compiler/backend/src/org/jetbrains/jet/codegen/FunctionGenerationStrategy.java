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
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetDeclarationWithBody;

import java.util.ArrayList;
import java.util.Collection;

public abstract class FunctionGenerationStrategy {
    private final Collection<String> localVariableNames = new ArrayList<String>();

    public abstract void generateBody(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature signature,
            @NotNull MethodContext context,
            @NotNull FrameMap frameMap
    );

    protected void addLocalVariableName(@NotNull String name) {
        localVariableNames.add(name);
    }

    @NotNull
    public Collection<String> getLocalVariableNames() {
        return localVariableNames;
    }


    public static class Default extends FunctionGenerationStrategy {
        private final GenerationState state;
        private final JetDeclarationWithBody declaration;

        public Default(@NotNull GenerationState state, @NotNull JetDeclarationWithBody declaration) {
            this.state = state;
            this.declaration = declaration;
        }

        @Override
        public void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context,
                @NotNull FrameMap frameMap
        ) {
            ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getAsmMethod().getReturnType(), context, state);

            codegen.returnExpression(declaration.getBodyExpression());

            for (String name : codegen.getLocalVariableNamesForExpression()) {
                addLocalVariableName(name);
            }
        }
    }
}
