package org.jetbrains.jet.codegen.inline;

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Label;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.commons.RemappingMethodAdapter;
import org.jetbrains.asm4.tree.*;
import org.jetbrains.asm4.tree.analysis.*;
import org.jetbrains.jet.codegen.ClosureCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.JetTypeMapper;

import java.util.*;

import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.isInvokeOnLambda;
import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.isLambdaConstructorCall;

public class MethodInliner {

    private final MethodNode node;

    private final Parameters parameters;

    private final InliningContext inliningContext;

    private final FieldRemapper nodeRemapper;

    private final boolean isSameModule;

    private final String errorPrefix;

    private final JetTypeMapper typeMapper;

    private final List<InvokeCall> invokeCalls = new ArrayList<InvokeCall>();

    //keeps order
    private final List<ConstructorInvocation> constructorInvocations = new ArrayList<ConstructorInvocation>();
    //current state
    private final Map<String, String> currentTypeMapping = new HashMap<String, String>();

    private final InlineResult result;

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
            @NotNull String errorPrefix
    ) {
        this.node = node;
        this.parameters = parameters;
        this.inliningContext = parent;
        this.nodeRemapper = nodeRemapper;
        this.isSameModule = isSameModule;
        this.errorPrefix = errorPrefix;
        this.typeMapper = parent.state.getTypeMapper();
        this.result = InlineResult.create();
    }


    public InlineResult doInline(MethodVisitor adapter, VarRemapper.ParamRemapper remapper, FieldRemapper capturedRemapper) {
        return doInline(adapter, remapper, capturedRemapper, true);
    }

    public InlineResult doInline(
            MethodVisitor adapter,
            VarRemapper.ParamRemapper remapper,
            FieldRemapper capturedRemapper, boolean remapReturn
    ) {
        //analyze body
        MethodNode transformedNode = markPlacesForInlineAndRemoveInlinable(node);

        transformedNode = doInline(transformedNode, capturedRemapper);
        removeClosureAssertions(transformedNode);
        transformedNode.instructions.resetLabels();

        Label end = new Label();
        RemapVisitor visitor = new RemapVisitor(adapter, end, remapper, remapReturn, nodeRemapper);
        try {
            transformedNode.accept(visitor);
        }
        catch (Exception e) {
            throw wrapException(e, transformedNode, "couldn't inline method call");
        }

        visitor.visitLabel(end);

        return result;
    }

    private MethodNode doInline(MethodNode node, final FieldRemapper capturedRemapper) {

        final Deque<InvokeCall> currentInvokes = new LinkedList<InvokeCall>(invokeCalls);

        MethodNode resultNode = new MethodNode(node.access, node.name, node.desc, node.signature, null);

        final Iterator<ConstructorInvocation> iterator = constructorInvocations.iterator();

        RemappingMethodAdapter remappingMethodAdapter = new RemappingMethodAdapter(resultNode.access, resultNode.desc, resultNode,
                                                                                   new TypeRemapper(currentTypeMapping));

        InlineAdapter inliner = new InlineAdapter(remappingMethodAdapter, parameters.totalSize()) {

            private ConstructorInvocation invocation;
            @Override
            public void anew(Type type) {
                if (isLambdaConstructorCall(type.getInternalName(), "<init>")) {
                    invocation = iterator.next();

                    if (invocation.shouldRegenerate()) {
                        //TODO: need poping of type but what to do with local funs???
                        Type newLambdaType = Type.getObjectType(inliningContext.nameGenerator.genLambdaClassName());
                        currentTypeMapping.put(invocation.getOwnerInternalName(), newLambdaType.getInternalName());
                        LambdaTransformer transformer = new LambdaTransformer(invocation.getOwnerInternalName(),
                                                                              inliningContext.subInline(inliningContext.nameGenerator, currentTypeMapping).classRegeneration(),
                                                                              isSameModule, newLambdaType);

                        InlineResult transformResult = transformer.doTransform(invocation, capturedRemapper);
                        result.addAllClassesToRemove(transformResult);

                        if (inliningContext.isInliningLambda) {
                            //this class is transformed and original not used so we should remove original one after inlining
                            result.addClassToRemove(invocation.getOwnerInternalName());
                        }
                    }
                }

                //in case of regenerated invocation type would be remapped to new one via remappingMethodAdapter
                super.anew(type);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                if (/*INLINE_RUNTIME.equals(owner) &&*/ isInvokeOnLambda(owner, name)) { //TODO add method
                    assert !currentInvokes.isEmpty();
                    InvokeCall invokeCall = currentInvokes.remove();
                    LambdaInfo info = invokeCall.lambdaInfo;

                    if (info == null) {
                        //noninlinable lambda
                        super.visitMethodInsn(opcode, owner, name, desc);
                        return;
                    }

                    int valueParamShift = getNextLocalIndex();//NB: don't inline cause it changes
                    putStackValuesIntoLocals(info.getParamsWithoutCapturedValOrVar(), valueParamShift, this, desc);

                    Parameters lambdaParameters = info.addAllParameters(capturedRemapper);

                    InlinedLambdaRemapper newCapturedRemapper =
                            new InlinedLambdaRemapper(info.getLambdaClassType().getInternalName(), capturedRemapper, lambdaParameters);

                    FieldRemapper fieldRemapper =
                            new FieldRemapper(info.getLambdaClassType().getInternalName(), capturedRemapper, lambdaParameters);

                    setInlining(true);
                    MethodInliner inliner = new MethodInliner(info.getNode(), lambdaParameters,
                                                              inliningContext.subInlineLambda(info),
                                                              fieldRemapper, true /*cause all calls in same module as lambda*/,
                                                              "Lambda inlining " + info.getLambdaClassType().getInternalName());

                    VarRemapper.ParamRemapper remapper = new VarRemapper.ParamRemapper(lambdaParameters, valueParamShift);
                    InlineResult lambdaResult = inliner.doInline(this.mv, remapper, newCapturedRemapper);//TODO add skipped this and receiver
                    result.addAllClassesToRemove(lambdaResult);

                    //return value boxing/unboxing
                    Method bridge = typeMapper.mapSignature(ClosureCodegen.getInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), this);
                    setInlining(false);
                }
                else if (isLambdaConstructorCall(owner, name)) { //TODO add method
                    assert invocation != null : "<init> call not corresponds to new call" + owner + " " + name;
                    if (invocation.shouldRegenerate()) {
                        //put additional captured parameters on stack
                        for (CapturedParamInfo capturedParamInfo : invocation.getAllRecapturedParameters()) {
                            visitFieldInsn(Opcodes.GETSTATIC, capturedParamInfo.getContainingLambdaName(), "$$$" + capturedParamInfo.getFieldName(), capturedParamInfo.getType().getDescriptor());
                        }
                        super.visitMethodInsn(opcode, invocation.getNewLambdaType().getInternalName(), name, invocation.getNewConstructorDescriptor());
                        invocation = null;
                    } else {
                        super.visitMethodInsn(opcode, changeOwnerForExternalPackage(owner, opcode), name, desc);
                    }
                }
                else {
                    super.visitMethodInsn(opcode, changeOwnerForExternalPackage(owner, opcode), name, desc);
                }
            }

        };

        node.accept(inliner);

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
        MethodNode transformedNode = new MethodNode(node.access, node.name, Type.getMethodDescriptor(returnType, allTypes), node.signature, null) {

            @Override
            public void visitVarInsn(int opcode, int var) {
                int newIndex;
                if (var < realParametersSize) {
                    newIndex = var;
                } else {
                    newIndex = var + capturedParamsSize;
                }
                super.visitVarInsn(opcode, newIndex);
            }

            @Override
            public void visitIincInsn(int var, int increment) {
                int newIndex;
                if (var < realParametersSize) {
                    newIndex = var;
                } else {
                    newIndex = var + capturedParamsSize;
                }
                super.visitIincInsn(newIndex, increment);
            }

            @Override
            public void visitMaxs(int maxStack, int maxLocals) {
                super.visitMaxs(maxStack, maxLocals + capturedParamsSize);
            }
        };

        node.accept(transformedNode);

        transformCaptured(transformedNode);

        return transformedNode;
    }

    @NotNull
    protected MethodNode markPlacesForInlineAndRemoveInlinable(@NotNull MethodNode node) {
        node = prepareNode(node);

        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter());
        Frame<SourceValue>[] sources;
        try {
            sources = analyzer.analyze("fake", node);
        }
        catch (AnalyzerException e) {
            throw wrapException(e, node, "couldn't inline method call");
        }

        AbstractInsnNode cur = node.instructions.getFirst();
        int index = 0;
        Set<LabelNode> deadLabels = new HashSet<LabelNode>();

        while (cur != null) {
            Frame<SourceValue> frame = sources[index];

            if (frame != null) {
                if (cur.getType() == AbstractInsnNode.METHOD_INSN) {
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
                    else if (isLambdaConstructorCall(owner, name)) {
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

                        constructorInvocations.add(new ConstructorInvocation(owner, lambdaMapping, isSameModule, inliningContext.classRegeneration));
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
                    deadLabels.add((LabelNode) prevNode);
                } else {
                    node.instructions.remove(prevNode);
                }
            }
        }

        //clean dead try/catch blocks
        List<TryCatchBlockNode> blocks = node.tryCatchBlocks;
        for (Iterator<TryCatchBlockNode> iterator = blocks.iterator(); iterator.hasNext(); ) {
            TryCatchBlockNode block = iterator.next();
            if (deadLabels.contains(block.start) && deadLabels.contains(block.end)) {
                iterator.remove();
            }
        }

        return node;
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
                if (methodInsnNode.name.equals("checkParameterIsNotNull") && methodInsnNode.owner.equals("kotlin/jvm/internal/Intrinsics")) {
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
                    AbstractInsnNode insnNode = nodeRemapper.transformIfNeeded(accessChain, node);
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

    //TODO: check annotation on class - it's package part
    //TODO: check it's external module
    //TODO?: assert method exists in facade?
    public String changeOwnerForExternalPackage(String type, int opcode) {
        if (isSameModule || (opcode & Opcodes.INVOKESTATIC) == 0) {
            return type;
        }

        int i = type.indexOf('-');
        if (i >= 0) {
            return type.substring(0, i);
        }
        return type;
    }


    public RuntimeException wrapException(@NotNull Exception originalException, @NotNull MethodNode node, @NotNull String errorSuffix) {
        if (originalException instanceof InlineException) {
            return new InlineException(errorPrefix + ": " + errorSuffix, originalException);
        } else {
            return new InlineException(errorPrefix + ": " + errorSuffix + "\ncause: " +
                                       InlineCodegen.getNodeText(node), originalException);
        }
    }
}
