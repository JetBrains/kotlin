package org.jetbrains.jet.codegen.asm;

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
import org.jetbrains.asm4.tree.*;
import org.jetbrains.asm4.tree.analysis.*;
import org.jetbrains.jet.codegen.ClosureCodegen;
import org.jetbrains.jet.codegen.StackValue;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;

import java.util.*;

import static org.jetbrains.jet.codegen.asm.InlineCodegenUtil.*;

public class MethodInliner {

    private final MethodNode node;

    private final Parameters parameters;

    private final InliningInfo parent;

    @Nullable
    private final Type lambdaInfo;

    private final LambdaFieldRemapper lambdaFieldRemapper;

    private final JetTypeMapper typeMapper;

    private final List<InlinableAccess> inlinableInvocation = new ArrayList<InlinableAccess>();

    //keeps order
    private final List<ConstructorInvocation> constructorInvocationList = new ArrayList<ConstructorInvocation>();
    //current state
    private final Map<String, ConstructorInvocation> constructorInvocation = new LinkedHashMap<String, ConstructorInvocation>();

    /*
     *
     * @param node
     * @param parameters
     * @param parent
     * @param lambdaInfo - in case on lambda 'invoke' inlining
     */
    public MethodInliner(
            @NotNull MethodNode node,
            Parameters parameters,
            @NotNull InliningInfo parent,
            @Nullable Type lambdaInfo,
            LambdaFieldRemapper lambdaFieldRemapper
    ) {
        this.node = node;
        this.parameters = parameters;
        this.parent = parent;
        this.lambdaInfo = lambdaInfo;
        this.lambdaFieldRemapper = lambdaFieldRemapper;
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
            throw new RuntimeException(e);
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

        final Deque<InlinableAccess> infos = new LinkedList<InlinableAccess>(inlinableInvocation);

        MethodNode resultNode = new MethodNode(node.access, node.name, node.desc, node.signature, null);

        final Iterator<ConstructorInvocation> iterator = constructorInvocationList.iterator();

        //TODO add reset to counter
        InlineAdapter inliner = new InlineAdapter(resultNode, parameters.totalSize()) {

            private ConstructorInvocation invocation;
            @Override
            public void anew(Type type) {
                if (isLambdaConstructorCall(type.getInternalName(), "<init>")) {
                    invocation = iterator.next();

                    if (invocation.isInlinable()) {
                        LambdaTransformer transformer = new LambdaTransformer(invocation.getOwnerInternalName(), parent.subInline(parent.nameGenerator));
                        transformer.doTransform(invocation);
                        super.anew(transformer.getNewLambdaType());
                        constructorInvocation.put(invocation.getOwnerInternalName(), invocation);
                    } else {
                        super.anew(type);
                    }
                } else {
                    super.anew(type);
                }
            }

            //for local function support
            @Override
            public void checkcast(Type type) {
                super.checkcast(changeOwnerIfLocalFun(type));
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                if (/*INLINE_RUNTIME.equals(owner) &&*/ isInvokeOnInlinable(owner, name)) { //TODO add method
                    assert !infos.isEmpty();
                    InlinableAccess inlinableAccess = infos.remove();

                    if (!inlinableAccess.isInlinable()) {
                        //noninlinable closure
                        super.visitMethodInsn(opcode, owner, name, desc);
                        return;
                    }
                    LambdaInfo info = getLambda(inlinableAccess.index);

                    int valueParamShift = getNextLocalIndex();

                    putStackValuesIntoLocals(info.getParamsWithoutCapturedValOrVar(), valueParamShift, this, desc);

                    List<ParameterInfo> lambdaParameters = inlinableAccess.getParameters();

                    Parameters params = new Parameters(lambdaParameters, Parameters.transformList(capturedRemapper.markRecaptured(info.getCapturedVars(), info), lambdaParameters.size()));

                    this.setInlining(true);
                    MethodInliner inliner = new MethodInliner(info.getNode(), params, parent.subInline(parent.nameGenerator.subGenerator("lambda")), info.getLambdaClassType(),
                                                              capturedRemapper);

                    VarRemapper.ParamRemapper remapper = new VarRemapper.ParamRemapper(params, valueParamShift);
                    inliner.doTransformAndMerge(this.mv, remapper); //TODO add skipped this and receiver
                    Method bridge = typeMapper.mapSignature(ClosureCodegen.getInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), this);
                    this.setInlining(true);
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
                                //TODO more sofisticated check
                                if (info.getFieldName().equals(capturedParamInfo.getFieldName())) {
                                    result = info;
                                }
                            }
                            super.visitVarInsn(type.getOpcode(Opcodes.ILOAD), result.getIndex());
                        }
                        super.visitMethodInsn(opcode, invocation.getNewLambdaType().getInternalName(), name, invocation.getNewConstructorDescriptor());
                        invocation = null;
                    } else {
                        super.visitMethodInsn(opcode, owner, name, desc);
                    }
                }
                else {
                    super.visitMethodInsn(opcode, changeOwnerIfLocalFun(owner), name, desc);
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

        if (lambdaInfo != null) {
            transformCaptured(transformedNode, parameters, lambdaInfo, lambdaFieldRemapper);
        }
        return transformedNode;
    }

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
                    if (isInvokeOnInlinable(owner, name) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                        SourceValue sourceValue = frame.getStack(frame.getStackSize() - paramLength);

                        boolean isInlinable = false;
                        LambdaInfo lambdaInfo = null;
                        int varIndex = -1;

                        if (sourceValue.insns.size() == 1) {
                            AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                            if (insnNode.getType() == AbstractInsnNode.VAR_INSN) {
                                assert insnNode.getOpcode() == Opcodes.ALOAD : insnNode.toString();
                                varIndex = ((VarInsnNode) insnNode).var;
                                lambdaInfo = getLambda(varIndex);
                                isInlinable = lambdaInfo != null;

                                if (isInlinable) {
                                    //remove inlinable access
                                    node.instructions.remove(insnNode);
                                }
                            }
                        }

                        inlinableInvocation.add(new InlinableAccess(varIndex, isInlinable, getParametersInfo(lambdaInfo, desc)));
                    }
                    else if (isLambdaConstructorCall(owner, name)) {
                        Map<Integer, InlinableAccess> infos = new HashMap<Integer, InlinableAccess>();
                        int paramStart = frame.getStackSize() - paramLength;

                        for (int i = 0; i < paramLength; i++) {
                            SourceValue sourceValue = frame.getStack(paramStart + i);
                            if (sourceValue.insns.size() == 1) {
                                AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                                if (insnNode.getType() == AbstractInsnNode.VAR_INSN && insnNode.getOpcode() == Opcodes.ALOAD) {
                                    int varIndex = ((VarInsnNode) insnNode).var;
                                    LambdaInfo lambdaInfo = getLambda(varIndex);
                                    if (lambdaInfo != null) {
                                        InlinableAccess inlinableAccess = new InlinableAccess(varIndex, true, null);
                                        inlinableAccess.setInfo(lambdaInfo);
                                        infos.put(i, inlinableAccess);

                                        //remove inlinable access
                                        node.instructions.remove(insnNode);
                                    }
                                }
                            }
                        }

                        ConstructorInvocation invocation = new ConstructorInvocation(owner, infos);
                        constructorInvocationList.add(invocation);
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

        //clean dead try catch blocks
        List<TryCatchBlockNode> blocks = node.tryCatchBlocks;
        for (Iterator<TryCatchBlockNode> iterator = blocks.iterator(); iterator.hasNext(); ) {
            TryCatchBlockNode block = iterator.next();
            if (deadLabels.contains(block.start) && deadLabels.contains(block.end)) {
                iterator.remove();
            }
        }

        return node;
    }

    public List<ParameterInfo> getParametersInfo(LambdaInfo info, String desc) {
        List<ParameterInfo> result = new ArrayList<ParameterInfo>();
        Type[] types = Type.getArgumentTypes(desc);

        //add skipped this cause closure doesn't have it
        //result.add(ParameterInfo.STUB);
        ParameterInfo thiz = new ParameterInfo(AsmTypeConstants.OBJECT_TYPE, true, -1, -1);
        thiz.setLambda(info);

        result.add(thiz);
        int index = 1;

        if (info != null) {
            List<ValueParameterDescriptor> valueParameters = info.getFunctionDescriptor().getValueParameters();
            for (ValueParameterDescriptor parameter : valueParameters) {
                Type type = typeMapper.mapType(parameter.getType());
                int paramIndex = index++;
                result.add(new ParameterInfo(type, false, paramIndex, -1));
                if (type.getSize() == 2) {
                    result.add(ParameterInfo.STUB);
                }
            }
        } else {
            for (Type type : types) {
                int paramIndex = index++;
                result.add(new ParameterInfo(type, false, paramIndex, -1));
                if (type.getSize() == 2) {
                    result.add(ParameterInfo.STUB);
                }
            }
        }
        return result;
    }

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

    static List<FieldAccess> transformCaptured(
            @NotNull MethodNode node,
            @NotNull Parameters paramsToSearch,
            @NotNull Type lambdaClassType,
            @NotNull LambdaFieldRemapper lambdaFieldRemapper
    ) {
        List<FieldAccess> capturedFields = new ArrayList<FieldAccess>();

        //remove all this and shift all variables to captured ones size
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) cur;
                //TODO check closure
                String owner = fieldInsnNode.owner;
                if (lambdaFieldRemapper.canProcess(fieldInsnNode.owner, lambdaClassType.getInternalName())) {
                    String name = fieldInsnNode.name;
                    String desc = fieldInsnNode.desc;

                    Collection<CapturedParamInfo> vars = paramsToSearch.getCaptured();
                    CapturedParamInfo result = lambdaFieldRemapper.findField(fieldInsnNode, paramsToSearch.getCaptured());

                    if (result == null) {
                        throw new UnsupportedOperationException("Coudn't find field " +
                                                                owner +
                                                                "." +
                                                                name +
                                                                " (" +
                                                                desc +
                                                                ") in captured vars of " + lambdaClassType);
                    }

                    if (result.isSkipped()) {
                        //lambda class transformation skip this captured
                    } else {
                        cur = lambdaFieldRemapper.doTransform(node, fieldInsnNode, result);

                        //AbstractInsnNode prev = getPreviousNoLableNoLine(cur);
                        //
                        //assert prev.getType() == AbstractInsnNode.VAR_INSN;
                        //VarInsnNode loadThis = (VarInsnNode) prev;
                        //assert /*loadThis.var == info.getCapturedVarsSize() - 1 && */loadThis.getOpcode() == Opcodes.ALOAD;
                        //
                        //int opcode = fieldInsnNode.getOpcode() == Opcodes.GETFIELD ? result.getType().getOpcode(Opcodes.ILOAD) : result.getType().getOpcode(Opcodes.ISTORE);
                        //VarInsnNode insn = new VarInsnNode(opcode, result.getIndex());
                        //
                        //node.instructions.remove(prev); //remove aload this
                        //node.instructions.insertBefore(cur, insn);
                        //node.instructions.remove(cur); //remove aload field
                        //
                        //cur = insn;
                        //
                        //FieldAccess fieldAccess = new FieldAccess(fieldInsnNode.name, fieldInsnNode.desc, new FieldAccess("" + loadThis.var, lambdaClassType.getInternalName()));
                        //capturedFields.add(fieldAccess);
                    }
                }
            }
            cur = cur.getNext();
        }

        return capturedFields;
    }

    public static AbstractInsnNode getPreviousNoLabelNoLine(AbstractInsnNode cur) {
        AbstractInsnNode prev = cur.getPrevious();
        while (prev.getType() == AbstractInsnNode.LABEL || prev.getType() == AbstractInsnNode.LINE) {
            prev = prev.getPrevious();
        }
        return prev;
    }

    public static void putStackValuesIntoLocals(List<Type> directOrder, int shift, InstructionAdapter mv, String descriptor) {
        Type[] actualParams = Type.getArgumentTypes(descriptor); //last param is closure itself
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
                StackValue.onStack(typeOnStack).put(next, mv);
            }
            mv.visitVarInsn(next.getOpcode(Opcodes.ISTORE), shift);
        }
    }

    private Type changeOwnerIfLocalFun(Type oldType) {
        if (isLambdaClass(oldType.getInternalName())) {
            ConstructorInvocation invocation1 = constructorInvocation.get(oldType.getInternalName());
            if (invocation1 != null && invocation1.isInlinable()) {
                return invocation1.getNewLambdaType();
            }
        }

        return oldType;
    }

    private String changeOwnerIfLocalFun(String oldType) {
        if (isLambdaClass(oldType)) {
            ConstructorInvocation invocation1 = constructorInvocation.get(oldType);
            if (invocation1 != null && invocation1.isInlinable()) {
                return invocation1.getNewLambdaType().getInternalName();
            }
        }

        return oldType;
    }
}
