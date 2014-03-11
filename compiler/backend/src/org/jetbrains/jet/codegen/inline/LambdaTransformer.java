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

package org.jetbrains.jet.codegen.inline;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.asm4.tree.VarInsnNode;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.ClosureCodegen;
import org.jetbrains.jet.codegen.FieldInfo;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.asm4.Opcodes.ASM4;
import static org.jetbrains.asm4.Opcodes.V1_6;

public class LambdaTransformer {

    protected final GenerationState state;

    protected final JetTypeMapper typeMapper;

    private final MethodNode constructor;

    private final MethodNode invoke;

    private final MethodNode bridge;

    private final InliningContext inliningContext;

    private final Type oldLambdaType;

    private final Type newLambdaType;

    private int classAccess;
    private String signature;
    private String superName;
    private String[] interfaces;
    private final boolean isSameModule;

    public LambdaTransformer(String lambdaInternalName, InliningContext inliningContext, boolean isSameModule, Type newLambdaType) {
        this.isSameModule = isSameModule;
        this.state = inliningContext.state;
        this.typeMapper = state.getTypeMapper();
        this.inliningContext = inliningContext;
        this.oldLambdaType = Type.getObjectType(lambdaInternalName);
        this.newLambdaType = newLambdaType;

        //try to find just compiled classes then in dependencies
        ClassReader reader;
        try {
            OutputFile outputFile = state.getFactory().get(lambdaInternalName + ".class");
            if (outputFile != null) {
                reader = new ClassReader(outputFile.asByteArray());
            } else {
                VirtualFile file = InlineCodegenUtil.findVirtualFile(state.getProject(), lambdaInternalName);
                if (file == null) {
                    throw new RuntimeException("Couldn't find virtual file for " + lambdaInternalName);
                }
                reader = new ClassReader(file.getInputStream());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        //TODO rewrite to one step
        constructor = getMethodNode(reader, true, false);
        invoke = getMethodNode(reader, false, false);
        bridge = getMethodNode(reader, false, true);
    }

    private void buildInvokeParams(ParametersBuilder builder) {
        builder.addThis(oldLambdaType, false);

        Type[] types = Type.getArgumentTypes(invoke.desc);
        for (Type type : types) {
            builder.addNextParameter(type, false, null);
        }
    }

    public InlineResult doTransform(ConstructorInvocation invocation, LambdaFieldRemapper parentRemapper) {
        ClassBuilder classBuilder = createClassBuilder();

        //TODO: public visibility for inline function
        classBuilder.defineClass(null,
                                 V1_6,
                                 classAccess,
                                 newLambdaType.getInternalName(),
                                 signature,
                                 superName,
                                 interfaces
        );

        // TODO: load synthetic class kind from the transformed class and write the same kind to the copy of that class here
        // See AsmUtil.writeKotlinSyntheticClassAnnotation

        ParametersBuilder builder = ParametersBuilder.newBuilder();
        Parameters parameters = getLambdaParameters(builder, invocation);

        MethodVisitor invokeVisitor = newMethod(classBuilder, invoke);
        RegeneratedLambdaFieldRemapper
                remapper = new RegeneratedLambdaFieldRemapper(oldLambdaType.getInternalName(), newLambdaType.getInternalName(), parameters, invocation.getCapturedLambdasToInline(),
                                                              parentRemapper);
        MethodInliner inliner = new MethodInliner(invoke, parameters, inliningContext.subInline(inliningContext.nameGenerator.subGenerator("lambda")), oldLambdaType,
                                                  remapper, isSameModule);
        InlineResult result = inliner.doInline(invokeVisitor, new VarRemapper.ParamRemapper(parameters, 0), remapper, false);
        invokeVisitor.visitMaxs(-1, -1);

        generateConstructorAndFields(classBuilder, builder, invocation);

        if (bridge != null) {
            MethodVisitor invokeBridge = newMethod(classBuilder, bridge);
            bridge.accept(new MethodVisitor(ASM4, invokeBridge) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                    if (owner.equals(oldLambdaType.getInternalName())) {
                        super.visitMethodInsn(opcode, newLambdaType.getInternalName(), name, desc);
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }
                }
            });
        }

        classBuilder.done();

        invocation.setNewLambdaType(newLambdaType);
        return result;
    }

    private void generateConstructorAndFields(@NotNull ClassBuilder classBuilder, @NotNull ParametersBuilder builder, @NotNull ConstructorInvocation invocation) {
        List<CapturedParamInfo> infos = builder.buildCaptured();
        List<Pair<String, Type>> newConstructorSignature = new ArrayList<Pair<String, Type>>();
        for (CapturedParamInfo capturedParamInfo : infos) {
            if (capturedParamInfo.getLambda() == null) { //not inlined
                newConstructorSignature.add(new Pair<String, Type>(capturedParamInfo.getFieldName(), capturedParamInfo.getType()));
            }
        }

        List<FieldInfo> fields = AsmUtil.transformCapturedParams(newConstructorSignature, newLambdaType);

        AsmUtil.genClosureFields(newConstructorSignature, classBuilder);

        //TODO for inline method make public class
        Method newConstructor = ClosureCodegen.generateConstructor(classBuilder, fields, null, Type.getObjectType(superName), state, AsmUtil.NO_FLAG_PACKAGE_PRIVATE);
        invocation.setNewConstructorDescriptor(newConstructor.getDescriptor());
    }

    private Parameters getLambdaParameters(ParametersBuilder builder, ConstructorInvocation invocation) {
        buildInvokeParams(builder);
        extractParametersMapping(constructor, builder, invocation);
        return builder.buildParameters();
    }

    private ClassBuilder createClassBuilder() {
        return new RemappingClassBuilder(state.getFactory().forLambdaInlining(newLambdaType, inliningContext.call.getCallElement().getContainingFile()),
                     new TypeRemapper(inliningContext.typeMapping));
    }

    private static MethodVisitor newMethod(ClassBuilder builder, MethodNode original) {
        return builder.newMethod(
                null,
                original.access,
                original.name,
                original.desc,
                original.signature,
                null //TODO: change signature to list
        );
    }

    private static void extractParametersMapping(MethodNode constructor, ParametersBuilder builder, ConstructorInvocation invocation) {
        Map<Integer, LambdaInfo> indexToLambda = invocation.getLambdasToInline();

        AbstractInsnNode cur = constructor.instructions.getFirst();
        cur = cur.getNext(); //skip super call
        List<LambdaInfo> capturedLambdas = new ArrayList<LambdaInfo>(); //captured var of inlined parameter
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldNode = (FieldInsnNode) cur;
                CapturedParamInfo info = builder.addCapturedParam(fieldNode.name, Type.getType(fieldNode.desc), false, null);

                assert fieldNode.getPrevious() instanceof VarInsnNode : "Previous instruction should be VarInsnNode but was " + fieldNode.getPrevious();
                VarInsnNode previous = (VarInsnNode) fieldNode.getPrevious();
                int varIndex = previous.var;
                LambdaInfo lambdaInfo = indexToLambda.get(varIndex);
                if (lambdaInfo != null) {
                    info.setLambda(lambdaInfo);
                    capturedLambdas.add(lambdaInfo);
                }
            }
            cur = cur.getNext();
        }

        //For all inlined lambdas add their captured parameters
        //TODO: some of such parameters could be skipped - we should perform additional analysis
        Map<String, LambdaInfo> capturedLambdasToInline = new HashMap<String, LambdaInfo>(); //captured var of inlined parameter
        List<CapturedParamInfo> allRecapturedParameters = new ArrayList<CapturedParamInfo>();
        for (LambdaInfo info : capturedLambdas) {
            for (CapturedParamInfo var : info.getCapturedVars()) {
                CapturedParamInfo recapturedParamInfo = builder.addCapturedParam(getNewFieldName(var.getFieldName()), var.getType(), true, var);
                recapturedParamInfo.setRecapturedFrom(info);
                allRecapturedParameters.add(var);
            }
            capturedLambdasToInline.put(info.getLambdaClassType().getInternalName(), info);
        }

        invocation.setAllRecapturedParameters(allRecapturedParameters);
        invocation.setCapturedLambdasToInline(capturedLambdasToInline);
    }

    @Nullable
    public MethodNode getMethodNode(@NotNull ClassReader reader, final boolean findConstructor, final boolean findBridge) {
        final MethodNode[] methodNode = new MethodNode[1];
        reader.accept(new ClassVisitor(InlineCodegenUtil.API) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                LambdaTransformer.this.classAccess = access;
                LambdaTransformer.this.signature = signature;
                LambdaTransformer.this.superName = superName;
                LambdaTransformer.this.interfaces = interfaces;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                boolean isConstructorMethod = "<init>".equals(name);
                boolean isBridge = (access & Opcodes.ACC_BRIDGE) != 0;
                if (findConstructor && isConstructorMethod || (!findConstructor && !isConstructorMethod && (isBridge == findBridge))) {
                    assert methodNode[0] == null : "Wrong lambda/sam structure: " + methodNode[0].name + " conflicts with " + name;
                    return methodNode[0] = new MethodNode(access, name, desc, signature, exceptions);
                }
                return null;
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        if (methodNode[0] == null && !findBridge) {
            throw new RuntimeException("Couldn't find operation method of lambda/sam class " + oldLambdaType.getInternalName() + ": findConstructor = " + findConstructor);
        }

        return methodNode[0];
    }

    public static String getNewFieldName(String oldName) {
        if (oldName.equals("this$0")) {
            //"this$0" couldn't clash and we should keep this name invariant for further transformations
            return oldName;
        }
        return oldName + "$inlined";
    }
}
