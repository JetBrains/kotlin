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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.ClosureCodegen;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import org.jetbrains.kotlin.resolve.jvm.JvmClassName;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache;
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.commons.RemappingMethodAdapter;
import org.jetbrains.org.objectweb.asm.tree.*;
import org.jetbrains.org.objectweb.asm.tree.analysis.*;

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

    private final JetTypeMapper typeMapper;

    private final List<InvokeCall> invokeCalls = new ArrayList<InvokeCall>();

    //keeps order
    private final List<AnonymousObjectGeneration> anonymousObjectGenerations = new ArrayList<AnonymousObjectGeneration>();
    //current state
    private final Map<String, String> currentTypeMapping = new HashMap<String, String>();

    private final InlineResult result;

    private int lambdasFinallyBlocks;

    /*
     *
     * @param node
     * @param parameters
     * @param inliningContext
     * @param lambdaType - in case on lambda 'invoke' inlining
     */
    public MethodInliner(
            @NotNull MethodNode node,
            @NotNull Parameters parameters,
            @NotNull InliningContext parent,
            @NotNull FieldRemapper nodeRemapper,
            boolean isSameModule,
            @NotNull String errorPrefix,
            @NotNull SourceMapper sourceMapper
    ) {
        this.node = node;
        this.parameters = parameters;
        this.inliningContext = parent;
        this.nodeRemapper = nodeRemapper;
        this.isSameModule = isSameModule;
        this.errorPrefix = errorPrefix;
        this.sourceMapper = sourceMapper;
        this.typeMapper = parent.state.getTypeMapper();
        this.result = InlineResult.create();
    }

    public InlineResult doInline(
            @NotNull MethodVisitor adapter,
            @NotNull LocalVarRemapper remapper,
            boolean remapReturn,
            @NotNull LabelOwner labelOwner
    ) {
        //analyze body
        MethodNode transformedNode = markPlacesForInlineAndRemoveInlinable(node);

        //substitute returns with "goto end" instruction to keep non local returns in lambdas
        Label end = new Label();
        transformedNode = doInline(transformedNode);
        removeClosureAssertions(transformedNode);
        InsnList instructions = transformedNode.instructions;
        instructions.resetLabels();

        MethodNode resultNode = new MethodNode(InlineCodegenUtil.API, transformedNode.access, transformedNode.name, transformedNode.desc,
                                         transformedNode.signature, ArrayUtil.toStringArray(transformedNode.exceptions));
        RemapVisitor visitor = new RemapVisitor(resultNode, remapper, nodeRemapper);
        try {
            transformedNode.accept(visitor);
        }
        catch (Exception e) {
            throw wrapException(e, transformedNode, "couldn't inline method call");
        }

        resultNode.visitLabel(end);

        if (inliningContext.isRoot()) {
            InternalFinallyBlockInliner.processInlineFunFinallyBlocks(resultNode, lambdasFinallyBlocks);
        }

        processReturns(resultNode, labelOwner, remapReturn, end);
        //flush transformed node to output
        resultNode.accept(new InliningInstructionAdapter(adapter));

        sourceMapper.endMapping();
        return result;
    }

    private MethodNode doInline(MethodNode node) {

        final Deque<InvokeCall> currentInvokes = new LinkedList<InvokeCall>(invokeCalls);

        final MethodNode resultNode = new MethodNode(node.access, node.name, node.desc, node.signature, null);

        final Iterator<AnonymousObjectGeneration> iterator = anonymousObjectGenerations.iterator();

        RemappingMethodAdapter remappingMethodAdapter = new RemappingMethodAdapter(resultNode.access, resultNode.desc, resultNode,
                                                                                   new TypeRemapper(currentTypeMapping));

        InlineAdapter lambdaInliner = new InlineAdapter(remappingMethodAdapter, parameters.totalSize(), sourceMapper) {

            private AnonymousObjectGeneration anonymousObjectGen;
            private void handleAnonymousObjectGeneration() {
                anonymousObjectGen = iterator.next();

                if (anonymousObjectGen.shouldRegenerate()) {
                    //TODO: need poping of type but what to do with local funs???
                    Type newLambdaType = Type.getObjectType(inliningContext.nameGenerator.genLambdaClassName());
                    currentTypeMapping.put(anonymousObjectGen.getOwnerInternalName(), newLambdaType.getInternalName());
                    AnonymousObjectTransformer transformer =
                            new AnonymousObjectTransformer(anonymousObjectGen.getOwnerInternalName(),
                                                           inliningContext
                                                                   .subInlineWithClassRegeneration(
                                                                           inliningContext.nameGenerator,
                                                                           currentTypeMapping,
                                                                           anonymousObjectGen),
                                                           isSameModule, newLambdaType
                            );

                    InlineResult transformResult = transformer.doTransform(anonymousObjectGen, nodeRemapper);
                    result.addAllClassesToRemove(transformResult);

                    if (inliningContext.isInliningLambda && !anonymousObjectGen.isStaticOrigin()) {
                        // this class is transformed and original not used so we should remove original one after inlining
                        // Note: It is unsafe to remove anonymous class that is referenced by GETSTATIC within lambda
                        // because it can be local function from outer scope
                        result.addClassToRemove(anonymousObjectGen.getOwnerInternalName());
                    }

                    if (transformResult.getReifiedTypeParametersUsages().wereUsedReifiedParameters()) {
                        ReifiedTypeInliner.putNeedClassReificationMarker(mv);
                        result.getReifiedTypeParametersUsages().mergeAll(transformResult.getReifiedTypeParametersUsages());
                    }
                }
            }

            @Override
            public void anew(@NotNull Type type) {
                if (isAnonymousConstructorCall(type.getInternalName(), "<init>")) {
                    handleAnonymousObjectGeneration();
                }

                //in case of regenerated anonymousObjectGen type would be remapped to new one via remappingMethodAdapter
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

                    int valueParamShift = getNextLocalIndex();//NB: don't inline cause it changes
                    putStackValuesIntoLocals(info.getInvokeParamsWithoutCaptured(), valueParamShift, this, desc);

                    addInlineMarker(this, true);
                    Parameters lambdaParameters = info.addAllParameters(nodeRemapper);

                    InlinedLambdaRemapper newCapturedRemapper =
                            new InlinedLambdaRemapper(info.getLambdaClassType().getInternalName(), nodeRemapper, lambdaParameters);

                    setLambdaInlining(true);
                    SMAP lambdaSMAP = info.getNode().getClassSMAP();
                    SourceMapper mapper =
                            inliningContext.classRegeneration && !inliningContext.isInliningLambda ?
                            new NestedSourceMapper(sourceMapper, lambdaSMAP.getIntervals(), lambdaSMAP.getSourceInfo())
                            : new InlineLambdaSourceMapper(sourceMapper.getParent(), info.getNode());
                    MethodInliner inliner = new MethodInliner(info.getNode().getNode(), lambdaParameters,
                                                              inliningContext.subInlineLambda(info),
                                                              newCapturedRemapper, true /*cause all calls in same module as lambda*/,
                                                              "Lambda inlining " + info.getLambdaClassType().getInternalName(),
                                                              mapper);

                    LocalVarRemapper remapper = new LocalVarRemapper(lambdaParameters, valueParamShift);
                    InlineResult lambdaResult = inliner.doInline(this.mv, remapper, true, info);//TODO add skipped this and receiver
                    result.addAllClassesToRemove(lambdaResult);

                    //return value boxing/unboxing
                    Method bridge =
                            typeMapper.mapSignature(ClosureCodegen.getErasedInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), this);
                    setLambdaInlining(false);
                    addInlineMarker(this, false);
                    mapper.endMapping();
                }
                else if (isAnonymousConstructorCall(owner, name)) { //TODO add method
                    assert anonymousObjectGen != null : "<init> call not corresponds to new call" + owner + " " + name;
                    if (anonymousObjectGen.shouldRegenerate()) {
                        //put additional captured parameters on stack
                        for (CapturedParamDesc capturedParamDesc : anonymousObjectGen.getAllRecapturedParameters()) {
                            visitFieldInsn(Opcodes.GETSTATIC, capturedParamDesc.getContainingLambdaName(),
                                           "$$$" + capturedParamDesc.getFieldName(), capturedParamDesc.getType().getDescriptor());
                        }
                        super.visitMethodInsn(opcode, anonymousObjectGen.getNewLambdaType().getInternalName(), name, anonymousObjectGen.getNewConstructorDescriptor(), itf);
                        anonymousObjectGen = null;
                    } else {
                        super.visitMethodInsn(opcode, changeOwnerForExternalPackage(owner, opcode), name, desc, itf);
                    }
                }
                else if (ReifiedTypeInliner.isNeedClassReificationMarker(new MethodInsnNode(opcode, owner, name, desc, false))) {
                    // we will put it if needed in anew processing
                }
                else {
                    super.visitMethodInsn(opcode, changeOwnerForExternalPackage(owner, opcode), name, desc, itf);
                }
            }

            @Override
            public void visitFieldInsn(int opcode, @NotNull String owner, @NotNull String name, @NotNull String desc) {
                if (opcode == Opcodes.GETSTATIC && isAnonymousSingletonLoad(owner, name)) {
                    handleAnonymousObjectGeneration();
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
    public static CapturedParamInfo findCapturedField(FieldInsnNode node, FieldRemapper fieldRemapper) {
        assert node.name.startsWith("$$$") : "Captured field template should start with $$$ prefix";
        FieldInsnNode fin = new FieldInsnNode(node.getOpcode(), node.owner, node.name.substring(3), node.desc);
        CapturedParamInfo field = fieldRemapper.findField(fin);
        if (field == null) {
            throw new IllegalStateException("Couldn't find captured field " + node.owner + "." + node.name + " in " + fieldRemapper.getLambdaInternalName());
        }
        return field;
    }

    @NotNull
    public MethodNode prepareNode(@NotNull MethodNode node) {
        final int capturedParamsSize = parameters.getCaptured().size();
        final int realParametersSize = parameters.getReal().size();
        Type[] types = Type.getArgumentTypes(node.desc);
        Type returnType = Type.getReturnType(node.desc);

        ArrayList<Type> capturedTypes = parameters.getCapturedTypes();
        Type[] allTypes = ArrayUtil.mergeArrays(types, capturedTypes.toArray(new Type[capturedTypes.size()]));

        node.instructions.resetLabels();
        MethodNode transformedNode = new MethodNode(InlineCodegenUtil.API, node.access, node.name, Type.getMethodDescriptor(returnType, allTypes), node.signature, null) {

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
            public void visitLocalVariable(
                    @NotNull String name, @NotNull String desc, String signature, @NotNull Label start, @NotNull Label end, int index
            ) {
                if (isInliningLambda) {
                    super.visitLocalVariable(name, desc, signature, start, end, getNewIndex(index));
                }
            }
        };

        node.accept(transformedNode);

        transformCaptured(transformedNode);

        return transformedNode;
    }

    @NotNull
    protected MethodNode markPlacesForInlineAndRemoveInlinable(@NotNull MethodNode node) {
        node = prepareNode(node);

        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter()) {
            @NotNull
            @Override
            protected Frame<SourceValue> newFrame(
                    int nLocals, int nStack
            ) {
                return new Frame<SourceValue>(nLocals, nStack) {
                    @Override
                    public void execute(
                            @NotNull AbstractInsnNode insn, Interpreter<SourceValue> interpreter
                    ) throws AnalyzerException {
                        if (insn.getOpcode() == Opcodes.RETURN) {
                            //there is exception on void non local return in frame
                            return;
                        }
                        super.execute(insn, interpreter);
                    }
                };
            }
        };

        Frame<SourceValue>[] sources;
        try {
            sources = analyzer.analyze("fake", node);
        }
        catch (AnalyzerException e) {
            throw wrapException(e, node, "couldn't inline method call");
        }

        AbstractInsnNode cur = node.instructions.getFirst();
        int index = 0;

        boolean awaitClassReification = false;

        while (cur != null) {
            Frame<SourceValue> frame = sources[index];

            if (frame != null) {
                if (ReifiedTypeInliner.isNeedClassReificationMarker(cur)) {
                    awaitClassReification = true;
                }
                else if (cur.getType() == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) cur;
                    String owner = methodInsnNode.owner;
                    String desc = methodInsnNode.desc;
                    String name = methodInsnNode.name;
                    //TODO check closure
                    int paramLength = Type.getArgumentTypes(desc).length + 1;//non static
                    if (isInvokeOnLambda(owner, name) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                        SourceValue sourceValue = frame.getStack(frame.getStackSize() - paramLength);

                        LambdaInfo lambdaInfo = null;
                        int varIndex = -1;

                        if (sourceValue.insns.size() == 1) {
                            AbstractInsnNode insnNode = sourceValue.insns.iterator().next();

                            lambdaInfo = getLambdaIfExists(insnNode);
                            if (lambdaInfo != null) {
                                //remove inlinable access
                                node.instructions.remove(insnNode);
                            }
                        }

                        invokeCalls.add(new InvokeCall(varIndex, lambdaInfo));
                    }
                    else if (isAnonymousConstructorCall(owner, name)) {
                        Map<Integer, LambdaInfo> lambdaMapping = new HashMap<Integer, LambdaInfo>();
                        int paramStart = frame.getStackSize() - paramLength;

                        for (int i = 0; i < paramLength; i++) {
                            SourceValue sourceValue = frame.getStack(paramStart + i);
                            if (sourceValue.insns.size() == 1) {
                                AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                                LambdaInfo lambdaInfo = getLambdaIfExists(insnNode);
                                if (lambdaInfo != null) {
                                    lambdaMapping.put(i, lambdaInfo);
                                    node.instructions.remove(insnNode);
                                }
                            }
                        }

                        anonymousObjectGenerations.add(
                                buildConstructorInvocation(
                                        owner, desc, lambdaMapping, awaitClassReification
                                )
                        );
                        awaitClassReification = false;
                    }
                }
                else if (cur.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode fieldInsnNode = (FieldInsnNode) cur;
                    String owner = fieldInsnNode.owner;
                    if (isAnonymousSingletonLoad(owner, fieldInsnNode.name)) {
                        anonymousObjectGenerations.add(
                                new AnonymousObjectGeneration(
                                        owner, isSameModule, awaitClassReification, isAlreadyRegenerated(owner), true
                                )
                        );
                        awaitClassReification = false;
                    }
                }
            }
            AbstractInsnNode prevNode = cur;
            cur = cur.getNext();
            index++;

            //given frame is <tt>null</tt> if and only if the corresponding instruction cannot be reached (dead code).
            if (frame == null) {
                //clean dead code otherwise there is problems in unreachable finally block, don't touch label it cause try/catch/finally problems
                if (prevNode.getType() == AbstractInsnNode.LABEL) {
                    //NB: Cause we generate exception table for default handler using gaps (see ExpressionCodegen.visitTryExpression)
                    //it may occurs that interval for default handler starts before catch start label, so this label seems as dead,
                    //but as result all this labels will be merged into one (see KT-5863)
                } else {
                    node.instructions.remove(prevNode);
                }
            }
        }

        //clean dead try/catch blocks
        List<TryCatchBlockNode> blocks = node.tryCatchBlocks;
        for (Iterator<TryCatchBlockNode> iterator = blocks.iterator(); iterator.hasNext(); ) {
            TryCatchBlockNode block = iterator.next();
            if (isEmptyTryInterval(block)) {
                iterator.remove();
            }
        }

        return node;
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
    private AnonymousObjectGeneration buildConstructorInvocation(
            @NotNull String owner,
            @NotNull String desc,
            @NotNull Map<Integer, LambdaInfo> lambdaMapping,
            boolean needReification
    ) {
        return new AnonymousObjectGeneration(
                owner, needReification, isSameModule, lambdaMapping,
                inliningContext.classRegeneration,
                isAlreadyRegenerated(owner),
                desc,
                false
        );
    }

    private boolean isAlreadyRegenerated(@NotNull String owner) {
        return inliningContext.typeMapping.containsKey(owner);
    }

    public LambdaInfo getLambdaIfExists(AbstractInsnNode insnNode) {
        if (insnNode.getOpcode() == Opcodes.ALOAD) {
            int varIndex = ((VarInsnNode) insnNode).var;
            if (varIndex < parameters.totalSize()) {
                return parameters.get(varIndex).getLambda();
            }
        }
        else if (insnNode instanceof FieldInsnNode) {
            FieldInsnNode fieldInsnNode = (FieldInsnNode) insnNode;
            if (fieldInsnNode.name.startsWith("$$$")) {
                return findCapturedField(fieldInsnNode, nodeRemapper).getLambda();
            }
        }

        return null;
    }

    private static void removeClosureAssertions(MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                if (methodInsnNode.name.equals("checkParameterIsNotNull") && methodInsnNode.owner.equals(IntrinsicMethods.INTRINSICS_CLASS_NAME)) {
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
                if (((VarInsnNode) cur).var == 0) {
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

    @NotNull
    public static List<AbstractInsnNode> getCapturedFieldAccessChain(@NotNull VarInsnNode aload0) {
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

    public static void putStackValuesIntoLocals(List<Type> directOrder, int shift, InstructionAdapter iv, String descriptor) {
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

    //TODO: check it's external module
    //TODO?: assert method exists in facade?
    public String changeOwnerForExternalPackage(String type, int opcode) {
        if (isSameModule || (opcode & Opcodes.INVOKESTATIC) == 0) {
            return type;
        }

        JvmClassName name = JvmClassName.byInternalName(type);
        String packageClassInternalName = PackageClassUtils.getPackageClassInternalName(name.getPackageFqName());
        if (type.startsWith(packageClassInternalName + '$')) {
            VirtualFile virtualFile = InlineCodegenUtil.findVirtualFile(inliningContext.state.getProject(), type);
            if (virtualFile != null) {
                KotlinJvmBinaryClass klass = KotlinBinaryClassCache.getKotlinBinaryClass(virtualFile);
                if (klass != null && klass.getClassHeader().getSyntheticClassKind() == KotlinSyntheticClass.Kind.PACKAGE_PART) {
                    return packageClassInternalName;
                }
            }
        }

        return type;
    }

    @NotNull
    public RuntimeException wrapException(@NotNull Exception originalException, @NotNull MethodNode node, @NotNull String errorSuffix) {
        if (originalException instanceof InlineException) {
            return new InlineException(errorPrefix + ": " + errorSuffix, originalException);
        }
        else {
            return new InlineException(errorPrefix + ": " + errorSuffix + "\ncause: " +
                                       getNodeText(node), originalException);
        }
    }

    @NotNull
    //process local and global returns (local substituted with goto end-label global kept unchanged)
    public static List<PointForExternalFinallyBlocks> processReturns(@NotNull MethodNode node, @NotNull LabelOwner labelOwner, boolean remapReturn, Label endLabel) {
        if (!remapReturn) {
            return Collections.emptyList();
        }
        List<PointForExternalFinallyBlocks> result = new ArrayList<PointForExternalFinallyBlocks>();
        InsnList instructions = node.instructions;
        AbstractInsnNode insnNode = instructions.getFirst();
        while (insnNode != null) {
            if (InlineCodegenUtil.isReturnOpcode(insnNode.getOpcode())) {
                AbstractInsnNode previous = insnNode.getPrevious();
                MethodInsnNode flagNode;
                boolean isLocalReturn = true;
                String labelName = null;
                if (previous != null && previous instanceof MethodInsnNode && InlineCodegenUtil.NON_LOCAL_RETURN.equals(((MethodInsnNode) previous).owner)) {
                    flagNode = (MethodInsnNode) previous;
                    labelName = flagNode.name;
                }

                if (labelName != null) {
                    isLocalReturn = labelOwner.isMyLabel(labelName);
                    //remove global return flag
                    if (isLocalReturn) {
                        instructions.remove(previous);
                    }
                }

                if (isLocalReturn && endLabel != null) {
                    LabelNode labelNode = (LabelNode) endLabel.info;
                    JumpInsnNode jumpInsnNode = new JumpInsnNode(Opcodes.GOTO, labelNode);
                    instructions.insert(insnNode, jumpInsnNode);
                    instructions.remove(insnNode);
                    insnNode = jumpInsnNode;
                }

                //genetate finally block before nonLocalReturn flag/return/goto
                result.add(new PointForExternalFinallyBlocks(isLocalReturn ? insnNode : insnNode.getPrevious(), getReturnType(insnNode.getOpcode())
                ));
            }
            insnNode = insnNode.getNext();
        }
        return result;
    }

    //Place to insert finally blocks from try blocks that wraps inline fun call
    public static class PointForExternalFinallyBlocks {

        final AbstractInsnNode beforeIns;

        final Type returnType;

        public PointForExternalFinallyBlocks(AbstractInsnNode beforeIns, Type returnType) {
            this.beforeIns = beforeIns;
            this.returnType = returnType;
        }

    }

}
