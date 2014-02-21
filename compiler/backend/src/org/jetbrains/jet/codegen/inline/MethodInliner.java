package org.jetbrains.jet.codegen.inline;

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.jetbrains.jet.utils.UtilsPackage;

import java.util.*;

import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.*;

public class MethodInliner {

    private final MethodNode node;

    private final Parameters parameters;

    private final InliningInfo parent;

    @Nullable
    private final Type lambdaType;

    private final LambdaFieldRemapper lambdaFieldRemapper;

    private final boolean isSameModule;

    private final JetTypeMapper typeMapper;

    private final List<InvokeCall> invokeCalls = new ArrayList<InvokeCall>();

    //keeps order
    private final List<ConstructorInvocation> constructorInvocations = new ArrayList<ConstructorInvocation>();
    //current state
    private final Map<String, String> currentTypeMapping = new HashMap<String, String>();

    /*
     *
     * @param node
     * @param parameters
     * @param parent
     * @param lambdaType - in case on lambda 'invoke' inlining
     */
    public MethodInliner(
            @NotNull MethodNode node,
            @NotNull Parameters parameters,
            @NotNull InliningInfo parent,
            @Nullable Type lambdaType,
            LambdaFieldRemapper lambdaFieldRemapper,
            boolean isSameModule
    ) {
        this.node = node;
        this.parameters = parameters;
        this.parent = parent;
        this.lambdaType = lambdaType;
        this.lambdaFieldRemapper = lambdaFieldRemapper;
        this.isSameModule = isSameModule;
        this.typeMapper = parent.state.getTypeMapper();
    }


    public void doTransformAndMerge(MethodVisitor adapter, VarRemapper.ParamRemapper remapper) {
        doTransformAndMerge(adapter, remapper, new LambdaFieldRemapper(), true);
    }

    public void doTransformAndMerge(
            MethodVisitor adapter,
            VarRemapper.ParamRemapper remapper,
            LambdaFieldRemapper capturedRemapper, boolean remapReturn
    ) {
        //analyze body
        MethodNode transformedNode = node;
        try {
            transformedNode = markPlacesForInlineAndRemoveInlinable(transformedNode);
        }
        catch (AnalyzerException e) {
            throw UtilsPackage.rethrow(e);
        }

        transformedNode = doInline(transformedNode, capturedRemapper);
        removeClosureAssertions(transformedNode);
        transformedNode.instructions.resetLabels();

        Label end = new Label();
        RemapVisitor visitor = new RemapVisitor(adapter, end, remapper, remapReturn);
        transformedNode.accept(visitor);
        visitor.visitLabel(end);

    }

    private MethodNode doInline(MethodNode node, final LambdaFieldRemapper capturedRemapper) {

        final Deque<InvokeCall> currentInvokes = new LinkedList<InvokeCall>(invokeCalls);

        MethodNode resultNode = new MethodNode(node.access, node.name, node.desc, node.signature, null);

        final Iterator<ConstructorInvocation> iterator = constructorInvocations.iterator();

        RemappingMethodAdapter remappingMethodAdapter = new RemappingMethodAdapter(resultNode.access, resultNode.desc, resultNode,
                                                                                   new TypeRemapper(currentTypeMapping, isSameModule));

        InlineAdapter inliner = new InlineAdapter(remappingMethodAdapter, parameters.totalSize()) {

            private ConstructorInvocation invocation;
            @Override
            public void anew(Type type) {
                if (isLambdaConstructorCall(type.getInternalName(), "<init>")) {
                    invocation = iterator.next();

                    if (invocation.isInlinable()) {
                        //TODO: need poping of type but what to do with local funs???
                        Type newLambdaType = Type.getObjectType(parent.nameGenerator.genLambdaClassName());
                        currentTypeMapping.put(invocation.getOwnerInternalName(), newLambdaType.getInternalName());
                        LambdaTransformer transformer = new LambdaTransformer(invocation.getOwnerInternalName(), parent.subInline(parent.nameGenerator, currentTypeMapping),
                                                                              isSameModule, newLambdaType);

                        transformer.doTransform(invocation);
                    }
                    super.anew(type);
                } else {
                    super.anew(type);
                }
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

                    setInlining(true);
                    MethodInliner inliner = new MethodInliner(info.getNode(), lambdaParameters, parent.subInline(parent.nameGenerator.subGenerator("lambda")), info.getLambdaClassType(),
                                                              capturedRemapper, true /*cause all calls in same module as lambda*/);

                    VarRemapper.ParamRemapper remapper = new VarRemapper.ParamRemapper(lambdaParameters, valueParamShift);
                    inliner.doTransformAndMerge(this.mv, remapper); //TODO add skipped this and receiver

                    //return value boxing/unboxing
                    Method bridge = typeMapper.mapSignature(ClosureCodegen.getInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), this);
                    setInlining(false);
                }
                else if (isLambdaConstructorCall(owner, name)) { //TODO add method
                    assert invocation != null : "<init> call not corresponds to new call" + owner + " " + name;
                    if (invocation.isInlinable()) {
                        //put additional captured parameters on stack
                        List<CapturedParamInfo> recaptured = invocation.getRecaptured();
                        for (CapturedParamInfo capturedParamInfo : recaptured) {
                            Type type = capturedParamInfo.getType();
                            List<CapturedParamInfo> contextCaptured = MethodInliner.this.parameters.getCaptured();
                            CapturedParamInfo result = null;
                            for (CapturedParamInfo info : contextCaptured) {
                                //TODO more sophisticated check
                                if (info.getFieldName().equals(capturedParamInfo.getFieldName())) {
                                    result = info;
                                }
                            }
                            super.visitVarInsn(type.getOpcode(Opcodes.ILOAD), result.getIndex());
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

    public void merge() {

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
    protected MethodNode markPlacesForInlineAndRemoveInlinable(@NotNull MethodNode node) throws AnalyzerException {
        node = prepareNode(node);

        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter());
        Frame<SourceValue>[] sources = analyzer.analyze("fake", node);

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
                            if (insnNode.getType() == AbstractInsnNode.VAR_INSN) {
                                assert insnNode.getOpcode() == Opcodes.ALOAD : insnNode.toString();
                                varIndex = ((VarInsnNode) insnNode).var;
                                lambdaInfo = getLambda(varIndex);

                                if (lambdaInfo != null) {
                                    //remove inlinable access
                                    node.instructions.remove(insnNode);
                                }
                            }
                        }

                        invokeCalls.add(new InvokeCall(varIndex, lambdaInfo));
                    }
                    else if (isLambdaConstructorCall(owner, name)) {
                        Map<Integer, InvokeCall> infos = new HashMap<Integer, InvokeCall>();
                        int paramStart = frame.getStackSize() - paramLength;

                        for (int i = 0; i < paramLength; i++) {
                            SourceValue sourceValue = frame.getStack(paramStart + i);
                            if (sourceValue.insns.size() == 1) {
                                AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                                if (insnNode.getType() == AbstractInsnNode.VAR_INSN && insnNode.getOpcode() == Opcodes.ALOAD) {
                                    int varIndex = ((VarInsnNode) insnNode).var;
                                    LambdaInfo lambdaInfo = getLambda(varIndex);
                                    if (lambdaInfo != null) {
                                        infos.put(i, new InvokeCall(varIndex, lambdaInfo));
                                        node.instructions.remove(insnNode);
                                    }
                                }
                            }
                        }

                        constructorInvocations.add(new ConstructorInvocation(owner, infos, isSameModule));
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

    @Nullable
    public LambdaInfo getLambda(int index) {
        if (index < parameters.totalSize()) {
            return parameters.get(index).getLambda();
        }
        return null;
    }

    private void removeClosureAssertions(MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                if (methodInsnNode.name.equals("checkParameterIsNotNull") && methodInsnNode.owner.equals("jet/runtime/Intrinsics")) {
                    AbstractInsnNode prev = cur.getPrevious();
                    boolean delete = false;
                    if (prev.getOpcode() == Opcodes.INVOKESTATIC) {
                        //parameter boxing
                        AbstractInsnNode prePrevious = cur.getPrevious();
                        if (prePrevious.getType() == AbstractInsnNode.VAR_INSN &&
                            prePrevious.getOpcode() < Opcodes.ALOAD &&
                            prePrevious.getOpcode() >= Opcodes.ILOAD) {
                            delete = true;
                            node.instructions.remove(prePrevious);
                        }
                    } else {
                        assert prev.getType() == AbstractInsnNode.VAR_INSN && prev.getOpcode() == Opcodes.ALOAD;
                        int varIndex = ((VarInsnNode) prev).var;
                        LambdaInfo closure = getLambda(varIndex);
                        delete = closure != null;
                    }
                    if (delete) {
                        node.instructions.remove(prev);
                        node.instructions.remove(cur);
                        cur = next.getNext();
                        node.instructions.remove(next);
                        next = cur;
                    }
                }
            }
            cur = next;
        }
    }

    private void transformCaptured(@NotNull MethodNode node) {
        if (lambdaType == null) {
            return;
        }

        //remove all this and shift all variables to captured ones size
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) cur;
                //TODO check closure
                if (lambdaFieldRemapper.canProcess(fieldInsnNode.owner, lambdaType.getInternalName())) {
                    CapturedParamInfo result = this.lambdaFieldRemapper.findField(fieldInsnNode, parameters.getCaptured());

                    if (result == null) {
                        throw new UnsupportedOperationException("Coudn't find field " +
                                                                fieldInsnNode.owner +
                                                                "." +
                                                                fieldInsnNode.name +
                                                                " (" +
                                                                fieldInsnNode.desc +
                                                                ") in captured vars of " + lambdaType);
                    }

                    if (result.isSkipped()) {
                        //lambda class transformation: skip captured this
                    } else {
                        cur = this.lambdaFieldRemapper.doTransform(node, fieldInsnNode, result);
                    }
                }
            }
            cur = cur.getNext();
        }
    }

    public static AbstractInsnNode getPreviousNoLabelNoLine(AbstractInsnNode cur) {
        AbstractInsnNode prev = cur.getPrevious();
        while (prev.getType() == AbstractInsnNode.LABEL || prev.getType() == AbstractInsnNode.LINE) {
            prev = prev.getPrevious();
        }
        return prev;
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
}
