/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.pushDefaultValueOnStack;

class ObjectSuperCallArgumentGenerator extends ArgumentGenerator {
    private final List<JvmMethodParameterSignature> parameters;
    private final InstructionAdapter iv;
    private int offset;

    public ObjectSuperCallArgumentGenerator(
            @NotNull List<JvmMethodParameterSignature> superParameters,
            @NotNull InstructionAdapter iv,
            int firstValueParamOffset,
            @NotNull ResolvedCall<ConstructorDescriptor> superConstructorCall
    ) {
        this.parameters = superParameters;
        this.iv = iv;
        this.offset = firstValueParamOffset;
    }

    @Override
    public void generateExpression(int i, @NotNull ExpressionValueArgument argument) {
        generateSuperCallArgument(i);
    }

    @Override
    public void generateDefault(int i, @NotNull DefaultValueArgument argument) {
        Type type = parameters.get(i).getAsmType();
        pushDefaultValueOnStack(type, iv);
    }

    @Override
    public void generateVararg(int i, @NotNull VarargValueArgument argument) {
        generateSuperCallArgument(i);
    }

    private void generateSuperCallArgument(int i) {
        Type type = parameters.get(i).getAsmType();
        iv.load(offset, type);
        offset += type.getSize();
    }

    @Override
    protected void reorderArgumentsIfNeeded(@NotNull List<ArgumentAndDeclIndex> args) {

    }
}
