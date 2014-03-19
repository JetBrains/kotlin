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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.asm4.util.Textifier;
import org.jetbrains.asm4.util.TraceMethodVisitor;
import org.jetbrains.jet.codegen.*;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.context.PackageContext;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.types.lang.InlineStrategy;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.jet.codegen.AsmUtil.getMethodAsmFlags;
import static org.jetbrains.jet.codegen.AsmUtil.isPrimitive;

public class InlineCodegen implements ParentCodegenAware, CallGenerator {

    private final JetTypeMapper typeMapper;

    private final ExpressionCodegen codegen;

    private final boolean asFunctionInline;

    private final GenerationState state;

    private final Call call;

    private final SimpleFunctionDescriptor functionDescriptor;

    private final BindingContext bindingContext;

    private final MethodContext context;

    private final FrameMap originalFunctionFrame;

    private final int initialFrameSize;

    private final JvmMethodSignature jvmSignature;

    private final boolean isSameModule;

    private LambdaInfo activeLambda;

    protected final List<ParameterInfo> actualParameters = new ArrayList<ParameterInfo>();

    protected final Map<Integer, LambdaInfo> expressionMap = new HashMap<Integer, LambdaInfo>();

    public InlineCodegen(
            @NotNull ExpressionCodegen codegen,
            @NotNull GenerationState state,
            @NotNull SimpleFunctionDescriptor functionDescriptor,
            @NotNull Call call
    ) {
        assert functionDescriptor.getInlineStrategy().isInline() : "InlineCodegen could inline only inline function but " + functionDescriptor;

        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.codegen = codegen;
        this.call = call;
        this.functionDescriptor = functionDescriptor.getOriginal();
        bindingContext = codegen.getBindingContext();
        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        context = (MethodContext) getContext(functionDescriptor, state);
        originalFunctionFrame = context.prepareFrame(typeMapper);
        jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());

        InlineStrategy inlineStrategy =
                codegen.getContext().isInlineFunction() ? InlineStrategy.IN_PLACE : functionDescriptor.getInlineStrategy();
        this.asFunctionInline = false;

        isSameModule = !(functionDescriptor instanceof DeserializedSimpleFunctionDescriptor) /*not compiled library*/ &&
                       CodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext());
    }


    @Override
    public void genCall(CallableMethod callableMethod, ResolvedCall<?> resolvedCall, int mask, ExpressionCodegen codegen) {
        assert mask == 0 : "Default method invocation couldn't be inlined " + resolvedCall;

        MethodNode node = null;

        try {
            node = createMethodNode(callableMethod);
            endCall(inlineCall(node));
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (Exception e) {
            boolean generateNodeText = !(e instanceof InlineException);
            PsiElement element = BindingContextUtils.descriptorToDeclaration(bindingContext, this.codegen.getContext().getContextDescriptor());
            throw new CompilationException("Couldn't inline method call '" +
                                       functionDescriptor.getName() +
                                       "' into \n" + (element != null ? element.getText() : "null psi element " + this.codegen.getContext().getContextDescriptor()) +
                                       (generateNodeText ? ("\ncause: " + getNodeText(node)) : ""),
                                       e, call.getCallElement());
        }


    }

    private void endCall(@NotNull InlineResult result) {
        leaveTemps();

        state.getFactory().removeInlinedClasses(result.getClassesToRemove());
    }

    @NotNull
    private MethodNode createMethodNode(CallableMethod callableMethod)
            throws ClassNotFoundException, IOException {
        MethodNode node;
        if (functionDescriptor instanceof DeserializedSimpleFunctionDescriptor) {
            VirtualFile file = InlineCodegenUtil.getVirtualFileForCallable((DeserializedSimpleFunctionDescriptor) functionDescriptor, state);
            String methodDesc = callableMethod.getAsmMethod().getDescriptor();
            DeclarationDescriptor parentDescriptor = functionDescriptor.getContainingDeclaration();
            if (DescriptorUtils.isTrait(parentDescriptor)) {
                methodDesc = "(" + typeMapper.mapType((ClassDescriptor) parentDescriptor).getDescriptor() + methodDesc.substring(1);
            }
            node = InlineCodegenUtil.getMethodNode(file.getInputStream(), functionDescriptor.getName().asString(), methodDesc);

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

            //for maxLocals calculation
            MethodVisitor adapter = InlineCodegenUtil.wrapWithMaxLocalCalc(node);
            FunctionCodegen.generateMethodBody(adapter, functionDescriptor, context.getParentContext().intoFunction(functionDescriptor),
                                               jvmSignature,
                                               new FunctionGenerationStrategy.FunctionDefault(state,
                                                                                              functionDescriptor,
                                                                                              (JetDeclarationWithBody) element),
                                               getParentCodegen());
            adapter.visitMaxs(-1, -1);
            adapter.visitEnd();
        }
        return node;
    }

    private InlineResult inlineCall(MethodNode node) {
        generateClosuresBodies();

        List<ParameterInfo> realParams = new ArrayList<ParameterInfo>(actualParameters);

        putClosureParametersOnStack();

        List<CapturedParamInfo> captured = getAllCaptured();

        Parameters parameters = new Parameters(realParams, Parameters.shiftAndAddStubs(captured, realParams.size()));

        InliningContext info =
                new InliningContext(expressionMap, null, null, null, state,
                                 codegen.getInlineNameGenerator().subGenerator(functionDescriptor.getName().asString()),
                                 codegen.getContext(), call, Collections.<String, String>emptyMap(), false, false);

        MethodInliner inliner = new MethodInliner(node, parameters, info, new FieldRemapper(null, null, parameters), isSameModule, "Method inlining " + call.getCallElement().getText()); //with captured

        LocalVarRemapper remapper = new LocalVarRemapper(parameters, initialFrameSize);

        return inliner.doInline(codegen.v, remapper);
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
        MethodNode methodNode = new MethodNode(Opcodes.ASM4, getMethodAsmFlags(descriptor, context.getContextKind()), asmMethod.getName(), asmMethod.getDescriptor(), jvmMethodSignature.getGenericsSignature(), null);

        MethodVisitor adapter = InlineCodegenUtil.wrapWithMaxLocalCalc(methodNode);

        FunctionCodegen.generateMethodBody(adapter, descriptor, context, jvmMethodSignature, new FunctionGenerationStrategy.FunctionDefault(state, descriptor, declaration), codegen.getParentCodegen());
        adapter.visitMaxs(-1, -1);

        return methodNode;
    }



    @Override
    public void afterParameterPut(@NotNull Type type, @Nullable StackValue stackValue, ValueParameterDescriptor valueParameterDescriptor) {
        putCapturedInLocal(type, stackValue, valueParameterDescriptor, -1);
    }

    public void putCapturedInLocal(
            @NotNull Type type, @Nullable StackValue stackValue, @Nullable ValueParameterDescriptor valueParameterDescriptor, int capturedParamIndex
    ) {
        if (!asFunctionInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutValue(type, stackValue, valueParameterDescriptor);
            StackValue remappedIndex = couldBeRemapped ? stackValue : null;

            ParameterInfo info = new ParameterInfo(type, false, couldBeRemapped ? -1 : codegen.getFrameMap().enterTemp(type), remappedIndex);

            if (capturedParamIndex >= 0 && couldBeRemapped) {
                CapturedParamInfo capturedParamInfo = activeLambda.getCapturedVars().get(capturedParamIndex);
                capturedParamInfo.setRemapValue(remappedIndex != null ? remappedIndex : StackValue.local(info.getIndex(), info.getType()));
            }

            doWithParameter(info);
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

    private void doWithParameter(ParameterInfo info) {
        recordParamInfo(info, true);
        putParameterOnStack(info);
    }

    private int recordParamInfo(ParameterInfo info, boolean addToFrame) {
        Type type = info.type;
        actualParameters.add(info);
        if (info.getType().getSize() == 2) {
            actualParameters.add(ParameterInfo.STUB);
        }
        if (addToFrame) {
            return originalFunctionFrame.enterTemp(type);
        }
        return -1;
    }

    private void putParameterOnStack(ParameterInfo info) {
        if (!info.isSkippedOrRemapped()) {
            int index = info.getIndex();
            Type type = info.type;
            StackValue.local(index, type).store(type, codegen.v);
        }
    }

    @Override
    public void putHiddenParams() {
        List<JvmMethodParameterSignature> types = jvmSignature.getValueParameters();

        if (!isStaticMethod(functionDescriptor, context)) {
            Type type = AsmTypeConstants.OBJECT_TYPE;
            ParameterInfo info = new ParameterInfo(type, false, codegen.getFrameMap().enterTemp(type), -1);
            recordParamInfo(info, false);
        }

        for (JvmMethodParameterSignature param : types) {
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            Type type = param.getAsmType();
            ParameterInfo info = new ParameterInfo(type, false, codegen.getFrameMap().enterTemp(type), -1);
            recordParamInfo(info, false);
        }

        for (ListIterator<? extends ParameterInfo> iterator = actualParameters.listIterator(actualParameters.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            putParameterOnStack(param);
        }
    }

    public void leaveTemps() {
        FrameMap frameMap = codegen.getFrameMap();
        for (ListIterator<? extends ParameterInfo> iterator = actualParameters.listIterator(actualParameters.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            if (!param.isSkippedOrRemapped()) {
                frameMap.leaveTemp(param.type);
            }
        }
    }

    public static boolean isInliningClosure(JetExpression expression, ValueParameterDescriptor valueParameterDescriptora) {
        //TODO deparenthisise
        return expression instanceof JetFunctionLiteralExpression &&
               !InlineUtil.hasNoinlineAnnotation(valueParameterDescriptora);
    }

    public void rememberClosure(JetFunctionLiteralExpression expression, Type type) {
        ParameterInfo closureInfo = new ParameterInfo(type, true, -1, -1);
        int index = recordParamInfo(closureInfo, true);

        LambdaInfo info = new LambdaInfo(expression, typeMapper);
        expressionMap.put(index, info);

        closureInfo.setLambda(info);
    }

    private void putClosureParametersOnStack() {
        //TODO: SORT
        int currentSize = actualParameters.size();
        for (LambdaInfo next : expressionMap.values()) {
            if (next.closure != null) {
                activeLambda = next;
                next.setParamOffset(currentSize);
                codegen.pushClosureOnStack(next.closure, false, this);
                currentSize += next.getCapturedVarsSize();
            }
        }
        activeLambda = null;
    }

    private List<CapturedParamInfo> getAllCaptured() {
        //TODO: SORT
        List<CapturedParamInfo> result = new ArrayList<CapturedParamInfo>();
        for (LambdaInfo next : expressionMap.values()) {
            if (next.closure != null) {
                result.addAll(next.getCapturedVars());
            }
        }
        return result;
    }

    @Nullable
    @Override
    public MemberCodegen getParentCodegen() {
        return codegen.getParentCodegen();
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

    @NotNull
    public static String getNodeText(@Nullable MethodNode node) {
        if (node == null) {
            return "Not generated";
        }
        Textifier p = new Textifier();
        node.accept(new TraceMethodVisitor(p));
        StringWriter sw = new StringWriter();
        p.print(new PrintWriter(sw));
        sw.flush();
        return node.name + " " + node.desc + ": \n " + sw.getBuffer().toString();
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
            rememberClosure((JetFunctionLiteralExpression) argumentExpression, parameterType);
        } else {
            StackValue value = codegen.gen(argumentExpression);
            if (shouldPutValue(parameterType, value, valueParameterDescriptor)) {
                value.put(parameterType, codegen.v);
            }
            afterParameterPut(parameterType, value, valueParameterDescriptor);
        }
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
}
