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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode;

import java.io.IOException;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;

public class AnonymousObjectTransformer {

    protected final GenerationState state;

    protected final JetTypeMapper typeMapper;

    private MethodNode constructor;

    private final InliningContext inliningContext;

    private final Type oldObjectType;

    private final Type newLambdaType;

    private final ClassReader reader;

    private final boolean isSameModule;

    private final Map<String, List<String>> fieldNames = new HashMap<String, List<String>>();

    public AnonymousObjectTransformer(
            @NotNull String objectInternalName,
            @NotNull InliningContext inliningContext,
            boolean isSameModule,
            @NotNull Type newLambdaType
    ) {
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
        ClassBuilder classBuilder = createClassBuilder();
        final List<MethodNode> methodsToTransform = new ArrayList<MethodNode>();
        reader.accept(new ClassVisitor(InlineCodegenUtil.API, classBuilder.getVisitor()) {

            @Override
            public void visitOuterClass(@NotNull String owner, String name, String desc) {
                InliningContext parent = inliningContext.getParent();
                assert parent != null : "Context for transformer should have parent one: " + inliningContext;

                //we don't write owner info for lamdbas and SAMs just only for objects
                if (parent.isRoot() || parent.isInliningLambdaRootContext()) {
                    //TODO: think about writing method info - there is some problem with new constructor desc calculation
                    super.visitOuterClass(inliningContext.getParent().getClassNameToInline(), null, null);
                    return;
                }

                super.visitOuterClass(owner, name, desc);
            }

            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions
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
                    int access, @NotNull String name, @NotNull String desc, String signature, Object value
            ) {
                addUniqueField(name);
                if (InlineCodegenUtil.isCapturedFieldName(name)) {
                    return null;
                } else {
                    return super.visitField(access, name, desc, signature, value);
                }
            }
        }, ClassReader.SKIP_FRAMES);

        ParametersBuilder allCapturedParamBuilder = ParametersBuilder.newBuilder();
        ParametersBuilder constructorParamBuilder = ParametersBuilder.newBuilder();
        List<CapturedParamInfo> additionalFakeParams =
                extractParametersMappingAndPatchConstructor(constructor, allCapturedParamBuilder, constructorParamBuilder, invocation);

        InlineResult result = InlineResult.create();
        for (MethodNode next : methodsToTransform) {
            MethodVisitor visitor = newMethod(classBuilder, next);
            InlineResult funResult = inlineMethod(invocation, parentRemapper, visitor, next, allCapturedParamBuilder);
            result.addAllClassesToRemove(funResult);
        }

        InlineResult constructorResult =
                generateConstructorAndFields(classBuilder, allCapturedParamBuilder, constructorParamBuilder, invocation, parentRemapper, additionalFakeParams);

        result.addAllClassesToRemove(constructorResult);

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
        resultVisitor.visitEnd();
        return result;
    }

    private InlineResult generateConstructorAndFields(
            @NotNull ClassBuilder classBuilder,
            @NotNull ParametersBuilder allCapturedBuilder,
            @NotNull ParametersBuilder constructorInlineBuilder,
            @NotNull ConstructorInvocation invocation,
            @NotNull FieldRemapper parentRemapper,
            @NotNull List<CapturedParamInfo> constructorAdditionalFakeParams
    ) {
        List<Type> descTypes = new ArrayList<Type>();

        Parameters constructorParams = constructorInlineBuilder.buildParameters();
        int [] capturedIndexes = new int [constructorParams.totalSize()];
        int index = 0;
        int size = 0;

        //complex processing cause it could have super constructor call params
        for (ParameterInfo info : constructorParams) {
            if (!info.isSkipped()) { //not inlined
                if (info.isCaptured() || info instanceof CapturedParamInfo) {
                    capturedIndexes[index] = size;
                    index++;
                }

                if (size != 0) { //skip this
                    descTypes.add(info.getType());
                }
                size += info.getType().getSize();
            }
        }

        List<Pair<String, Type>> capturedFieldsToGenerate = new ArrayList<Pair<String, Type>>();
        for (CapturedParamInfo capturedParamInfo : allCapturedBuilder.listCaptured()) {
            if (capturedParamInfo.getLambda() == null) { //not inlined
                capturedFieldsToGenerate.add(new Pair<String, Type>(capturedParamInfo.getNewFieldName(), capturedParamInfo.getType()));
            }
        }

        String constructorDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, descTypes.toArray(new Type[descTypes.size()]));

        MethodVisitor constructorVisitor = classBuilder.newMethod(NO_ORIGIN,
                                                                  AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                                                                  "<init>", constructorDescriptor,
                                                                  null, ArrayUtil.EMPTY_STRING_ARRAY);

        //initialize captured fields
        List<FieldInfo> fields = AsmUtil.transformCapturedParams(capturedFieldsToGenerate, newLambdaType);
        int paramIndex = 0;
        InstructionAdapter capturedFieldInitializer = new InstructionAdapter(constructorVisitor);
        for (FieldInfo fieldInfo : fields) {
            AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, capturedIndexes[paramIndex], capturedFieldInitializer);
            paramIndex++;
        }

        //then transform constructor
        //HACK: in inlinining into constructor we access original captured fields with field access not local var
        //but this fields added to general params (this assumes local var access) not captured one,
        //so we need to add them to captured params
        for (CapturedParamInfo info : constructorAdditionalFakeParams) {
            CapturedParamInfo fake = constructorInlineBuilder.addCapturedParamCopy(info);

            if (fake.getLambda() != null) {
                //set remap value to skip this fake (captured with lambda already skipped)
                StackValue composed = StackValue.composed(StackValue.local(0, oldObjectType),
                                                          StackValue.field(fake.getType(),
                                                                           oldObjectType,
                                                                           fake.getNewFieldName(), false)
                );
                fake.setRemapValue(composed);
            }
        }

        Parameters constructorParameters = constructorInlineBuilder.buildParameters();

        RegeneratedLambdaFieldRemapper remapper =
                new RegeneratedLambdaFieldRemapper(oldObjectType.getInternalName(), newLambdaType.getInternalName(),
                                                   constructorParameters, invocation.getCapturedLambdasToInline(),
                                                   parentRemapper);

        MethodInliner inliner = new MethodInliner(constructor, constructorParameters, inliningContext.subInline(inliningContext.nameGenerator.subGenerator("lambda")),
                                                  remapper, isSameModule, "Transformer for constructor of " + invocation.getOwnerInternalName());
        InlineResult result = inliner.doInline(capturedFieldInitializer, new LocalVarRemapper(constructorParameters, 0), false);
        constructorVisitor.visitMaxs(-1, -1);
        constructorVisitor.visitEnd();

        AsmUtil.genClosureFields(capturedFieldsToGenerate, classBuilder);
        //TODO for inline method make public class
        invocation.setNewConstructorDescriptor(constructorDescriptor);
        return result;
    }

    @NotNull
    private Parameters getMethodParametersWithCaptured(
            @NotNull ParametersBuilder capturedBuilder,
            @NotNull MethodNode sourceNode
    ) {
        ParametersBuilder builder = ParametersBuilder.newBuilder();
        buildInvokeParamsFor(builder, sourceNode);
        for (CapturedParamInfo param : capturedBuilder.listCaptured()) {
            builder.addCapturedParamCopy(param);
        }
        return builder.buildParameters();
    }

    @NotNull
    private ClassBuilder createClassBuilder() {
        ClassBuilder classBuilder = state.getFactory().newVisitor(NO_ORIGIN, newLambdaType, inliningContext.getRoot().callElement.getContainingFile());
        return new RemappingClassBuilder(classBuilder, new TypeRemapper(inliningContext.typeMapping));
    }

    @NotNull
    private static MethodVisitor newMethod(@NotNull ClassBuilder builder, @NotNull MethodNode original) {
        return builder.newMethod(
                NO_ORIGIN,
                original.access,
                original.name,
                original.desc,
                original.signature,
                original.exceptions.toArray(new String [original.exceptions.size()])
        );
    }

    private List<CapturedParamInfo> extractParametersMappingAndPatchConstructor(
            @NotNull MethodNode constructor,
            @NotNull ParametersBuilder capturedParamBuilder,
            @NotNull ParametersBuilder constructorParamBuilder,
            @NotNull final ConstructorInvocation invocation
    ) {

        CapturedParamOwner owner = new CapturedParamOwner() {
            @Override
            public Type getType() {
                return Type.getObjectType(invocation.getOwnerInternalName());
            }
        };

        List<LambdaInfo> capturedLambdas = new ArrayList<LambdaInfo>(); //captured var of inlined parameter
        List<CapturedParamInfo> constructorAdditionalFakeParams = new ArrayList<CapturedParamInfo>();
        Map<Integer, LambdaInfo> indexToLambda = invocation.getLambdasToInline();
        Set<Integer> capturedParams = new HashSet<Integer>();

        //load captured parameters and patch instruction list (NB: there is also could be object fields)
        AbstractInsnNode cur = constructor.instructions.getFirst();
        while (cur != null) {
            if (cur instanceof FieldInsnNode) {
                FieldInsnNode fieldNode = (FieldInsnNode) cur;
                if (fieldNode.getOpcode() == Opcodes.PUTFIELD && InlineCodegenUtil.isCapturedFieldName(fieldNode.name)) {

                    boolean isPrevVarNode = fieldNode.getPrevious() instanceof VarInsnNode;
                    boolean isPrevPrevVarNode = isPrevVarNode && fieldNode.getPrevious().getPrevious() instanceof VarInsnNode;

                    if (isPrevPrevVarNode) {
                        VarInsnNode node = (VarInsnNode) fieldNode.getPrevious().getPrevious();
                        if (node.var == 0) {
                            VarInsnNode previous = (VarInsnNode) fieldNode.getPrevious();
                            int varIndex = previous.var;
                            LambdaInfo lambdaInfo = indexToLambda.get(varIndex);
                            CapturedParamInfo info = capturedParamBuilder.addCapturedParam(owner, fieldNode.name, Type.getType(fieldNode.desc), lambdaInfo != null, null);
                            if (lambdaInfo != null) {
                                info.setLambda(lambdaInfo);
                                capturedLambdas.add(lambdaInfo);
                            }
                            constructorAdditionalFakeParams.add(info);
                            capturedParams.add(varIndex);

                            constructor.instructions.remove(previous.getPrevious());
                            constructor.instructions.remove(previous);
                            AbstractInsnNode temp = cur;
                            cur = cur.getNext();
                            constructor.instructions.remove(temp);
                            continue;
                        }
                    }
                }
            }
            cur = cur.getNext();
        }

        constructorParamBuilder.addThis(oldObjectType, false);
        Type [] types = Type.getArgumentTypes(invocation.getDesc());
        for (Type type : types) {
            LambdaInfo info = indexToLambda.get(constructorParamBuilder.getNextValueParameterIndex());
            ParameterInfo parameterInfo = constructorParamBuilder.addNextParameter(type, info != null, null);
            parameterInfo.setLambda(info);
            if (capturedParams.contains(parameterInfo.getIndex())) {
                parameterInfo.setCaptured(true);
            } else {
                //otherwise it's super constructor parameter
            }
        }

        //For all inlined lambdas add their captured parameters
        //TODO: some of such parameters could be skipped - we should perform additional analysis
        Map<String, LambdaInfo> capturedLambdasToInline = new HashMap<String, LambdaInfo>(); //captured var of inlined parameter
        List<CapturedParamInfo> allRecapturedParameters = new ArrayList<CapturedParamInfo>();
        for (LambdaInfo info : capturedLambdas) {
            for (CapturedParamInfo var : info.getCapturedVars()) {
                CapturedParamInfo recapturedParamInfo = capturedParamBuilder.addCapturedParam(var,
                                                                                              getNewFieldName(var.getOriginalFieldName()));
                StackValue composed = StackValue.composed(StackValue.local(0, oldObjectType),
                                                          StackValue.field(var.getType(),
                                                                           oldObjectType, /*TODO owner type*/
                                                                           recapturedParamInfo.getNewFieldName(), false)
                );
                recapturedParamInfo.setRemapValue(composed);
                allRecapturedParameters.add(var);

                constructorParamBuilder.addCapturedParam(var, recapturedParamInfo.getNewFieldName()).setRemapValue(composed);
            }
            capturedLambdasToInline.put(info.getLambdaClassType().getInternalName(), info);
        }



        invocation.setAllRecapturedParameters(allRecapturedParameters);
        invocation.setCapturedLambdasToInline(capturedLambdasToInline);

        return constructorAdditionalFakeParams;
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
