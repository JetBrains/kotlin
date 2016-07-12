/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.ClosureCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.optimization.FixStackWithLabelNormalizationMethodTransformer;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.utils.SmartList;
import org.jetbrains.kotlin.utils.SmartSet;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.commons.RemappingMethodAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;
import org.jetbrains.org.objectweb.asm.util.Printer;

import java.util.*;

import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.*;

public class MethodInliner {
    private final MethodNode node;
    private final Parameters parameters;
    private final InliningContext inliningContext;
    private final FieldRemapper nodeRemapper;
    private final boolean isSameModule;
    private final String errorPrefix;
    private final SourceMapper sourceMapper;
    private final InlineCallSiteInfo inlineCallSiteInfo;
    private final KotlinTypeMapper typeMapper;
    private final List<InvokeCall> invokeCalls = new ArrayList<InvokeCall>();
    //keeps order
    private final List<TransformationInfo> transformations = new ArrayList<TransformationInfo>();
    //current state
    private final Map<String, String> currentTypeMapping = new HashMap<String, String>();
    private final InlineResult result;
    private int lambdasFinallyBlocks;
    private final InlineOnlySmapSkipper inlineOnlySmapSkipper;

    public MethodInliner(
            @NotNull MethodNode node,
            @NotNull Parameters parameters,
            @NotNull InliningContext inliningContext,
            @NotNull FieldRemapper nodeRemapper,
            boolean isSameModule,
            @NotNull String errorPrefix,
            @NotNull SourceMapper sourceMapper,
            @NotNull InlineCallSiteInfo inlineCallSiteInfo,
            @Nullable InlineOnlySmapSkipper smapSkipper //non null only for root
    ) {
        this.node = node;
        this.parameters = parameters;
        this.inliningContext = inliningContext;
        this.nodeRemapper = nodeRemapper;
        this.isSameModule = isSameModule;
        this.errorPrefix = errorPrefix;
        this.sourceMapper = sourceMapper;
        this.inlineCallSiteInfo = inlineCallSiteInfo;
        this.typeMapper = inliningContext.state.getTypeMapper();
        this.result = InlineResult.create();
        this.inlineOnlySmapSkipper = smapSkipper;
    }

    @NotNull
    public InlineResult doInline(
            @NotNull MethodVisitor adapter,
            @NotNull LocalVarRemapper remapper,
            boolean remapReturn,
            @NotNull LabelOwner labelOwner
    ) {
        return doInline(adapter, remapper, remapReturn, labelOwner, 0);
    }

    @NotNull
    private InlineResult doInline(
            @NotNull MethodVisitor adapter,
            @NotNull LocalVarRemapper remapper,
            boolean remapReturn,
            @NotNull LabelOwner labelOwner,
            int finallyDeepShift
    ) {
        //analyze body
        MethodNode transformedNode = markPlacesForInlineAndRemoveInlinable(node, labelOwner, finallyDeepShift);

        //substitute returns with "goto end" instruction to keep non local returns in lambdas
        Label end = new Label();
        transformedNode = doInline(transformedNode);
        removeClosureAssertions(transformedNode);
        transformedNode.instructions.resetLabels();

        MethodNode resultNode = new MethodNode(
                InlineCodegenUtil.API, transformedNode.access, transformedNode.name, transformedNode.desc,
                transformedNode.signature, ArrayUtil.toStringArray(transformedNode.exceptions)
        );
        RemapVisitor visitor = new RemapVisitor(resultNode, remapper, nodeRemapper);
        try {
            transformedNode.accept(visitor);
        }
        catch (Throwable e) {
            throw wrapException(e, transformedNode, "couldn't inline method call");
        }

        resultNode.visitLabel(end);

        if (inliningContext.isRoot()) {
            StackValue remapValue = remapper.remap(parameters.getArgsSizeOnStack() + 1).value;
            InternalFinallyBlockInliner.processInlineFunFinallyBlocks(
                    resultNode, lambdasFinallyBlocks, ((StackValue.Local) remapValue).index
            );
        }

        processReturns(resultNode, labelOwner, remapReturn, end);
        //flush transformed node to output
        resultNode.accept(new MethodBodyVisitor(adapter));

        sourceMapper.endMapping();
        return result;
    }

    @NotNull
    private MethodNode doInline(@NotNull MethodNode node) {
        final Deque<InvokeCall> currentInvokes = new LinkedList<InvokeCall>(invokeCalls);

        final MethodNode resultNode = new MethodNode(node.access, node.name, node.desc, node.signature, null);

        final Iterator<TransformationInfo> iterator = transformations.iterator();

        final TypeRemapper remapper = TypeRemapper.createFrom(currentTypeMapping);
        final RemappingMethodAdapter remappingMethodAdapter = new RemappingMethodAdapter(
                resultNode.access,
                resultNode.desc,
                resultNode,
                new AsmTypeRemapper(remapper, inliningContext.getRoot().typeParameterMappings == null, result)
        );

        final int markerShift = InlineCodegenUtil.calcMarkerShift(parameters, node);
        InlineAdapter lambdaInliner = new InlineAdapter(remappingMethodAdapter, parameters.getArgsSizeOnStack(), sourceMapper) {
            private TransformationInfo transformationInfo;

            private void handleAnonymousObjectRegeneration() {
                transformationInfo = iterator.next();

                if (transformationInfo.shouldRegenerate(isSameModule)) {
                    //TODO: need poping of type but what to do with local funs???
                    String oldClassName = transformationInfo.getOldClassName();
                    String newClassName = transformationInfo.getNewClassName();
                    remapper.addMapping(oldClassName, newClassName);

                    InliningContext childInliningContext = inliningContext.subInlineWithClassRegeneration(
                            inliningContext.nameGenerator,
                            currentTypeMapping,
                            inlineCallSiteInfo
                    );
                    ObjectTransformer transformer = transformationInfo.createTransformer(childInliningContext, isSameModule);

                    InlineResult transformResult = transformer.doTransform(nodeRemapper);
                    result.addAllClassesToRemove(transformResult);
                    result.addChangedType(oldClassName, newClassName);

                    if (inliningContext.isInliningLambda && transformationInfo.canRemoveAfterTransformation()) {
                        // this class is transformed and original not used so we should remove original one after inlining
                        result.addClassToRemove(oldClassName);
                    }

                    if (transformResult.getReifiedTypeParametersUsages().wereUsedReifiedParameters()) {
                        ReifiedTypeInliner.putNeedClassReificationMarker(mv);
                        result.getReifiedTypeParametersUsages().mergeAll(transformResult.getReifiedTypeParametersUsages());
                    }
                }
            }

            @Override
            public void anew(@NotNull Type type) {
                if (isAnonymousClass(type.getInternalName())) {
                    handleAnonymousObjectRegeneration();
                }

                //in case of regenerated transformationInfo type would be remapped to new one via remappingMethodAdapter
                super.anew(type);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                if (/*INLINE_RUNTIME.equals(owner) &&*/ isInvokeOnLambda(owner, name)) { //TODO add method
                    assert !currentInvokes.isEmpty();
                    InvokeCall invokeCall = currentInvokes.remove();
                    LambdaInfo info = invokeCall.lambdaInfo;

                    if (info == null) {
                        //noninlinable lambda
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                        return;
                    }

                    int valueParamShift = Math.max(getNextLocalIndex(), markerShift);//NB: don't inline cause it changes
                    putStackValuesIntoLocals(info.getInvokeParamsWithoutCaptured(), valueParamShift, this, desc);

                    addInlineMarker(this, true);
                    Parameters lambdaParameters = info.addAllParameters(nodeRemapper);

                    InlinedLambdaRemapper newCapturedRemapper =
                            new InlinedLambdaRemapper(info.getLambdaClassType().getInternalName(), nodeRemapper, lambdaParameters);

                    setLambdaInlining(true);
                    SMAP lambdaSMAP = info.getNode().getClassSMAP();
                    //noinspection ConstantConditions
                    SourceMapper mapper =
                            inliningContext.classRegeneration && !inliningContext.isInliningLambda
                            ? new NestedSourceMapper(sourceMapper, lambdaSMAP.getIntervals(), lambdaSMAP.getSourceInfo())
                            : new InlineLambdaSourceMapper(sourceMapper.getParent(), info.getNode());
                    MethodInliner inliner = new MethodInliner(
                            info.getNode().getNode(), lambdaParameters, inliningContext.subInlineLambda(info),
                            newCapturedRemapper, true /*cause all calls in same module as lambda*/,
                            "Lambda inlining " + info.getLambdaClassType().getInternalName(),
                            mapper, inlineCallSiteInfo, null
                    );

                    LocalVarRemapper remapper = new LocalVarRemapper(lambdaParameters, valueParamShift);
                    //TODO add skipped this and receiver
                    InlineResult lambdaResult = inliner.doInline(this.mv, remapper, true, info, invokeCall.finallyDepthShift);
                    result.addAllClassesToRemove(lambdaResult);

                    //return value boxing/unboxing
                    Method bridge = typeMapper.mapAsmMethod(ClosureCodegen.getErasedInvokeFunction(info.getFunctionDescriptor()));
                    Method delegate = typeMapper.mapAsmMethod(info.getFunctionDescriptor());
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), this);
                    setLambdaInlining(false);
                    addInlineMarker(this, false);
                    mapper.endMapping();
                    if (inlineOnlySmapSkipper != null) {
                        inlineOnlySmapSkipper.markCallSiteLineNumber(remappingMethodAdapter);
                    }
                }
                else if (isAnonymousConstructorCall(owner, name)) { //TODO add method
                    //TODO add proper message
                    assert transformationInfo instanceof AnonymousObjectTransformationInfo :
                            "<init> call doesn't correspond to object transformation info: " +
                            owner + "." + name + ", info " + transformationInfo;
                    if (transformationInfo.shouldRegenerate(isSameModule)) {
                        //put additional captured parameters on stack
                        AnonymousObjectTransformationInfo info = (AnonymousObjectTransformationInfo) transformationInfo;
                        for (CapturedParamDesc capturedParamDesc : info.getAllRecapturedParameters()) {
                            visitFieldInsn(
                                    Opcodes.GETSTATIC, capturedParamDesc.getContainingLambdaName(),
                                    "$$$" + capturedParamDesc.getFieldName(), capturedParamDesc.getType().getDescriptor()
                            );
                        }
                        super.visitMethodInsn(opcode, transformationInfo.getNewClassName(), name, info.getNewConstructorDescriptor(), itf);

                        //TODO: add new inner class also for other contexts
                        if (inliningContext.getParent() instanceof RegeneratedClassContext) {
                            inliningContext.getParent().typeRemapper.addAdditionalMappings(
                                    transformationInfo.getOldClassName(), transformationInfo.getNewClassName()
                            );
                        }

                        transformationInfo = null;
                    }
                    else {
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                }
                else if (!inliningContext.isInliningLambda &&
                         ReifiedTypeInliner.isNeedClassReificationMarker(new MethodInsnNode(opcode, owner, name, desc, false))) {
                    //we shouldn't process here content of inlining lambda it should be reified at external level
                }
                else {
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                }
            }

            @Override
            public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc) {
                if (opcode == Opcodes.GETSTATIC && (isAnonymousSingletonLoad(owner, name) || isWhenMappingAccess(owner, name))) {
                    handleAnonymousObjectRegeneration();
                }
                super.visitFieldInsn(opcode, owner, name, desc);
            }

            @Override
            public void visitMaxs(int stack, int locals) {
                lambdasFinallyBlocks = resultNode.tryCatchBlocks.size();
                super.visitMaxs(stack, locals);
            }
        };

        node.accept(lambdaInliner);

        return resultNode;
    }

    @NotNull
    public static CapturedParamInfo findCapturedField(@NotNull FieldInsnNode node, @NotNull FieldRemapper fieldRemapper) {
        assert node.name.startsWith("$$$") : "Captured field template should start with $$$ prefix";
        FieldInsnNode fin = new FieldInsnNode(node.getOpcode(), node.owner, node.name.substring(3), node.desc);
        CapturedParamInfo field = fieldRemapper.findField(fin);
        if (field == null) {
            throw new IllegalStateException(
                    "Couldn't find captured field " + node.owner + "." + node.name + " in " + fieldRemapper.getLambdaInternalName()
            );
        }
        return field;
    }

    @NotNull
    private MethodNode prepareNode(@NotNull MethodNode node, int finallyDeepShift) {
        final int capturedParamsSize = parameters.getCapturedArgsSizeOnStack();
        final int realParametersSize = parameters.getRealArgsSizeOnStack();
        Type[] types = Type.getArgumentTypes(node.desc);
        Type returnType = Type.getReturnType(node.desc);

        List<Type> capturedTypes = parameters.getCapturedTypes();
        Type[] allTypes = ArrayUtil.mergeArrays(types, capturedTypes.toArray(new Type[capturedTypes.size()]));

        node.instructions.resetLabels();
        MethodNode transformedNode = new MethodNode(
                InlineCodegenUtil.API, node.access, node.name, Type.getMethodDescriptor(returnType, allTypes), node.signature, null
        ) {
            @SuppressWarnings("ConstantConditions")
            private final boolean GENERATE_DEBUG_INFO = InlineCodegenUtil.GENERATE_SMAP && inlineOnlySmapSkipper == null;

            private final boolean isInliningLambda = nodeRemapper.isInsideInliningLambda();

            private int getNewIndex(int var) {
                return var + (var < realParametersSize ? 0 : capturedParamsSize);
            }

            @Override
            public void visitVarInsn(int opcode, int var) {
                super.visitVarInsn(opcode, getNewIndex(var));
            }

            @Override
            public void visitIincInsn(int var, int increment) {
                super.visitIincInsn(getNewIndex(var), increment);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                super.visitMaxs(maxStack, maxLocals + capturedParamsSize);
            }

            @Override
            public void visitLineNumber(int line, @NotNull Label start) {
                if (isInliningLambda || GENERATE_DEBUG_INFO) {
                    super.visitLineNumber(line, start);
                }
            }

            @Override
            public void visitLocalVariable(
                    @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
            ) {
                if (isInliningLambda || GENERATE_DEBUG_INFO) {
                    String varSuffix =
                            inliningContext.isRoot() && !InlineCodegenUtil.isFakeLocalVariableForInline(name) ? INLINE_FUN_VAR_SUFFIX : "";
                    String varName = !varSuffix.isEmpty() && name.equals("this") ? name + "_" : name;
                    super.visitLocalVariable(varName + varSuffix, desc, signature, start, end, getNewIndex(index));
                }
            }
        };

        node.accept(transformedNode);

        transformCaptured(transformedNode);
        transformFinallyDeepIndex(transformedNode, finallyDeepShift);

        return transformedNode;
    }

    @NotNull
    private MethodNode markPlacesForInlineAndRemoveInlinable(
            @NotNull MethodNode node, @NotNull LabelOwner labelOwner, int finallyDeepShift
    ) {
        node = prepareNode(node, finallyDeepShift);

        Frame<SourceValue>[] sources = analyzeMethodNodeBeforeInline(node);
        LocalReturnsNormalizer localReturnsNormalizer = LocalReturnsNormalizer.createFor(node, labelOwner, sources);

        Set<AbstractInsnNode> toDelete = SmartSet.create();
        InsnList instructions = node.instructions;
        AbstractInsnNode cur = instructions.getFirst();

        boolean awaitClassReification = false;
        int currentFinallyDeep = 0;

        while (cur != null) {
            Frame<SourceValue> frame = sources[instructions.indexOf(cur)];

            if (frame != null) {
                if (ReifiedTypeInliner.isNeedClassReificationMarker(cur)) {
                    awaitClassReification = true;
                }
                else if (cur.getType() == AbstractInsnNode.METHOD_INSN) {
                    if (InlineCodegenUtil.isFinallyStart(cur)) {
                        //TODO deep index calc could be more precise
                        currentFinallyDeep = InlineCodegenUtil.getConstant(cur.getPrevious());
                    }

                    MethodInsnNode methodInsnNode = (MethodInsnNode) cur;
                    String owner = methodInsnNode.owner;
                    String desc = methodInsnNode.desc;
                    String name = methodInsnNode.name;
                    //TODO check closure
                    Type[] argTypes = Type.getArgumentTypes(desc);
                    int paramCount = argTypes.length + 1;//non static
                    int firstParameterIndex = frame.getStackSize() - paramCount;
                    if (isInvokeOnLambda(owner, name) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                        SourceValue sourceValue = frame.getStack(firstParameterIndex);

                        LambdaInfo lambdaInfo = MethodInlinerUtilKt.getLambdaIfExistsAndMarkInstructions(
                                this, MethodInlinerUtilKt.singleOrNullInsn(sourceValue), true, instructions, sources, toDelete
                        );

                        invokeCalls.add(new InvokeCall(lambdaInfo, currentFinallyDeep));
                    }
                    else if (isAnonymousConstructorCall(owner, name)) {
                        Map<Integer, LambdaInfo> lambdaMapping = new HashMap<Integer, LambdaInfo>();

                        int offset = 0;
                        for (int i = 0; i < paramCount; i++) {
                            SourceValue sourceValue = frame.getStack(firstParameterIndex + i);
                            LambdaInfo lambdaInfo = MethodInlinerUtilKt.getLambdaIfExistsAndMarkInstructions(
                                    this, MethodInlinerUtilKt.singleOrNullInsn(sourceValue), false, instructions, sources, toDelete
                            );
                            if (lambdaInfo != null) {
                                lambdaMapping.put(offset, lambdaInfo);
                            }

                            offset += i == 0 ? 1 : argTypes[i - 1].getSize();
                        }

                        transformations.add(
                                buildConstructorInvocation(owner, desc, lambdaMapping, awaitClassReification)
                        );
                        awaitClassReification = false;
                    }
                }
                else if (cur.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) cur;
                    String className = fieldInsnNode.owner;
                    if (isAnonymousSingletonLoad(className, fieldInsnNode.name)) {
                        transformations.add(
                                new AnonymousObjectTransformationInfo(
                                        className, awaitClassReification, isAlreadyRegenerated(className), true,
                                        inliningContext.nameGenerator
                                )
                        );
                        awaitClassReification = false;
                    }
                    else if (isWhenMappingAccess(className, fieldInsnNode.name)) {
                        transformations.add(
                            new WhenMappingTransformationInfo(
                                    className, inliningContext.nameGenerator, isAlreadyRegenerated(className), fieldInsnNode
                            )
                        );
                    }

                }
            }
            AbstractInsnNode prevNode = cur;
            cur = cur.getNext();

            //given frame is <tt>null</tt> if and only if the corresponding instruction cannot be reached (dead code).
            if (frame == null) {
                //clean dead code otherwise there is problems in unreachable finally block, don't touch label it cause try/catch/finally problems
                if (prevNode.getType() == AbstractInsnNode.LABEL) {
                    //NB: Cause we generate exception table for default handler using gaps (see ExpressionCodegen.visitTryExpression)
                    //it may occurs that interval for default handler starts before catch start label, so this label seems as dead,
                    //but as result all this labels will be merged into one (see KT-5863)
                }
                else {
                    toDelete.add(prevNode);
                }
            }
        }

        for (AbstractInsnNode insnNode : toDelete) {
            instructions.remove(insnNode);
        }

        //clean dead try/catch blocks
        List<TryCatchBlockNode> blocks = node.tryCatchBlocks;
        for (Iterator<TryCatchBlockNode> iterator = blocks.iterator(); iterator.hasNext(); ) {
            TryCatchBlockNode block = iterator.next();
            if (isEmptyTryInterval(block)) {
                iterator.remove();
            }
        }

        localReturnsNormalizer.transform(node);

        return node;
    }

    @NotNull
    private Frame<SourceValue>[] analyzeMethodNodeBeforeInline(@NotNull MethodNode node) {
        try {
            new FixStackWithLabelNormalizationMethodTransformer().transform("fake", node);
        }
        catch (Throwable e) {
            throw wrapException(e, node, "couldn't inline method call");
        }

        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter()) {
            @NotNull
            @Override
            protected Frame<SourceValue> newFrame(int nLocals, int nStack) {
                return new Frame<SourceValue>(nLocals, nStack) {
                    @Override
                    public void execute(@NotNull AbstractInsnNode insn, Interpreter<SourceValue> interpreter) throws AnalyzerException {
                        // This can be a void non-local return from a non-void method; Frame#execute would throw and do nothing else.
                        if (insn.getOpcode() == Opcodes.RETURN) return;
                        super.execute(insn, interpreter);
                    }
                };
            }
        };

        try {
            return analyzer.analyze("fake", node);
        }
        catch (AnalyzerException e) {
            throw wrapException(e, node, "couldn't inline method call");
        }
    }

    private static boolean isEmptyTryInterval(@NotNull TryCatchBlockNode tryCatchBlockNode) {
        LabelNode start = tryCatchBlockNode.start;
        AbstractInsnNode end = tryCatchBlockNode.end;
        while (end != start && end instanceof LabelNode) {
            end = end.getPrevious();
        }
        return start == end;
    }

    @NotNull
    private AnonymousObjectTransformationInfo buildConstructorInvocation(
            @NotNull String anonymousType,
            @NotNull String desc,
            @NotNull Map<Integer, LambdaInfo> lambdaMapping,
            boolean needReification
    ) {
        return new AnonymousObjectTransformationInfo(
                anonymousType, needReification, lambdaMapping,
                inliningContext.classRegeneration,
                isAlreadyRegenerated(anonymousType),
                desc,
                false,
                inliningContext.nameGenerator
        );
    }

    private boolean isAlreadyRegenerated(@NotNull String owner) {
        return inliningContext.typeRemapper.hasNoAdditionalMapping(owner);
    }

    @Nullable
    LambdaInfo getLambdaIfExists(@Nullable AbstractInsnNode insnNode) {
        if (insnNode == null) {
            return null;
        }

        if (insnNode.getOpcode() == Opcodes.ALOAD) {
            int varIndex = ((VarInsnNode) insnNode).var;
            return getLambdaIfExists(varIndex);
        }

        if (insnNode instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
            if (fieldInsnNode.name.startsWith("$$$")) {
                return findCapturedField(fieldInsnNode, nodeRemapper).getLambda();
            }
        }

        return null;
    }

    @Nullable
    private LambdaInfo getLambdaIfExists(int varIndex) {
        if (varIndex < parameters.getArgsSizeOnStack()) {
            return parameters.getParameterByDeclarationSlot(varIndex).getLambda();
        }
        return null;
    }

    private static void removeClosureAssertions(@NotNull MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                if (methodInsnNode.name.equals("checkParameterIsNotNull") &&
                    methodInsnNode.owner.equals(IntrinsicMethods.INTRINSICS_CLASS_NAME)) {
                    AbstractInsnNode prev = cur.getPrevious();

                    assert cur.getOpcode() == Opcodes.LDC : "checkParameterIsNotNull should go after LDC but " + cur;
                    assert prev.getOpcode() == Opcodes.ALOAD : "checkParameterIsNotNull should be invoked on local var but " + prev;

                    node.instructions.remove(prev);
                    node.instructions.remove(cur);
                    cur = next.getNext();
                    node.instructions.remove(next);
                    next = cur;
                }
            }
            cur = next;
        }
    }

    private void transformCaptured(@NotNull MethodNode node) {
        if (nodeRemapper.isRoot()) {
            return;
        }

        //Fold all captured variable chain - ALOAD 0 ALOAD this$0 GETFIELD $captured - to GETFIELD $$$$captured
        //On future decoding this field could be inline or unfolded in another field access chain (it can differ in some missed this$0)
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null) {
            if (cur instanceof VarInsnNode && cur.getOpcode() == Opcodes.ALOAD) {
                int varIndex = ((VarInsnNode) cur).var;
                if (varIndex == 0 || nodeRemapper.processNonAload0FieldAccessChains(getLambdaIfExists(varIndex) != null)) {
                    List<AbstractInsnNode> accessChain = getCapturedFieldAccessChain((VarInsnNode) cur);
                    AbstractInsnNode insnNode = nodeRemapper.foldFieldAccessChainIfNeeded(accessChain, node);
                    if (insnNode != null) {
                        cur = insnNode;
                    }
                }
            }
            cur = cur.getNext();
        }
    }

    private static void transformFinallyDeepIndex(@NotNull MethodNode node, int finallyDeepShift) {
        if (finallyDeepShift == 0) {
            return;
        }

        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null) {
            if (cur instanceof MethodInsnNode && InlineCodegenUtil.isFinallyMarker(cur)) {
                AbstractInsnNode constant = cur.getPrevious();
                int curDeep = InlineCodegenUtil.getConstant(constant);
                node.instructions.insert(constant, new LdcInsnNode(curDeep + finallyDeepShift));
                node.instructions.remove(constant);
            }
            cur = cur.getNext();
        }
    }

    @NotNull
    private static List<AbstractInsnNode> getCapturedFieldAccessChain(@NotNull VarInsnNode aload0) {
        List<AbstractInsnNode> fieldAccessChain = new ArrayList<AbstractInsnNode>();
        fieldAccessChain.add(aload0);
        AbstractInsnNode next = aload0.getNext();
        while (next != null && next instanceof FieldInsnNode || next instanceof LabelNode) {
            if (next instanceof LabelNode) {
                next = next.getNext();
                continue; //it will be delete on transformation
            }
            fieldAccessChain.add(next);
            if ("this$0".equals(((FieldInsnNode) next).name)) {
                next = next.getNext();
            }
            else {
                break;
            }
        }

        return fieldAccessChain;
    }

    private static void putStackValuesIntoLocals(
            @NotNull List<Type> directOrder, int shift, @NotNull InstructionAdapter iv, @NotNull String descriptor
    ) {
        Type[] actualParams = Type.getArgumentTypes(descriptor);
        assert actualParams.length == directOrder.size() : "Number of expected and actual params should be equals!";

        int size = 0;
        for (Type next : directOrder) {
            size += next.getSize();
        }

        shift += size;
        int index = directOrder.size();

        for (Type next : Lists.reverse(directOrder)) {
            shift -= next.getSize();
            Type typeOnStack = actualParams[--index];
            if (!typeOnStack.equals(next)) {
                StackValue.onStack(typeOnStack).put(next, iv);
            }
            iv.store(shift, next);
        }
    }

    @NotNull
    private RuntimeException wrapException(@NotNull Throwable originalException, @NotNull MethodNode node, @NotNull String errorSuffix) {
        if (originalException instanceof InlineException) {
            return new InlineException(errorPrefix + ": " + errorSuffix, originalException);
        }
        else {
            return new InlineException(errorPrefix + ": " + errorSuffix + "\nCause: " + getNodeText(node), originalException);
        }
    }

    @NotNull
    //process local and global returns (local substituted with goto end-label global kept unchanged)
    public static List<PointForExternalFinallyBlocks> processReturns(
            @NotNull MethodNode node, @NotNull LabelOwner labelOwner, boolean remapReturn, @Nullable Label endLabel
    ) {
        if (!remapReturn) {
            return Collections.emptyList();
        }
        List<PointForExternalFinallyBlocks> result = new ArrayList<PointForExternalFinallyBlocks>();
        InsnList instructions = node.instructions;
        AbstractInsnNode insnNode = instructions.getFirst();
        while (insnNode != null) {
            if (InlineCodegenUtil.isReturnOpcode(insnNode.getOpcode())) {
                boolean isLocalReturn = true;
                String labelName = InlineCodegenUtil.getMarkedReturnLabelOrNull(insnNode);

                if (labelName != null) {
                    isLocalReturn = labelOwner.isMyLabel(labelName);
                    //remove global return flag
                    if (isLocalReturn) {
                        instructions.remove(insnNode.getPrevious());
                    }
                }

                if (isLocalReturn && endLabel != null) {
                    LabelNode labelNode = (LabelNode) endLabel.info;
                    JumpInsnNode jumpInsnNode = new JumpInsnNode(Opcodes.GOTO, labelNode);
                    instructions.insert(insnNode, jumpInsnNode);
                    instructions.remove(insnNode);
                    insnNode = jumpInsnNode;
                }

                //generate finally block before nonLocalReturn flag/return/goto
                LabelNode label = new LabelNode();
                instructions.insert(insnNode, label);
                result.add(new PointForExternalFinallyBlocks(
                        getInstructionToInsertFinallyBefore(insnNode, isLocalReturn), getReturnType(insnNode.getOpcode()), label
                ));
            }
            insnNode = insnNode.getNext();
        }
        return result;
    }

    private static class LocalReturnsNormalizer {
        private static class LocalReturn {
            private final AbstractInsnNode returnInsn;
            private final AbstractInsnNode insertBeforeInsn;
            private final Frame<SourceValue> frame;

            public LocalReturn(
                    @NotNull AbstractInsnNode returnInsn,
                    @NotNull AbstractInsnNode insertBeforeInsn,
                    @NotNull Frame<SourceValue> frame
            ) {
                this.returnInsn = returnInsn;
                this.insertBeforeInsn = insertBeforeInsn;
                this.frame = frame;
            }

            public void transform(@NotNull InsnList insnList, int returnVariableIndex) {
                boolean isReturnWithValue = returnInsn.getOpcode() != Opcodes.RETURN;

                int expectedStackSize = isReturnWithValue ? 1 : 0;
                int actualStackSize = frame.getStackSize();
                if (expectedStackSize == actualStackSize) return;

                int stackSize = actualStackSize;
                if (isReturnWithValue) {
                    int storeOpcode = Opcodes.ISTORE + returnInsn.getOpcode() - Opcodes.IRETURN;
                    insnList.insertBefore(insertBeforeInsn, new VarInsnNode(storeOpcode, returnVariableIndex));
                    stackSize--;
                }

                while (stackSize > 0) {
                    int stackElementSize = frame.getStack(stackSize - 1).getSize();
                    int popOpcode = stackElementSize == 1 ? Opcodes.POP : Opcodes.POP2;
                    insnList.insertBefore(insertBeforeInsn, new InsnNode(popOpcode));
                    stackSize--;
                }

                if (isReturnWithValue) {
                    int loadOpcode = Opcodes.ILOAD + returnInsn.getOpcode() - Opcodes.IRETURN;
                    insnList.insertBefore(insertBeforeInsn, new VarInsnNode(loadOpcode, returnVariableIndex));
                }
            }
        }

        private final List<LocalReturn> localReturns = new SmartList<LocalReturn>();

        private boolean needsReturnVariable = false;
        private int returnOpcode = -1;

        private void addLocalReturnToTransform(
                @NotNull AbstractInsnNode returnInsn,
                @NotNull AbstractInsnNode insertBeforeInsn,
                @NotNull Frame<SourceValue> sourceValueFrame
        ) {
            assert InlineCodegenUtil.isReturnOpcode(returnInsn.getOpcode()) : "return instruction expected";
            assert returnOpcode < 0 || returnOpcode == returnInsn.getOpcode() :
                    "Return op should be " + Printer.OPCODES[returnOpcode] + ", got " + Printer.OPCODES[returnInsn.getOpcode()];
            returnOpcode = returnInsn.getOpcode();

            localReturns.add(new LocalReturn(returnInsn, insertBeforeInsn, sourceValueFrame));

            if (returnInsn.getOpcode() != Opcodes.RETURN && sourceValueFrame.getStackSize() > 1) {
                needsReturnVariable = true;
            }
        }

        public void transform(@NotNull MethodNode methodNode) {
            int returnVariableIndex = -1;
            if (needsReturnVariable) {
                returnVariableIndex = methodNode.maxLocals;
                methodNode.maxLocals++;
            }

            for (LocalReturn localReturn : localReturns) {
                localReturn.transform(methodNode.instructions, returnVariableIndex);
            }
        }

        @NotNull
        public static LocalReturnsNormalizer createFor(
                @NotNull MethodNode methodNode,
                @NotNull LabelOwner owner,
                @NotNull Frame<SourceValue>[] frames
        ) {
            LocalReturnsNormalizer result = new LocalReturnsNormalizer();

            AbstractInsnNode[] instructions = methodNode.instructions.toArray();

            for (int i = 0; i < instructions.length; ++i) {
                Frame<SourceValue> frame = frames[i];
                // Don't care about dead code, it will be eliminated
                if (frame == null) continue;

                AbstractInsnNode insnNode = instructions[i];
                if (!InlineCodegenUtil.isReturnOpcode(insnNode.getOpcode())) continue;

                AbstractInsnNode insertBeforeInsn = insnNode;

                // TODO extract isLocalReturn / isNonLocalReturn, see processReturns
                String labelName = getMarkedReturnLabelOrNull(insnNode);
                if (labelName != null) {
                    if (!owner.isMyLabel(labelName)) continue;
                    insertBeforeInsn = insnNode.getPrevious();
                }

                result.addLocalReturnToTransform(insnNode, insertBeforeInsn, frame);
            }

            return result;
        }
    }

    @NotNull
    private static AbstractInsnNode getInstructionToInsertFinallyBefore(@NotNull AbstractInsnNode nonLocalReturnOrJump, boolean isLocal) {
        return isLocal ? nonLocalReturnOrJump : nonLocalReturnOrJump.getPrevious();
    }

    //Place to insert finally blocks from try blocks that wraps inline fun call
    public static class PointForExternalFinallyBlocks {
        public final AbstractInsnNode beforeIns;
        public final Type returnType;
        public final LabelNode finallyIntervalEnd;

        public PointForExternalFinallyBlocks(
                @NotNull AbstractInsnNode beforeIns,
                @NotNull Type returnType,
                @NotNull LabelNode finallyIntervalEnd
        ) {
            this.beforeIns = beforeIns;
            this.returnType = returnType;
            this.finallyIntervalEnd = finallyIntervalEnd;
        }
    }
}
