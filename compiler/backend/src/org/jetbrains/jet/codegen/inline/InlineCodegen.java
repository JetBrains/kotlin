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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.binding.CodegenBinding;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.jet.lang.resolve.java.jvmSignature.JvmMethodSignature;
import org.jetbrains.jet.lang.types.lang.InlineStrategy;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.LabelNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode;

import java.io.IOException;
import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.getMethodAsmFlags;
import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.jet.codegen.inline.InlineCodegenUtil.addInlineMarker;

public class InlineCodegen implements CallGenerator {
    private final GenerationState state;
    private final JetTypeMapper typeMapper;

    private final SimpleFunctionDescriptor functionDescriptor;
    private final JvmMethodSignature jvmSignature;
    private final JetElement callElement;
    private final MethodContext context;
    private final ExpressionCodegen codegen;

    private final boolean asFunctionInline;
    private final int initialFrameSize;
    private final boolean isSameModule;

    protected final ParametersBuilder invocationParamBuilder = ParametersBuilder.newBuilder();
    protected final Map<Integer, LambdaInfo> expressionMap = new HashMap<Integer, LambdaInfo>();

    private final ReifiedTypeInliner reifiedTypeInliner;

    private LambdaInfo activeLambda;

    public InlineCodegen(
            @NotNull ExpressionCodegen codegen,
            @NotNull GenerationState state,
            @NotNull SimpleFunctionDescriptor functionDescriptor,
            @NotNull JetElement callElement,
            @Nullable ReifiedTypeParameterMappings typeParameterMappings
    ) {
        assert functionDescriptor.getInlineStrategy().isInline() : "InlineCodegen could inline only inline function but " + functionDescriptor;

        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.codegen = codegen;
        this.callElement = callElement;
        this.functionDescriptor = functionDescriptor.getOriginal();

        reifiedTypeInliner = new ReifiedTypeInliner(typeParameterMappings);

        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        context = (MethodContext) getContext(functionDescriptor, state);
        jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());

        InlineStrategy inlineStrategy =
                codegen.getContext().isInlineFunction() ? InlineStrategy.IN_PLACE : functionDescriptor.getInlineStrategy();
        this.asFunctionInline = false;

        isSameModule = JvmCodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext(), state.getOutDirectory());
    }

    @Override
    public void genCallWithoutAssertions(
            @NotNull CallableMethod callableMethod, @NotNull ExpressionCodegen codegen
    ) {
        genCall(callableMethod, null, false, codegen);
    }

    @Override
    public void genCall(@NotNull CallableMethod callableMethod, @Nullable ResolvedCall<?> resolvedCall, boolean callDefault, @NotNull ExpressionCodegen codegen) {
        MethodNode node = null;

        try {
            node = createMethodNode(callDefault);
            endCall(inlineCall(node));
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (Exception e) {
            boolean generateNodeText = !(e instanceof InlineException);
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(this.codegen.getContext().getContextDescriptor());
            throw new CompilationException("Couldn't inline method call '" +
                                       functionDescriptor.getName() +
                                       "' into \n" + (element != null ? element.getText() : "null psi element " + this.codegen.getContext().getContextDescriptor()) +
                                       (generateNodeText ? ("\ncause: " + InlineCodegenUtil.getNodeText(node)) : ""),
                                       e, callElement);
        }


    }

    private void endCall(@NotNull InlineResult result) {
        leaveTemps();

        state.getFactory().removeInlinedClasses(result.getClassesToRemove());
    }

    @NotNull
    private MethodNode createMethodNode(boolean callDefault) throws ClassNotFoundException, IOException {
        JvmMethodSignature jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());

        Method asmMethod;
        if (callDefault) {
            asmMethod = typeMapper.mapDefaultMethod(functionDescriptor, context.getContextKind(), context);
        }
        else {
            asmMethod = jvmSignature.getAsmMethod();
        }

        MethodNode node;
        if (functionDescriptor instanceof DeserializedSimpleFunctionDescriptor) {
            VirtualFile file = InlineCodegenUtil.getVirtualFileForCallable((DeserializedSimpleFunctionDescriptor) functionDescriptor, state);
            node = InlineCodegenUtil.getMethodNode(file.contentsToByteArray(), asmMethod.getName(), asmMethod.getDescriptor());

            if (node == null) {
                throw new RuntimeException("Couldn't obtain compiled function body for " + descriptorName(functionDescriptor));
            }
        }
        else {
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);

            if (element == null) {
                throw new RuntimeException("Couldn't find declaration for function " + descriptorName(functionDescriptor));
            }

            node = new MethodNode(InlineCodegenUtil.API,
                                           getMethodAsmFlags(functionDescriptor, context.getContextKind()) | (callDefault ? Opcodes.ACC_STATIC : 0),
                                           asmMethod.getName(),
                                           asmMethod.getDescriptor(),
                                           jvmSignature.getGenericsSignature(),
                                           null);

            //for maxLocals calculation
            MethodVisitor maxCalcAdapter = InlineCodegenUtil.wrapWithMaxLocalCalc(node);
            MethodContext methodContext = context.getParentContext().intoFunction(functionDescriptor);
            MemberCodegen<?> parentCodegen = codegen.getParentCodegen();
            if (callDefault) {
                boolean isStatic = AsmUtil.isStaticMethod(context.getContextKind(), functionDescriptor);
                FunctionCodegen.generateDefaultImplBody(
                        methodContext, jvmSignature, functionDescriptor, isStatic, maxCalcAdapter, DefaultParameterValueLoader.DEFAULT,
                        (JetNamedFunction) element, parentCodegen, state
                );
            }
            else {
                FunctionCodegen.generateMethodBody(
                        maxCalcAdapter, functionDescriptor, methodContext, jvmSignature,
                        new FunctionGenerationStrategy.FunctionDefault(state, functionDescriptor, (JetDeclarationWithBody) element),
                        parentCodegen
                );
            }
            maxCalcAdapter.visitMaxs(-1, -1);
            maxCalcAdapter.visitEnd();
        }
        return node;
    }

    private InlineResult inlineCall(MethodNode node) {
        reifiedTypeInliner.reifyInstructions(node.instructions);
        generateClosuresBodies();

        //through generation captured parameters will be added to invocationParamBuilder
        putClosureParametersOnStack();

        addInlineMarker(codegen.v, true);

        Parameters parameters = invocationParamBuilder.buildParameters();

        InliningContext info = new RootInliningContext(expressionMap,
                                                       state,
                                                       codegen.getInlineNameGenerator()
                                                               .subGenerator(functionDescriptor.getName().asString()),
                                                       codegen.getContext(),
                                                       callElement,
                                                       codegen.getParentCodegen().getClassName());

        MethodInliner inliner = new MethodInliner(node, parameters, info, new FieldRemapper(null, null, parameters), isSameModule, "Method inlining " + callElement.getText()); //with captured

        LocalVarRemapper remapper = new LocalVarRemapper(parameters, initialFrameSize);


        MethodNode adapter = InlineCodegenUtil.createEmptyMethodNode();
        InlineResult result = inliner.doInline(adapter, remapper, true, LabelOwner.SKIP_ALL);

        LabelOwner labelOwner = new LabelOwner() {

            final CallableMemberDescriptor descriptor = codegen.getContext().getContextDescriptor();

            final boolean isLambda = CodegenBinding.isLocalFunOrLambda(descriptor) && descriptor.getName().isSpecial();

            @Override
            public boolean isMyLabel(@NotNull String name) {
                if (InlineCodegenUtil.ROOT_LABEL.equals(name)) {
                    return !isLambda;
                }
                else {
                    return descriptor.getName().asString().equals(name);
                }
            }
        };
        List<MethodInliner.PointForExternalFinallyBlocks> infos = MethodInliner.processReturns(adapter, labelOwner, true, null);
        generateAndInsertFinallyBlocks(adapter, infos);

        adapter.accept(new InliningInstructionAdapter(codegen.v));

        addInlineMarker(codegen.v, false);

        return result;
    }

    private void generateClosuresBodies() {
        for (LambdaInfo info : expressionMap.values()) {
            info.setNode(generateLambdaBody(info));
        }
    }

    private MethodNode generateLambdaBody(LambdaInfo info) {
        JetFunctionLiteral declaration = info.getFunctionLiteral();
        FunctionDescriptor descriptor = info.getFunctionDescriptor();

        MethodContext parentContext = codegen.getContext();

        MethodContext context = parentContext.intoClosure(descriptor, codegen, typeMapper).intoInlinedLambda(descriptor);

        JvmMethodSignature jvmMethodSignature = typeMapper.mapSignature(descriptor);
        Method asmMethod = jvmMethodSignature.getAsmMethod();
        MethodNode methodNode = new MethodNode(InlineCodegenUtil.API, getMethodAsmFlags(descriptor, context.getContextKind()), asmMethod.getName(), asmMethod.getDescriptor(), jvmMethodSignature.getGenericsSignature(), null);

        MethodVisitor adapter = InlineCodegenUtil.wrapWithMaxLocalCalc(methodNode);

        FunctionCodegen.generateMethodBody(adapter, descriptor, context, jvmMethodSignature, new FunctionGenerationStrategy.FunctionDefault(state, descriptor, declaration), codegen.getParentCodegen());
        adapter.visitMaxs(-1, -1);

        return methodNode;
    }



    @Override
    public void afterParameterPut(@NotNull Type type, @Nullable StackValue stackValue, @Nullable ValueParameterDescriptor valueParameterDescriptor) {
        putCapturedInLocal(type, stackValue, valueParameterDescriptor, -1);
    }

    private void putCapturedInLocal(
            @NotNull Type type, @Nullable StackValue stackValue, @Nullable ValueParameterDescriptor valueParameterDescriptor, int capturedParamIndex
    ) {
        if (!asFunctionInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutValue(type, stackValue, valueParameterDescriptor);
            StackValue remappedIndex = couldBeRemapped ? stackValue : null;

            ParameterInfo info;
            if (capturedParamIndex >= 0) {
                CapturedParamDesc capturedParamInfoInLambda = activeLambda.getCapturedVars().get(capturedParamIndex);
                info = invocationParamBuilder.addCapturedParam(capturedParamInfoInLambda, capturedParamInfoInLambda.getFieldName());
                info.setRemapValue(remappedIndex);
            }
            else {
                info = invocationParamBuilder.addNextParameter(type, false, remappedIndex);
            }

            putParameterOnStack(info);
        }
    }

    /*descriptor is null for captured vars*/
    public boolean shouldPutValue(
            @NotNull Type type,
            @Nullable StackValue stackValue,
            @Nullable ValueParameterDescriptor descriptor
    ) {

        if (stackValue == null) {
            //default or vararg
            return true;
        }

        //remap only inline functions (and maybe non primitives)
        //TODO - clean asserion and remapping logic
        if (isPrimitive(type) != isPrimitive(stackValue.type)) {
            //don't remap boxing/unboxing primitives - lost identity and perfomance
            return true;
        }

        if (stackValue instanceof StackValue.Local) {
            return false;
        }

        if (stackValue instanceof StackValue.Composed) {
            //see: Method.isSpecialStackValue: go through aload 0
            if (codegen.getContext().isInliningLambda() && codegen.getContext().getContextDescriptor() instanceof AnonymousFunctionDescriptor) {
                if (descriptor != null && !InlineUtil.hasNoinlineAnnotation(descriptor)) {
                    //TODO: check type of context
                    return false;
                }
            }
        }
        return true;
    }

    private void putParameterOnStack(ParameterInfo... infos) {
        int[] index = new int[infos.length];
        for (int i = 0; i < infos.length; i++) {
            ParameterInfo info = infos[i];
            if (!info.isSkippedOrRemapped()) {
                index[i] = codegen.getFrameMap().enterTemp(info.getType());
            }
            else {
                index[i] = -1;
            }
        }

        for (int i = infos.length - 1; i >= 0; i--) {
            ParameterInfo info = infos[i];
            if (!info.isSkippedOrRemapped()) {
                Type type = info.type;
                StackValue.local(index[i], type).store(type, codegen.v);
            }
        }
    }

    @Override
    public void putHiddenParams() {
        List<JvmMethodParameterSignature> valueParameters = jvmSignature.getValueParameters();

        if (!isStaticMethod(functionDescriptor, context)) {
            invocationParamBuilder.addNextParameter(AsmTypeConstants.OBJECT_TYPE, false, null);
        }

        for (JvmMethodParameterSignature param : valueParameters) {
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            invocationParamBuilder.addNextParameter(param.getAsmType(), false, null);
        }

        List<ParameterInfo> infos = invocationParamBuilder.listNotCaptured();
        putParameterOnStack(infos.toArray(new ParameterInfo[infos.size()]));
    }

    public void leaveTemps() {
        FrameMap frameMap = codegen.getFrameMap();
        List<ParameterInfo> infos = invocationParamBuilder.listAllParams();
        for (ListIterator<? extends ParameterInfo> iterator = infos.listIterator(infos.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            if (!param.isSkippedOrRemapped()) {
                frameMap.leaveTemp(param.type);
            }
        }
    }

    public static boolean isInliningClosure(JetExpression expression, ValueParameterDescriptor valueParameterDescriptora) {
        //TODO deparenthisise typed
        JetExpression deparenthesize = JetPsiUtil.deparenthesize(expression);
        return deparenthesize instanceof JetFunctionLiteralExpression &&
               !InlineUtil.hasNoinlineAnnotation(valueParameterDescriptora);
    }

    public void rememberClosure(JetExpression expression, Type type) {
        JetFunctionLiteralExpression lambda = (JetFunctionLiteralExpression) JetPsiUtil.deparenthesize(expression);
        assert lambda != null : "Couldn't find lambda in " + expression.getText();

        String labelNameIfPresent = null;
        PsiElement parent = lambda.getParent();
        if (parent instanceof JetLabeledExpression) {
            labelNameIfPresent = ((JetLabeledExpression) parent).getLabelName();
        }
        LambdaInfo info = new LambdaInfo(lambda, typeMapper, labelNameIfPresent);

        ParameterInfo closureInfo = invocationParamBuilder.addNextParameter(type, true, null);
        closureInfo.setLambda(info);
        expressionMap.put(closureInfo.getIndex(), info);
    }

    private void putClosureParametersOnStack() {
        for (LambdaInfo next : expressionMap.values()) {
            activeLambda = next;
            codegen.pushClosureOnStack(next.getClassDescriptor(), true, this);
        }
        activeLambda = null;
    }

    public static CodegenContext getContext(DeclarationDescriptor descriptor, GenerationState state) {
        if (descriptor instanceof PackageFragmentDescriptor) {
            return new PackageContext((PackageFragmentDescriptor) descriptor, null, null);
        }

        CodegenContext parent = getContext(descriptor.getContainingDeclaration(), state);

        if (descriptor instanceof ClassDescriptor) {
            OwnerKind kind = DescriptorUtils.isTrait(descriptor) ? OwnerKind.TRAIT_IMPL : OwnerKind.IMPLEMENTATION;
            return parent.intoClass((ClassDescriptor) descriptor, kind, state);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return parent.intoFunction((FunctionDescriptor) descriptor);
        }

        throw new IllegalStateException("Couldn't build context for " + descriptorName(descriptor));
    }

    private static boolean isStaticMethod(FunctionDescriptor functionDescriptor, MethodContext context) {
        return (getMethodAsmFlags(functionDescriptor, context.getContextKind()) & Opcodes.ACC_STATIC) != 0;
    }

    private static String descriptorName(DeclarationDescriptor descriptor) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor);
    }

    @Override
    public void genValueAndPut(
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull JetExpression argumentExpression,
            @NotNull Type parameterType
    ) {
        //TODO deparenthisise
        if (isInliningClosure(argumentExpression, valueParameterDescriptor)) {
            rememberClosure(argumentExpression, parameterType);
        } else {
            StackValue value = codegen.gen(argumentExpression);
            putValueIfNeeded(valueParameterDescriptor, parameterType, value);
        }
    }

    @Override
    public void putValueIfNeeded(@Nullable ValueParameterDescriptor valueParameterDescriptor, @NotNull Type parameterType, @NotNull StackValue value) {
        if (shouldPutValue(parameterType, value, valueParameterDescriptor)) {
            value.put(parameterType, codegen.v);
        }
        afterParameterPut(parameterType, value, valueParameterDescriptor);
    }

    @Override
    public void putCapturedValueOnStack(
            @NotNull StackValue stackValue, @NotNull Type valueType, int paramIndex
    ) {
        if (shouldPutValue(stackValue.type, stackValue, null)) {
            stackValue.put(stackValue.type, codegen.v);
        }
        putCapturedInLocal(stackValue.type, stackValue, null, paramIndex);
    }


    public void generateAndInsertFinallyBlocks(MethodNode intoNode, List<MethodInliner.PointForExternalFinallyBlocks> insertPoints) {
        if (!codegen.hasFinallyBlocks()) return;

        Map<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks> extensionPoints =
                new HashMap<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks>();
        for (MethodInliner.PointForExternalFinallyBlocks insertPoint : insertPoints) {
            extensionPoints.put(insertPoint.beforeIns, insertPoint);
        }

        DefaultProcessor processor = new DefaultProcessor(intoNode);

        AbstractInsnNode curInstr = intoNode.instructions.getFirst();
        while (curInstr != null) {
            processor.updateCoveringTryBlocks(curInstr, true);

            MethodInliner.PointForExternalFinallyBlocks extension = extensionPoints.get(curInstr);
            if (extension != null) {
                Label start = new Label();
                Label end = new Label();

                MethodNode finallyNode = InlineCodegenUtil.createEmptyMethodNode();
                finallyNode.visitLabel(start);

                ExpressionCodegen finallyCodegen =
                        new ExpressionCodegen(finallyNode, codegen.getFrameMap(), codegen.getReturnType(),
                                              codegen.getContext(), codegen.getState(), codegen.getParentCodegen());
                finallyCodegen.addBlockStackElementsForNonLocalReturns(codegen.getBlockStackElements());

                finallyCodegen.generateFinallyBlocksIfNeeded(extension.returnType);
                finallyNode.visitLabel(end);

                //Exception table for external try/catch/finally blocks will be generated in original codegen after exiting this method
                InlineCodegenUtil.insertNodeBefore(finallyNode, intoNode, curInstr);

                List<TryCatchBlockNodeWrapper> blocks = processor.getCoveringFromInnermost();
                ListIterator<TryCatchBlockNodeWrapper> iterator = blocks.listIterator(blocks.size());
                while (iterator.hasPrevious()) {
                    TryCatchBlockNodeWrapper previous = iterator.previous();
                    LabelNode oldStart = previous.getStartLabel();
                    TryCatchBlockNode node = previous.getNode();
                    node.start = (LabelNode) end.info;
                    processor.remapStartLabel(oldStart, previous);

                    TryCatchBlockNode additionalNode = new TryCatchBlockNode(oldStart, (LabelNode) start.info, node.handler, node.type);
                    processor.addNode(additionalNode);
                }
            }

            curInstr = curInstr.getNext();
        }

        processor.sortTryCatchBlocks();
        Iterable<TryCatchBlockNodeWrapper> nodes = processor.getNonEmptyNodes();
        intoNode.tryCatchBlocks.clear();
        for (TryCatchBlockNodeWrapper node : nodes) {
            intoNode.tryCatchBlocks.add(node.getNode());
        }
    }

}
