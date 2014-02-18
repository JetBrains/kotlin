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
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.ScriptContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class ScriptCodegen extends MemberCodegen {

    public static ScriptCodegen createScriptCodegen(
            @NotNull JetScript declaration,
            @NotNull GenerationState state,
            @NotNull CodegenContext parentContext
    ) {
        ScriptDescriptor scriptDescriptor = state.getBindingContext().get(BindingContext.SCRIPT, declaration);
        assert scriptDescriptor != null;

        ClassDescriptor classDescriptorForScript = state.getBindingContext().get(CLASS_FOR_SCRIPT, scriptDescriptor);
        assert classDescriptorForScript != null;

        Type className = state.getBindingContext().get(ASM_TYPE, classDescriptorForScript);
        assert className != null;

        ClassBuilder builder = state.getFactory().newVisitor(className, declaration.getContainingFile());
        ScriptContext scriptContext = parentContext.intoScript(scriptDescriptor, classDescriptorForScript);
        return new ScriptCodegen(declaration, state, scriptContext, state.getEarlierScriptsForReplInterpreter(), builder);
    }

    @NotNull
    private JetScript scriptDeclaration;

    @NotNull
    private final ScriptContext context;

    @NotNull
    private List<ScriptDescriptor> earlierScripts;

    private ScriptCodegen(
            @NotNull JetScript scriptDeclaration,
            @NotNull GenerationState state,
            @NotNull ScriptContext context,
            @Nullable List<ScriptDescriptor> earlierScripts,
            @NotNull ClassBuilder builder
    ) {
        super(state, null, context, builder);
        this.scriptDeclaration = scriptDeclaration;
        this.context = context;
        this.earlierScripts = earlierScripts == null ? Collections.<ScriptDescriptor>emptyList() : earlierScripts;
    }

    public void generate() {
        ScriptDescriptor scriptDescriptor = context.getScriptDescriptor();
        ClassDescriptor classDescriptorForScript = context.getContextDescriptor();
        Type classType = bindingContext.get(ASM_TYPE, classDescriptorForScript);
        assert classType != null;

        ClassBuilder classBuilder = getBuilder();
        classBuilder.defineClass(scriptDeclaration,
                                 V1_6,
                                 ACC_PUBLIC,
                                 classType.getInternalName(),
                                 null,
                                 "java/lang/Object",
                                 new String[0]);

        genMembers(context, classBuilder);
        genFieldsForParameters(scriptDescriptor, classBuilder);
        genConstructor(scriptDescriptor, classDescriptorForScript, classBuilder,
                       context.intoFunction(scriptDescriptor.getScriptCodeDescriptor()));

        classBuilder.done();
    }

    private void genConstructor(
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ClassDescriptor classDescriptorForScript,
            @NotNull ClassBuilder classBuilder,
            @NotNull MethodContext context
    ) {

        Type blockType = typeMapper.mapType(scriptDescriptor.getReturnType());

        classBuilder.newField(null, ACC_PUBLIC | ACC_FINAL, ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME,
                              blockType.getDescriptor(), null, null);

        JvmMethodSignature jvmSignature = typeMapper.mapScriptSignature(scriptDescriptor, earlierScripts);

        MethodVisitor mv = classBuilder.newMethod(
                scriptDeclaration, ACC_PUBLIC, jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor(),
                null, null);

        mv.visitCode();

        InstructionAdapter instructionAdapter = new InstructionAdapter(mv);

        Type classType = bindingContext.get(ASM_TYPE, classDescriptorForScript);
        assert classType != null;

        instructionAdapter.load(0, classType);
        instructionAdapter.invokespecial("java/lang/Object", "<init>", "()V");

        instructionAdapter.load(0, classType);

        FrameMap frameMap = context.prepareFrame(typeMapper);

        for (ScriptDescriptor importedScript : earlierScripts) {
            frameMap.enter(importedScript, OBJECT_TYPE);
        }

        Type[] argTypes = jvmSignature.getAsmMethod().getArgumentTypes();
        int add = 0;

        for (int i = 0; i < scriptDescriptor.getValueParameters().size(); i++) {
            ValueParameterDescriptor parameter = scriptDescriptor.getValueParameters().get(i);
            frameMap.enter(parameter, argTypes[i + add]);
        }

        ImplementationBodyCodegen.generateInitializers(
                new ExpressionCodegen(instructionAdapter, frameMap, Type.VOID_TYPE, context, state, this),
                scriptDeclaration.getDeclarations(),
                bindingContext,
                state);

        int offset = 1;

        for (ScriptDescriptor earlierScript : earlierScripts) {
            Type earlierClassType = asmTypeForScriptDescriptor(bindingContext, earlierScript);
            instructionAdapter.load(0, classType);
            instructionAdapter.load(offset, earlierClassType);
            offset += earlierClassType.getSize();
            instructionAdapter.putfield(classType.getInternalName(), getScriptFieldName(earlierScript), earlierClassType.getDescriptor());
        }

        for (ValueParameterDescriptor parameter : scriptDescriptor.getValueParameters()) {
            Type parameterType = typeMapper.mapType(parameter.getType());
            instructionAdapter.load(0, classType);
            instructionAdapter.load(offset, parameterType);
            offset += parameterType.getSize();
            instructionAdapter.putfield(classType.getInternalName(), parameter.getName().getIdentifier(), parameterType.getDescriptor());
        }

        StackValue stackValue =
                new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state, this).gen(scriptDeclaration.getBlockExpression());
        if (stackValue.type != Type.VOID_TYPE) {
            stackValue.put(stackValue.type, instructionAdapter);
            instructionAdapter.putfield(classType.getInternalName(), ScriptDescriptor.LAST_EXPRESSION_VALUE_FIELD_NAME,
                                        blockType.getDescriptor());
        }

        instructionAdapter.areturn(Type.VOID_TYPE);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void genFieldsForParameters(@NotNull ScriptDescriptor script, @NotNull ClassBuilder classBuilder) {
        for (ScriptDescriptor earlierScript : earlierScripts) {
            Type earlierClassName = asmTypeForScriptDescriptor(bindingContext, earlierScript);
            int access = ACC_PRIVATE | ACC_FINAL;
            classBuilder.newField(null, access, getScriptFieldName(earlierScript), earlierClassName.getDescriptor(), null, null);
        }

        for (ValueParameterDescriptor parameter : script.getValueParameters()) {
            Type parameterType = typeMapper.mapType(parameter);
            int access = ACC_PUBLIC | ACC_FINAL;
            classBuilder.newField(null, access, parameter.getName().getIdentifier(), parameterType.getDescriptor(), null, null);
        }
    }

    private void genMembers(@NotNull FieldOwnerContext context, @NotNull ClassBuilder classBuilder) {
        for (JetDeclaration decl : scriptDeclaration.getDeclarations()) {
            genFunctionOrProperty(context, (JetTypeParameterListOwner) decl, classBuilder);
        }
    }

    private int getScriptIndex(@NotNull ScriptDescriptor scriptDescriptor) {
        int index = earlierScripts.indexOf(scriptDescriptor);
        if (index < 0) {
            throw new IllegalStateException("Unregistered script: " + scriptDescriptor);
        }
        return index + 1;
    }

    public String getScriptFieldName(@NotNull ScriptDescriptor scriptDescriptor) {
        return "script$" + getScriptIndex(scriptDescriptor);
    }
}
