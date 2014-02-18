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

package org.jetbrains.jet.codegen.asm;

import com.google.common.collect.Lists;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.*;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.tree.*;
import org.jetbrains.asm4.tree.analysis.*;
import org.jetbrains.asm4.tree.analysis.Frame;
import org.jetbrains.asm4.util.Textifier;
import org.jetbrains.asm4.util.TraceMethodVisitor;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


import static org.jetbrains.jet.codegen.AsmUtil.*;

public class InlineCodegen implements ParentCodegenAware, Inliner {

    public final static String INVOKE = "invoke";

    private final ExpressionCodegen codegen;

    private final boolean notSeparateInline;

    private final GenerationState state;

    private final boolean disabled;

    private final SimpleFunctionDescriptor functionDescriptor;

    private final List<ParameterInfo> tempTypes = new ArrayList<ParameterInfo>();

    private final List<ClosureUsage> closures = new ArrayList<ClosureUsage>();

    private final Map<Integer, ClosureInfo> expressionMap = new HashMap<Integer, ClosureInfo>();

    private final JetTypeMapper typeMapper;

    private final BindingContext bindingContext;

    private final MethodContext context;

    private final FrameMap originalFunctionFrame;

    private final int initialFrameSize;

    private final JvmMethodSignature jvmSignature;

    public InlineCodegen(
            @NotNull ExpressionCodegen codegen,
            boolean notSeparateInline,
            @NotNull GenerationState state,
            boolean disabled,
            @NotNull SimpleFunctionDescriptor functionDescriptor
    ) {
        this.codegen = codegen;
        this.notSeparateInline = notSeparateInline;
        this.state = state;
        this.disabled = disabled;
        this.functionDescriptor = functionDescriptor.getOriginal();
        typeMapper = codegen.getTypeMapper();
        bindingContext = codegen.getBindingContext();
        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        context = (MethodContext) getContext(functionDescriptor, state);
        originalFunctionFrame = context.prepareFrame(typeMapper);
        jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());
    }


    @Override
    public void inlineCall(CallableMethod callableMethod, ClassVisitor visitor) {
        MethodNode node = null;

        try {
            node = createMethodNode(callableMethod);
            inlineCall(node, true);
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (Exception e) {
            String text = getNodeText(node);
            PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, codegen.getContext().getContextDescriptor());
            throw new CompilationException("Couldn't inline method call '" +
                                       functionDescriptor.getName() +
                                       "' into \n" + (element != null ? element.getText() : "null psi element " + codegen.getContext().getContextDescriptor()) +
                                       "\ncause: " +
                                       text, e, null);
        }
    }

    @NotNull
    private MethodNode createMethodNode(CallableMethod callableMethod)
            throws ClassNotFoundException, IOException {
        MethodNode node = null;
        if (functionDescriptor instanceof DeserializedSimpleFunctionDescriptor) {
            VirtualFile file = InlineCodegenUtil.getVirtualFileForCallable((DeserializedSimpleFunctionDescriptor) functionDescriptor, state);
            node = InlineCodegenUtil.getMethodNode(file.getInputStream(), functionDescriptor.getName().asString(),
                                 callableMethod.getAsmMethod().getDescriptor());

            if (node == null) {
                throw new RuntimeException("Couldn't obtain compiled function body for " + descriptorName(functionDescriptor));
            }
        }
        else {
            PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, functionDescriptor);

            if (element == null) {
                throw new RuntimeException("Couldn't find declaration for function " + descriptorName(functionDescriptor));
            }

            JvmMethodSignature jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());
            Method asmMethod = jvmSignature.getAsmMethod();
            node = new MethodNode(Opcodes.ASM4,
                                           getMethodAsmFlags(functionDescriptor, context.getContextKind()),
                                           asmMethod.getName(),
                                           asmMethod.getDescriptor(),
                                           jvmSignature.getGenericsSignature(),
                                           null);

            FunctionCodegen.generateMethodBody(node, functionDescriptor, context.getParentContext().intoFunction(functionDescriptor),
                                               jvmSignature,
                                               new FunctionGenerationStrategy.FunctionDefault(state,
                                                                                              functionDescriptor,
                                                                                              (JetDeclarationWithBody) element),
                                               getParentCodegen());
            //TODO
            node.visitMaxs(20, 20);
            node.visitEnd();
        }
        return node;
    }

    private void inlineCall(MethodNode node, boolean inlineClosures) {
        if (inlineClosures) {
            removeClosureAssertions(node);
            try {
                markPlacesForInlineAndRemoveInlinable(node);
            }
            catch (AnalyzerException e) {
                throw new RuntimeException(e);
            }
        }

        int valueParamSize = originalFunctionFrame.getCurrentSize();
        int originalSize = codegen.getFrameMap().getCurrentSize();
        generateClosuresBodies();
        putClosureParametersOnStack();
        int additionalParams = codegen.getFrameMap().getCurrentSize() - originalSize;
        VarRemapper remapper = new VarRemapper.ParamRemapper(initialFrameSize, valueParamSize, additionalParams, tempTypes);

        doInline(node.access, node.desc, codegen.getMethodVisitor(), node, remapper.doRemap(valueParamSize + additionalParams), inlineClosures, remapper);
    }

    private void removeClosureAssertions(MethodNode node) {
        AbstractInsnNode cur = node.instructions.getFirst();
        while (cur != null && cur.getNext() != null) {
            AbstractInsnNode next = cur.getNext();
            if (next.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                if (methodInsnNode.name.equals("checkParameterIsNotNull") && methodInsnNode.owner.equals("jet/runtime/Intrinsics")) {
                    AbstractInsnNode prev = cur.getPrevious();
                    assert prev.getType() == AbstractInsnNode.VAR_INSN && prev.getOpcode() == Opcodes.ALOAD;
                    int varIndex = ((VarInsnNode) prev).var;
                    ClosureInfo closure = expressionMap.get(varIndex);
                    if (closure != null) {
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

    private void markPlacesForInlineAndRemoveInlinable(@NotNull MethodNode node) throws AnalyzerException {
        Analyzer<SourceValue> analyzer = new Analyzer<SourceValue>(new SourceInterpreter());
        Frame<SourceValue>[] sources = analyzer.analyze("fake", node);

        AbstractInsnNode cur = node.instructions.getFirst();
        int index = 0;
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) cur;
                //TODO check closure
                if (isInvokeOnInlinable(methodInsnNode.owner, methodInsnNode.name) /*&& methodInsnNode.owner.equals(INLINE_RUNTIME)*/) {
                    Frame<SourceValue> frame = sources[index];
                    SourceValue sourceValue = frame.getStack(frame.getStackSize() - Type.getArgumentTypes(methodInsnNode.desc).length - 1);
                    assert sourceValue.insns.size() == 1;

                    AbstractInsnNode insnNode = sourceValue.insns.iterator().next();
                    assert insnNode.getType() == AbstractInsnNode.VAR_INSN && insnNode.getOpcode() == Opcodes.ALOAD;
                    int varIndex = ((VarInsnNode) insnNode).var;
                    ClosureInfo closureInfo = expressionMap.get(varIndex);
                    if (closureInfo != null) { //TODO: maybe add separate map for noninlinable closures
                        closures.add(new ClosureUsage(varIndex, true));
                        node.instructions.remove(insnNode);
                    } else {
                        closures.add(new ClosureUsage(varIndex, false));
                    }
                }
            }
            cur = cur.getNext();
            index++;
        }
    }

    private void doInline(
            int access,
            String desc,
            MethodVisitor mv,
            MethodNode methodNode,
            int frameSize,
            final boolean inlineClosures,
            @NotNull VarRemapper remapper
    ) {

        Label end = new Label();

        final LinkedList<ClosureUsage> infos = new LinkedList<ClosureUsage>(closures);
        methodNode.instructions.resetLabels();
        MethodVisitor methodVisitor = codegen.getMethodVisitor();

        InliningAdapter inliner = new InliningAdapter(methodVisitor, Opcodes.ASM4, desc, end, frameSize, remapper) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                if (inlineClosures && /*INLINE_RUNTIME.equals(owner) &&*/ isInvokeOnInlinable(owner, name)) { //TODO add method
                    assert !infos.isEmpty();
                    ClosureUsage closureUsage = infos.remove();
                    ClosureInfo info = expressionMap.get(closureUsage.index);

                    if (!closureUsage.isInlinable()) {
                        //noninlinable closure
                        super.visitMethodInsn(opcode, owner, name, desc);
                        return;
                    }

                    //TODO replace with codegen
                    int valueParamShift = getNextLocalIndex();
                    remapper.setNestedRemap(true);
                    putStackValuesIntoLocals(info.getParamsWithoutCapturedValOrVar(), valueParamShift, this, desc);
                    Label closureEnd = new Label();
                    InliningAdapter closureInliner = new InliningAdapter(mv, Opcodes.ASM4, desc, closureEnd, getNextLocalIndex(),
                                                              new VarRemapper.ClosureRemapper(info, valueParamShift, tempTypes));

                    info.getNode().instructions.resetLabels();
                    info.getNode().accept(closureInliner); //TODO

                    remapper.setNestedRemap(false);
                    mv.visitLabel(closureEnd);

                    Method bridge = typeMapper.mapSignature(ClosureCodegen.getInvokeFunction(info.getFunctionDescriptor())).getAsmMethod();
                    Method delegate = typeMapper.mapSignature(info.getFunctionDescriptor()).getAsmMethod();
                    StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), closureInliner);
                }
                else {
                    super.visitMethodInsn(opcode, owner, name, desc);
                }
            }
        };

        methodNode.accept(inliner);

        methodVisitor.visitLabel(end);
    }

    private boolean isInvokeOnInlinable(String owner, String name) {
        return INVOKE.equals(name) && /*TODO: check type*/owner.contains("Function");
    }


    private void generateClosuresBodies() {
        for (ClosureInfo info : expressionMap.values()) {
            info.setNode(generateClosureBody(info));
        }
    }

    private MethodNode generateClosureBody(ClosureInfo info) {
        JetFunctionLiteral declaration = info.getFunctionLiteral();
        FunctionDescriptor descriptor = info.getFunctionDescriptor();

        MethodContext parentContext = codegen.getContext();

        MethodContext context = parentContext.intoClosure(descriptor, codegen, typeMapper).intoFunction(descriptor);

        JvmMethodSignature jvmMethodSignature = typeMapper.mapSignature(descriptor);
        Method asmMethod = jvmMethodSignature.getAsmMethod();
        MethodNode methodNode = new MethodNode(Opcodes.ASM4, getMethodAsmFlags(descriptor, context.getContextKind()), asmMethod.getName(), asmMethod.getDescriptor(), jvmMethodSignature.getGenericsSignature(), null);

        FunctionCodegen.generateMethodBody(methodNode, descriptor, context, jvmMethodSignature, new FunctionGenerationStrategy.FunctionDefault(state, descriptor, declaration) {
            @Override
            public boolean generateLocalVarTable() {
                return false;
            }
        }, codegen.getParentCodegen());

        return transformClosure(methodNode, info);
    }

    @NotNull
    private static MethodNode transformClosure(@NotNull MethodNode node, @NotNull ClosureInfo info) {
        //remove all this and shift all variables to captured ones size
        final int localVarSHift = info.getCapturedVarsSize();
        MethodNode transformedNode = new MethodNode(node.access, node.name, node.desc, node.signature, null) {

            private boolean remappingCaptured = false;
            @Override
            public void visitVarInsn(int opcode, int var) {
                super.visitVarInsn(opcode, var + (remappingCaptured ? 0 : localVarSHift - 1/*remove this*/));
            }
        };
        node.accept(transformedNode);


        //remove all field access to local var
        AbstractInsnNode cur = transformedNode.instructions.getFirst();
        while (cur != null) {
            if (cur.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldInsnNode = (FieldInsnNode) cur;
                //TODO check closure
                String owner = fieldInsnNode.owner;
                if (info.getClosureClassType().getInternalName().equals(fieldInsnNode.owner)) {
                    int opcode = fieldInsnNode.getOpcode();
                    String name = fieldInsnNode.name;
                    String desc = fieldInsnNode.desc;

                    Collection<EnclosedValueDescriptor> vars = info.getCapturedVars();
                    int index = 0;//skip this
                    boolean found = false;
                    for (EnclosedValueDescriptor valueDescriptor : vars) {
                        Type type = valueDescriptor.getType();
                        if (valueDescriptor.getFieldName().equals(name)) {
                            opcode = opcode == Opcodes.GETFIELD ? type.getOpcode(Opcodes.ILOAD) : type.getOpcode(Opcodes.ISTORE);
                            found = true;
                            break;
                        }
                        index += type.getSize();
                    }
                    if (!found) {
                        throw new UnsupportedOperationException("Coudn't find field " +
                                                                owner +
                                                                "." +
                                                                name +
                                                                " (" +
                                                                desc +
                                                                ") in captured vars of " +
                                                                info.getFunctionLiteral().getText());
                    }


                    VarInsnNode varInsNode = new VarInsnNode(opcode, index);

                    AbstractInsnNode prev = cur.getPrevious();
                    while (prev.getType() == AbstractInsnNode.LABEL || prev.getType() == AbstractInsnNode.LINE) {
                        prev = prev.getPrevious();
                    }

                    assert prev.getType() == AbstractInsnNode.VAR_INSN;
                    VarInsnNode loadThis = (VarInsnNode) prev;
                    assert /*loadThis.var == info.getCapturedVarsSize() - 1 && */loadThis.getOpcode() == Opcodes.ALOAD;

                    transformedNode.instructions.remove(prev);
                    transformedNode.instructions.insertBefore(cur, varInsNode);
                    transformedNode.instructions.remove(cur);
                    cur = varInsNode;

                }
            }
            cur = cur.getNext();
        }

        return transformedNode;
    }

    @Override
    public void putInLocal(Type type, StackValue stackValue) {
        if (!disabled && notSeparateInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutValue(type, stackValue, codegen.getContext());
            int remappedIndex = couldBeRemapped ? ((StackValue.Local) stackValue).getIndex() : -1;

            ParameterInfo info = new ParameterInfo(type, false, remappedIndex, couldBeRemapped ? -1 : codegen.getFrameMap().enterTemp(type));

            doWithParameter(info);
        }
    }

    @Override
    public boolean shouldPutValue(Type type, StackValue stackValue, MethodContext context) {
        if (stackValue != null && context.isInlineFunction() && stackValue instanceof StackValue.Local) {
            if (isInvokeOnInlinable(type.getClassName(), "invoke")) {
                //TODO remap only inlinable closure => otherwise we could get a lot of problem
                return false; //TODO check annotations
            }
        }
        return true;
    }

    private void doWithParameter(ParameterInfo info) {
        recordParamInfo(info, true);
        putParameterOnStack(info);
    }

    private int recordParamInfo(ParameterInfo info, boolean addToFrame) {
        Type type = info.type;
        tempTypes.add(info);
        if (info.getType().getSize() == 2) {
            tempTypes.add(ParameterInfo.STUB);
        }
        if (addToFrame) {
            return originalFunctionFrame.enterTemp(type);
        }
        return -1;
    }

    private void putParameterOnStack(ParameterInfo info) {
        if (!info.isSkippedOrRemapped()) {
            int index = info.index;
            Type type = info.type;
            StackValue.local(index, type).store(type, codegen.getInstructionAdapter());
        }
    }

    @Override
    public void putHiddenParams() {
        List<JvmMethodParameterSignature> types = jvmSignature.getValueParameters();

        if (!isStaticMethod(functionDescriptor, context)) {
            Type type = AsmTypeConstants.OBJECT_TYPE;
            ParameterInfo info = new ParameterInfo(type, false, -1, codegen.getFrameMap().enterTemp(type));
            recordParamInfo(info, false);
        }

        for (JvmMethodParameterSignature param : types) {
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            Type type = param.getAsmType();
            ParameterInfo info = new ParameterInfo(type, false, -1, codegen.getFrameMap().enterTemp(type));
            recordParamInfo(info, false);
        }

        for (ListIterator<ParameterInfo> iterator = tempTypes.listIterator(tempTypes.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            putParameterOnStack(param);
        }
    }

    @Override
    public void leaveTemps() {
        FrameMap frameMap = codegen.getFrameMap();
        for (ListIterator<ParameterInfo> iterator = tempTypes.listIterator(tempTypes.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            if (!param.isSkippedOrRemapped()) {
                frameMap.leaveTemp(param.type);
            }
        }
    }

    public boolean isDisabled() {
        return disabled;
    }

    private static void putStackValuesIntoLocals(List<Type> directOrder, int shift, InstructionAdapter mv, String descriptor) {
        Type [] actualParams = Type.getArgumentTypes(descriptor); //last param is closure itself
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

    @Override
    public boolean isInliningClosure(JetExpression expression) {
        return !disabled && expression instanceof JetFunctionLiteralExpression;
    }

    @Override
    public void rememberClosure(JetFunctionLiteralExpression expression, Type type) {
        ParameterInfo closureInfo = new ParameterInfo(type, true, -1, -1);
        int index = recordParamInfo(closureInfo, true);

        ClosureInfo info = new ClosureInfo(expression, typeMapper);
        expressionMap.put(index, info);
    }

    private void putClosureParametersOnStack() {
        //TODO: SORT
        for (ClosureInfo next : expressionMap.values()) {
            if (next.closure != null) {
                int size = tempTypes.size();
                next.setParamOffset(size);
                codegen.pushClosureOnStack(next.closure, false, this);
            }
        }
    }

    @Nullable
    @Override
    public MemberCodegen getParentCodegen() {
        return codegen.getParentCodegen();
    }

    public static CodegenContext getContext(DeclarationDescriptor descriptor, GenerationState state) {
        if (descriptor instanceof NamespaceDescriptor) {
            return new NamespaceContext((NamespaceDescriptor) descriptor, null);
        }

        CodegenContext parent = getContext(descriptor.getContainingDeclaration(), state);

        if (descriptor instanceof ClassDescriptor) {
            return parent.intoClass((ClassDescriptor) descriptor, OwnerKind.IMPLEMENTATION, state);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return parent.intoFunction((FunctionDescriptor) descriptor);
        }

        throw new IllegalStateException("Coudn't build context for " + descriptorName(descriptor));
    }

    private static boolean isStaticMethod(FunctionDescriptor functionDescriptor, MethodContext context) {
        return (getMethodAsmFlags(functionDescriptor, context.getContextKind()) & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
    }

    @NotNull
    private static String getNodeText(@Nullable MethodNode node) {
        if (node == null) {
            return "Not generated";
        }
        Textifier p = new Textifier();
        node.accept(new TraceMethodVisitor(p));
        StringWriter sw = new StringWriter();
        p.print(new PrintWriter(sw));
        sw.flush();
        return node.name + ": \n " + sw.getBuffer().toString();
    }

    private static String descriptorName(DeclarationDescriptor descriptor) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor);
    }
}
