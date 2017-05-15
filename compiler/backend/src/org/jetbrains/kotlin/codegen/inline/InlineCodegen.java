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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment;
import org.jetbrains.kotlin.codegen.*;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicArrayConstructorsKt;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.incremental.KotlinLookupLocation;
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor;
import org.jetbrains.kotlin.resolve.calls.callUtil.CallUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS;
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
import static org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.*;
import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUtilKt.isInlineOnly;
import static org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.isFunctionLiteral;

public class InlineCodegen extends CallGenerator {
    private final GenerationState state;
    private final KotlinTypeMapper typeMapper;

    private final FunctionDescriptor functionDescriptor;
    private final JvmMethodSignature jvmSignature;
    private final KtElement callElement;
    private final MethodContext context;
    private final ExpressionCodegen codegen;

    private final boolean asFunctionInline;
    private final int initialFrameSize;
    private final boolean isSameModule;

    private final ParametersBuilder invocationParamBuilder = ParametersBuilder.newBuilder();
    private final Map<Integer, LambdaInfo> expressionMap = new LinkedHashMap<>();

    private final ReifiedTypeInliner reifiedTypeInliner;

    @NotNull
    private final TypeParameterMappings typeParameterMappings;

    private LambdaInfo activeLambda;

    private final SourceMapper sourceMapper;

    private Runnable delayedHiddenWriting;

    private List<Integer> maskValues = new ArrayList<>();
    private int maskStartIndex = -1;
    private int methodHandleInDefaultMethodIndex = -1;

    public InlineCodegen(
            @NotNull ExpressionCodegen codegen,
            @NotNull GenerationState state,
            @NotNull FunctionDescriptor function,
            @NotNull KtElement callElement,
            @NotNull TypeParameterMappings typeParameterMappings
    ) {
        assert InlineUtil.isInline(function) || InlineUtil.isArrayConstructorWithLambda(function) :
                "InlineCodegen can inline only inline functions and array constructors: " + function;
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
        jvmSignature = typeMapper.mapSignatureWithGeneric(functionDescriptor, context.getContextKind());

        // TODO: implement AS_FUNCTION inline strategy
        this.asFunctionInline = false;

        isSameModule = JvmCodegenUtil.isCallInsideSameModuleAsDeclared(functionDescriptor, codegen.getContext(), state.getOutDirectory());

        sourceMapper = codegen.getParentCodegen().getOrCreateSourceMapper();

        if (!(functionDescriptor instanceof FictitiousArrayConstructor)) {
            reportIncrementalInfo(functionDescriptor, codegen.getContext().getFunctionDescriptor().getOriginal(), jvmSignature, state);
            String functionOrAccessorName = typeMapper.mapAsmMethod(function).getName();
            //track changes for property accessor and @JvmName inline functions/property accessors
            if(!functionOrAccessorName.equals(functionDescriptor.getName().asString())) {
                MemberScope scope = getMemberScope(functionDescriptor);
                if (scope != null) {
                    //Fake lookup to track track changes for property accessors and @JvmName functions/property accessors
                    scope.getContributedFunctions(Name.identifier(functionOrAccessorName), new KotlinLookupLocation(callElement));
                }
            }
        }
    }

    @Nullable
    private static MemberScope getMemberScope(@NotNull FunctionDescriptor functionOrAccessor) {
        CallableMemberDescriptor callableMemberDescriptor = JvmCodegenUtil.getDirectMember(functionOrAccessor);
        DeclarationDescriptor classOrPackageFragment = callableMemberDescriptor.getContainingDeclaration();
        if (classOrPackageFragment instanceof ClassDescriptor) {
            return ((ClassDescriptor) classOrPackageFragment).getUnsubstitutedMemberScope();
        }
        else if (classOrPackageFragment instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) classOrPackageFragment).getMemberScope();
        }
        return null;
    }

    @Override
    public void genCallInner(
            @NotNull Callable callableMethod,
            @Nullable ResolvedCall<?> resolvedCall,
            boolean callDefault,
            @NotNull ExpressionCodegen codegen
    ) {
        if (!state.getInlineCycleReporter().enterIntoInlining(resolvedCall)) {
            generateStub(resolvedCall, codegen);
            return;
        }

        SMAPAndMethodNode nodeAndSmap = null;
        try {
            nodeAndSmap = createMethodNode(functionDescriptor, jvmSignature, codegen, context, callDefault, resolvedCall);
            endCall(inlineCall(nodeAndSmap, callDefault));
        }
        catch (CompilationException e) {
            throw e;
        }
        catch (InlineException e) {
            throw throwCompilationException(nodeAndSmap, e, false);
        }
        catch (Exception e) {
            throw throwCompilationException(nodeAndSmap, e, true);
        }
        finally {
            state.getInlineCycleReporter().exitFromInliningOf(resolvedCall);
        }
    }

    @NotNull
    private CompilationException throwCompilationException(
            @Nullable SMAPAndMethodNode nodeAndSmap, @NotNull Exception e, boolean generateNodeText
    ) {
        CallableMemberDescriptor contextDescriptor = codegen.getContext().getContextDescriptor();
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(contextDescriptor);
        MethodNode node = nodeAndSmap != null ? nodeAndSmap.getNode() : null;
        throw new CompilationException(
                "Couldn't inline method call '" + functionDescriptor.getName() + "' into\n" +
                DescriptorRenderer.DEBUG_TEXT.render(contextDescriptor) + "\n" +
                (element != null ? element.getText() : "<no source>") +
                (generateNodeText ? ("\nCause: " + InlineCodegenUtil.getNodeText(node)) : ""),
                e, callElement
        );
    }

    private void generateStub(@Nullable ResolvedCall<?> resolvedCall, @NotNull ExpressionCodegen codegen) {
        leaveTemps();
        assert resolvedCall != null;
        String message = "Call is part of inline cycle: " + resolvedCall.getCall().getCallElement().getText();
        AsmUtil.genThrow(codegen.v, "java/lang/UnsupportedOperationException", message);
    }

    private void endCall(@NotNull InlineResult result) {
        leaveTemps();

        codegen.propagateChildReifiedTypeParametersUsages(result.getReifiedTypeParametersUsages());

        state.getFactory().removeClasses(result.calcClassesToRemove());

        codegen.markLineNumberAfterInlineIfNeeded();
    }

    @NotNull
    static SMAPAndMethodNode createMethodNode(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull ExpressionCodegen codegen,
            @NotNull CodegenContext context,
            boolean callDefault,
            @Nullable ResolvedCall<?> resolvedCall
    ) {
        if (InlineCodegenUtil.isSpecialEnumMethod(functionDescriptor)) {
            assert resolvedCall != null : "Resolved call for " + functionDescriptor + " should be not null";
            Map<TypeParameterDescriptor, KotlinType> arguments = resolvedCall.getTypeArguments();
            assert arguments.size() == 1 : "Resolved call for " + functionDescriptor + " should have 1 type argument";

            MethodNode node =
                    InlineCodegenUtil.createSpecialEnumMethodBody(
                            codegen,
                            functionDescriptor.getName().asString(),
                            arguments.keySet().iterator().next().getDefaultType(),
                            codegen.getState().getTypeMapper()
                    );
            return new SMAPAndMethodNode(node, SMAPParser.parseOrCreateDefault(null, null, "fake", -1, -1));
        }
        else if (CoroutineCodegenUtilKt.isBuiltInSuspendCoroutineOrReturnInJvm(functionDescriptor)) {
            return new SMAPAndMethodNode(
                    CoroutineCodegenUtilKt.createMethodNodeForSuspendCoroutineOrReturn(
                            functionDescriptor, codegen.getState().getTypeMapper()
                    ),
                    SMAPParser.parseOrCreateDefault(null, null, "fake", -1, -1)
            );
        }

        GenerationState state = codegen.getState();
        Method asmMethod =
                callDefault
                ? state.getTypeMapper().mapDefaultMethod(functionDescriptor, context.getContextKind())
                : jvmSignature.getAsmMethod();

        MethodId methodId = new MethodId(DescriptorUtils.getFqNameSafe(functionDescriptor.getContainingDeclaration()), asmMethod);
        CallableMemberDescriptor directMember = getDirectMemberAndCallableFromObject(functionDescriptor);
        if (!isBuiltInArrayIntrinsic(functionDescriptor) && !(directMember instanceof DeserializedCallableMemberDescriptor)) {
            return doCreateMethodNodeFromSource(functionDescriptor, jvmSignature, codegen, context, callDefault, state, asmMethod);
        }

        SMAPAndMethodNode resultInCache = InlineCacheKt.getOrPut(
                state.getInlineCache().getMethodNodeById(), methodId, () -> {
                    SMAPAndMethodNode result = doCreateMethodNodeFromCompiled(directMember, state, asmMethod);
                    if (result == null) {
                        throw new IllegalStateException("Couldn't obtain compiled function body for " + functionDescriptor);
                    }
                    return result;
                }
        );

        return resultInCache.copyWithNewNode(cloneMethodNode(resultInCache.getNode()));
    }

    @NotNull
    private static CallableMemberDescriptor getDirectMemberAndCallableFromObject(@NotNull FunctionDescriptor functionDescriptor) {
        CallableMemberDescriptor directMember = JvmCodegenUtil.getDirectMember(functionDescriptor);
        if (directMember instanceof ImportedFromObjectCallableDescriptor) {
            return  ((ImportedFromObjectCallableDescriptor) directMember).getCallableFromObject();
        }
        return directMember;
    }

    @NotNull
    private static MethodNode cloneMethodNode(@NotNull MethodNode methodNode) {
        methodNode.instructions.resetLabels();
        MethodNode result = new MethodNode(
                API, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature,
                ArrayUtil.toStringArray(methodNode.exceptions)
        );
        methodNode.accept(result);
        return result;
    }

    @Nullable
    private static SMAPAndMethodNode doCreateMethodNodeFromCompiled(
            @NotNull CallableMemberDescriptor callableDescriptor,
            @NotNull GenerationState state,
            @NotNull Method asmMethod
    ) {
        if (isBuiltInArrayIntrinsic(callableDescriptor)) {
            ClassId classId = IntrinsicArrayConstructorsKt.getClassId();
            byte[] bytes =
                    InlineCacheKt.getOrPut(state.getInlineCache().getClassBytes(), classId, IntrinsicArrayConstructorsKt::getBytecode);
            return InlineCodegenUtil.getMethodNode(bytes, asmMethod.getName(), asmMethod.getDescriptor(), classId.asString());
        }

        assert callableDescriptor instanceof DeserializedCallableMemberDescriptor : "Not a deserialized function or proper: " + callableDescriptor;

        KotlinTypeMapper.ContainingClassesInfo containingClasses =
                state.getTypeMapper().getContainingClassesForDeserializedCallable((DeserializedCallableMemberDescriptor) callableDescriptor);

        ClassId containerId = containingClasses.getImplClassId();

        byte[] bytes = InlineCacheKt.getOrPut(state.getInlineCache().getClassBytes(), containerId, () -> {
            VirtualFile file = InlineCodegenUtil.findVirtualFile(state, containerId);
            if (file == null) {
                throw new IllegalStateException("Couldn't find declaration file for " + containerId);
            }
            try {
                return file.contentsToByteArray();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return InlineCodegenUtil.getMethodNode(bytes, asmMethod.getName(), asmMethod.getDescriptor(), containerId.asString());
    }

    @NotNull
    private static SMAPAndMethodNode doCreateMethodNodeFromSource(
            @NotNull FunctionDescriptor callableDescriptor,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull ExpressionCodegen codegen,
            @NotNull CodegenContext context,
            boolean callDefault,
            @NotNull GenerationState state,
            @NotNull Method asmMethod
    ) {
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(callableDescriptor);

        if (!(element instanceof KtNamedFunction || element instanceof KtPropertyAccessor)) {
            throw new IllegalStateException("Couldn't find declaration for function " + callableDescriptor);
        }
        KtDeclarationWithBody inliningFunction = (KtDeclarationWithBody) element;

        MethodNode node = new MethodNode(
                InlineCodegenUtil.API,
                getMethodAsmFlags(callableDescriptor, context.getContextKind(), state) | (callDefault ? Opcodes.ACC_STATIC : 0),
                asmMethod.getName(),
                asmMethod.getDescriptor(),
                null, null
        );

        //for maxLocals calculation
        MethodVisitor maxCalcAdapter = InlineCodegenUtil.wrapWithMaxLocalCalc(node);
        CodegenContext parentContext = context.getParentContext();
        assert parentContext != null : "Context has no parent: " + context;
        MethodContext methodContext = parentContext.intoFunction(callableDescriptor);

        SMAP smap;
        if (callDefault) {
            Type implementationOwner = state.getTypeMapper().mapImplementationOwner(callableDescriptor);
            FakeMemberCodegen parentCodegen = new FakeMemberCodegen(
                    codegen.getParentCodegen(), inliningFunction, (FieldOwnerContext) methodContext.getParentContext(),
                    implementationOwner.getInternalName()
            );
            if (!(element instanceof KtNamedFunction)) {
                throw new IllegalStateException("Property accessors with default parameters not supported " + callableDescriptor);
            }
            FunctionCodegen.generateDefaultImplBody(
                    methodContext, callableDescriptor, maxCalcAdapter, DefaultParameterValueLoader.DEFAULT,
                    (KtNamedFunction) inliningFunction, parentCodegen, asmMethod
            );
            smap = createSMAPWithDefaultMapping(inliningFunction, parentCodegen.getOrCreateSourceMapper().getResultMappings());
        }
        else {
            smap = generateMethodBody(maxCalcAdapter, callableDescriptor, methodContext, inliningFunction, jvmSignature, codegen,
                                      null);
        }
        maxCalcAdapter.visitMaxs(-1, -1);
        maxCalcAdapter.visitEnd();

        return new SMAPAndMethodNode(node, smap);
    }

    private static boolean isBuiltInArrayIntrinsic(@NotNull CallableMemberDescriptor callableDescriptor) {
        if (callableDescriptor instanceof FictitiousArrayConstructor) return true;
        String name = callableDescriptor.getName().asString();
        return (name.equals("arrayOf") || name.equals("emptyArray")) &&
               callableDescriptor.getContainingDeclaration() instanceof BuiltInsPackageFragment;
    }

    @NotNull
    private InlineResult inlineCall(@NotNull SMAPAndMethodNode nodeAndSmap, boolean callDefault) {
        assert delayedHiddenWriting == null : "'putHiddenParamsIntoLocals' should be called after 'processAndPutHiddenParameters(true)'";
        DefaultSourceMapper defaultSourceMapper = codegen.getParentCodegen().getOrCreateSourceMapper();
        defaultSourceMapper.setCallSiteMarker(new CallSiteMarker(codegen.getLastLineNumber()));
        MethodNode node = nodeAndSmap.getNode();
        if (callDefault) {
            List<DefaultLambda> defaultLambdas = DefaultMethodUtilKt.expandMaskConditionsAndUpdateVariableNodes(
                    node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex,
                    DefaultMethodUtilKt.extractDefaultLambdaOffsetAndDescriptor(jvmSignature, functionDescriptor)
            );
            for (DefaultLambda lambda : defaultLambdas) {
                invocationParamBuilder.buildParameters().getParameterByDeclarationSlot(lambda.getOffset()).setLambda(lambda);
                LambdaInfo prev = expressionMap.put(lambda.getOffset(), lambda);
                assert prev == null : "Lambda with offset " + lambda.getOffset() + " already exists: " + prev;
            }
        }
        ReifiedTypeParametersUsages reificationResult = reifiedTypeInliner.reifyInstructions(node);
        generateClosuresBodies();

        //through generation captured parameters will be added to invocationParamBuilder
        putClosureParametersOnStack();

        addInlineMarker(codegen.v, true);

        Parameters parameters = invocationParamBuilder.buildParameters();

        InliningContext info = new RootInliningContext(
                expressionMap, state, codegen.getInlineNameGenerator().subGenerator(jvmSignature.getAsmMethod().getName()),
                callElement, getInlineCallSiteInfo(), reifiedTypeInliner, typeParameterMappings
        );

        MethodInliner inliner = new MethodInliner(
                node, parameters, info, new FieldRemapper(null, null, parameters), isSameModule,
                "Method inlining " + callElement.getText(),
                createNestedSourceMapper(nodeAndSmap, sourceMapper), info.getCallSiteInfo(),
                isInlineOnly(functionDescriptor) ? new InlineOnlySmapSkipper(codegen) : null
        ); //with captured

        LocalVarRemapper remapper = new LocalVarRemapper(parameters, initialFrameSize);

        MethodNode adapter = InlineCodegenUtil.createEmptyMethodNode();
        //hack to keep linenumber info, otherwise jdi will skip begin of linenumber chain
        adapter.visitInsn(Opcodes.NOP);

        InlineResult result = inliner.doInline(adapter, remapper, true, LabelOwner.SKIP_ALL);
        result.getReifiedTypeParametersUsages().mergeAll(reificationResult);

        CallableMemberDescriptor descriptor = getLabelOwnerDescriptor(codegen.getContext());
        Set<String> labels = getDeclarationLabels(DescriptorToSourceUtils.descriptorToDeclaration(descriptor), descriptor);

        List<MethodInliner.PointForExternalFinallyBlocks> infos = MethodInliner.processReturns(adapter, labels::contains, true, null);
        generateAndInsertFinallyBlocks(
                adapter, infos, ((StackValue.Local) remapper.remap(parameters.getArgsSizeOnStack() + 1).value).index
        );
        removeStaticInitializationTrigger(adapter);
        if (!InlineCodegenUtil.isFinallyMarkerRequired(codegen.getContext())) {
            InlineCodegenUtil.removeFinallyMarkers(adapter);
        }

        adapter.accept(new MethodBodyVisitor(codegen.v));

        addInlineMarker(codegen.v, false);

        defaultSourceMapper.setCallSiteMarker(null);

        return result;
    }

    @NotNull
    private static CallableMemberDescriptor getLabelOwnerDescriptor(@NotNull MethodContext context) {
        if (context.getParentContext() instanceof ClosureContext &&
            ((ClosureContext) context.getParentContext()).getOriginalSuspendLambdaDescriptor() != null) {
            //noinspection ConstantConditions
            return ((ClosureContext) context.getParentContext()).getOriginalSuspendLambdaDescriptor();
        }

        return context.getContextDescriptor();
    }

    private static void removeStaticInitializationTrigger(@NotNull MethodNode methodNode) {
        InsnList insnList = methodNode.instructions;
        AbstractInsnNode insn = insnList.getFirst();
        while (insn != null) {
            if (MultifileClassPartCodegen.isStaticInitTrigger(insn)) {
                AbstractInsnNode clinitTriggerCall = insn;
                insn = insn.getNext();
                insnList.remove(clinitTriggerCall);
            }
            else {
                insn = insn.getNext();
            }
        }
    }

    @NotNull
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

        JvmMethodSignature signature = typeMapper.mapSignatureSkipGeneric(context.getFunctionDescriptor(), context.getContextKind());
        return new InlineCallSiteInfo(
                parentCodegen.getClassName(), signature.getAsmMethod().getName(), signature.getAsmMethod().getDescriptor()
        );
    }

    private void generateClosuresBodies() {
        for (LambdaInfo info : expressionMap.values()) {
            info.generateLambdaBody(codegen, reifiedTypeInliner);
        }
    }

    @NotNull
    public static SMAP generateMethodBody(
            @NotNull MethodVisitor adapter,
            @NotNull FunctionDescriptor descriptor,
            @NotNull MethodContext context,
            @NotNull KtExpression expression,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @NotNull ExpressionCodegen codegen,
            @Nullable ExpressionLambda lambdaInfo
    ) {
        boolean isLambda = lambdaInfo != null;
        GenerationState state = codegen.getState();

        // Wrapping for preventing marking actual parent codegen as containing reified markers
        FakeMemberCodegen parentCodegen = new FakeMemberCodegen(
                codegen.getParentCodegen(), expression, (FieldOwnerContext) context.getParentContext(),
                isLambda ? codegen.getParentCodegen().getClassName()
                         : state.getTypeMapper().mapImplementationOwner(descriptor).getInternalName()
        );

        FunctionGenerationStrategy strategy;
        if (expression instanceof KtCallableReferenceExpression) {
            KtCallableReferenceExpression callableReferenceExpression = (KtCallableReferenceExpression) expression;
            KtExpression receiverExpression = callableReferenceExpression.getReceiverExpression();
            Type receiverType =
                    receiverExpression != null && codegen.getBindingContext().getType(receiverExpression) != null
                    ? codegen.getState().getTypeMapper().mapType(codegen.getBindingContext().getType(receiverExpression))
                    : null;

            if (isLambda && lambdaInfo.isPropertyReference()) {
                Type asmType = state.getTypeMapper().mapClass(lambdaInfo.getClassDescriptor());
                PropertyReferenceInfo info = lambdaInfo.getPropertyReferenceInfo();
                strategy = new PropertyReferenceCodegen.PropertyReferenceGenerationStrategy(
                        true, info.getGetFunction(), info.getTarget(), asmType, receiverType,
                        lambdaInfo.getFunctionWithBodyOrCallableReference(), state, true);
            }
            else {
                strategy = new FunctionReferenceGenerationStrategy(
                        state,
                        descriptor,
                        CallUtilKt
                                .getResolvedCallWithAssert(callableReferenceExpression.getCallableReference(), codegen.getBindingContext()),
                        receiverType,
                        null,
                        true
                );
            }
        }
        else if (expression instanceof KtFunctionLiteral) {
            strategy = new ClosureGenerationStrategy(state, (KtDeclarationWithBody) expression);
        }
        else {
            strategy = new FunctionGenerationStrategy.FunctionDefault(state, (KtDeclarationWithBody) expression);
        }

        FunctionCodegen.generateMethodBody(adapter, descriptor, context, jvmMethodSignature, strategy, parentCodegen);

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
        private final MemberCodegen delegate;
        private final String className;

        @SuppressWarnings("unchecked")
        public FakeMemberCodegen(
                @NotNull MemberCodegen wrapped,
                @NotNull KtElement declaration,
                @NotNull FieldOwnerContext codegenContext,
                @NotNull String className
        ) {
            super(wrapped, declaration, codegenContext);
            this.delegate = wrapped;
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

    private void putArgumentOrCapturedToLocalVal(
            @NotNull Type type,
            @NotNull StackValue stackValue,
            int capturedParamIndex,
            int parameterIndex,
            @NotNull ValueKind kind
    ) {
        boolean isDefaultParameter = kind == ValueKind.DEFAULT_PARAMETER;
        if (!isDefaultParameter && shouldPutGeneralValue(type, stackValue)) {
            stackValue.put(type, codegen.v);
        }

        if (!asFunctionInline && Type.VOID_TYPE != type) {
            //TODO remap only inlinable closure => otherwise we could get a lot of problem
            boolean couldBeRemapped = !shouldPutGeneralValue(type, stackValue) && kind != ValueKind.DEFAULT_PARAMETER;
            StackValue remappedValue = couldBeRemapped ? stackValue : null;

            ParameterInfo info;
            if (capturedParamIndex >= 0) {
                CapturedParamDesc capturedParamInfoInLambda = activeLambda.getCapturedVars().get(capturedParamIndex);
                info = invocationParamBuilder.addCapturedParam(capturedParamInfoInLambda, capturedParamInfoInLambda.getFieldName(), false);
                info.setRemapValue(remappedValue);
            }
            else {
                info = invocationParamBuilder.addNextValueParameter(type, false, remappedValue, parameterIndex);
            }

            recordParameterValueInLocalVal(
                    false,
                    isDefaultParameter || kind == ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER,
                    info
            );
        }
    }

    /*descriptor is null for captured vars*/
    private static boolean shouldPutGeneralValue(@NotNull Type type, @NotNull StackValue stackValue) {
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

    private Runnable recordParameterValueInLocalVal(boolean delayedWritingToLocals, boolean skipStore, @NotNull ParameterInfo... infos) {
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

        Runnable runnable = () -> {
            for (int i = infos.length - 1; i >= 0; i--) {
                ParameterInfo info = infos[i];
                if (!info.isSkippedOrRemapped()) {
                    Type type = info.type;
                    StackValue.Local local = StackValue.local(index[i], type);
                    if (!skipStore) {
                        local.store(StackValue.onStack(type), codegen.v);
                    }
                    if (info instanceof CapturedParamInfo) {
                        info.setRemapValue(local);
                        ((CapturedParamInfo) info).setSynthetic(true);
                    }
                }
            }
        };

        if (delayedWritingToLocals) return runnable;
        runnable.run();
        return null;
    }

    @Override
    public void processAndPutHiddenParameters(boolean justProcess) {
        if ((getMethodAsmFlags(functionDescriptor, context.getContextKind(), state) & Opcodes.ACC_STATIC) == 0) {
            invocationParamBuilder.addNextParameter(AsmTypes.OBJECT_TYPE, false);
        }

        for (JvmMethodParameterSignature param : jvmSignature.getValueParameters()) {
            if (param.getKind() == JvmMethodParameterKind.VALUE) {
                break;
            }
            invocationParamBuilder.addNextParameter(param.getAsmType(), false);
        }

        invocationParamBuilder.markValueParametersStart();
        List<ParameterInfo> hiddenParameters = invocationParamBuilder.buildParameters().getParameters();

        delayedHiddenWriting = recordParameterValueInLocalVal(justProcess, false, hiddenParameters.toArray(new ParameterInfo[hiddenParameters.size()]));
    }

    private void leaveTemps() {
        List<ParameterInfo> infos = invocationParamBuilder.listAllParams();
        for (ListIterator<? extends ParameterInfo> iterator = infos.listIterator(infos.size()); iterator.hasPrevious(); ) {
            ParameterInfo param = iterator.previous();
            if (!param.isSkippedOrRemapped() || CapturedParamInfo.isSynthetic(param)) {
                codegen.getFrameMap().leaveTemp(param.type);
            }
        }
    }

    /*lambda or callable reference*/
    private static boolean isInliningParameter(@NotNull KtExpression expression, @NotNull ValueParameterDescriptor valueParameterDescriptor) {
        //TODO deparenthisise typed
        KtExpression deparenthesized = KtPsiUtil.deparenthesize(expression);

        return InlineUtil.isInlineLambdaParameter(valueParameterDescriptor) &&
               isInlinableParameterExpression(deparenthesized);
    }

    private LambdaInfo rememberClosure(@NotNull KtExpression expression, @NotNull Type type, @NotNull ValueParameterDescriptor parameter) {
        KtExpression lambda = KtPsiUtil.deparenthesize(expression);
        assert isInlinableParameterExpression(lambda) : "Couldn't find inline expression in " + expression.getText();

        LambdaInfo info =
                new ExpressionLambda(lambda, typeMapper, parameter.isCrossinline(), getBoundCallableReferenceReceiver(expression) != null);

        ParameterInfo closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.getIndex());
        closureInfo.setLambda(info);
        expressionMap.put(closureInfo.getIndex(), info);
        return info;
    }

    @NotNull
    public static Set<String> getDeclarationLabels(@Nullable PsiElement lambdaOrFun, @NotNull DeclarationDescriptor descriptor) {
        Set<String> result = new HashSet<>();

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
            //closure parameters for bounded callable references are generated inplace
            if (next.isBoundCallableReference) continue;
            putClosureParametersOnStack(next, null);
        }
    }

    private void putClosureParametersOnStack(@NotNull LambdaInfo next, @Nullable StackValue functionReferenceReceiver) {
        activeLambda = next;
        if (next instanceof ExpressionLambda) {
            codegen.pushClosureOnStack(((ExpressionLambda) next).getClassDescriptor(), true, this, functionReferenceReceiver);
        }
        else if (next instanceof DefaultLambda) {
            rememberCapturedForDefaultLambda((DefaultLambda) next);
        }
        else {
            throw new RuntimeException("Unknown lambda: " + next);
        }
        activeLambda = null;
    }

    private void rememberCapturedForDefaultLambda(@NotNull DefaultLambda defaultLambda) {
        List<CapturedParamDesc> vars = defaultLambda.getCapturedVars();
        int paramIndex = 0;
        for (CapturedParamDesc captured : vars) {
            putArgumentOrCapturedToLocalVal(
                    captured.getType(),
                    //HACK: actually parameter would be placed on stack in default function
                    // also see ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER check
                    StackValue.onStack(captured.getType()),
                    paramIndex,
                    paramIndex,
                    ValueKind.DEFAULT_LAMBDA_CAPTURED_PARAMETER
            );

            paramIndex++;
            defaultLambda.getParameterOffsetsInDefault().add(invocationParamBuilder.getNextParameterOffset());
        }
    }

    @NotNull
    public static CodegenContext getContext(
            @NotNull DeclarationDescriptor descriptor, @NotNull GenerationState state, @Nullable KtFile sourceFile
    ) {
        if (descriptor instanceof PackageFragmentDescriptor) {
            return new PackageContext((PackageFragmentDescriptor) descriptor, state.getRootContext(), null, sourceFile);
        }

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        assert container != null : "No container for descriptor: " + descriptor;
        CodegenContext parent = getContext(container, state, sourceFile);

        if (descriptor instanceof ScriptDescriptor) {
            List<ScriptDescriptor> earlierScripts = state.getReplSpecific().getEarlierScriptsForReplInterpreter();
            return parent.intoScript(
                    (ScriptDescriptor) descriptor,
                    earlierScripts == null ? Collections.emptyList() : earlierScripts,
                    (ClassDescriptor) descriptor, state.getTypeMapper()
            );
        }
        else if (descriptor instanceof ClassDescriptor) {
            OwnerKind kind = DescriptorUtils.isInterface(descriptor) ? OwnerKind.DEFAULT_IMPLS : OwnerKind.IMPLEMENTATION;
            return parent.intoClass((ClassDescriptor) descriptor, kind, state);
        }
        else if (descriptor instanceof FunctionDescriptor) {
            return parent.intoFunction((FunctionDescriptor) descriptor);
        }

        throw new IllegalStateException("Couldn't build context for " + descriptor);
    }

    @Override
    public void genValueAndPut(
            @NotNull ValueParameterDescriptor valueParameterDescriptor,
            @NotNull KtExpression argumentExpression,
            @NotNull Type parameterType,
            int parameterIndex
    ) {
        if (isInliningParameter(argumentExpression, valueParameterDescriptor)) {
            LambdaInfo lambdaInfo = rememberClosure(argumentExpression, parameterType, valueParameterDescriptor);

            KtExpression receiver = getBoundCallableReferenceReceiver(argumentExpression);
            if (receiver != null) {
                putClosureParametersOnStack(lambdaInfo, codegen.gen(receiver));
            }
        }
        else {
            StackValue value = codegen.gen(argumentExpression);
            putValueIfNeeded(parameterType, value, ValueKind.GENERAL, valueParameterDescriptor.getIndex());
        }
    }

    private KtExpression getBoundCallableReferenceReceiver(
            @NotNull KtExpression argumentExpression
    ) {
        KtExpression deparenthesized = KtPsiUtil.deparenthesize(argumentExpression);
        if (deparenthesized instanceof KtCallableReferenceExpression) {
            KtExpression receiverExpression = ((KtCallableReferenceExpression) deparenthesized).getReceiverExpression();
            if (receiverExpression != null) {
                DoubleColonLHS lhs = state.getBindingContext().get(BindingContext.DOUBLE_COLON_LHS, receiverExpression);
                if (lhs instanceof DoubleColonLHS.Expression) return receiverExpression;
            }
        }
        return null;
    }

    @Override
    public void putValueIfNeeded(@NotNull Type parameterType, @NotNull StackValue value, @NotNull ValueKind kind, int parameterIndex) {
        if (processDefaultMaskOrMethodHandler(value, kind)) return;

        assert maskValues.isEmpty() : "Additional default call arguments should be last ones, but " + value;

        putArgumentOrCapturedToLocalVal(parameterType, value, -1, parameterIndex, kind);
    }

    private boolean processDefaultMaskOrMethodHandler(@NotNull StackValue value, @NotNull ValueKind kind) {
        if (kind != ValueKind.DEFAULT_MASK && kind != ValueKind.METHOD_HANDLE_IN_DEFAULT) {
            return false;
        }
        assert value instanceof StackValue.Constant : "Additional default method argument should be constant, but " + value;
        Object constantValue = ((StackValue.Constant) value).value;
        if (kind == ValueKind.DEFAULT_MASK) {
            assert constantValue instanceof Integer : "Mask should be of Integer type, but " + constantValue;
            maskValues.add((Integer) constantValue);
            if (maskStartIndex == -1) {
                maskStartIndex = invocationParamBuilder.getNextParameterOffset();
            }
        }
        else {
            assert constantValue == null : "Additional method handle for default argument should be null, but " + constantValue;
            methodHandleInDefaultMethodIndex = maskStartIndex + maskValues.size();
        }
        return true;
    }

    @Override
    public void putCapturedValueOnStack(@NotNull StackValue stackValue, @NotNull Type valueType, int paramIndex) {
        putArgumentOrCapturedToLocalVal(stackValue.type, stackValue, paramIndex, paramIndex, ValueKind.CAPTURED);
    }

    private void generateAndInsertFinallyBlocks(
            @NotNull MethodNode intoNode,
            @NotNull List<MethodInliner.PointForExternalFinallyBlocks> insertPoints,
            int offsetForFinallyLocalVar
    ) {
        if (!codegen.hasFinallyBlocks()) return;

        Map<AbstractInsnNode, MethodInliner.PointForExternalFinallyBlocks> extensionPoints = new HashMap<>();
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

    @NotNull
    public static SourceMapper createNestedSourceMapper(@NotNull SMAPAndMethodNode nodeAndSmap, @NotNull SourceMapper parent) {
        return new NestedSourceMapper(parent, nodeAndSmap.getSortedRanges(), nodeAndSmap.getClassSMAP().getSourceInfo());
    }

    static void reportIncrementalInfo(
            @NotNull FunctionDescriptor sourceDescriptor,
            @NotNull FunctionDescriptor targetDescriptor,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull GenerationState state
    ) {
        IncrementalCache incrementalCache = state.getIncrementalCacheForThisTarget();
        if (incrementalCache == null) return;
        String classFilePath = InlineCodegenUtilsKt.getClassFilePath(sourceDescriptor, state.getTypeMapper(), incrementalCache);
        String sourceFilePath = InlineCodegenUtilsKt.getSourceFilePath(targetDescriptor);
        Method method = jvmSignature.getAsmMethod();
        incrementalCache.registerInline(classFilePath, method.getName() + method.getDescriptor(), sourceFilePath);
    }

    @Override
    public void reorderArgumentsIfNeeded(
            @NotNull List<ArgumentAndDeclIndex> actualArgsWithDeclIndex, @NotNull List<? extends Type> valueParameterTypes
    ) {
    }

    @Override
    public void putHiddenParamsIntoLocals() {
        assert delayedHiddenWriting != null : "processAndPutHiddenParameters(true) should be called before putHiddenParamsIntoLocals";
        delayedHiddenWriting.run();
        delayedHiddenWriting = null;
    }
}
