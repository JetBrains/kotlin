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
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.*;

import static org.jetbrains.org.objectweb.asm.Opcodes.V1_6;

public class LambdaTransformer {

    protected final GenerationState state;

    protected final JetTypeMapper typeMapper;

    private MethodNode constructor;

    private final InliningContext inliningContext;

    private final Type oldObjectType;

    private final Type newLambdaType;

    private final ClassReader reader;

    private String superName;

    private final boolean isSameModule;

    private final Map<String, List<String>> fieldNames = new HashMap<String, List<String>>();

    public LambdaTransformer(@NotNull String objectInternalName, @NotNull InliningContext inliningContext, boolean isSameModule, @NotNull Type newLambdaType) {
        this.isSameModule = isSameModule;
        this.state = inliningContext.state;
        this.typeMapper = state.getTypeMapper();
        this.inliningContext = inliningContext;
        this.oldObjectType = Type.getObjectType(objectInternalName);
        this.newLambdaType = newLambdaType;

        //try to find just compiled classes then in dependencies
        try {
            OutputFile outputFile = state.getFactory().get(objectInternalName + ".class");
            if (outputFile != null) {
                reader = new ClassReader(outputFile.asByteArray());
            } else {
                VirtualFile file = InlineCodegenUtil.findVirtualFile(state.getProject(), objectInternalName);
                if (file == null) {
                    throw new RuntimeException("Couldn't find virtual file for " + objectInternalName);
                }
                reader = new ClassReader(file.getInputStream());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildInvokeParamsFor(@NotNull ParametersBuilder builder, @NotNull MethodNode node) {
        builder.addThis(oldObjectType, false);

        Type[] types = Type.getArgumentTypes(node.desc);
        for (Type type : types) {
            builder.addNextParameter(type, false, null);
        }
    }

    @NotNull
    public InlineResult doTransform(@NotNull ConstructorInvocation invocation, @NotNull FieldRemapper parentRemapper) {
        final ClassBuilder classBuilder = createClassBuilder();
        final List<MethodNode> methodsToTransform = new ArrayList<MethodNode>();
        final List<FieldNode> fieldToAdd = new ArrayList<FieldNode>();
        reader.accept(new ClassVisitor(InlineCodegenUtil.API, classBuilder.getVisitor()) {

            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                //TODO: public visibility for inline function
                LambdaTransformer.this.superName = superName;
                classBuilder.defineClass(null,
                                         V1_6,
                                         access,
                                         newLambdaType.getInternalName(),
                                         signature,
                                         superName,
                                         interfaces
                );
            }

            @Override
            public MethodVisitor visitMethod(
                    int access, String name, String desc, String signature, String[] exceptions
            ) {
                MethodNode node = new MethodNode(access, name, desc, signature, exceptions);
                if (name.equals("<init>")){
                    if (constructor != null)
                        throw new RuntimeException("Lambda, SAM or anonymous object should have only one constructor");

                    constructor = node;
                } else {
                    methodsToTransform.add(node);
                }
                return node;
            }

            @Override
            public FieldVisitor visitField(
                    int access, String name, String desc, String signature, Object value
            ) {
                addUniqueField(name);
                FieldNode fieldNode = new FieldNode(access, name, desc, signature, value);
                fieldToAdd.add(fieldNode);
                return fieldNode;
            }
        }, ClassReader.SKIP_FRAMES);

        ParametersBuilder capturedBuilder = ParametersBuilder.newBuilder();
        extractParametersMappingAndPatchConstructor(constructor, capturedBuilder, invocation);

        InlineResult result = InlineResult.create();
        for (MethodNode next : methodsToTransform) {
            MethodVisitor visitor = newMethod(classBuilder, next);
            InlineResult funResult = inlineMethod(invocation, parentRemapper, visitor, next, capturedBuilder);
            result.addAllClassesToRemove(funResult);
        }

        generateConstructorAndFields(classBuilder, capturedBuilder, invocation);

        classBuilder.done();

        invocation.setNewLambdaType(newLambdaType);
        return result;
    }

    @NotNull
    private InlineResult inlineMethod(
            @NotNull ConstructorInvocation invocation,
            @NotNull FieldRemapper parentRemapper,
            @NotNull MethodVisitor resultVisitor,
            @NotNull MethodNode sourceNode,
            @NotNull ParametersBuilder capturedBuilder
    ) {

        Parameters parameters = getMethodParametersWithCaptured(capturedBuilder, sourceNode);

        RegeneratedLambdaFieldRemapper remapper =
                new RegeneratedLambdaFieldRemapper(oldObjectType.getInternalName(), newLambdaType.getInternalName(),
                                                   parameters, invocation.getCapturedLambdasToInline(),
                                                   parentRemapper);

        MethodInliner inliner = new MethodInliner(sourceNode, parameters, inliningContext.subInline(inliningContext.nameGenerator.subGenerator("lambda")),
                                                  remapper, isSameModule, "Transformer for " + invocation.getOwnerInternalName());
        InlineResult result = inliner.doInline(resultVisitor, new LocalVarRemapper(parameters, 0), false);
        resultVisitor.visitMaxs(-1, -1);
        return result;
    }

    private void generateConstructorAndFields(@NotNull ClassBuilder classBuilder, @NotNull ParametersBuilder builder, @NotNull ConstructorInvocation invocation) {
        List<CapturedParamInfo> infos = builder.buildCaptured();
        List<Pair<String, Type>> newConstructorSignature = new ArrayList<Pair<String, Type>>();
        for (CapturedParamInfo capturedParamInfo : infos) {
            if (capturedParamInfo.getLambda() == null) { //not inlined
                newConstructorSignature.add(new Pair<String, Type>(capturedParamInfo.getNewFieldName(), capturedParamInfo.getType()));
            }
        }

        List<FieldInfo> fields = AsmUtil.transformCapturedParams(newConstructorSignature, newLambdaType);

        AsmUtil.genClosureFields(newConstructorSignature, classBuilder);

        //TODO for inline method make public class
        Method newConstructor = ClosureCodegen.generateConstructor(classBuilder, fields, null, Type.getObjectType(superName), state, AsmUtil.NO_FLAG_PACKAGE_PRIVATE);
        invocation.setNewConstructorDescriptor(newConstructor.getDescriptor());
    }

    @NotNull
    private Parameters getMethodParametersWithCaptured(
            @NotNull ParametersBuilder capturedBuilder,
            @NotNull MethodNode sourceNode
    ) {
        ParametersBuilder builder = ParametersBuilder.newBuilder();
        buildInvokeParamsFor(builder, sourceNode);
        for (CapturedParamInfo param : capturedBuilder.getCapturedParams()) {
            builder.addCapturedParamCopy(param);
        }
        return builder.buildParameters();
    }

    @NotNull
    private ClassBuilder createClassBuilder() {
        return new RemappingClassBuilder(state.getFactory().forLambdaInlining(newLambdaType, inliningContext.call.getCallElement().getContainingFile()),
                     new TypeRemapper(inliningContext.typeMapping));
    }

    @NotNull
    private static MethodVisitor newMethod(@NotNull ClassBuilder builder, @NotNull MethodNode original) {
        return builder.newMethod(
                null,
                original.access,
                original.name,
                original.desc,
                original.signature,
                original.exceptions.toArray(new String [original.exceptions.size()])
        );
    }

    private void extractParametersMappingAndPatchConstructor(
            @NotNull MethodNode constructor,
            @NotNull ParametersBuilder builder,
            @NotNull final ConstructorInvocation invocation
    ) {
        Map<Integer, LambdaInfo> indexToLambda = invocation.getLambdasToInline();
        List<LambdaInfo> capturedLambdas = new ArrayList<LambdaInfo>(); //captured var of inlined parameter
        CapturedParamOwner owner = new CapturedParamOwner() {
            @Override
            public Type getType() {
                return Type.getObjectType(invocation.getOwnerInternalName());
            }
        };

        AbstractInsnNode cur = constructor.instructions.getFirst();
        //load captured parameters (NB: there is also could be object fields)
        while (cur != null) {
            if (cur instanceof FieldInsnNode && cur.getOpcode() == Opcodes.PUTFIELD && InlineCodegenUtil.isCapturedFieldName(((FieldInsnNode) cur).name)) {
                FieldInsnNode fieldNode = (FieldInsnNode) cur;
                CapturedParamInfo info = builder.addCapturedParam(owner, fieldNode.name, Type.getType(fieldNode.desc), false, null);

                boolean isPrevVarNode = fieldNode.getPrevious() instanceof VarInsnNode;
                boolean isPrevPrevVarNode = isPrevVarNode && fieldNode.getPrevious().getPrevious() instanceof VarInsnNode;
                if (isPrevPrevVarNode) {
                    VarInsnNode node = (VarInsnNode) fieldNode.getPrevious().getPrevious();
                    if (node.var == 0) {
                        VarInsnNode previous = (VarInsnNode) fieldNode.getPrevious();
                        int varIndex = previous.var;
                        LambdaInfo lambdaInfo = indexToLambda.get(varIndex);
                        if (lambdaInfo != null) {
                            info.setLambda(lambdaInfo);
                            capturedLambdas.add(lambdaInfo);
                        }
                    }
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
                CapturedParamInfo recapturedParamInfo = builder.addCapturedParam(var, getNewFieldName(var.getOriginalFieldName()));
                StackValue composed = StackValue.composed(StackValue.local(0, oldObjectType),
                                                          StackValue.field(var.getType(),
                                                                           oldObjectType, /*TODO owner type*/
                                                                           recapturedParamInfo.getNewFieldName(), false)
                );
                recapturedParamInfo.setRemapValue(composed);
                allRecapturedParameters.add(var);
            }
            capturedLambdasToInline.put(info.getLambdaClassType().getInternalName(), info);
        }

        invocation.setAllRecapturedParameters(allRecapturedParameters);
        invocation.setCapturedLambdasToInline(capturedLambdasToInline);
    }

    @NotNull
    public String getNewFieldName(@NotNull String oldName) {
        if (InlineCodegenUtil.THIS$0.equals(oldName)) {
            //"this$0" couldn't clash and we should keep this name invariant for further transformations
            return oldName;
        }
        return addUniqueField(oldName + "$inlined");
    }

    @NotNull
    private String addUniqueField(@NotNull String name) {
        List<String> existNames = fieldNames.get(name);
        if (existNames == null) {
            existNames = new LinkedList<String>();
            fieldNames.put(name, existNames);
        }
        String suffix = existNames.isEmpty() ? "" : "$" + existNames.size();
        String newName = name + suffix;
        existNames.add(newName);
        return newName;
    }
}
