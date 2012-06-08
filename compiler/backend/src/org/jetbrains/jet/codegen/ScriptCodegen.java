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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.psi.JetTypeParameterListOwner;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JdkNames;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class ScriptCodegen {

    public static final JvmClassName SCRIPT_DEFAULT_CLASS_NAME = JvmClassName.byInternalName("Script");

    public static final String LAST_EXPRESSION_VALUE_FIELD_NAME = "rv";

    @NotNull
    private GenerationState state;
    @NotNull
    private ClassFileFactory classFileFactory;
    @NotNull
    private JetTypeMapper jetTypeMapper;
    @NotNull
    private MemberCodegen memberCodegen;
    @NotNull
    private ClosureAnnotator closureAnnotator;
    @NotNull
    private BindingContext bindingContext;

    private List<ScriptDescriptor> earlierScripts;


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

    @Inject
    public void setMemberCodegen(@NotNull MemberCodegen memberCodegen) {
        this.memberCodegen = memberCodegen;
    }

    @Inject
    public void setClosureAnnotator(@NotNull ClosureAnnotator closureAnnotator) {
        this.closureAnnotator = closureAnnotator;
    }

    @Inject
    public void setBindingContext(@NotNull BindingContext bindingContext) {
        this.bindingContext = bindingContext;
    }



    public void generate(JetScript scriptDeclaration) {

        ScriptDescriptor scriptDescriptor = (ScriptDescriptor) state.getBindingContext().get(BindingContext.SCRIPT, scriptDeclaration);

        ClassDescriptor classDescriptorForScript = closureAnnotator.classDescriptorForScrpitDescriptor(scriptDescriptor);

        CodegenContexts.ScriptContext context = (CodegenContexts.ScriptContext) CodegenContexts.STATIC.intoScript(scriptDescriptor, classDescriptorForScript);

        JvmClassName className = closureAnnotator.classNameForClassDescriptor(classDescriptorForScript);

        ClassBuilder classBuilder = classFileFactory.newVisitor(className.getInternalName() + ".class");
        classBuilder.defineClass(scriptDeclaration,
                Opcodes.V1_6,
                Opcodes.ACC_PUBLIC,
                className.getInternalName(),
                null,
                JdkNames.JL_OBJECT.getInternalName(),
                new String[0]);

        genMembers(scriptDeclaration, context, classBuilder);
        genFieldsForParameters(scriptDescriptor, classBuilder);
        genConstructor(scriptDeclaration, scriptDescriptor, classDescriptorForScript, classBuilder,
                context.intoFunction(scriptDescriptor.getScriptCodeDescriptor()), earlierScripts);

        classBuilder.done();
    }

    private void genConstructor(
            @NotNull JetScript scriptDeclaration,
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull ClassDescriptor classDescriptorForScript,
            @NotNull ClassBuilder classBuilder,
            @NotNull CodegenContext context,
            @NotNull List<ScriptDescriptor> importedScripts) {

        Type blockType = jetTypeMapper.mapType(scriptDescriptor.getReturnType(), MapTypeMode.VALUE);

        classBuilder.newField(null, Opcodes.ACC_PUBLIC, LAST_EXPRESSION_VALUE_FIELD_NAME, blockType.getDescriptor(), null, null);

        JvmMethodSignature jvmSignature = jetTypeMapper.mapScriptSignature(scriptDescriptor, importedScripts);

        state.setScriptConstructorMethod(jvmSignature.getAsmMethod());

        MethodVisitor mv = classBuilder.newMethod(
                scriptDeclaration, Opcodes.ACC_PUBLIC, jvmSignature.getAsmMethod().getName(), jvmSignature.getAsmMethod().getDescriptor(), null, null);

        mv.visitCode();

        InstructionAdapter instructionAdapter = new InstructionAdapter(mv);

        JvmClassName className = closureAnnotator.classNameForClassDescriptor(classDescriptorForScript);

        instructionAdapter.load(0, className.getAsmType());
        instructionAdapter.invokespecial(JdkNames.JL_OBJECT.getInternalName(), "<init>", "()V");

        instructionAdapter.load(0, className.getAsmType());

        FrameMap frameMap = context.prepareFrame(jetTypeMapper);

        for (ScriptDescriptor importedScript : importedScripts) {
            frameMap.enter(importedScript, 1);
        }

        Type[] argTypes = jvmSignature.getAsmMethod().getArgumentTypes();
        int add = 0;

        for (int i = 0; i < scriptDescriptor.getValueParameters().size(); i++) {
            ValueParameterDescriptor parameter = scriptDescriptor.getValueParameters().get(i);
            frameMap.enter(parameter, argTypes[i+add].getSize());
        }

        ImplementationBodyCodegen.generateInitializers(
                new ExpressionCodegen(instructionAdapter, frameMap, Type.VOID_TYPE, context, state),
                instructionAdapter,
                scriptDeclaration.getDeclarations(),
                bindingContext,
                jetTypeMapper);

        int offset = 1;

        for (ScriptDescriptor earlierScript : importedScripts) {
            JvmClassName earlierClassName = closureAnnotator.classNameForScriptDescriptor(earlierScript);
            instructionAdapter.load(0, className.getAsmType());
            instructionAdapter.load(offset, earlierClassName.getAsmType());
            offset += earlierClassName.getAsmType().getSize();
            instructionAdapter.putfield(className.getInternalName(), getScriptFieldName(earlierScript), earlierClassName.getAsmType().getDescriptor());
        }

        for (ValueParameterDescriptor parameter : scriptDescriptor.getValueParameters()) {
            Type parameterType = jetTypeMapper.mapType(parameter.getType(), MapTypeMode.VALUE);
            instructionAdapter.load(0, className.getAsmType());
            instructionAdapter.load(offset, parameterType);
            offset += parameterType.getSize();
            instructionAdapter.putfield(className.getInternalName(), parameter.getName().getIdentifier(), parameterType.getDescriptor());
        }

        StackValue stackValue = new ExpressionCodegen(mv, frameMap, Type.VOID_TYPE, context, state).gen(scriptDeclaration.getBlockExpression());
        if (stackValue.type != Type.VOID_TYPE) {
            stackValue.put(stackValue.type, instructionAdapter);
            instructionAdapter.putfield(className.getInternalName(), LAST_EXPRESSION_VALUE_FIELD_NAME, blockType.getDescriptor());
        }

        instructionAdapter.areturn(Type.VOID_TYPE);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void genFieldsForParameters(@NotNull ScriptDescriptor script, @NotNull ClassBuilder classBuilder) {
        for (ScriptDescriptor earlierScript : earlierScripts) {
            JvmClassName earlierClassName = closureAnnotator.classNameForScriptDescriptor(earlierScript);
            int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
            classBuilder.newField(null, access, getScriptFieldName(earlierScript), earlierClassName.getDescriptor(), null, null);
        }

        for (ValueParameterDescriptor parameter : script.getValueParameters()) {
            Type parameterType = jetTypeMapper.mapType(parameter.getType(), MapTypeMode.VALUE);
            int access = Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL;
            classBuilder.newField(null, access, parameter.getName().getIdentifier(), parameterType.getDescriptor(), null, null);
        }
    }

    private void genMembers(@NotNull JetScript scriptDeclaration, @NotNull CodegenContext context, @NotNull ClassBuilder classBuilder) {
        for (JetDeclaration decl : scriptDeclaration.getDeclarations()) {
            memberCodegen.generateFunctionOrProperty((JetTypeParameterListOwner) decl, context, classBuilder);
        }
    }

    public void registerEarlierScripts(List<Pair<ScriptDescriptor, JvmClassName>> earlierScripts) {
        for (Pair<ScriptDescriptor, JvmClassName> t : earlierScripts) {
            ScriptDescriptor earlierDescriptor = t.first;
            JvmClassName earlierClassName = t.second;

            closureAnnotator.registerClassNameForScript(earlierDescriptor, earlierClassName);
        }

        List<ScriptDescriptor> earlierScriptDescriptors = Lists.newArrayList();
        for (Pair<ScriptDescriptor, JvmClassName> t : earlierScripts) {
            ScriptDescriptor earlierDescriptor = t.first;
            JvmClassName earlierClassName = t.second;
            earlierScriptDescriptors.add(earlierDescriptor);
        }
        this.earlierScripts = earlierScriptDescriptors;
    }

    public int getScriptIndex(@NotNull ScriptDescriptor scriptDescriptor) {
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
