/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.util.ArrayUtil;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.bridges.Bridge;
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.codegen.binding.CodegenBinding;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.coroutines.SuspendFunctionGenerationStrategy;
import org.jetbrains.kotlin.codegen.coroutines.SuspendInlineFunctionGenerationStrategy;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.JvmDefaultMode;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.BuiltinMethodsWithSpecialGenericSignature;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.java.SpecialBuiltinMembers;
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.util.UnderscoreUtilKt;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.KClassValue;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.AsmTypes;
import org.jetbrains.kotlin.resolve.jvm.RuntimeAssertionInfo;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import static org.jetbrains.kotlin.builtins.KotlinBuiltIns.isNullableAny;
import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.CodegenUtilKt.generateBridgeForMainFunctionIfNecessary;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.METHOD_FOR_FUNCTION;
import static org.jetbrains.kotlin.codegen.state.KotlinTypeMapper.isAccessor;
import static org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind.DECLARATION;
import static org.jetbrains.kotlin.descriptors.ModalityKt.isOverridable;
import static org.jetbrains.kotlin.descriptors.annotations.AnnotationUtilKt.isEffectivelyInlineOnly;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.getSourceFromDescriptor;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.InlineClassManglingRulesKt.shouldHideConstructorDueToInlineClassTypeValueParameters;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmDefaultAnnotation;
import static org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils.*;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class FunctionCodegen {
    public final GenerationState state;
    private final KotlinTypeMapper typeMapper;
    private final BindingContext bindingContext;
    private final CodegenContext owner;
    private final ClassBuilder v;
    private final MemberCodegen<?> memberCodegen;

    private final Function1<CallableMemberDescriptor, Boolean> DECLARATION_AND_DEFINITION_CHECKER = new Function1<CallableMemberDescriptor, Boolean>() {
        @Override
        public Boolean invoke(CallableMemberDescriptor descriptor) {
            return !isInterface(descriptor.getContainingDeclaration()) ||
                   (state.getTarget() != JvmTarget.JVM_1_6 &&
                   hasJvmDefaultAnnotation(descriptor));
        }
    };

    public FunctionCodegen(
            @NotNull CodegenContext owner,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @NotNull MemberCodegen<?> memberCodegen
    ) {
        this.owner = owner;
        this.v = v;
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = state.getBindingContext();
        this.memberCodegen = memberCodegen;
    }

    public void gen(@NotNull KtNamedFunction function) {
        SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, function);
        if (bindingContext.get(CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, functionDescriptor) != null) {
            functionDescriptor =
                    (SimpleFunctionDescriptor) bindingContext.get(CodegenBinding.SUSPEND_FUNCTION_TO_JVM_VIEW, functionDescriptor);
        }

        if (functionDescriptor == null) {
            throw ExceptionLogger.logDescriptorNotFound("No descriptor for function " + function.getName(), function);
        }

        if (owner.getContextKind() != OwnerKind.DEFAULT_IMPLS || function.hasBody()) {
            FunctionGenerationStrategy strategy;
            if (functionDescriptor.isSuspend()) {
                if (AnnotationUtilKt.isEffectivelyInlineOnly(functionDescriptor)) {
                    strategy = new FunctionGenerationStrategy.FunctionDefault(state, function);
                } else if (!functionDescriptor.isInline()) {
                    strategy = new SuspendFunctionGenerationStrategy(
                            state,
                            CoroutineCodegenUtilKt.<FunctionDescriptor>unwrapInitialDescriptorForSuspendFunction(functionDescriptor),
                            function,
                            v.getThisName(),
                            state.getConstructorCallNormalizationMode()
                    );
                } else {
                    strategy = new SuspendInlineFunctionGenerationStrategy(
                            state,
                            CoroutineCodegenUtilKt.<FunctionDescriptor>unwrapInitialDescriptorForSuspendFunction(functionDescriptor),
                            function,
                            v.getThisName(),
                            state.getConstructorCallNormalizationMode(),
                            this
                    );
                }
            } else {
                strategy = new FunctionGenerationStrategy.FunctionDefault(state, function);
            }

            generateMethod(JvmDeclarationOriginKt.OtherOrigin(function, functionDescriptor), functionDescriptor, strategy);
        }

        generateDefaultIfNeeded(owner.intoFunction(functionDescriptor, true), functionDescriptor, owner.getContextKind(),
                                DefaultParameterValueLoader.DEFAULT, function);

        generateOverloadsWithDefaultValues(function, functionDescriptor, functionDescriptor);
    }

    public void generateOverloadsWithDefaultValues(
            @Nullable KtNamedFunction function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull FunctionDescriptor delegateFunctionDescriptor
    ) {
        new DefaultParameterValueSubstitutor(state).generateOverloadsIfNeeded(
                function, functionDescriptor, delegateFunctionDescriptor, owner.getContextKind(), v, memberCodegen
        );
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        if (CoroutineCodegenUtilKt.isSuspendFunctionNotSuspensionView(descriptor)) {
            generateMethod(origin, CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(descriptor, state), strategy);
            return;
        }

        generateMethod(origin, descriptor, owner.intoFunction(descriptor), strategy);
    }

    public void generateMethod(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy
    ) {
        OwnerKind contextKind = methodContext.getContextKind();
        DeclarationDescriptor containingDeclaration = functionDescriptor.getContainingDeclaration();
        if (isInterface(containingDeclaration) &&
            !processInterfaceMethod(functionDescriptor, contextKind, false, false, state.getJvmDefaultMode())) {
            return;
        }

        if (!shouldGenerateMethodInsideInlineClass(origin, functionDescriptor, contextKind, containingDeclaration)) {
            return;
        }

        boolean hasSpecialBridge = hasSpecialBridgeMethod(functionDescriptor);
        JvmMethodGenericSignature jvmSignature = strategy.mapMethodSignature(functionDescriptor, typeMapper, contextKind, hasSpecialBridge);
        Method asmMethod = jvmSignature.getAsmMethod();

        int flags = getMethodAsmFlags(functionDescriptor, contextKind, state);

        if (origin.getOriginKind() == JvmDeclarationOriginKind.SAM_DELEGATION) {
            flags |= ACC_SYNTHETIC;
        }

        if (functionDescriptor.isExternal() && owner instanceof MultifileClassFacadeContext) {
            // Native methods are only defined in facades and do not need package part implementations
            return;
        }

        MethodVisitor mv =
                strategy.wrapMethodVisitor(
                        newMethod(origin,
                                  flags,
                                  asmMethod.getName(),
                                  asmMethod.getDescriptor(),
                                  strategy.skipGenericSignature() ? null : jvmSignature.getGenericsSignature(),
                                  getThrownExceptions(functionDescriptor, typeMapper)
                        ),
                        flags, asmMethod.getName(),
                        asmMethod.getDescriptor()
                );

        recordMethodForFunctionIfAppropriate(functionDescriptor, asmMethod);

        generateMethodAnnotationsIfRequired(functionDescriptor, asmMethod, jvmSignature, mv);

        GenerateJava8ParameterNamesKt.generateParameterNames(functionDescriptor, mv, jvmSignature, state, (flags & ACC_SYNTHETIC) != 0);

        if (contextKind != OwnerKind.ERASED_INLINE_CLASS) {
            generateBridges(functionDescriptor);
        }

        boolean staticInCompanionObject = CodegenUtilKt.isJvmStaticInCompanionObject(functionDescriptor);
        if (staticInCompanionObject) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addAdditionalTask(new JvmStaticInCompanionObjectGenerator(functionDescriptor, origin, state, parentBodyCodegen));
        }

        generateBridgeForMainFunctionIfNecessary(state, v, functionDescriptor, jvmSignature, origin, methodContext.getParentContext());

        boolean isOpenSuspendInClass =
                functionDescriptor.isSuspend() &&
                functionDescriptor.getModality() != Modality.ABSTRACT && isOverridable(functionDescriptor) &&
                !isInterface(containingDeclaration) &&
                !(containingDeclaration instanceof PackageFragmentDescriptor) &&
                origin.getOriginKind() != JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL;

        if (isOpenSuspendInClass) {
            generateOpenMethodInSuspendClass(
                    origin, functionDescriptor, methodContext, strategy, mv, jvmSignature, asmMethod, flags, staticInCompanionObject
            );
        }
        else if (canDelegateMethodBodyToInlineClass(origin, functionDescriptor, contextKind, containingDeclaration)) {
            generateMethodInsideInlineClassWrapper(origin, functionDescriptor, (ClassDescriptor) containingDeclaration, mv, typeMapper);
        }
        else {
            generateMethodBody(
                    origin, functionDescriptor, methodContext, strategy, mv, jvmSignature, staticInCompanionObject
            );
        }
    }

    private void recordMethodForFunctionIfAppropriate(
            @NotNull FunctionDescriptor functionDescriptor,
            Method asmMethod
    ) {
        if (functionDescriptor instanceof AccessorForConstructorDescriptor) {
            ConstructorDescriptor originalConstructor = ((AccessorForConstructorDescriptor) functionDescriptor).getCalleeDescriptor();
            if (shouldHideConstructorDueToInlineClassTypeValueParameters(originalConstructor)) {
                functionDescriptor = originalConstructor;
            }
        }
        else if (shouldHideConstructorDueToInlineClassTypeValueParameters(functionDescriptor)) {
            return;
        }

        functionDescriptor = CodegenUtilKt.unwrapFrontendVersion(functionDescriptor);

        if (!CodegenContextUtil.isImplementationOwner(owner, functionDescriptor)) return;
        v.getSerializationBindings().put(METHOD_FOR_FUNCTION, functionDescriptor, asmMethod);
    }

    private void generateMethodAnnotationsIfRequired(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull Method asmMethod,
            @NotNull JvmMethodGenericSignature jvmSignature,
            @NotNull MethodVisitor mv
    ) {
        FunctionDescriptor annotationsOwner;
        if (shouldHideConstructorDueToInlineClassTypeValueParameters(functionDescriptor)) {
            if (functionDescriptor instanceof AccessorForConstructorDescriptor) {
                annotationsOwner = ((AccessorForConstructorDescriptor) functionDescriptor).getCalleeDescriptor();
            }
            else {
                return;
            }
        }
        else {
            annotationsOwner = functionDescriptor;
        }

        AnnotationCodegen.forMethod(mv, memberCodegen, state).genAnnotations(annotationsOwner, asmMethod.getReturnType());
        generateParameterAnnotations(annotationsOwner, mv, jvmSignature, memberCodegen, state);
    }

    @NotNull
    public MethodVisitor newMethod(
            @NotNull JvmDeclarationOrigin origin,
            int access,
            @NotNull String name,
            @NotNull String desc,
            @Nullable String signature,
            @Nullable String[] exceptions
    ) {
        return v.newMethod(origin, access, name, desc, signature, exceptions);
    }

    private static boolean shouldGenerateMethodInsideInlineClass(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind contextKind,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        return !canDelegateMethodBodyToInlineClass(origin, functionDescriptor, contextKind, containingDeclaration) ||
               !functionDescriptor.getOverriddenDescriptors().isEmpty() ||
               CodegenUtilKt.isJvmStaticInInlineClass(functionDescriptor);
    }

    private static boolean canDelegateMethodBodyToInlineClass(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind contextKind,
            @NotNull DeclarationDescriptor containingDeclaration
    ) {
        // special kind / function
        if (contextKind == OwnerKind.ERASED_INLINE_CLASS) return false;
        if (origin.getOriginKind() == JvmDeclarationOriginKind.UNBOX_METHOD_OF_INLINE_CLASS) return false;

        // Synthesized class member descriptors corresponding to JvmStatic members of companion object
        if (CodegenUtilKt.isJvmStaticInInlineClass(functionDescriptor)) return false;

        // descriptor corresponds to the underlying value
        if (functionDescriptor instanceof PropertyAccessorDescriptor) {
            PropertyDescriptor property = ((PropertyAccessorDescriptor) functionDescriptor).getCorrespondingProperty();
            if (InlineClassesUtilsKt.isUnderlyingPropertyOfInlineClass(property)) {
                return false;
            }
        }

        // base check
        boolean isInlineClass = InlineClassesUtilsKt.isInlineClass(containingDeclaration);
        boolean simpleFunctionOrProperty =
                !(functionDescriptor instanceof ConstructorDescriptor) && !isAccessor(functionDescriptor);

        return isInlineClass && simpleFunctionOrProperty;
    }

    public static void generateMethodInsideInlineClassWrapper(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull ClassDescriptor containingDeclaration,
            @NotNull MethodVisitor mv,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        mv.visitCode();

        Type fieldOwnerType = typeMapper.mapClass(containingDeclaration);
        Method erasedMethodImpl = typeMapper.mapAsmMethod(functionDescriptor.getOriginal(), OwnerKind.ERASED_INLINE_CLASS);

        ValueParameterDescriptor valueRepresentation = InlineClassesUtilsKt.underlyingRepresentation(containingDeclaration);
        if (valueRepresentation == null) return;

        Type fieldType = typeMapper.mapType(valueRepresentation);

        generateDelegateToStaticErasedVersion(
                mv, erasedMethodImpl,
                fieldOwnerType, valueRepresentation.getName().asString(), fieldType
        );

        endVisit(mv, null, origin.getElement());
    }

    private void generateOpenMethodInSuspendClass(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull Method asmMethod,
            int flags,
            boolean staticInCompanionObject
    ) {
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        int index = 1;
        for (Type type : asmMethod.getArgumentTypes()) {
            mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), index);
            index += type.getSize();
        }

        Method asmMethodForOpenSuspendImpl = CoroutineCodegenUtilKt.getImplForOpenMethod(asmMethod, v.getThisName());
        // remove generic signature as it's unnecessary for synthetic methods
        JvmMethodSignature jvmSignatureForOpenSuspendImpl =
                new JvmMethodGenericSignature(
                        asmMethodForOpenSuspendImpl,
                        jvmSignature.getValueParameters(),
                        null
                );

        mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                v.getThisName(), asmMethodForOpenSuspendImpl.getName(), asmMethodForOpenSuspendImpl.getDescriptor(),
                false
        );

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitEnd();

        int flagsForOpenSuspendImpl = flags;
        flagsForOpenSuspendImpl |= Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC;
        flagsForOpenSuspendImpl &= ~getVisibilityAccessFlag(functionDescriptor);
        flagsForOpenSuspendImpl |= AsmUtil.NO_FLAG_PACKAGE_PRIVATE;

        MethodVisitor mvForOpenSuspendImpl = strategy.wrapMethodVisitor(
                v.newMethod(origin,
                            flagsForOpenSuspendImpl,
                            asmMethodForOpenSuspendImpl.getName(),
                            asmMethodForOpenSuspendImpl.getDescriptor(),
                            null,
                            getThrownExceptions(functionDescriptor, typeMapper)
                ),
                flagsForOpenSuspendImpl, asmMethodForOpenSuspendImpl.getName(),
                asmMethodForOpenSuspendImpl.getDescriptor()
        );

        generateMethodBody(
                origin, functionDescriptor, methodContext, strategy, mvForOpenSuspendImpl, jvmSignatureForOpenSuspendImpl,
                staticInCompanionObject
        );
    }

    private void generateMethodBody(
            @NotNull JvmDeclarationOrigin origin,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext methodContext,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature,
            boolean staticInCompanionObject
    ) {
        OwnerKind contextKind = methodContext.getContextKind();
        if (!state.getClassBuilderMode().generateBodies || isAbstractMethod(functionDescriptor, contextKind)) {
            generateLocalVariableTable(
                    mv,
                    jvmSignature,
                    functionDescriptor,
                    getThisTypeForFunction(functionDescriptor, methodContext, typeMapper),
                    new Label(),
                    new Label(),
                    contextKind,
                    typeMapper,
                    Collections.emptyList(),
                    0);

            mv.visitEnd();
            return;
        }

        if (!functionDescriptor.isExternal()) {
            generateMethodBody(mv, functionDescriptor, methodContext, jvmSignature, strategy, memberCodegen, state.getJvmDefaultMode(),
                               state.getLanguageVersionSettings().supportsFeature(LanguageFeature.ReleaseCoroutines));
        }
        else if (staticInCompanionObject) {
            // native @JvmStatic foo() in companion object should delegate to the static native function moved to the outer class
            mv.visitCode();
            FunctionDescriptor staticFunctionDescriptor = JvmStaticInCompanionObjectGenerator
                    .createStaticFunctionDescriptor(functionDescriptor);
            Method accessorMethod =
                    typeMapper.mapAsmMethod(memberCodegen.getContext().accessibleDescriptor(staticFunctionDescriptor, null));
            Type owningType = typeMapper.mapClass((ClassifierDescriptor) staticFunctionDescriptor.getContainingDeclaration());
            generateDelegateToStaticMethodBody(false, mv, accessorMethod, owningType.getInternalName(), false);
        }

        endVisit(mv, null, origin.getElement());
    }

    public static void generateParameterAnnotations(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        generateParameterAnnotations(
                functionDescriptor, mv, jvmSignature, functionDescriptor.getValueParameters(), innerClassConsumer, state
        );
    }

    public static void generateParameterAnnotations(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmSignature,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            @NotNull InnerClassConsumer innerClassConsumer,
            @NotNull GenerationState state
    ) {
        if (isAccessor(functionDescriptor)) return;

        Iterator<ValueParameterDescriptor> iterator = valueParameters.iterator();
        List<JvmMethodParameterSignature> kotlinParameterTypes = jvmSignature.getValueParameters();

        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            JvmMethodParameterSignature parameterSignature = kotlinParameterTypes.get(i);
            JvmMethodParameterKind kind = parameterSignature.getKind();
            if (kind.isSkippedInGenericSignature()) {
                markEnumOrInnerConstructorParameterAsSynthetic(mv, i, state.getClassBuilderMode());
                continue;
            }

            Annotated annotated =
                    kind == JvmMethodParameterKind.VALUE
                    ? iterator.next()
                    : kind == JvmMethodParameterKind.RECEIVER
                      ? JvmCodegenUtil.getDirectMember(functionDescriptor).getExtensionReceiverParameter()
                      : null;

            if (annotated != null) {
                AnnotationCodegen.forParameter(i, mv, innerClassConsumer, state).genAnnotations(annotated, parameterSignature.getAsmType());
            }
        }
    }

    private static void markEnumOrInnerConstructorParameterAsSynthetic(MethodVisitor mv, int i, ClassBuilderMode mode) {
        // IDEA's ClsPsi builder fails to annotate synthetic parameters
        if (mode == ClassBuilderMode.LIGHT_CLASSES) return;

        // This is needed to avoid RuntimeInvisibleParameterAnnotations error in javac:
        // see MethodWriter.visitParameterAnnotation()

        AnnotationVisitor av = mv.visitParameterAnnotation(i, "Ljava/lang/Synthetic;", true);
        if (av != null) {
            av.visitEnd();
        }
    }

    @Nullable
    private static Type getThisTypeForFunction(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull KotlinTypeMapper typeMapper
    ) {
        ReceiverParameterDescriptor dispatchReceiver = functionDescriptor.getDispatchReceiverParameter();
        // all functions inside erased version of inline class are static, so they don't have `this` as is,
        // but functions inside wrapper class should use type of wrapper class, not the underlying type
        if (functionDescriptor instanceof ConstructorDescriptor) {
            return typeMapper.mapTypeAsDeclaration(functionDescriptor);
        }
        else if (dispatchReceiver != null) {
            return typeMapper.mapTypeAsDeclaration(dispatchReceiver.getType());
        }
        else if (isFunctionLiteral(functionDescriptor) ||
                 isLocalFunction(functionDescriptor) ||
                 isFunctionExpression(functionDescriptor)) {
            return typeMapper.mapClass(context.getThisDescriptor());
        }
        else {
            return null;
        }
    }

    public static void generateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull JvmMethodSignature signature,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull JvmDefaultMode jvmDefaultMode,
            boolean isReleaseCoroutines
    ) {
        mv.visitCode();

        Label methodBegin = new Label();
        mv.visitLabel(methodBegin);

        KotlinTypeMapper typeMapper = parentCodegen.typeMapper;
        if (BuiltinSpecialBridgesUtil.shouldHaveTypeSafeBarrier(functionDescriptor, typeMapper::mapAsmMethod)) {
            generateTypeCheckBarrierIfNeeded(
                    new InstructionAdapter(mv), functionDescriptor, signature.getReturnType(), null, typeMapper,
                    isReleaseCoroutines);
        }

        Label methodEnd;

        int functionFakeIndex = -1;
        int lambdaFakeIndex = -1;

        if (context.getParentContext() instanceof MultifileClassFacadeContext) {
            generateFacadeDelegateMethodBody(mv, signature.getAsmMethod(), (MultifileClassFacadeContext) context.getParentContext());
            methodEnd = new Label();
        }
        else if (isCompatibilityStubInDefaultImpls(functionDescriptor, context, jvmDefaultMode)) {
            FunctionDescriptor compatibility = ((DefaultImplsClassContext) context.getParentContext()).getInterfaceContext()
                    .getAccessorForJvmDefaultCompatibility(functionDescriptor);
            int flags = AsmUtil.getMethodAsmFlags(functionDescriptor, OwnerKind.DEFAULT_IMPLS, context.getState());
            assert (flags & Opcodes.ACC_ABSTRACT) == 0 : "Interface method with body should be non-abstract" + functionDescriptor;
            CallableMethod method = typeMapper.mapToCallableMethod(compatibility, false);

            generateDelegateToStaticMethodBody(
                    true, mv,
                    method.getAsmMethod(),
                    method.getOwner().getInternalName(),
                    true);
            methodEnd = new Label();
        }
        else {
            FrameMap frameMap = createFrameMap(
                    parentCodegen.state, signature, functionDescriptor.getExtensionReceiverParameter(),
                    functionDescriptor.getValueParameters(), isStaticMethod(context.getContextKind(), functionDescriptor)
            );
            if (context.isInlineMethodContext()) {
                functionFakeIndex = frameMap.enterTemp(Type.INT_TYPE);
            }

            if (context instanceof InlineLambdaContext) {
                lambdaFakeIndex = frameMap.enterTemp(Type.INT_TYPE);
            }

            Label methodEntry = new Label();
            mv.visitLabel(methodEntry);
            context.setMethodStartLabel(methodEntry);

            if (!strategy.skipNotNullAssertionsForParameters()) {
                genNotNullAssertionsForParameters(new InstructionAdapter(mv), parentCodegen.state, functionDescriptor, frameMap);
            }

            parentCodegen.beforeMethodBody(mv);

            methodEnd = new Label();
            context.setMethodEndLabel(methodEnd);
            strategy.generateBody(mv, frameMap, signature, context, parentCodegen);
        }

        mv.visitLabel(methodEnd);

        Type thisType = getThisTypeForFunction(functionDescriptor, context, typeMapper);

        if (functionDescriptor instanceof AnonymousFunctionDescriptor && functionDescriptor.isSuspend()) {
            functionDescriptor = CoroutineCodegenUtilKt.getOrCreateJvmSuspendFunctionView(
                    functionDescriptor,
                    parentCodegen.state
            );
        }

        List<ValueParameterDescriptor> destructuredParametersForSuspendLambda = new ArrayList<>();
        if (context.getParentContext() instanceof ClosureContext) {
            if (context instanceof InlineLambdaContext) {
                CallableMemberDescriptor lambdaDescriptor = context.getContextDescriptor();
                if (lambdaDescriptor instanceof FunctionDescriptor &&
                    ((FunctionDescriptor) lambdaDescriptor).isSuspend()) {
                    destructuredParametersForSuspendLambda.addAll(lambdaDescriptor.getValueParameters());
                }
            } else {
                FunctionDescriptor lambdaDescriptor = ((ClosureContext) context.getParentContext()).getOriginalSuspendLambdaDescriptor();
                if (lambdaDescriptor != null &&
                    CoroutineCodegenUtilKt.isResumeImplMethodName(
                            parentCodegen.state.getLanguageVersionSettings(), functionDescriptor.getName().asString()
                    )) {
                    destructuredParametersForSuspendLambda.addAll(lambdaDescriptor.getValueParameters());
                }
            }
        }

        generateLocalVariableTable(
                mv, signature, functionDescriptor, thisType, methodBegin, methodEnd, context.getContextKind(), typeMapper,
                destructuredParametersForSuspendLambda, (functionFakeIndex >= 0 ? 1 : 0) + (lambdaFakeIndex >= 0 ? 1 : 0)
        );

        //TODO: it's best to move all below logic to 'generateLocalVariableTable' method
        if (context.isInlineMethodContext() && functionFakeIndex != -1) {
            mv.visitLocalVariable(
                    JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_FUNCTION + typeMapper.mapAsmMethod(functionDescriptor).getName(),
                    Type.INT_TYPE.getDescriptor(), null,
                    methodBegin, methodEnd,
                    functionFakeIndex);
        }

        if (context instanceof InlineLambdaContext && thisType != null && lambdaFakeIndex != -1) {
            String name = thisType.getClassName();
            int indexOfLambdaOrdinal = name.lastIndexOf("$");
            if (indexOfLambdaOrdinal > 0) {
                int lambdaOrdinal = Integer.parseInt(name.substring(indexOfLambdaOrdinal + 1));

                KtPureElement functionArgument = parentCodegen.element;
                String functionName = "unknown";
                if (functionArgument instanceof KtFunction) {
                    ValueParameterDescriptor inlineArgumentDescriptor =
                            InlineUtil.getInlineArgumentDescriptor((KtFunction) functionArgument, parentCodegen.bindingContext);
                    if (inlineArgumentDescriptor != null) {
                        functionName = inlineArgumentDescriptor.getContainingDeclaration().getName().asString();
                    }
                }
                mv.visitLocalVariable(
                        JvmAbi.LOCAL_VARIABLE_NAME_PREFIX_INLINE_ARGUMENT + lambdaOrdinal +  "$" + functionName,
                        Type.INT_TYPE.getDescriptor(), null,
                        methodBegin, methodEnd,
                        lambdaFakeIndex);
            }
        }
    }

    private static boolean isCompatibilityStubInDefaultImpls(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodContext context,
            @NotNull JvmDefaultMode jvmDefaultMode
    ) {
        return OwnerKind.DEFAULT_IMPLS == context.getContextKind() &&
               hasJvmDefaultAnnotation(functionDescriptor) &&
               jvmDefaultMode.isCompatibility();
    }

    private static void generateLocalVariableTable(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @NotNull FunctionDescriptor functionDescriptor,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            @NotNull OwnerKind ownerKind,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull List<ValueParameterDescriptor> destructuredParametersForSuspendLambda,
            int shiftForDestructuringVariables
    ) {
        if (functionDescriptor.isSuspend() && !(functionDescriptor instanceof AnonymousFunctionDescriptor)) {
            FunctionDescriptor unwrapped = CoroutineCodegenUtilKt.unwrapInitialDescriptorForSuspendFunction(
                    functionDescriptor
            );

            if (unwrapped != functionDescriptor) {
                generateLocalVariableTable(
                        mv,
                        new JvmMethodSignature(
                               jvmMethodSignature.getAsmMethod(),
                               jvmMethodSignature.getValueParameters().subList(
                                       0,
                                       jvmMethodSignature.getValueParameters().size() - 1
                               )
                        ),
                        unwrapped,
                        thisType, methodBegin, methodEnd, ownerKind, typeMapper, destructuredParametersForSuspendLambda,
                        shiftForDestructuringVariables
                );
                return;
            }
        }

        generateLocalVariablesForParameters(mv,
                                            jvmMethodSignature,
                                            thisType, methodBegin, methodEnd, functionDescriptor.getValueParameters(),
                                            destructuredParametersForSuspendLambda,
                                            AsmUtil.isStaticMethod(ownerKind, functionDescriptor), typeMapper, shiftForDestructuringVariables
        );
    }

    public static void generateLocalVariablesForParameters(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            Collection<ValueParameterDescriptor> valueParameters,
            boolean isStatic,
            KotlinTypeMapper typeMapper
    ) {
        generateLocalVariablesForParameters(
                mv, jvmMethodSignature, thisType, methodBegin, methodEnd, valueParameters, Collections.emptyList(), isStatic, typeMapper,
                0);
    }

    private static void generateLocalVariablesForParameters(
            @NotNull MethodVisitor mv,
            @NotNull JvmMethodSignature jvmMethodSignature,
            @Nullable Type thisType,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            Collection<ValueParameterDescriptor> valueParameters,
            @NotNull List<ValueParameterDescriptor> destructuredParametersForSuspendLambda,
            boolean isStatic,
            KotlinTypeMapper typeMapper,
            int shiftForDestructuringVariables
    ) {
        Iterator<ValueParameterDescriptor> valueParameterIterator = valueParameters.iterator();
        List<JvmMethodParameterSignature> params = jvmMethodSignature.getValueParameters();
        int shift = 0;

        if (!isStatic) {
            //add this
            if (thisType != null) {
                mv.visitLocalVariable("this", thisType.getDescriptor(), null, methodBegin, methodEnd, shift);
            }
            else {
                //TODO: provide thisType for callable reference
            }
            shift++;
        }

        for (int i = 0; i < params.size(); i++) {
            JvmMethodParameterSignature param = params.get(i);
            JvmMethodParameterKind kind = param.getKind();
            String parameterName;

            if (kind == JvmMethodParameterKind.VALUE) {
                ValueParameterDescriptor parameter = valueParameterIterator.next();
                String nameForDestructuredParameter = ValueParameterDescriptorImpl.getNameForDestructuredParameterOrNull(parameter);

                parameterName =
                        nameForDestructuredParameter == null
                        ? computeParameterName(i, parameter)
                        : nameForDestructuredParameter;
            }
            else {
                String lowercaseKind = kind.name().toLowerCase();
                parameterName = needIndexForVar(kind)
                                ? "$" + lowercaseKind + "$" + i
                                : "$" + lowercaseKind;
            }

            Type type = param.getAsmType();
            mv.visitLocalVariable(parameterName, type.getDescriptor(), null, methodBegin, methodEnd, shift);
            shift += type.getSize();
        }

        shift += shiftForDestructuringVariables;
        shift = generateDestructuredParameterEntries(mv, methodBegin, methodEnd, valueParameters, typeMapper, shift);
        shift = generateDestructuredParametersForSuspendLambda(mv, methodBegin, methodEnd, typeMapper, shift, destructuredParametersForSuspendLambda);
        generateDestructuredParameterEntries(mv, methodBegin, methodEnd, destructuredParametersForSuspendLambda, typeMapper, shift);
    }

    private static int generateDestructuredParameterEntries(
            @NotNull MethodVisitor mv,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            Collection<ValueParameterDescriptor> valueParameters,
            KotlinTypeMapper typeMapper,
            int shift
    ) {
        for (ValueParameterDescriptor parameter : valueParameters) {
            List<VariableDescriptor> destructuringVariables = ValueParameterDescriptorImpl.getDestructuringVariablesOrNull(parameter);
            if (destructuringVariables == null) continue;

            for (VariableDescriptor entry : CodegenUtilKt.filterOutDescriptorsWithSpecialNames(destructuringVariables)) {
                Type type = typeMapper.mapType(entry.getType());
                mv.visitLocalVariable(entry.getName().asString(), type.getDescriptor(), null, methodBegin, methodEnd, shift);
                shift += type.getSize();
            }
        }
        return shift;
    }

    private static int generateDestructuredParametersForSuspendLambda(
            @NotNull MethodVisitor mv,
            @NotNull Label methodBegin,
            @NotNull Label methodEnd,
            KotlinTypeMapper typeMapper,
            int shift,
            List<ValueParameterDescriptor> destructuredParametersForSuspendLambda
    ) {
        for (ValueParameterDescriptor parameter : destructuredParametersForSuspendLambda) {
            String nameForDestructuredParameter = ValueParameterDescriptorImpl.getNameForDestructuredParameterOrNull(parameter);
            if (nameForDestructuredParameter == null) continue;

            Type type = typeMapper.mapType(parameter.getType());
            mv.visitLocalVariable(nameForDestructuredParameter, type.getDescriptor(), null, methodBegin, methodEnd, shift);
            shift += type.getSize();
        }
        return shift;
    }

    private static String computeParameterName(int i, ValueParameterDescriptor parameter) {
        PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(parameter);
        if (element instanceof KtParameter && UnderscoreUtilKt.isSingleUnderscore((KtParameter) element)) {
            return "$noName_" + i;
        }

        return parameter.getName().asString();
    }

    public static void generateFacadeDelegateMethodBody(
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull MultifileClassFacadeContext context
    ) {
        generateDelegateToStaticMethodBody(true, mv, asmMethod, context.getFilePartType().getInternalName(), false);
    }

    private static void generateDelegateToMethodBody(
            // -1 means to add additional this parameter on stack
            int firstParamIndex,
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull String classToDelegateTo,
            int opcode,
            boolean isInterface
    ) {
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = asmMethod.getArgumentTypes();

        // The first line of some package file is written to the line number attribute of a static delegate to allow to 'step into' it
        // This is similar to what javac does with bridge methods
        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        int paramIndex = firstParamIndex;
        if (paramIndex == -1) {
            iv.load(0, AsmTypes.OBJECT_TYPE);
            paramIndex = 1;
        }

        for (Type argType : argTypes) {
            iv.load(paramIndex, argType);
            paramIndex += argType.getSize();
        }
        iv.visitMethodInsn(opcode, classToDelegateTo, asmMethod.getName(), asmMethod.getDescriptor(), isInterface);
        iv.areturn(asmMethod.getReturnType());
    }

    private static void generateDelegateToStaticErasedVersion(
            @NotNull MethodVisitor mv,
            @NotNull Method erasedStaticAsmMethod,
            @NotNull Type fieldOwnerType,
            @NotNull String fieldName,
            @NotNull Type fieldType
    ) {
        String internalName = fieldOwnerType.getInternalName();
        InstructionAdapter iv = new InstructionAdapter(mv);
        Type[] argTypes = erasedStaticAsmMethod.getArgumentTypes();

        Label label = new Label();
        iv.visitLabel(label);
        iv.visitLineNumber(1, label);

        iv.load(0, AsmTypes.OBJECT_TYPE);
        iv.visitFieldInsn(Opcodes.GETFIELD, internalName, fieldName, fieldType.getDescriptor());

        int k = 1;
        for (int i = 1; i < argTypes.length; i++) {
            Type argType = argTypes[i];
            iv.load(k, argType);
            k += argType.getSize();
        }

        iv.invokestatic(internalName, erasedStaticAsmMethod.getName(), erasedStaticAsmMethod.getDescriptor(), false);
        iv.areturn(erasedStaticAsmMethod.getReturnType());
    }

    private static void generateDelegateToStaticMethodBody(
            boolean isStatic,
            @NotNull MethodVisitor mv,
            @NotNull Method asmMethod,
            @NotNull String classToDelegateTo,
            boolean isInterfaceMethodCall
    ) {
        generateDelegateToMethodBody(isStatic ? 0 : 1, mv, asmMethod, classToDelegateTo, Opcodes.INVOKESTATIC, isInterfaceMethodCall);
    }

    private static boolean needIndexForVar(JvmMethodParameterKind kind) {
        return kind == JvmMethodParameterKind.CAPTURED_LOCAL_VARIABLE ||
               kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL ||
               kind == JvmMethodParameterKind.SUPER_CALL_PARAM;
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description) {
        endVisit(mv, description, (PsiElement)null);
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @Nullable KtPureElement method) {
        endVisit(mv, description, (PsiElement)(method == null ? null : method.getPsiOrParent()));
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @NotNull KtElement method) {
        endVisit(mv, description, (PsiElement)method);
    }

    public static void endVisit(MethodVisitor mv, @Nullable String description, @Nullable PsiElement method) {
        try {
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable t) {
            String bytecode = renderByteCodeIfAvailable(mv);
            throw new CompilationException(
                    "wrong code generated\n" +
                    (description != null ? " for " + description : "") +
                    t.getClass().getName() +
                    " " +
                    t.getMessage() +
                    (bytecode != null ? "\nbytecode:\n" + bytecode : ""),
                    t, method);
        }
    }

    @SuppressWarnings("WeakerAccess") // Useful in debug
    @Nullable
    public static String renderByteCodeIfAvailable(@NotNull MethodVisitor mv) {
        String bytecode = null;

        if (mv instanceof TransformationMethodVisitor) {
            mv = ((TransformationMethodVisitor) mv).getTraceMethodVisitorIfPossible();
        }

        if (mv instanceof TraceMethodVisitor) {
            TraceMethodVisitor traceMethodVisitor = (TraceMethodVisitor) mv;
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            traceMethodVisitor.p.print(pw);
            pw.close();
            bytecode = sw.toString();
        }
        return bytecode;
    }

    private boolean hasSpecialBridgeMethod(@NotNull FunctionDescriptor descriptor) {
        if (SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(descriptor) == null) return false;
        return !BuiltinSpecialBridgesUtil.generateBridgesForBuiltinSpecial(
                descriptor, typeMapper::mapAsmMethod, DECLARATION_AND_DEFINITION_CHECKER
        ).isEmpty();
    }

    public void generateBridges(@NotNull FunctionDescriptor descriptor) {
        if (descriptor instanceof ConstructorDescriptor) return;
        if (owner.getContextKind() == OwnerKind.DEFAULT_IMPLS) return;
        if (!DECLARATION_AND_DEFINITION_CHECKER.invoke(descriptor)) return;

        // equals(Any?), hashCode(), toString() never need bridges
        if (isMethodOfAny(descriptor)) return;

        boolean isSpecial = SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(descriptor) != null;

        Set<Bridge<Method>> bridgesToGenerate;
        if (!isSpecial) {
            bridgesToGenerate =
                    ImplKt.generateBridgesForFunctionDescriptor(descriptor, typeMapper::mapAsmMethod, DECLARATION_AND_DEFINITION_CHECKER);
            if (!bridgesToGenerate.isEmpty()) {
                PsiElement origin = descriptor.getKind() == DECLARATION ? getSourceFromDescriptor(descriptor) : null;
                boolean isSpecialBridge =
                        BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor) != null;

                for (Bridge<Method> bridge : bridgesToGenerate) {
                    generateBridge(origin, descriptor, bridge.getFrom(), bridge.getTo(), isSpecialBridge, false);
                }
            }
        }
        else {
            Set<BridgeForBuiltinSpecial<Method>> specials = BuiltinSpecialBridgesUtil.generateBridgesForBuiltinSpecial(
                    descriptor, typeMapper::mapAsmMethod, DECLARATION_AND_DEFINITION_CHECKER
            );

            if (!specials.isEmpty()) {
                PsiElement origin = descriptor.getKind() == DECLARATION ? getSourceFromDescriptor(descriptor) : null;
                for (BridgeForBuiltinSpecial<Method> bridge : specials) {
                    generateBridge(
                            origin, descriptor, bridge.getFrom(), bridge.getTo(),
                            bridge.isSpecial(), bridge.isDelegateToSuper());
                }
            }

            if (!descriptor.getKind().isReal() && isAbstractMethod(descriptor, OwnerKind.IMPLEMENTATION)) {
                CallableDescriptor overridden = SpecialBuiltinMembers.getOverriddenBuiltinReflectingJvmDescriptor(descriptor);
                assert overridden != null;

                if (!isThereOverriddenInKotlinClass(descriptor)) {
                    Method method = typeMapper.mapAsmMethod(descriptor);
                    int flags = ACC_ABSTRACT | getVisibilityAccessFlag(descriptor);
                    v.newMethod(JvmDeclarationOriginKt.AugmentedBuiltInApi(overridden), flags, method.getName(), method.getDescriptor(), null, null);
                }
            }
        }
    }

    public static boolean isThereOverriddenInKotlinClass(@NotNull CallableMemberDescriptor descriptor) {
        return CollectionsKt.any(
                getAllOverriddenDescriptors(descriptor),
                overridden -> !(overridden.getContainingDeclaration() instanceof JavaClassDescriptor) &&
                              isClass(overridden.getContainingDeclaration())
        );
    }

    public static boolean isMethodOfAny(@NotNull FunctionDescriptor descriptor) {
        String name = descriptor.getName().asString();
        List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();
        if (parameters.isEmpty()) {
            return name.equals("hashCode") || name.equals("toString");
        }
        else if (parameters.size() == 1 && name.equals("equals")) {
            return isNullableAny(parameters.get(0).getType());
        }
        return false;
    }

    @NotNull
    public static String[] getThrownExceptions(@NotNull FunctionDescriptor function, @NotNull KotlinTypeMapper typeMapper) {
        AnnotationDescriptor annotation = function.getAnnotations().findAnnotation(new FqName("kotlin.throws"));
        if (annotation == null) {
            annotation = function.getAnnotations().findAnnotation(new FqName("kotlin.jvm.Throws"));
        }

        if (annotation == null) return ArrayUtil.EMPTY_STRING_ARRAY;

        Collection<ConstantValue<?>> values = annotation.getAllValueArguments().values();
        if (values.isEmpty()) return ArrayUtil.EMPTY_STRING_ARRAY;

        Object value = values.iterator().next();
        if (!(value instanceof ArrayValue)) return ArrayUtil.EMPTY_STRING_ARRAY;
        ArrayValue arrayValue = (ArrayValue) value;

        List<String> strings = CollectionsKt.mapNotNull(
                arrayValue.getValue(),
                (ConstantValue<?> constant) -> {
                    if (constant instanceof KClassValue) {
                        ClassDescriptor classDescriptor = DescriptorUtils.getClassDescriptorForType(
                                ((KClassValue) constant).getArgumentType(DescriptorUtilsKt.getModule(function))
                        );
                        return typeMapper.mapClass(classDescriptor).getInternalName();
                    }
                    return null;
                }
        );
        return ArrayUtil.toStringArray(strings);
    }

    void generateDefaultIfNeeded(
            @NotNull MethodContext owner,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull OwnerKind kind,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable KtNamedFunction function
    ) {
        DeclarationDescriptor contextClass = owner.getContextDescriptor().getContainingDeclaration();

        if (isInterface(contextClass) &&
            !processInterfaceMethod(functionDescriptor, kind, true, false, state.getJvmDefaultMode())) {
            return;
        }

        if (InlineClassesUtilsKt.isInlineClass(contextClass) && kind != OwnerKind.ERASED_INLINE_CLASS) {
            return;
        }

        if (!isDefaultNeeded(functionDescriptor, function)) {
            return;
        }

        // $default methods are never private to be accessible from other class files (e.g. inner) without the need of synthetic accessors
        // $default methods are never protected to be accessible from subclass nested classes
        int visibilityFlag = Visibilities.isPrivate(functionDescriptor.getVisibility()) ||
                             isEffectivelyInlineOnly(functionDescriptor) ?
                             AsmUtil.NO_FLAG_PACKAGE_PRIVATE : Opcodes.ACC_PUBLIC;
        int flags =  visibilityFlag | getDeprecatedAccessFlag(functionDescriptor) | ACC_SYNTHETIC;
        if (!(functionDescriptor instanceof ConstructorDescriptor &&
              !InlineClassesUtilsKt.isInlineClass(functionDescriptor.getContainingDeclaration()))
        ) {
            flags |= ACC_STATIC;
        }

        Method defaultMethod = typeMapper.mapDefaultMethod(functionDescriptor, kind);

        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.Synthetic(function, functionDescriptor),
                flags,
                defaultMethod.getName(),
                defaultMethod.getDescriptor(), null,
                getThrownExceptions(functionDescriptor, typeMapper)
        );

        // Only method annotations are copied to the $default method. Parameter annotations are not copied until there are valid use cases;
        // enum constructors have two additional synthetic parameters which somewhat complicate this task
        AnnotationCodegen.forMethod(mv, memberCodegen, state).genAnnotations(functionDescriptor, defaultMethod.getReturnType());

        if (!state.getClassBuilderMode().generateBodies) {
            if (this.owner instanceof MultifileClassFacadeContext)
                endVisit(mv, "default method delegation", getSourceFromDescriptor(functionDescriptor));
            else
                endVisit(mv, "default method", getSourceFromDescriptor(functionDescriptor));
            return;
        }

        if (this.owner instanceof MultifileClassFacadeContext) {
            mv.visitCode();
            generateFacadeDelegateMethodBody(mv, defaultMethod, (MultifileClassFacadeContext) this.owner);
            endVisit(mv, "default method delegation", getSourceFromDescriptor(functionDescriptor));
        }
        else if (isCompatibilityStubInDefaultImpls(functionDescriptor, owner, state.getJvmDefaultMode())) {
            mv.visitCode();
            Method interfaceDefaultMethod = typeMapper.mapDefaultMethod(functionDescriptor, OwnerKind.IMPLEMENTATION);
            generateDelegateToStaticMethodBody(
                    true, mv,
                    interfaceDefaultMethod,
                    typeMapper.mapOwner(functionDescriptor).getInternalName(),
                    true
            );
            endVisit(mv, "default method delegation to interface one", getSourceFromDescriptor(functionDescriptor));
        }
        else {
            mv.visitCode();
            generateDefaultImplBody(owner, functionDescriptor, mv, loadStrategy, function, memberCodegen, defaultMethod);
            endVisit(mv, "default method", getSourceFromDescriptor(functionDescriptor));
        }
    }

    public static void generateDefaultImplBody(
            @NotNull MethodContext methodContext,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull MethodVisitor mv,
            @NotNull DefaultParameterValueLoader loadStrategy,
            @Nullable KtNamedFunction function,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull Method defaultMethod
    ) {
        GenerationState state = parentCodegen.state;
        JvmMethodSignature signature = state.getTypeMapper().mapSignatureWithGeneric(functionDescriptor, methodContext.getContextKind());

        // 'null' because the "could not find expected declaration" error has been already reported in isDefaultNeeded earlier
        List<ValueParameterDescriptor> valueParameters =
                CodegenUtil.getFunctionParametersForDefaultValueGeneration(functionDescriptor, null);

        boolean isStatic = isStaticMethod(methodContext.getContextKind(), functionDescriptor);
        FrameMap frameMap = createFrameMap(state, signature, functionDescriptor.getExtensionReceiverParameter(), valueParameters, isStatic);

        ExpressionCodegen codegen = new ExpressionCodegen(mv, frameMap, signature.getReturnType(), methodContext, state, parentCodegen);

        CallGenerator generator = codegen.getOrCreateCallGeneratorForDefaultImplBody(functionDescriptor, function);

        InstructionAdapter iv = new InstructionAdapter(mv);
        genDefaultSuperCallCheckIfNeeded(iv, functionDescriptor, defaultMethod);

        List<JvmMethodParameterSignature> mappedParameters = signature.getValueParameters();
        int capturedArgumentsCount = 0;
        while (capturedArgumentsCount < mappedParameters.size() &&
               mappedParameters.get(capturedArgumentsCount).getKind() != JvmMethodParameterKind.VALUE) {
            capturedArgumentsCount++;
        }

        assert valueParameters.size() > 0 : "Expecting value parameters to generate default function " + functionDescriptor;
        int firstMaskIndex = frameMap.enterTemp(Type.INT_TYPE);
        for (int index = 1; index < valueParameters.size(); index++) {
            if (index % Integer.SIZE == 0) {
                frameMap.enterTemp(Type.INT_TYPE);
            }
        }
        //default handler or constructor marker
        frameMap.enterTemp(AsmTypes.OBJECT_TYPE);

        for (int index = 0; index < valueParameters.size(); index++) {
            int maskIndex = firstMaskIndex + index / Integer.SIZE;
            ValueParameterDescriptor parameterDescriptor = valueParameters.get(index);
            Type type = mappedParameters.get(capturedArgumentsCount + index).getAsmType();

            int parameterIndex = frameMap.getIndex(parameterDescriptor);
            if (parameterDescriptor.declaresDefaultValue()) {
                iv.load(maskIndex, Type.INT_TYPE);
                iv.iconst(1 << (index % Integer.SIZE));
                iv.and(Type.INT_TYPE);
                Label loadArg = new Label();
                iv.ifeq(loadArg);

                StackValue.local(parameterIndex, type, parameterDescriptor.getType())
                        .store(loadStrategy.genValue(parameterDescriptor, codegen), iv);

                iv.mark(loadArg);
            }
        }

        // load arguments after defaults generation to avoid redundant stack normalization operations
        loadExplicitArgumentsOnStack(OBJECT_TYPE, isStatic, signature, generator);

        for (int index = 0; index < valueParameters.size(); index++) {
            ValueParameterDescriptor parameterDescriptor = valueParameters.get(index);
            Type type = mappedParameters.get(capturedArgumentsCount + index).getAsmType();
            int parameterIndex = frameMap.getIndex(parameterDescriptor);
            generator.putValueIfNeeded(new JvmKotlinType(type, null), StackValue.local(parameterIndex, type));
        }

        CallableMethod method = state.getTypeMapper().mapToCallableMethod(functionDescriptor, false, methodContext.getContextKind());

        generator.genCall(method, null, false, codegen);

        iv.areturn(signature.getReturnType());
    }

    private static void genDefaultSuperCallCheckIfNeeded(
            @NotNull InstructionAdapter iv, @NotNull FunctionDescriptor descriptor, @NotNull Method defaultMethod
    ) {
        if (descriptor instanceof ConstructorDescriptor) return;

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (!(container instanceof ClassDescriptor)) return;
        if (((ClassDescriptor) container).getModality() == Modality.FINAL) return;

        Label end = new Label();
        int handleIndex = (Type.getArgumentsAndReturnSizes(defaultMethod.getDescriptor()) >> 2) - 2; /*-1 for this, and -1 for handle*/
        iv.load(handleIndex, OBJECT_TYPE);
        iv.ifnull(end);
        AsmUtil.genThrow(
                iv, "java/lang/UnsupportedOperationException",
                "Super calls with default arguments not supported in this target, function: " + descriptor.getName().asString()
        );
        iv.visitLabel(end);
    }

    @NotNull
    private static FrameMap createFrameMap(
            @NotNull GenerationState state,
            @NotNull JvmMethodSignature signature,
            @Nullable ReceiverParameterDescriptor extensionReceiverParameter,
            @NotNull List<ValueParameterDescriptor> valueParameters,
            boolean isStatic
    ) {
        FrameMap frameMap = new FrameMap();
        if (!isStatic) {
            frameMap.enterTemp(OBJECT_TYPE);
        }

        for (JvmMethodParameterSignature parameter : signature.getValueParameters()) {
            if (parameter.getKind() == JvmMethodParameterKind.RECEIVER) {
                if (extensionReceiverParameter != null) {
                    frameMap.enter(extensionReceiverParameter, state.getTypeMapper().mapType(extensionReceiverParameter));
                }
                else {
                    frameMap.enterTemp(parameter.getAsmType());
                }
            }
            else if (parameter.getKind() != JvmMethodParameterKind.VALUE) {
                frameMap.enterTemp(parameter.getAsmType());
            }
        }

        for (ValueParameterDescriptor parameter : valueParameters) {
            frameMap.enter(parameter, state.getTypeMapper().mapType(parameter));
        }

        return frameMap;
    }

    private static void loadExplicitArgumentsOnStack(
            @NotNull Type ownerType,
            boolean isStatic,
            @NotNull JvmMethodSignature signature,
            @NotNull CallGenerator callGenerator
    ) {
        int var = 0;
        if (!isStatic) {
            callGenerator.putValueIfNeeded(new JvmKotlinType(ownerType, null), StackValue.local(var, ownerType));
            var += ownerType.getSize();
        }

        for (JvmMethodParameterSignature parameterSignature : signature.getValueParameters()) {
            if (parameterSignature.getKind() != JvmMethodParameterKind.VALUE) {
                Type type = parameterSignature.getAsmType();
                callGenerator.putValueIfNeeded(new JvmKotlinType(type, null), StackValue.local(var, type));
                var += type.getSize();
            }
        }
    }

    private boolean isDefaultNeeded(@NotNull FunctionDescriptor descriptor, @Nullable KtNamedFunction function) {
        List<ValueParameterDescriptor> parameters =
                CodegenUtil.getFunctionParametersForDefaultValueGeneration(descriptor, state.getDiagnostics());
        return CollectionsKt.any(parameters, ValueParameterDescriptor::declaresDefaultValue);
    }

    private void generateBridge(
            @Nullable PsiElement origin,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Method bridge,
            @NotNull Method delegateTo,
            boolean isSpecialBridge,
            boolean isStubDeclarationWithDelegationToSuper
    ) {
        boolean isSpecialOrDelegationToSuper = isSpecialBridge || isStubDeclarationWithDelegationToSuper;
        int flags = ACC_PUBLIC | ACC_BRIDGE | (!isSpecialOrDelegationToSuper ? ACC_SYNTHETIC : 0) | (isSpecialBridge ? ACC_FINAL : 0); // TODO.

        String bridgeSignature =
                isSpecialBridge ? typeMapper.mapSignatureWithGeneric(descriptor, OwnerKind.IMPLEMENTATION).getGenericsSignature()
                                : null;

        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.Bridge(descriptor, origin), flags,
                                       bridge.getName(), bridge.getDescriptor(), bridgeSignature, null);
        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();
        InstructionAdapter iv = new InstructionAdapter(mv);

        Type[] argTypes = bridge.getArgumentTypes();
        Type[] originalArgTypes = delegateTo.getArgumentTypes();

        List<ParameterDescriptor> allKotlinParameters = new ArrayList<>(originalArgTypes.length);
        if (descriptor.getExtensionReceiverParameter() != null) allKotlinParameters.add(descriptor.getExtensionReceiverParameter());
        allKotlinParameters.addAll(descriptor.getValueParameters());

        boolean safeToUseKotlinTypes = allKotlinParameters.size() == originalArgTypes.length;

        boolean isVarargInvoke = JvmCodegenUtil.isOverrideOfBigArityFunctionInvoke(descriptor);
        if (isVarargInvoke) {
            assert argTypes.length == 1 && argTypes[0].equals(AsmUtil.getArrayType(OBJECT_TYPE)) :
                    "Vararg invoke must have one parameter of type [Ljava/lang/Object;: " + bridge;
            AsmUtil.generateVarargInvokeArityAssert(iv, originalArgTypes.length);
        }
        else {
            assert argTypes.length == originalArgTypes.length :
                    "Number of parameters of the bridge and delegate must be the same.\n" +
                    "Descriptor: " + descriptor + "\nBridge: " + bridge + "\nDelegate: " + delegateTo;
        }

        MemberCodegen.markLineNumberForDescriptor(owner.getThisDescriptor(), iv);

        if (delegateTo.getArgumentTypes().length > 0 && isSpecialBridge) {
            generateTypeCheckBarrierIfNeeded(iv, descriptor, bridge.getReturnType(), delegateTo.getArgumentTypes(), typeMapper,
                                             state.getLanguageVersionSettings().supportsFeature(LanguageFeature.ReleaseCoroutines));
        }

        iv.load(0, OBJECT_TYPE);
        for (int i = 0, reg = 1; i < originalArgTypes.length; i++) {
            KotlinType kotlinType = safeToUseKotlinTypes ? allKotlinParameters.get(i).getType() : null;
            StackValue value;
            if (isVarargInvoke) {
                value = StackValue.arrayElement(OBJECT_TYPE, null, StackValue.local(1, argTypes[0]), StackValue.constant(i));
            }
            else {
                value = StackValue.local(reg, argTypes[i], kotlinType);
                //noinspection AssignmentToForLoopParameter
                reg += argTypes[i].getSize();
            }
            value.put(originalArgTypes[i], kotlinType, iv);
        }

        if (isStubDeclarationWithDelegationToSuper) {
            ClassDescriptor parentClass = getSuperClassDescriptor((ClassDescriptor) descriptor.getContainingDeclaration());
            assert parentClass != null;
            String parentInternalName = typeMapper.mapClass(parentClass).getInternalName();
            iv.invokespecial(parentInternalName, delegateTo.getName(), delegateTo.getDescriptor(), false);
        }
        else {
            if (hasJvmDefaultAnnotation(descriptor)) {
                iv.invokeinterface(v.getThisName(), delegateTo.getName(), delegateTo.getDescriptor());
            }
            else {
                iv.invokevirtual(v.getThisName(), delegateTo.getName(), delegateTo.getDescriptor(), false);
            }
        }

        KotlinType returnType = descriptor.getReturnType();
        StackValue.coerce(delegateTo.getReturnType(), returnType, bridge.getReturnType(), returnType, iv);
        iv.areturn(bridge.getReturnType());

        endVisit(mv, "bridge method", origin);
    }

    private static void generateTypeCheckBarrierIfNeeded(
            @NotNull InstructionAdapter iv,
            @NotNull FunctionDescriptor descriptor,
            @NotNull Type returnType,
            @Nullable Type[] delegateParameterTypes,
            @NotNull KotlinTypeMapper typeMapper,
            boolean isReleaseCoroutines
    ) {
        BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription typeSafeBarrierDescription =
                BuiltinMethodsWithSpecialGenericSignature.getDefaultValueForOverriddenBuiltinFunction(descriptor);
        if (typeSafeBarrierDescription == null) return;

        FunctionDescriptor overriddenBuiltin =
                BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(descriptor);

        assert overriddenBuiltin != null : "Overridden built-in method should not be null for " + descriptor;

        Label defaultBranch = new Label();

        for (int i = 0; i < descriptor.getValueParameters().size(); i++) {
            if (!typeSafeBarrierDescription.checkParameter(i)) continue;
            boolean isCheckForAny = delegateParameterTypes == null || OBJECT_TYPE.equals(delegateParameterTypes[i]);

            KotlinType kotlinType = descriptor.getValueParameters().get(i).getType();

            if (isCheckForAny && TypeUtils.isNullableType(kotlinType)) continue;

            iv.load(1 + i, OBJECT_TYPE);

            if (isCheckForAny) {
                assert !TypeUtils.isNullableType(kotlinType) : "Only bridges for not-nullable types are necessary";
                iv.ifnull(defaultBranch);
            }
            else {
                Type targetBoxedType;
                if (InlineClassesUtilsKt.isInlineClassType(kotlinType)) {
                    targetBoxedType = typeMapper.mapTypeAsDeclaration(kotlinType);
                } else {
                    targetBoxedType = boxType(delegateParameterTypes[i]);
                }
                CodegenUtilKt.generateIsCheck(iv, kotlinType, targetBoxedType, isReleaseCoroutines);
                iv.ifeq(defaultBranch);
            }
        }

        Label afterDefaultBranch = new Label();

        iv.goTo(afterDefaultBranch);

        iv.visitLabel(defaultBranch);

        if (typeSafeBarrierDescription.equals(BuiltinMethodsWithSpecialGenericSignature.TypeSafeBarrierDescription.MAP_GET_OR_DEFAULT)) {
            iv.load(2, returnType);
        }
        else {
            StackValue.constant(typeSafeBarrierDescription.getDefaultValue(), returnType).put(returnType, iv);
        }
        iv.areturn(returnType);

        iv.visitLabel(afterDefaultBranch);
    }

    public void genSamDelegate(@NotNull FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        FunctionDescriptor delegatedTo = overriddenDescriptor.getOriginal();
        JvmDeclarationOrigin declarationOrigin = JvmDeclarationOriginKt.SamDelegation(functionDescriptor);
        // Skip writing generic signature for the SAM wrapper class method because it may reference type parameters from the SAM interface
        // which would make little sense outside of that interface and may break Java reflection.
        // E.g. functionDescriptor for a SAM wrapper for java.util.function.Predicate is the method `test` with signature "(T) -> Boolean"
        genDelegate(
                functionDescriptor, delegatedTo, declarationOrigin,
                (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(),
                field, true
        );
    }

    public void genDelegate(@NotNull FunctionDescriptor functionDescriptor, FunctionDescriptor overriddenDescriptor, StackValue field) {
        genDelegate(functionDescriptor, overriddenDescriptor.getOriginal(),
                    (ClassDescriptor) overriddenDescriptor.getContainingDeclaration(), field);
    }

    public void genDelegate(
            @NotNull FunctionDescriptor delegateFunction,
            FunctionDescriptor delegatedTo,
            ClassDescriptor toClass,
            StackValue field
    ) {
        JvmDeclarationOrigin declarationOrigin =
                JvmDeclarationOriginKt.Delegation(DescriptorToSourceUtils.descriptorToDeclaration(delegatedTo), delegateFunction);
        genDelegate(delegateFunction, delegatedTo, declarationOrigin, toClass, field, false);
    }

    private void genDelegate(
            @NotNull FunctionDescriptor delegateFunction,
            FunctionDescriptor delegatedTo,
            @NotNull JvmDeclarationOrigin declarationOrigin,
            ClassDescriptor toClass,
            StackValue field,
            boolean skipGenericSignature
    ) {
        generateMethod(
                declarationOrigin, delegateFunction,
                new FunctionGenerationStrategy() {
                    @Override
                    public void generateBody(
                            @NotNull MethodVisitor mv,
                            @NotNull FrameMap frameMap,
                            @NotNull JvmMethodSignature signature,
                            @NotNull MethodContext context,
                            @NotNull MemberCodegen<?> parentCodegen
                    ) {
                        Method delegateToMethod = typeMapper.mapToCallableMethod(delegatedTo, /* superCall = */ false).getAsmMethod();
                        Method delegateMethod = typeMapper.mapAsmMethod(delegateFunction);

                        Type[] argTypes = delegateMethod.getArgumentTypes();
                        Type[] originalArgTypes = delegateToMethod.getArgumentTypes();

                        InstructionAdapter iv = new InstructionAdapter(mv);
                        iv.load(0, OBJECT_TYPE);
                        field.put(field.type, iv);
                        for (int i = 0, reg = 1; i < argTypes.length; i++) {
                            StackValue.local(reg, argTypes[i]).put(originalArgTypes[i], iv);
                            //noinspection AssignmentToForLoopParameter
                            reg += argTypes[i].getSize();
                        }

                        String internalName = typeMapper.mapClass(toClass).getInternalName();
                        if (toClass.getKind() == ClassKind.INTERFACE) {
                            iv.invokeinterface(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor());
                        }
                        else if (toClass.isInline()) {
                            iv.invokestatic(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor(), false);
                        }
                        else {
                            iv.invokevirtual(internalName, delegateToMethod.getName(), delegateToMethod.getDescriptor(), false);
                        }

                        StackValue stackValue = AsmUtil.genNotNullAssertions(
                                state,
                                StackValue.onStack(delegateToMethod.getReturnType(), delegatedTo.getReturnType()),
                                RuntimeAssertionInfo.create(
                                        delegateFunction.getReturnType(),
                                        delegatedTo.getReturnType(),
                                        new RuntimeAssertionInfo.DataFlowExtras.OnlyMessage(delegatedTo.getName() + "(...)")
                                )
                        );

                        stackValue.put(delegateMethod.getReturnType(), delegatedTo.getReturnType(), iv);

                        iv.areturn(delegateMethod.getReturnType());
                    }

                    @Override
                    public boolean skipNotNullAssertionsForParameters() {
                        return false;
                    }

                    @Override
                    public boolean skipGenericSignature() {
                        return skipGenericSignature;
                    }
                }
        );
    }

    public static boolean processInterfaceMethod(
            @NotNull CallableMemberDescriptor memberDescriptor,
            @NotNull OwnerKind kind,
            boolean isDefault,
            boolean isSynthetic,
            JvmDefaultMode mode
    ) {
        DeclarationDescriptor containingDeclaration = memberDescriptor.getContainingDeclaration();
        assert isInterface(containingDeclaration) : "'processInterfaceMethod' method should be called only for interfaces, but: " +
                                                    containingDeclaration;

        if (hasJvmDefaultAnnotation(memberDescriptor)) {
            return (kind != OwnerKind.DEFAULT_IMPLS && !isSynthetic) ||
                   (kind == OwnerKind.DEFAULT_IMPLS && (isSynthetic || mode.isCompatibility()));
        } else {
            switch (kind) {
                case DEFAULT_IMPLS: return true;
                case IMPLEMENTATION: return !Visibilities.isPrivate(memberDescriptor.getVisibility()) && !isDefault && !isSynthetic;
                default: return false;
            }
        }
    }
}
