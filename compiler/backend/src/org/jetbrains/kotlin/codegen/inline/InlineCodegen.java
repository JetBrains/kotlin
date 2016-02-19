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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents;
import org.jetbrains.kotlin.modules.TargetId;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor;
import org.jetbrains.kotlin.types.expressions.LabelResolver;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.LabelNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.codegen.AsmUtil.getMethodAsmFlags;
import static org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.addInlineMarker;
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.getConstant;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral;

public class InlineCodegen extends CallGenerator {
    private final GenerationState state;
    private final JetTypeMapper typeMapper;

    private final FunctionDescriptor functionDescriptor;
    private final JvmMethodSignature jvmSignature;
    private final KtElement callElement;
    private final MethodContext context;
    private final ExpressionCodegen codegen;

    private final boolean asFunctionInline;
    private final int initialFrameSize;
    private final boolean isSameModule;

    private final ParametersBuilder invocationParamBuilder = ParametersBuilder.newBuilder();
    private final Map<Integer, LambdaInfo> expressionMap = new HashMap<Integer, LambdaInfo>();

    private final ReifiedTypeInliner reifiedTypeInliner;
    @Nullable private final TypeParameterMappings typeParameterMappings;
    private final boolean isDefaultCompilation;

    private LambdaInfo activeLambda;

    private final SourceMapper sourceMapper;

    public InlineCodegen(
            @NotNull ExpressionCodegen codegen,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor function,
            @NotNull KtElement callElement,
            @Nullable TypeParameterMappings typeParameterMappings,
            boolean isDefaultCompilation
    ) {
        assert InlineUtil.isInline(function) || InlineUtil.isArrayConstructorWithLambda(function) :
                "InlineCodegen can inline only inline functions and array constructors: " + function;
        this.isDefaultCompilation = isDefaultCompilation;
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.codegen = codegen;
        this.callElement = callElement;
        this.functionDescriptor =
                InlineUtil.isArrayConstructorWithLambda(function)
                ? FictitiousArrayConstructor.create((ConstructorDescriptor) function)
                : function.getOriginal();
        this.typeParameterMappings = typeParameterMappings;

        reifiedTypeInliner = new ReifiedTypeInliner(typeParameterMappings);

        initialFrameSize = codegen.getFrameMap().getCurrentSize();

        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);
        context = (MethodContext) getContext(functionDescriptor, state, element != null ? (KtFile) element.getContainingFile() : null);
        jvmSignature = typeMapper.mapSignature(functionDescriptor, context.getContextKind());

        // TODO: implement AS_FUNCTION inline strategy
        this.asFunctionInline = false;

        isSameModule = JvmCodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext(), state.getOutDirectory());

        sourceMapper = codegen.getParentCodegen().getOrCreateSourceMapper();

        if (!(functionDescriptor instanceof FictitiousArrayConstructor)) {
            reportIncrementalInfo(functionDescriptor, codegen.getContext().getFunctionDescriptor().getOriginal());
        }
    }

    @Override
    public void genCallWithoutAssertions(@NotNull CallableMethod callableMethod, @NotNull ExpressionCodegen codegen) {
        genCall(callableMethod, null, false, codegen);
    }

    @Override
    public void genCallInner(@NotNull Callable callableMethod, @Nullable ResolvedCall<?> resolvedCall, boolean callDefault, @NotNull ExpressionCodegen codegen) {
        SMAPAndMethodNode nodeAndSmap = null;
        if (!state.getInlineCycleReporter().enterIntoInlining(resolvedCall)) {
            generateStub(resolvedCall, codegen);
            return;
        }

        try {
            nodeAndSmap = createMethodNode(callDefault);
            endCall(inlineCall(nodeAndSmap));
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
                                       (generateNodeText ? ("\ncause: " + InlineCodegenUtil.getNodeText(nodeAndSmap != null ? nodeAndSmap.getNode(): null)) : ""),
                                       e, callElement);
        }
        finally {
            state.getInlineCycleReporter().exitFromInliningOf(resolvedCall);
        }
    }

    protected void generateStub(@Nullable ResolvedCall<?> resolvedCall, @NotNull ExpressionCodegen codegen) {
        leaveTemps();
        assert resolvedCall != null;
        String message = "Call is part of inline cycle: " + resolvedCall.getCall().getCallElement().getText();
        AsmUtil.genThrow(codegen.v, "java/lang/UnsupportedOperationException", message);
    }

    private void endCall(@NotNull InlineResult result) {
        leaveTemps();

        codegen.propagateChildReifiedTypeParametersUsages(result.getReifiedTypeParametersUsages());

        state.getFactory().removeClasses(result.getClassesToRemove());

        codegen.markLineNumberAfterInlineIfNeeded();
    }

    @NotNull
    private SMAPAndMethodNode createMethodNode(boolean callDefault) throws IOException {
        Method asmMethod = callDefault
                           ? typeMapper.mapDefaultMethod(functionDescriptor, context.getContextKind())
                           : jvmSignature.getAsmMethod();

        SMAPAndMethodNode nodeAndSMAP;
        if (functionDescriptor instanceof FictitiousArrayConstructor) {
            nodeAndSMAP = InlineCodegenUtil.getMethodNode(
                    IntrinsicArrayConstructorsKt.getBytecode(),
                    asmMethod.getName(),
                    asmMethod.getDescriptor(),
                    IntrinsicArrayConstructorsKt.getClassId()
            );

            if (nodeAndSMAP == null) {
                throw new IllegalStateException("Couldn't obtain array constructor body for " + descriptorName(functionDescriptor));
            }
        }
        else if (functionDescriptor instanceof DeserializedSimpleFunctionDescriptor) {
            JetTypeMapper.ContainingClassesInfo containingClasses = typeMapper.getContainingClassesForDeserializedCallable(
                    (DeserializedSimpleFunctionDescriptor) functionDescriptor);

            ClassId containerId = containingClasses.getImplClassId();
            VirtualFile file = InlineCodegenUtil.findVirtualFile(state, containerId);
            if (file == null) {
                throw new IllegalStateException("Couldn't find declaration file for " + containerId);
            }

            nodeAndSMAP = InlineCodegenUtil.getMethodNode(
                    file.contentsToByteArray(), asmMethod.getName(), asmMethod.getDescriptor(), containerId
            );

            if (nodeAndSMAP == null) {
                throw new IllegalStateException("Couldn't obtain compiled function body for " + descriptorName(functionDescriptor));
            }
        }
        else {
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(functionDescriptor);

            if (!(element instanceof KtNamedFunction)) {
                throw new IllegalStateException("Couldn't find declaration for function " + descriptorName(functionDescriptor));
            }
            KtNamedFunction inliningFunction = (KtNamedFunction) element;

            MethodNode node = new MethodNode(InlineCodegenUtil.API,
                                           getMethodAsmFlags(functionDescriptor, context.getContextKind()) | (callDefault ? Opcodes.ACC_STATIC : 0),
                                           asmMethod.getName(),
                                           asmMethod.getDescriptor(),
                                           jvmSignature.getGenericsSignature(),
                                           null);

            //for maxLocals calculation
            MethodVisitor maxCalcAdapter = InlineCodegenUtil.wrapWithMaxLocalCalc(node);
            MethodContext methodContext = context.getParentContext().intoFunction(functionDescriptor);

            SMAP smap;
            if (callDefault) {
                Type implementationOwner = typeMapper.mapImplementationOwner(functionDescriptor);
                FakeMemberCodegen parentCodegen = new FakeMemberCodegen(codegen.getParentCodegen(), inliningFunction,
                                                                        (FieldOwnerContext) methodContext.getParentContext(),
                                                                        implementationOwner.getInternalName());
                FunctionCodegen.generateDefaultImplBody(
                        methodContext, functionDescriptor, maxCalcAdapter, DefaultParameterValueLoader.DEFAULT,
                        inliningFunction, parentCodegen, asmMethod
                );
                smap = createSMAPWithDefaultMapping(inliningFunction, parentCodegen.getOrCreateSourceMapper().getResultMappings());
            }
            else {
                smap = generateMethodBody(maxCalcAdapter, functionDescriptor, methodContext, inliningFunction, jvmSignature, false);
            }

            nodeAndSMAP = new SMAPAndMethodNode(node, smap);
            maxCalcAdapter.visitMaxs(-1, -1);
            maxCalcAdapter.visitEnd();
        }
        return nodeAndSMAP;
    }

    private InlineResult inlineCall(SMAPAndMethodNode nodeAndSmap) {
        MethodNode node = nodeAndSmap.getNode();
        ReifiedTypeParametersUsages reificationResult = reifiedTypeInliner.reifyInstructions(node);
        generateClosuresBodies();

        //through generation captured parameters will be added to invocationParamBuilder
        putClosureParametersOnStack();

        addInlineMarker(codegen.v, true);

        Parameters parameters = invocationParamBuilder.buildParameters();

        InliningContext info = new RootInliningContext(
                expressionMap, state, codegen.getInlineNameGenerator().subGenerator(jvmSignature.getAsmMethod().getName()),
                codegen.getContext(), callElement, getInlineCallSiteInfo(), reifiedTypeInliner, typeParameterMappings, isDefaultCompilation,
                AnnotationUtilKt.hasInlineOnlyAnnotation(functionDescriptor)
        );

        MethodInliner inliner = new MethodInliner(node, parameters, info, new FieldRemapper(null, null, parameters), isSameModule,
                                                  "Method inlining " + callElement.getText(),
                                                  createNestedSourceMapper(nodeAndSmap), info.getCallSiteInfo()); //with captured

        LocalVarRemapper remapper = new LocalVarRemapper(parameters, initialFrameSize);


        MethodNode adapter = InlineCodegenUtil.createEmptyMethodNode();
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP);

        InlineResult result = inliner.doInline(adapter, remapper, true, LabelOwner.SKIP_ALL);
        result.getReifiedTypeParametersUsages().mergeAll(reificationResult);

        CallableMemberDescriptor descriptor = codegen.getContext().getContextDescriptor();
        final Set<String> labels = getDeclarationLabels(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor);
        LabelOwner labelOwner = new LabelOwner() {
            @Override
            public boolean isMyLabel(@NotNull String name) {
                return labels.contains(name);
            }
        };

        List<MethodInliner.PointForExternalFinallyBlocks> infos = MethodInliner.processReturns(adapter, labelOwner, true, null);
        generateAndInsertFinallyBlocks(adapter, infos, ((StackValue.Local)remapper.remap(parameters.getArgsSizeOnStack() + 1).value).index);
        removeFinallyMarkers(adapter);

        adapter.accept(new MethodBodyVisitor(codegen.v));

        addInlineMarker(codegen.v, false);

        return result;
    }

    private InlineCallSiteInfo getInlineCallSiteInfo() {
        MethodContext context = codegen.getContext();
        MemberCodegen<?> parentCodegen = codegen.getParentCodegen();
        while (context instanceof InlineLambdaContext) {
            CodegenContext closureContext = context.getParentContext();
            assert closureContext instanceof ClosureContext : "Parent context of inline lambda should be closure context";
            assert closureContext.getParentContext() instanceof MethodContext : "Closure context should appear in method context";
            context = (MethodContext) closureContext.getParentContext();
            assert parentCodegen instanceof FakeMemberCodegen : "Parent codegen of inlined lambda should be FakeMemberCodegen";
            parentCodegen = ((FakeMemberCodegen) parentCodegen).delegate;
        }

        JvmMethodSignature signature = typeMapper.mapSignature(context.getFunctionDescriptor(), context.getContextKind());
        return new InlineCallSiteInfo(parentCodegen.getClassName(), signature.getAsmMethod().getName(), signature.getAsmMethod().getDescriptor());
    }

    private void generateClosuresBodies() {
        for (LambdaInfo info : expressionMap.values()) {
            info.setNode(generateLambdaBody(info));
        }
    }

    private SMAPAndMethodNode generateLambdaBody(LambdaInfo info) {
        KtExpression declaration = info.getFunctionWithBodyOrCallableReference();
        FunctionDescriptor descriptor = info.getFunctionDescriptor();

        MethodContext parentContext = codegen.getContext();

        MethodContext context = parentContext.intoClosure(descriptor, codegen, typeMapper).intoInlinedLambda(descriptor, info.isCrossInline);

        JvmMethodSignature jvmMethodSignature = typeMapper.mapSignature(descriptor);
        Method asmMethod = jvmMethodSignature.getAsmMethod();
        MethodNode methodNode = new MethodNode(InlineCodegenUtil.API, getMethodAsmFlags(descriptor, context.getContextKind()), asmMethod.getName(), asmMethod.getDescriptor(), jvmMethodSignature.getGenericsSignature(), null);

        MethodVisitor adapter = InlineCodegenUtil.wrapWithMaxLocalCalc(methodNode);

        SMAP smap = generateMethodBody(adapter, descriptor, context, declaration, jvmMethodSignature, true);
        adapter.visitMaxs(-1, -1);
        return new SMAPAndMethodNode(methodNode, smap);
    }

    private SMAP generateMethodBody(
            @NotNull MethodVisitor adapter,
            @NotNull FunctionDescriptor descriptor,
            @NotNull MethodContext context,
            @NotNull KtExpression expression,
            @NotNull JvmMethodSignature jvmMethodSignature,
            boolean isLambda
    ) {
        FakeMemberCodegen parentCodegen =
                new FakeMemberCodegen(codegen.getParentCodegen(), expression,
                                      (FieldOwnerContext) context.getParentContext(),
                                      isLambda ? codegen.getParentCodegen().getClassName()
                                               : typeMapper.mapImplementationOwner(descriptor).getInternalName());

        FunctionGenerationStrategy strategy =
                expression instanceof KtCallableReferenceExpression ?
                new FunctionReferenceGenerationStrategy(
                        state,
                        descriptor,
                        CallUtilKt.getResolvedCallWithAssert(((KtCallableReferenceExpression) expression).getCallableReference(),
                                                             codegen.getBindingContext()
                        )) :
                new FunctionGenerationStrategy.FunctionDefault(state, descriptor, (KtDeclarationWithBody) expression);

        FunctionCodegen.generateMethodBody(
                adapter, descriptor, context, jvmMethodSignature,
                strategy,
                // Wrapping for preventing marking actual parent codegen as containing reifier markers
                parentCodegen
        );

        if (isLambda) {
            codegen.propagateChildReifiedTypeParametersUsages(parentCodegen.getReifiedTypeParametersUsages());
        }

        return createSMAPWithDefaultMapping(expression, parentCodegen.getOrCreateSourceMapper().getResultMappings());
    }

    private static SMAP createSMAPWithDefaultMapping(
            @NotNull KtExpression declaration,
            @NotNull List<FileMapping> mappings
    ) {
        PsiFile containingFile = declaration.getContainingFile();
        Integer lineNumbers = CodegenUtil.getLineNumberForElement(containingFile, true);
        assert lineNumbers != null : "Couldn't extract line count in " + containingFile;

        return new SMAP(mappings);
    }

    private static class FakeMemberCodegen extends MemberCodegen {

        @NotNull final MemberCodegen delegate;
        @NotNull private final String className;

        public FakeMemberCodegen(@NotNull MemberCodegen wrapped, @NotNull KtElement declaration, @NotNull FieldOwnerContext codegenContext, @NotNull String className) {
            super(wrapped, declaration, codegenContext);
            delegate = wrapped;
            this.className = className;
        }

        @Override
        protected void generateDeclaration() {
            throw new IllegalStateException();
        }

        @Override
        protected void generateBody() {
            throw new IllegalStateException();
        }

        @Override
        protected void generateKotlinMetadataAnnotation() {
            throw new IllegalStateException();
        }

        @NotNull
        @Override
        public NameGenerator getInlineNameGenerator() {
            return delegate.getInlineNameGenerator();
        }

        @NotNull
        @Override
        //TODO: obtain name from context
        public String getClassName() {
            return className;
        }
    }

    @Override
    public void afterParameterPut(
            @NotNull Type type,
            @Nullable StackValue stackValue,
            int parameterIndex
    ) {
        putArgumentOrCapturedToLocalVal(type, stackValue, -1, parameterIndex);
    }

    private void putArgumentOrCapturedToLocalVal(
            @NotNull Type type,
            @Nullable StackValue stackValue,
            int capturedParamIndex,
            int parameterIndex
    ) {
        if (!asFunctionInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutValue(type, stackValue);
            StackValue remappedIndex = couldBeRemapped ? stackValue : null;

            ParameterInfo info;
            if (capturedParamIndex >= 0) {
                CapturedParamDesc capturedParamInfoInLambda = activeLambda.getCapturedVars().get(capturedParamIndex);
                info = invocationParamBuilder.addCapturedParam(capturedParamInfoInLambda, capturedParamInfoInLambda.getFieldName());
                info.setRemapValue(remappedIndex);
            }
            else {
                info = invocationParamBuilder.addNextValueParameter(type, false, remappedIndex, parameterIndex);
            }

            recordParameterValueInLocalVal(info);
        }
    }

    /*descriptor is null for captured vars*/
    public static boolean shouldPutValue(
            @NotNull Type type,
            @Nullable StackValue stackValue
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

        StackValue field = stackValue;
        if (stackValue instanceof StackValue.FieldForSharedVar) {
            field = ((StackValue.FieldForSharedVar) stackValue).receiver;
        }

        //check that value corresponds to captured inlining parameter
        if (field instanceof StackValue.Field) {
            DeclarationDescriptor varDescriptor = ((StackValue.Field) field).descriptor;
            //check that variable is inline function parameter
            return !(varDescriptor instanceof ParameterDescriptor &&
                     InlineUtil.isInlineLambdaParameter((ParameterDescriptor) varDescriptor) &&
                     InlineUtil.isInline(varDescriptor.getContainingDeclaration()));
        }

        return true;
    }

    private void recordParameterValueInLocalVal(ParameterInfo... infos) {
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
                StackValue.local(index[i], type).store(StackValue.onStack(type), codegen.v);
            }
        }
    }

    @Override
    public void putHiddenParams() {
        if ((getMethodAsmFlags(functionDescriptor, context.getContextKind()) & Opcodes.ACC_STATIC) == 0) {
            invocationParamBuilder.addNextParameter(AsmTypes.OBJECT_TYPE, false, null);
        }

        for (JvmMethodParameterSignature param : jvmSignature.getValueParameters()) {
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            invocationParamBuilder.addNextParameter(param.getAsmType(), false, null);
        }

        invocationParamBuilder.markValueParametesStart();
        List<ParameterInfo> hiddenParameters = invocationParamBuilder.buildParameters().getReal();
        recordParameterValueInLocalVal(hiddenParameters.toArray(new ParameterInfo[hiddenParameters.size()]));
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

    /*lambda or callable reference*/
    public boolean isInliningParameter(KtExpression expression, ValueParameterDescriptor valueParameterDescriptor) {
        //TODO deparenthisise typed
        KtExpression deparenthesized = KtPsiUtil.deparenthesize(expression);

        if (deparenthesized instanceof KtCallableReferenceExpression) {
            // TODO: support inline of property references passed to inlinable function parameters
            SimpleFunctionDescriptor functionReference = state.getBindingContext().get(BindingContext.FUNCTION, deparenthesized);
            if (functionReference == null) return false;
        }

        return InlineUtil.isInlineLambdaParameter(valueParameterDescriptor) &&
               isInlinableParameterExpression(deparenthesized);
    }

    protected static boolean isInlinableParameterExpression(KtExpression deparenthesized) {
        return deparenthesized instanceof KtLambdaExpression ||
               deparenthesized instanceof KtNamedFunction ||
               deparenthesized instanceof KtCallableReferenceExpression;
    }

    public void rememberClosure(KtExpression expression, Type type, ValueParameterDescriptor parameter) {
        KtExpression lambda = KtPsiUtil.deparenthesize(expression);
        assert isInlinableParameterExpression(lambda) : "Couldn't find inline expression in " + expression.getText();


        LambdaInfo info = new LambdaInfo(lambda, typeMapper, parameter.isCrossinline());

        ParameterInfo closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.getIndex());
        closureInfo.setLambda(info);
        expressionMap.put(closureInfo.getIndex(), info);
    }

    @NotNull
    protected static Set<String> getDeclarationLabels(@Nullable PsiElement lambdaOrFun, @NotNull DeclarationDescriptor descriptor) {
        Set<String> result = new HashSet<String>();

        if (lambdaOrFun != null) {
            Name label = LabelResolver.INSTANCE.getLabelNameIfAny(lambdaOrFun);
            if (label != null) {
                result.add(label.asString());
            }
        }

        if (!isFunctionLiteral(descriptor)) {
            if (!descriptor.getName().isSpecial()) {
                result.add(descriptor.getName().asString());
            }
            result.add(InlineCodegenUtil.FIRST_FUN_LABEL);
        }
        return result;
    }

    private void putClosureParametersOnStack() {
        for (LambdaInfo next : expressionMap.values()) {
            activeLambda = next;
            codegen.pushClosureOnStack(next.getClassDescriptor(), true, this);
        }
        activeLambda = null;
    }

    public static CodegenContext getContext(@NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state, @Nullable KtFile sourceFile) {
        if (descriptor instanceof PackageFragmentDescriptor) {
            return new PackageContext((PackageFragmentDescriptor) descriptor, state.getRootContext(), null, sourceFile);
        }

        CodegenContext parent = getContext(descriptor.getContainingDeclaration(), state, sourceFile);

        if (descriptor instanceof ScriptDescriptor) {
            List<ScriptDescriptor> earlierScripts = state.getReplSpecific().getEarlierScriptsForReplInterpreter();
            return parent.intoScript((ScriptDescriptor) descriptor,
                                     earlierScripts == null ? Collections.emptyList() : earlierScripts,
                                     (ClassDescriptor) descriptor, state.getTypeMapper());
        }
        else if (descriptor instanceof ClassDescriptor) {
            OwnerKind kind = DescriptorUtils.isInterface(descriptor) ? OwnerKind.DEFAULT_IMPLS : OwnerKind.IMPLEMENTATION;
            return parent.intoClass((ClassDescriptor) descriptor, kind, state);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return parent.intoFunction((FunctionDescriptor) descriptor);
        }

        throw new IllegalStateException("Couldn't build context for " + descriptorName(descriptor));
    }

    private static String descriptorName(DeclarationDescriptor descriptor) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.render(descriptor);
    }

    @Override
    public void genValueAndPut(
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull KtExpression argumentExpression,
            @NotNull Type parameterType,
            int parameterIndex
    ) {
        if (isInliningParameter(argumentExpression, valueParameterDescriptor)) {
            rememberClosure(argumentExpression, parameterType, valueParameterDescriptor);
        }
        else {
            StackValue value = codegen.gen(argumentExpression);
            putValueIfNeeded(parameterType, value, valueParameterDescriptor.getIndex());
        }
    }

    @Override
    public void putValueIfNeeded(
            @NotNull Type parameterType,
            @NotNull StackValue value
    ) {
        putValueIfNeeded(parameterType, value, -1);
    }

    private void putValueIfNeeded(
            @NotNull Type parameterType,
            @NotNull StackValue value,
            int index
    ) {
        if (shouldPutValue(parameterType, value)) {
            value.put(parameterType, codegen.v);
        }
        afterParameterPut(parameterType, value, index);
    }

    @Override
    public void putCapturedValueOnStack(
            @NotNull StackValue stackValue, @NotNull Type valueType, int paramIndex
    ) {
        if (shouldPutValue(stackValue.type, stackValue)) {
            stackValue.put(stackValue.type, codegen.v);
        }
        putArgumentOrCapturedToLocalVal(stackValue.type, stackValue, paramIndex, paramIndex);
    }


    public void generateAndInsertFinallyBlocks(
            @NotNull MethodNode intoNode,
            @NotNull List<MethodInliner.PointForExternalFinallyBlocks> insertPoints,
            int offsetForFinallyLocalVar
    ) {
        if (!codegen.hasFinallyBlocks()) return;

        Map<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks> extensionPoints =
                new HashMap<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks>();
        for (MethodInliner.PointForExternalFinallyBlocks insertPoint : insertPoints) {
            extensionPoints.put(insertPoint.beforeIns, insertPoint);
        }

        DefaultProcessor processor = new DefaultProcessor(intoNode, offsetForFinallyLocalVar);

        int curFinallyDepth = 0;
        AbstractInsnNode curInstr = intoNode.instructions.getFirst();
        while (curInstr != null) {
            processor.processInstruction(curInstr, true);
            if (InlineCodegenUtil.isFinallyStart(curInstr)) {
                //TODO depth index calc could be more precise
                curFinallyDepth = getConstant(curInstr.getPrevious());
            }

            MethodInliner.PointForExternalFinallyBlocks extension = extensionPoints.get(curInstr);
            if (extension != null) {
                Label start = new Label();

                MethodNode finallyNode = InlineCodegenUtil.createEmptyMethodNode();
                finallyNode.visitLabel(start);

                ExpressionCodegen finallyCodegen =
                        new ExpressionCodegen(finallyNode, codegen.getFrameMap(), codegen.getReturnType(),
                                              codegen.getContext(), codegen.getState(), codegen.getParentCodegen());
                finallyCodegen.addBlockStackElementsForNonLocalReturns(codegen.getBlockStackElements(), curFinallyDepth);

                FrameMap frameMap = finallyCodegen.getFrameMap();
                FrameMap.Mark mark = frameMap.mark();
                int marker = -1;
                Set<LocalVarNodeWrapper> intervals = processor.getLocalVarsMetaInfo().getCurrentIntervals();
                for (LocalVarNodeWrapper interval : intervals) {
                    marker = Math.max(interval.getNode().index + 1, marker);
                }
                while (frameMap.getCurrentSize() < Math.max(processor.getNextFreeLocalIndex(), offsetForFinallyLocalVar + marker)) {
                    frameMap.enterTemp(Type.INT_TYPE);
                }

                finallyCodegen.generateFinallyBlocksIfNeeded(extension.returnType, extension.finallyIntervalEnd.getLabel());

                //Exception table for external try/catch/finally blocks will be generated in original codegen after exiting this method
                InlineCodegenUtil.insertNodeBefore(finallyNode, intoNode, curInstr);

                SimpleInterval splitBy = new SimpleInterval((LabelNode) start.info, extension.finallyIntervalEnd);
                processor.getTryBlocksMetaInfo().splitCurrentIntervals(splitBy, true);

                //processor.getLocalVarsMetaInfo().splitAndRemoveIntervalsFromCurrents(splitBy);

                mark.dropTo();
            }

            curInstr = curInstr.getNext();
        }

        processor.substituteTryBlockNodes(intoNode);

        //processor.substituteLocalVarTable(intoNode);
    }

    public void removeFinallyMarkers(@NotNull MethodNode intoNode) {
        if (InlineCodegenUtil.isFinallyMarkerRequired(codegen.getContext())) return;

        InsnList instructions = intoNode.instructions;
        AbstractInsnNode curInstr = instructions.getFirst();
        while (curInstr != null) {
            if (InlineCodegenUtil.isFinallyMarker(curInstr)) {
                AbstractInsnNode marker = curInstr;
                //just to assert
                getConstant(marker.getPrevious());
                curInstr = curInstr.getNext();
                instructions.remove(marker.getPrevious());
                instructions.remove(marker);
                continue;
            }
            curInstr = curInstr.getNext();
        }
    }

    private SourceMapper createNestedSourceMapper(@NotNull SMAPAndMethodNode nodeAndSmap) {
        return new NestedSourceMapper(sourceMapper, nodeAndSmap.getRanges(), nodeAndSmap.getClassSMAP().getSourceInfo());
    }

    private void reportIncrementalInfo(
            @NotNull FunctionDescriptor sourceDescriptor,
            @NotNull FunctionDescriptor targetDescriptor
    ) {
        IncrementalCompilationComponents incrementalCompilationComponents = state.getIncrementalCompilationComponents();
        TargetId targetId = state.getTargetId();

        if (incrementalCompilationComponents == null || targetId == null) return;

        IncrementalCache incrementalCache = incrementalCompilationComponents.getIncrementalCache(targetId);
        String classFilePath = InlineCodegenUtilsKt.getClassFilePath(sourceDescriptor, typeMapper, incrementalCache);
        String sourceFilePath = InlineCodegenUtilsKt.getSourceFilePath(targetDescriptor);
        incrementalCache.registerInline(classFilePath, jvmSignature.toString(), sourceFilePath);
    }

    @Override
    public void reorderArgumentsIfNeeded(
            @NotNull List<ArgumentAndDeclIndex> actualArgsWithDeclIndex, @NotNull List<? extends Type> valueParameterTypes
    ) {

    }
}
