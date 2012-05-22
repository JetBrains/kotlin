/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JdkNames;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;
import sun.tools.tree.InlineReturnStatement;

import javax.inject.Inject;

/**
 * @author Stepan Koltsov
 */
public class ScriptCodegen {

    public static final String LAST_EXPRESSION_VALUE_FIELD_NAME = "rv";

    @NotNull
    private GenerationState state;
    @NotNull
    private ClassFileFactory classFileFactory;
    @NotNull
    private JetTypeMapper jetTypeMapper;


    @Inject
    public void setState(@NotNull GenerationState state) {
        this.state = state;
    }

    @Inject
    public void setClassFileFactory(@NotNull ClassFileFactory classFileFactory) {
        this.classFileFactory = classFileFactory;
    }

    @Inject
    public void setJetTypeMapper(@NotNull JetTypeMapper jetTypeMapper) {
        this.jetTypeMapper = jetTypeMapper;
    }



    public void generate(CodegenContext context, JetScript scriptDeclaration) {
        ScriptDescriptor scriptDescriptor = (ScriptDescriptor) state.getBindingContext().get(BindingContext.SCRIPT, scriptDeclaration);
        ClassBuilder classBuilder = classFileFactory.newVisitor("Script.class");
        classBuilder.defineClass(scriptDeclaration,
                Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                "Script",
                null,
                JdkNames.JL_OBJECT.getInternalName(),
                new String[0]);

        Type blockType = jetTypeMapper.mapType(scriptDescriptor.getReturnType(), MapTypeMode.VALUE);

        classBuilder.newField(null, Opcodes.ACC_PUBLIC, LAST_EXPRESSION_VALUE_FIELD_NAME, blockType.getDescriptor(), null, null);

        MethodVisitor mv = classBuilder.newMethod(scriptDeclaration, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);

        mv.visitCode();

        InstructionAdapter instructionAdapter = new InstructionAdapter(mv);

        instructionAdapter.load(0, Type.getObjectType("Script"));
        instructionAdapter.invokespecial(JdkNames.JL_OBJECT.getInternalName(), "<init>", "()V");

        instructionAdapter.load(0, Type.getObjectType("Script"));
        StackValue stackValue = new ExpressionCodegen(mv, new FrameMap(), Type.VOID_TYPE, context, state).gen(scriptDeclaration.getBlockExpression());
        if (stackValue.type != Type.VOID_TYPE) {
            instructionAdapter.putfield("Script", LAST_EXPRESSION_VALUE_FIELD_NAME, blockType.getDescriptor());
        }

        instructionAdapter.areturn(Type.VOID_TYPE);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();

        classBuilder.done();
    }
}
