/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline;

import com.intellij.util.ArrayUtil;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.codegen.ClassBuilder;
import org.jetbrains.kotlin.codegen.FieldInfo;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;

import java.util.*;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.isThis0;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;

public class AnonymousObjectTransformer extends ObjectTransformer<AnonymousObjectTransformationInfo> {
    private final InliningContext inliningContext;
    private final Type oldObjectType;
    private final boolean isSameModule;
    private final Map<String, List<String>> fieldNames = new HashMap<String, List<String>>();

    private MethodNode constructor;
    private String sourceInfo;
    private String debugInfo;
    private SourceMapper sourceMapper;

    public AnonymousObjectTransformer(
            @NotNull AnonymousObjectTransformationInfo transformationInfo,
            @NotNull InliningContext inliningContext,
            boolean isSameModule
    ) {
        super(transformationInfo, inliningContext.state);
        this.isSameModule = isSameModule;
        this.inliningContext = inliningContext;
        this.oldObjectType = Type.getObjectType(transformationInfo.getOldClassName());
    }

    @Override
    @NotNull
    public InlineResult doTransform(@NotNull FieldRemapper parentRemapper) {
        final List<InnerClassNode> innerClassNodes = new ArrayList<InnerClassNode>();
        final ClassBuilder classBuilder = createRemappingClassBuilderViaFactory(inliningContext);
        final List<MethodNode> methodsToTransform = new ArrayList<MethodNode>();

        createClassReader().accept(new ClassVisitor(InlineCodegenUtil.API, classBuilder.getVisitor()) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                InlineCodegenUtil.assertVersionNotGreaterThanGeneratedOne(version, name, inliningContext.state);
                classBuilder.defineClass(null, version, access, name, signature, superName, interfaces);
                if(CoroutineCodegenUtilKt.COROUTINE_IMPL_ASM_TYPE.getInternalName().equals(superName)) {
                    inliningContext.setContinuation(true);
                }
            }

            @Override
            public void visitInnerClass(@NotNull String name, String outerName, String innerName, int access) {
                innerClassNodes.add(new InnerClassNode(name, outerName, innerName, access));
            }

            @Override
            public MethodVisitor visitMethod(
                    int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions
            ) {
                MethodNode node = new MethodNode(access, name, desc, signature, exceptions);
                if (name.equals("<init>")) {
                    if (constructor != null) {
                        throw new RuntimeException("Lambda, SAM or anonymous object should have only one constructor");
                    }
                    constructor = node;
                }
                else {
                    methodsToTransform.add(node);
                }
                return node;
            }

            @Override
            public FieldVisitor visitField(int access, @NotNull String name, @NotNull String desc, String signature, Object value) {
                addUniqueField(name);
                if (InlineCodegenUtil.isCapturedFieldName(name)) {
                    return null;
                }
                else {
                    return classBuilder.newField(JvmDeclarationOrigin.NO_ORIGIN, access, name, desc, signature, value);
                }
            }

            @Override
            public void visitSource(String source, String debug) {
                sourceInfo = source;
                debugInfo = debug;
            }

            @Override
            public void visitEnd() {
            }
        }, ClassReader.SKIP_FRAMES);

        if (!inliningContext.isInliningLambda) {
            if (debugInfo != null && !debugInfo.isEmpty()) {
                sourceMapper = SourceMapper.Companion.createFromSmap(SMAPParser.parse(debugInfo));
            }
            else {
                //seems we can't do any clever mapping cause we don't know any about original class name
                sourceMapper = IdenticalSourceMapper.INSTANCE;
            }
            if (sourceInfo != null && !InlineCodegenUtil.GENERATE_SMAP) {
                classBuilder.visitSource(sourceInfo, debugInfo);
            }
        }
        else {
            if (sourceInfo != null) {
                classBuilder.visitSource(sourceInfo, debugInfo);
            }
            sourceMapper = IdenticalSourceMapper.INSTANCE;
        }

        ParametersBuilder allCapturedParamBuilder = ParametersBuilder.newBuilder();
        ParametersBuilder constructorParamBuilder = ParametersBuilder.newBuilder();
        List<CapturedParamInfo> additionalFakeParams =
                extractParametersMappingAndPatchConstructor(constructor, allCapturedParamBuilder, constructorParamBuilder,
                                                            transformationInfo, parentRemapper);
        List<DeferredMethodVisitor> deferringMethods = new ArrayList<DeferredMethodVisitor>();

        generateConstructorAndFields(classBuilder, allCapturedParamBuilder, constructorParamBuilder, parentRemapper, additionalFakeParams);

        for (MethodNode next : methodsToTransform) {
            DeferredMethodVisitor deferringVisitor = newMethod(classBuilder, next);
            InlineResult funResult =
                    inlineMethodAndUpdateGlobalResult(parentRemapper, deferringVisitor, next, allCapturedParamBuilder, false);

            Type returnType = Type.getReturnType(next.desc);
            if (!AsmUtil.isPrimitive(returnType)) {
                String oldFunReturnType = returnType.getInternalName();
                String newFunReturnType = funResult.getChangedTypes().get(oldFunReturnType);
                if (newFunReturnType != null) {
                    inliningContext.typeRemapper.addAdditionalMappings(oldFunReturnType, newFunReturnType);
                }
            }
            deferringMethods.add(deferringVisitor);
        }

        for (DeferredMethodVisitor method : deferringMethods) {
            InlineCodegenUtil.removeFinallyMarkers(method.getIntermediate());
            method.visitEnd();
        }

        SourceMapper.Companion.flushToClassBuilder(sourceMapper, classBuilder);

        ClassVisitor visitor = classBuilder.getVisitor();
        for (InnerClassNode node : innerClassNodes) {
            visitor.visitInnerClass(node.name, node.outerName, node.innerName, node.access);
        }

        writeOuterInfo(visitor);

        classBuilder.done();

        return transformationResult;
    }

    private void writeOuterInfo(@NotNull ClassVisitor visitor) {
        InlineCallSiteInfo info = inliningContext.getCallSiteInfo();
        visitor.visitOuterClass(info.getOwnerClassName(), info.getFunctionName(), info.getFunctionDesc());
    }

    @NotNull
    private InlineResult inlineMethodAndUpdateGlobalResult(
            @NotNull FieldRemapper parentRemapper,
            @NotNull MethodVisitor deferringVisitor,
            @NotNull MethodNode next,
            @NotNull ParametersBuilder allCapturedParamBuilder,
            boolean isConstructor
    ) {
        InlineResult funResult = inlineMethod(parentRemapper, deferringVisitor, next, allCapturedParamBuilder, isConstructor);
        transformationResult.merge(funResult);
        transformationResult.getReifiedTypeParametersUsages().mergeAll(funResult.getReifiedTypeParametersUsages());
        return funResult;
    }

    @NotNull
    private InlineResult inlineMethod(
            @NotNull FieldRemapper parentRemapper,
            @NotNull MethodVisitor deferringVisitor,
            @NotNull MethodNode sourceNode,
            @NotNull ParametersBuilder capturedBuilder,
            boolean isConstructor
    ) {
        ReifiedTypeParametersUsages typeParametersToReify = inliningContext.reifiedTypeInliner.reifyInstructions(sourceNode);
        Parameters parameters =
                isConstructor ? capturedBuilder.buildParameters() : getMethodParametersWithCaptured(capturedBuilder, sourceNode);

        RegeneratedLambdaFieldRemapper remapper = new RegeneratedLambdaFieldRemapper(
                oldObjectType.getInternalName(), transformationInfo.getNewClassName(), parameters,
                transformationInfo.getCapturedLambdasToInline(), parentRemapper, isConstructor
        );

        MethodInliner inliner = new MethodInliner(
                sourceNode,
                parameters,
                inliningContext.subInline(transformationInfo.getNameGenerator()),
                remapper,
                isSameModule,
                "Transformer for " + transformationInfo.getOldClassName(),
                sourceMapper,
                new InlineCallSiteInfo(
                        transformationInfo.getOldClassName(),
                        sourceNode.name,
                        isConstructor ? transformationInfo.getNewConstructorDescriptor() : sourceNode.desc
                ),
                null
        );

        InlineResult result = inliner.doInline(deferringVisitor, new LocalVarRemapper(parameters, 0), false, LabelOwner.NOT_APPLICABLE);
        result.getReifiedTypeParametersUsages().mergeAll(typeParametersToReify);
        deferringVisitor.visitMaxs(-1, -1);
        return result;
    }

    private void generateConstructorAndFields(
            @NotNull ClassBuilder classBuilder,
            @NotNull ParametersBuilder allCapturedBuilder,
            @NotNull ParametersBuilder constructorInlineBuilder,
            @NotNull FieldRemapper parentRemapper,
            @NotNull List<CapturedParamInfo> constructorAdditionalFakeParams
    ) {
        List<Type> descTypes = new ArrayList<Type>();

        Parameters constructorParams = constructorInlineBuilder.buildParameters();
        int[] capturedIndexes = new int[constructorParams.getParameters().size()];
        int index = 0;
        int size = 0;

        //complex processing cause it could have super constructor call params
        for (ParameterInfo info : constructorParams) {
            if (!info.isSkipped) { //not inlined
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

        String constructorDescriptor = Type.getMethodDescriptor(Type.VOID_TYPE, descTypes.toArray(new Type[descTypes.size()]));
        //TODO for inline method make public class
        transformationInfo.setNewConstructorDescriptor(constructorDescriptor);
        MethodVisitor constructorVisitor = classBuilder.newMethod(
                NO_ORIGIN, constructor.access, "<init>", constructorDescriptor, null, ArrayUtil.EMPTY_STRING_ARRAY
        );

        final Label newBodyStartLabel = new Label();
        constructorVisitor.visitLabel(newBodyStartLabel);
        //initialize captured fields
        List<NewJavaField> newFieldsWithSkipped = TransformationUtilsKt.getNewFieldsToGenerate(allCapturedBuilder.listCaptured());
        List<FieldInfo> fieldInfoWithSkipped =
                TransformationUtilsKt.transformToFieldInfo(Type.getObjectType(transformationInfo.getNewClassName()), newFieldsWithSkipped);

        int paramIndex = 0;
        InstructionAdapter capturedFieldInitializer = new InstructionAdapter(constructorVisitor);
        for (int i = 0; i < fieldInfoWithSkipped.size(); i++) {
            FieldInfo fieldInfo = fieldInfoWithSkipped.get(i);
            if (!newFieldsWithSkipped.get(i).getSkip()) {
                AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, capturedIndexes[paramIndex], capturedFieldInitializer);
            }
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
                StackValue composed = StackValue.field(
                        fake.getType(),
                        oldObjectType,
                        fake.getNewFieldName(),
                        false,
                        StackValue.LOCAL_0
                );
                fake.setRemapValue(composed);
            }
        }

        MethodNode intermediateMethodNode =
                new MethodNode(constructor.access, "<init>", constructorDescriptor, null, ArrayUtil.EMPTY_STRING_ARRAY);
        inlineMethodAndUpdateGlobalResult(parentRemapper, intermediateMethodNode, constructor, constructorInlineBuilder, true);
        InlineCodegenUtil.removeFinallyMarkers(intermediateMethodNode);

        AbstractInsnNode first = intermediateMethodNode.instructions.getFirst();
        final Label oldStartLabel = first instanceof LabelNode ? ((LabelNode) first).getLabel() : null;
        intermediateMethodNode.accept(new MethodBodyVisitor(capturedFieldInitializer) {
            @Override
            public void visitLocalVariable(
                    @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
            ) {
                if (oldStartLabel == start) {
                    start = newBodyStartLabel;//patch for jack&jill
                }
                super.visitLocalVariable(name, desc, signature, start, end, index);
            }
        });
        constructorVisitor.visitEnd();
        AsmUtil.genClosureFields(
                TransformationUtilsKt.toNameTypePair(TransformationUtilsKt.filterSkipped(newFieldsWithSkipped)), classBuilder
        );
    }

    @NotNull
    private Parameters getMethodParametersWithCaptured(@NotNull ParametersBuilder capturedBuilder, @NotNull MethodNode sourceNode) {
        ParametersBuilder builder = ParametersBuilder.initializeBuilderFrom(oldObjectType, sourceNode.desc);
        for (CapturedParamInfo param : capturedBuilder.listCaptured()) {
            builder.addCapturedParamCopy(param);
        }
        return builder.buildParameters();
    }

    @NotNull
    private static DeferredMethodVisitor newMethod(@NotNull final ClassBuilder builder, @NotNull final MethodNode original) {
        return new DeferredMethodVisitor(
                new MethodNode(
                        original.access, original.name, original.desc, original.signature,
                        ArrayUtil.toStringArray(original.exceptions)
                ),
                new Function0<MethodVisitor>() {
                    @Override
                    public MethodVisitor invoke() {
                        return builder.newMethod(
                                NO_ORIGIN, original.access, original.name, original.desc, original.signature,
                                ArrayUtil.toStringArray(original.exceptions)
                        );
                    }
                }
        );
    }

    @NotNull
    private List<CapturedParamInfo> extractParametersMappingAndPatchConstructor(
            @NotNull MethodNode constructor,
            @NotNull ParametersBuilder capturedParamBuilder,
            @NotNull ParametersBuilder constructorParamBuilder,
            @NotNull AnonymousObjectTransformationInfo transformationInfo,
            @NotNull FieldRemapper parentFieldRemapper
    ) {
        Set<LambdaInfo> capturedLambdas = new LinkedHashSet<LambdaInfo>(); //captured var of inlined parameter
        List<CapturedParamInfo> constructorAdditionalFakeParams = new ArrayList<CapturedParamInfo>();
        Map<Integer, LambdaInfo> indexToLambda = transformationInfo.getLambdasToInline();
        Set<Integer> capturedParams = new HashSet<Integer>();

        //load captured parameters and patch instruction list (NB: there is also could be object fields)
        AbstractInsnNode cur = constructor.instructions.getFirst();
        while (cur != null) {
            if (cur instanceof FieldInsnNode) {
                FieldInsnNode fieldNode = (FieldInsnNode) cur;
                String fieldName = fieldNode.name;
                if (fieldNode.getOpcode() == Opcodes.PUTFIELD && InlineCodegenUtil.isCapturedFieldName(fieldName)) {
                    boolean isPrevVarNode = fieldNode.getPrevious() instanceof VarInsnNode;
                    boolean isPrevPrevVarNode = isPrevVarNode && fieldNode.getPrevious().getPrevious() instanceof VarInsnNode;

                    if (isPrevPrevVarNode) {
                        VarInsnNode node = (VarInsnNode) fieldNode.getPrevious().getPrevious();
                        if (node.var == 0) {
                            VarInsnNode previous = (VarInsnNode) fieldNode.getPrevious();
                            int varIndex = previous.var;
                            LambdaInfo lambdaInfo = indexToLambda.get(varIndex);
                            String newFieldName =
                                    isThis0(fieldName) && shouldRenameThis0(parentFieldRemapper, indexToLambda.values())
                                    ? getNewFieldName(fieldName, true)
                                    : fieldName;
                            CapturedParamInfo info = capturedParamBuilder.addCapturedParam(
                                    Type.getObjectType(transformationInfo.getOldClassName()), fieldName, newFieldName,
                                    Type.getType(fieldNode.desc), lambdaInfo != null, null
                            );
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
        String constructorDesc = transformationInfo.getConstructorDesc();

        if (constructorDesc == null) {
            // in case of anonymous object with empty closure
            constructorDesc = Type.getMethodDescriptor(Type.VOID_TYPE);
        }

        Type[] types = Type.getArgumentTypes(constructorDesc);
        for (Type type : types) {
            LambdaInfo info = indexToLambda.get(constructorParamBuilder.getNextParameterOffset());
            ParameterInfo parameterInfo = constructorParamBuilder.addNextParameter(type, info != null);
            parameterInfo.setLambda(info);
            if (capturedParams.contains(parameterInfo.getIndex())) {
                parameterInfo.setCaptured(true);
            }
            else {
                //otherwise it's super constructor parameter
            }
        }

        //For all inlined lambdas add their captured parameters
        //TODO: some of such parameters could be skipped - we should perform additional analysis
        Map<String, LambdaInfo> capturedLambdasToInline = new HashMap<String, LambdaInfo>(); //captured var of inlined parameter
        List<CapturedParamDesc> allRecapturedParameters = new ArrayList<CapturedParamDesc>();
        boolean addCapturedNotAddOuter =
                parentFieldRemapper.isRoot() ||
                (parentFieldRemapper instanceof InlinedLambdaRemapper && parentFieldRemapper.getParent().isRoot());
        Map<String, CapturedParamInfo> alreadyAdded = new HashMap<String, CapturedParamInfo>();
        for (LambdaInfo info : capturedLambdas) {
            if (addCapturedNotAddOuter) {
                for (CapturedParamDesc desc : info.getCapturedVars()) {
                    String key = desc.getFieldName() + "$$$" + desc.getType().getClassName();
                    CapturedParamInfo alreadyAddedParam = alreadyAdded.get(key);

                    CapturedParamInfo recapturedParamInfo = capturedParamBuilder.addCapturedParam(
                            desc,
                            alreadyAddedParam != null ? alreadyAddedParam.getNewFieldName() : getNewFieldName(desc.getFieldName(), false),
                            alreadyAddedParam != null
                    );
                    StackValue composed = StackValue.field(
                            desc.getType(),
                            oldObjectType, /*TODO owner type*/
                            recapturedParamInfo.getNewFieldName(),
                            false,
                            StackValue.LOCAL_0
                    );
                    recapturedParamInfo.setRemapValue(composed);
                    allRecapturedParameters.add(desc);

                    constructorParamBuilder.addCapturedParam(recapturedParamInfo, recapturedParamInfo.getNewFieldName())
                            .setRemapValue(composed);

                    if (isThis0(desc.getFieldName())) {
                        alreadyAdded.put(key, recapturedParamInfo);
                    }
                }
            }
            capturedLambdasToInline.put(info.getLambdaClassType().getInternalName(), info);
        }

        if (parentFieldRemapper instanceof InlinedLambdaRemapper && !capturedLambdas.isEmpty() && !addCapturedNotAddOuter) {
            //lambda with non InlinedLambdaRemapper already have outer
            FieldRemapper parent = parentFieldRemapper.getParent();
            assert parent instanceof RegeneratedLambdaFieldRemapper;
            Type ownerType = Type.getObjectType(parent.getLambdaInternalName());
            CapturedParamDesc desc = new CapturedParamDesc(ownerType, InlineCodegenUtil.THIS, ownerType);
            CapturedParamInfo recapturedParamInfo =
                    capturedParamBuilder.addCapturedParam(desc, InlineCodegenUtil.THIS$0/*outer lambda/object*/, false);
            StackValue composed = StackValue.LOCAL_0;
            recapturedParamInfo.setRemapValue(composed);
            allRecapturedParameters.add(desc);

            constructorParamBuilder.addCapturedParam(recapturedParamInfo, recapturedParamInfo.getNewFieldName()).setRemapValue(composed);
        }

        transformationInfo.setAllRecapturedParameters(allRecapturedParameters);
        transformationInfo.setCapturedLambdasToInline(capturedLambdasToInline);

        return constructorAdditionalFakeParams;
    }

    private static boolean shouldRenameThis0(@NotNull FieldRemapper parentFieldRemapper, @NotNull Collection<LambdaInfo> values) {
        if (isFirstDeclSiteLambdaFieldRemapper(parentFieldRemapper)) {
            for (LambdaInfo value : values) {
                for (CapturedParamDesc desc : value.getCapturedVars()) {
                    if (isThis0(desc.getFieldName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @NotNull
    private String getNewFieldName(@NotNull String oldName, boolean originalField) {
        if (InlineCodegenUtil.THIS$0.equals(oldName)) {
            if (!originalField) {
                return oldName;
            }
            else {
                //rename original 'this$0' in declaration site lambda (inside inline function) to use this$0 only for outer lambda/object access on call site
                return addUniqueField(oldName + InlineCodegenUtil.INLINE_FUN_THIS_0_SUFFIX);
            }
        }
        return addUniqueField(oldName + InlineCodegenUtil.INLINE_TRANSFORMATION_SUFFIX);
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

    private static boolean isFirstDeclSiteLambdaFieldRemapper(@NotNull FieldRemapper parentRemapper) {
        return !(parentRemapper instanceof RegeneratedLambdaFieldRemapper) && !(parentRemapper instanceof InlinedLambdaRemapper);
    }
}
