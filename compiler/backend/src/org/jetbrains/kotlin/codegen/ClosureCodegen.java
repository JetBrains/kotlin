/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.SamType;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.ClosureContext;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.metadata.ProtoBuf;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.SimpleType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.CAPTURED_THIS_FIELD;
import static org.jetbrains.kotlin.codegen.CallableReferenceUtilKt.*;
import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConst;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.CLOSURE;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.METHOD_FOR_FUNCTION;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.*;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin.NO_ORIGIN;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class ClosureCodegen extends MemberCodegen<KtElement> {
    protected final FunctionDescriptor funDescriptor;
    private final ClassDescriptor classDescriptor;
    private final SamType samType;
    private final KotlinType superClassType;
    private final List<KotlinType> superInterfaceTypes;
    private final ResolvedCall<FunctionDescriptor> functionReferenceCall;
    private final FunctionDescriptor functionReferenceTarget;
    private final FunctionGenerationStrategy strategy;
    protected final CalculatedClosure closure;
    protected final Type asmType;
    protected final int visibilityFlag;
    private final boolean shouldHaveBoundReferenceReceiver;
    private final boolean isLegacyFunctionReference;
    private final boolean isOptimizedFunctionReference;
    private final boolean isAdaptedFunctionReference;

    private Method constructor;
    protected Type superClassAsmType;

    public ClosureCodegen(
            @NotNull GenerationState state,
            @NotNull KtElement element,
            @Nullable SamType samType,
            @NotNull ClosureContext context,
            @Nullable ResolvedCall<FunctionDescriptor> functionReferenceCall,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull ClassBuilder classBuilder
    ) {
        super(state, parentCodegen, context, element, classBuilder);

        this.funDescriptor = context.getFunctionDescriptor();
        this.classDescriptor = context.getContextDescriptor();
        this.samType = samType;
        this.functionReferenceCall = functionReferenceCall;
        this.functionReferenceTarget = functionReferenceCall != null ? functionReferenceCall.getResultingDescriptor() : null;
        this.strategy = strategy;

        if (samType == null) {
            this.superInterfaceTypes = new ArrayList<>();

            KotlinType superClassType = null;
            for (KotlinType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                ClassifierDescriptor classifier = supertype.getConstructor().getDeclarationDescriptor();
                if (DescriptorUtils.isInterface(classifier)) {
                    superInterfaceTypes.add(supertype);
                }
                else {
                    assert superClassType == null : "Closure class can't have more than one superclass: " + funDescriptor;
                    superClassType = supertype;
                }
            }
            assert superClassType != null : "Closure class should have a superclass: " + funDescriptor;

            this.superClassType = superClassType;
        }
        else {
            this.superInterfaceTypes = Collections.singletonList(samType.getType());
            this.superClassType = DescriptorUtilsKt.getBuiltIns(funDescriptor).getAnyType();
        }

        this.closure = bindingContext.get(CLOSURE, classDescriptor);
        assert closure != null : "Closure must be calculated for class: " + classDescriptor;

        this.shouldHaveBoundReferenceReceiver = CallableReferenceUtilKt.isForBoundCallableReference(closure);

        ClassifierDescriptor superClassDescriptor = superClassType.getConstructor().getDeclarationDescriptor();
        this.isLegacyFunctionReference =
                functionReferenceTarget != null &&
                superClassDescriptor == state.getJvmRuntimeTypes().getFunctionReference();
        this.isOptimizedFunctionReference =
                functionReferenceTarget != null &&
                superClassDescriptor == state.getJvmRuntimeTypes().getFunctionReferenceImpl();
        this.isAdaptedFunctionReference =
                functionReferenceTarget != null &&
                superClassDescriptor == state.getJvmRuntimeTypes().getAdaptedFunctionReference();

        this.asmType = typeMapper.mapClass(classDescriptor);

        visibilityFlag = DescriptorAsmUtil.getVisibilityAccessFlagForClass(classDescriptor);
    }

    @Override
    protected void generateDeclaration() {
        JvmSignatureWriter sw = new BothSignatureWriter(BothSignatureWriter.Mode.CLASS);
        if (samType != null) {
            typeMapper.writeFormalTypeParameters(samType.getType().getConstructor().getParameters(), sw);
        }
        sw.writeSuperclass();
        superClassAsmType = typeMapper.mapSupertype(superClassType, sw);
        sw.writeSuperclassEnd();
        String[] superInterfaceAsmTypes = new String[superInterfaceTypes.size()];
        for (int i = 0; i < superInterfaceTypes.size(); i++) {
            KotlinType superInterfaceType = superInterfaceTypes.get(i);
            sw.writeInterface();
            Type superInterfaceAsmType;
            if (samType != null && superInterfaceType.getConstructor() == samType.getType().getConstructor()) {
                superInterfaceAsmType = typeMapper.mapSupertype(superInterfaceType, null);
                sw.writeAsmType(superInterfaceAsmType);
            } else {
                superInterfaceAsmType = typeMapper.mapSupertype(superInterfaceType, sw);
            }
            superInterfaceAsmTypes[i] = superInterfaceAsmType.getInternalName();
            sw.writeInterfaceEnd();
        }

        v.defineClass(
                element,
                state.getClassFileVersion(),
                ACC_FINAL | ACC_SUPER | visibilityFlag | getSyntheticAccessFlagForLambdaClass(classDescriptor),
                asmType.getInternalName(),
                sw.makeJavaGenericSignature(),
                superClassAsmType.getInternalName(),
                superInterfaceAsmTypes
        );

        initDefaultSourceMappingIfNeeded();

        v.visitSource(element.getContainingFile().getName(), null);
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return JvmCodegenUtil.isArgumentWhichWillBeInlined(bindingContext, funDescriptor) ? null : classDescriptor;
    }

    @Override
    protected void generateBody() {
        generateBridges();
        generateClosureBody();

        if (samType != null) {
            SamWrapperCodegen.generateDelegatesToDefaultImpl(
                    asmType, classDescriptor, samType.getClassDescriptor(), functionCodegen, state
            );
        }

        this.constructor = generateConstructor();

        if (isConst(closure)) {
            generateConstInstance(asmType, asmType);
        }

        genClosureFields(closure, v, typeMapper, state.getLanguageVersionSettings());
    }

    protected void generateClosureBody() {
        functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), funDescriptor, strategy);

        if (isLegacyFunctionReference) {
            generateFunctionReferenceMethods(functionReferenceTarget);
        }

        functionCodegen.generateDefaultIfNeeded(
                context.intoFunction(funDescriptor), funDescriptor, context.getContextKind(), DefaultParameterValueLoader.DEFAULT, null
        );
    }

    protected void generateBridges() {
        FunctionDescriptor erasedInterfaceFunction;
        if (samType == null) {
            erasedInterfaceFunction = getErasedInvokeFunction(funDescriptor);
        }
        else {
            erasedInterfaceFunction = samType.getOriginalAbstractMethod();
        }

        List<KotlinType> bridgeParameterKotlinTypes = CollectionsKt.map(erasedInterfaceFunction.getValueParameters(), ValueDescriptor::getType);

        generateBridge(
                typeMapper.mapAsmMethod(erasedInterfaceFunction),
                bridgeParameterKotlinTypes,
                erasedInterfaceFunction.getReturnType(),
                typeMapper.mapAsmMethod(funDescriptor),
                funDescriptor.getReturnType(),
                JvmCodegenUtil.isDeclarationOfBigArityFunctionInvoke(erasedInterfaceFunction)
        );

        //TODO: rewrite cause ugly hack
        if (samType != null) {
            generateBridgesForSAM(erasedInterfaceFunction, funDescriptor, functionCodegen);
        }
    }

    static void generateBridgesForSAM(
            FunctionDescriptor erasedInterfaceFunction,
            FunctionDescriptor descriptor,
            FunctionCodegen codegen
    ) {
        SimpleFunctionDescriptorImpl descriptorForBridges = SimpleFunctionDescriptorImpl
                .create(descriptor.getContainingDeclaration(), descriptor.getAnnotations(),
                        erasedInterfaceFunction.getName(),
                        CallableMemberDescriptor.Kind.DECLARATION, descriptor.getSource());

        descriptorForBridges
                .initialize(erasedInterfaceFunction.getExtensionReceiverParameter(), erasedInterfaceFunction.getDispatchReceiverParameter(),
                            erasedInterfaceFunction.getContextReceiverParameters(),
                            erasedInterfaceFunction.getTypeParameters(), erasedInterfaceFunction.getValueParameters(),
                            erasedInterfaceFunction.getReturnType(), Modality.OPEN, erasedInterfaceFunction.getVisibility());

        descriptorForBridges.setSuspend(descriptor.isSuspend());

        DescriptorUtilsKt.setSingleOverridden(descriptorForBridges, erasedInterfaceFunction);
        codegen.generateBridges(descriptorForBridges);
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        FunctionDescriptor frontendFunDescriptor = CodegenUtilKt.unwrapFrontendVersion(funDescriptor);
        Method method = v.getSerializationBindings().get(METHOD_FOR_FUNCTION, frontendFunDescriptor);
        assert method != null : "No method for " + frontendFunDescriptor;

        FunctionDescriptor freeLambdaDescriptor = FakeDescriptorsForReferencesKt.createFreeFakeLambdaDescriptor(
                frontendFunDescriptor, state.getTypeApproximator()
        );
        v.getSerializationBindings().put(METHOD_FOR_FUNCTION, freeLambdaDescriptor, method);

        DescriptorSerializer serializer =
                DescriptorSerializer.createForLambda(new JvmSerializerExtension(v.getSerializationBindings(), state));

        ProtoBuf.Function.Builder builder = serializer.functionProto(freeLambdaDescriptor);
        if (builder == null) return;
        ProtoBuf.Function functionProto = builder.build();

        boolean publicAbi = InlineUtil.isInPublicInlineScope(frontendFunDescriptor);

        WriteAnnotationUtilKt.writeKotlinMetadata(v, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, publicAbi, 0, av -> {
            writeAnnotationData(av, serializer, functionProto);
            return Unit.INSTANCE;
        });
    }

    @Override
    protected void done() {
        writeOuterClassAndEnclosingMethod();
        super.done();
    }

    @NotNull
    public StackValue putInstanceOnStack(@NotNull ExpressionCodegen codegen, @Nullable StackValue functionReferenceReceiver) {
        return StackValue.operation(
                functionReferenceTarget != null ? K_FUNCTION : asmType,
                v -> {
                    if (isConst(closure)) {
                        v.getstatic(asmType.getInternalName(), JvmAbi.INSTANCE_FIELD, asmType.getDescriptor());
                    }
                    else {
                        v.anew(asmType);
                        v.dup();

                        codegen.pushClosureOnStack(classDescriptor, true, codegen.defaultCallGenerator, functionReferenceReceiver);
                        v.invokespecial(asmType.getInternalName(), "<init>", constructor.getDescriptor(), false);
                    }

                    return Unit.INSTANCE;
                }
        );
    }

    private void generateBridge(
            @NotNull Method bridge,
            @NotNull List<KotlinType> bridgeParameterKotlinTypes,
            @Nullable KotlinType bridgeReturnType,
            @NotNull Method delegate,
            @Nullable KotlinType delegateReturnType,
            boolean isVarargInvoke
    ) {
        if (bridge.equals(delegate)) return;

        MethodVisitor mv =
                v.newMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                            bridge.getName(), bridge.getDescriptor(), null, ArrayUtil.EMPTY_STRING_ARRAY);

        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);
        MemberCodegen.markLineNumberForDescriptor(DescriptorUtils.getParentOfType(funDescriptor, ClassDescriptor.class), iv);

        Type[] bridgeParameterTypes = bridge.getArgumentTypes();
        if (isVarargInvoke) {
            assert bridgeParameterTypes.length == 1 && bridgeParameterTypes[0].equals(AsmUtil.getArrayType(OBJECT_TYPE)) :
                    "Vararg invoke must have one parameter of type [Ljava/lang/Object;: " + bridge;
            generateVarargInvokeArityAssert(iv, delegate.getArgumentTypes().length);
        }
        else {
            assert bridgeParameterTypes.length == bridgeParameterKotlinTypes.size() :
                    "Asm parameter types should be the same length as Kotlin parameter types";
        }

        iv.load(0, asmType);

        List<ParameterDescriptor> calleeParameters = CollectionsKt.plus(
                CollectionsKt.listOfNotNull(funDescriptor.getExtensionReceiverParameter()),
                funDescriptor.getValueParameters()
        );

        int slot = 1;
        for (int i = 0; i < calleeParameters.size(); i++) {
            ParameterDescriptor calleeParameter = calleeParameters.get(i);
            KotlinType parameterType = calleeParameter.getType();
            StackValue value;
            if (isVarargInvoke) {
                value = StackValue.arrayElement(
                        OBJECT_TYPE, null,
                        StackValue.local(1, bridgeParameterTypes[0], bridgeParameterKotlinTypes.get(0)),
                        StackValue.constant(i)
                );
            }
            else {
                Type type = bridgeParameterTypes[i];
                value = StackValue.local(slot, type, bridgeParameterKotlinTypes.get(i));
                slot += type.getSize();
            }
            if (InlineClassesCodegenUtilKt.isInlineClassWithUnderlyingTypeAnyOrAnyN(parameterType) && functionReferenceCall == null) {
                ClassDescriptor descriptor = TypeUtils.getClassDescriptor(parameterType);
                InlineClassRepresentation<SimpleType> representation =
                        descriptor != null ? descriptor.getInlineClassRepresentation() : null;
                assert representation != null : "Not an inline class type: " + parameterType;
                parameterType = representation.getUnderlyingType();
            }
            value.put(typeMapper.mapType(calleeParameter), parameterType, iv);
        }

        iv.invokevirtual(asmType.getInternalName(), delegate.getName(), delegate.getDescriptor(), false);

        StackValue
                .onStack(delegate.getReturnType(), delegateReturnType)
                .put(bridge.getReturnType(), bridgeReturnType, iv);

        iv.areturn(bridge.getReturnType());

        FunctionCodegen.endVisit(mv, "bridge", element);
    }

    // TODO: ImplementationBodyCodegen.markLineNumberForSyntheticFunction?
    private void generateFunctionReferenceMethods(@NotNull FunctionDescriptor descriptor) {
        int flags = ACC_PUBLIC | ACC_FINAL;
        boolean generateBody = state.getClassBuilderMode().generateBodies;

        {
            MethodVisitor mv =
                    v.newMethod(NO_ORIGIN, flags, "getOwner", Type.getMethodDescriptor(K_DECLARATION_CONTAINER_TYPE), null, null);
            if (generateBody) {
                mv.visitCode();
                InstructionAdapter iv = new InstructionAdapter(mv);
                generateCallableReferenceDeclarationContainer(iv, descriptor, state);
                iv.areturn(K_DECLARATION_CONTAINER_TYPE);
                FunctionCodegen.endVisit(iv, "function reference getOwner", element);
            }
        }

        {
            MethodVisitor mv =
                    v.newMethod(NO_ORIGIN, flags, "getName", Type.getMethodDescriptor(JAVA_STRING_TYPE), null, null);
            if (generateBody) {
                mv.visitCode();
                InstructionAdapter iv = new InstructionAdapter(mv);
                iv.aconst(descriptor.getName().asString());
                iv.areturn(JAVA_STRING_TYPE);
                FunctionCodegen.endVisit(iv, "function reference getName", element);
            }
        }

        {
            MethodVisitor mv = v.newMethod(NO_ORIGIN, flags, "getSignature", Type.getMethodDescriptor(JAVA_STRING_TYPE), null, null);
            if (generateBody) {
                mv.visitCode();
                InstructionAdapter iv = new InstructionAdapter(mv);
                CallableReferenceUtilKt.generateFunctionReferenceSignature(iv, descriptor, state);
                iv.areturn(JAVA_STRING_TYPE);
                FunctionCodegen.endVisit(iv, "function reference getSignature", element);
            }
        }
    }

    @NotNull
    protected Method generateConstructor() {
        List<FieldInfo> args = calculateConstructorParameters(typeMapper, state.getLanguageVersionSettings(), closure, asmType);

        Type[] argTypes = fieldListToTypeArray(args);

        Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), visibilityFlag, "<init>", constructor.getDescriptor(), null,
                                       ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode().generateBodies) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            Pair<Integer, FieldInfo> receiverIndexAndFieldInfo =
                    CallableReferenceUtilKt.generateClosureFieldsInitializationFromParameters(iv, closure, args);
            if (shouldHaveBoundReferenceReceiver && receiverIndexAndFieldInfo == null) {
                throw new AssertionError("No bound reference receiver in constructor parameters: " + args);
            }

            int boundReceiverParameterIndex;
            Type boundReceiverType;
            KotlinType boundReceiverKotlinType;
            if (shouldHaveBoundReferenceReceiver) {
                boundReceiverParameterIndex = receiverIndexAndFieldInfo.getFirst();
                boundReceiverType = receiverIndexAndFieldInfo.getSecond().getFieldType();
                boundReceiverKotlinType = receiverIndexAndFieldInfo.getSecond().getFieldKotlinType();
            }
            else {
                boundReceiverParameterIndex = -1;
                boundReceiverType = null;
                boundReceiverKotlinType = null;
            }

            iv.load(0, superClassAsmType);

            List<Type> superCtorArgTypes = new ArrayList<>();
            if (superClassAsmType.equals(LAMBDA) || functionReferenceTarget != null ||
                CoroutineCodegenUtilKt.isCoroutineSuperClass(superClassAsmType.getInternalName())
            ) {
                iv.iconst(CodegenUtilKt.getArity(funDescriptor));
                superCtorArgTypes.add(Type.INT_TYPE);
                if (shouldHaveBoundReferenceReceiver) {
                    CallableReferenceUtilKt.loadBoundReferenceReceiverParameter(
                            iv, boundReceiverParameterIndex, boundReceiverType, boundReceiverKotlinType
                    );
                    superCtorArgTypes.add(OBJECT_TYPE);
                }
                if (isOptimizedFunctionReference || isAdaptedFunctionReference) {
                    assert functionReferenceTarget != null : "No function reference target: " + funDescriptor;
                    generateCallableReferenceDeclarationContainerClass(iv, functionReferenceTarget, state);
                    iv.aconst(functionReferenceTarget.getName().asString());
                    CallableReferenceUtilKt.generateFunctionReferenceSignature(iv, functionReferenceTarget, state);
                    int flags =
                            getCallableReferenceTopLevelFlag(functionReferenceTarget) +
                            (calculateFunctionReferenceFlags(functionReferenceCall, funDescriptor) << 1);
                    iv.aconst(flags);
                    superCtorArgTypes.add(JAVA_CLASS_TYPE);
                    superCtorArgTypes.add(JAVA_STRING_TYPE);
                    superCtorArgTypes.add(JAVA_STRING_TYPE);
                    superCtorArgTypes.add(Type.INT_TYPE);
                }
            }
            else {
                assert !shouldHaveBoundReferenceReceiver : "Unexpected bound reference with supertype " + superClassAsmType;
            }
            iv.invokespecial(
                    superClassAsmType.getInternalName(), "<init>",
                    Type.getMethodDescriptor(Type.VOID_TYPE, superCtorArgTypes.toArray(new Type[0])), false
            );

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", element);
        }
        return constructor;
    }

    private static int calculateFunctionReferenceFlags(
            @NotNull ResolvedCall<?> call,
            @NotNull FunctionDescriptor anonymousAdapterFunction
    ) {
        boolean hasVarargMappedToElement = false;
        FunctionDescriptor target = (FunctionDescriptor) call.getResultingDescriptor();
        int shift =
                (call.getDispatchReceiver() instanceof TransientReceiver ? 1 : 0) +
                (call.getExtensionReceiver() instanceof TransientReceiver ? 1 : 0);
        for (int i = shift;
             i < anonymousAdapterFunction.getValueParameters().size() && i - shift < target.getValueParameters().size();
             i++) {
            ValueParameterDescriptor targetParameter = target.getValueParameters().get(i - shift);
            ValueParameterDescriptor adaptedParameter = anonymousAdapterFunction.getValueParameters().get(i);

            // Vararg to element conversion is happening if the target parameter is vararg (e.g. `vararg xs: Int`),
            // but the adapted parameter's type is not equal to the target parameter's type (which is `IntArray`).
            if (targetParameter.getVarargElementType() != null &&
                !targetParameter.getType().equals(adaptedParameter.getType())) {
                hasVarargMappedToElement = true;
                break;
            }
        }

        //noinspection ConstantConditions
        boolean hasCoercionToUnit = KotlinBuiltIns.isUnit(anonymousAdapterFunction.getReturnType()) &&
                                    !KotlinBuiltIns.isUnit(target.getReturnType());

        boolean hasSuspendConversion = !target.isSuspend() && anonymousAdapterFunction.isSuspend();

        return (hasVarargMappedToElement ? 1 : 0) +
               (hasSuspendConversion ? 2 : 0) +
               ((hasCoercionToUnit ? 1 : 0) << 2);
    }

    @NotNull
    public static List<FieldInfo> calculateConstructorParameters(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull CalculatedClosure closure,
            @NotNull Type ownerType
    ) {
        List<FieldInfo> args = Lists.newArrayList();
        ClassDescriptor captureThis = closure.getCapturedOuterClassDescriptor();
        if (captureThis != null) {
            SimpleType thisType = captureThis.getDefaultType();
            Type type = typeMapper.mapType(thisType);
            args.add(FieldInfo.createForHiddenField(ownerType, type, thisType, CAPTURED_THIS_FIELD));
        }
        KotlinType captureReceiverType = closure.getCapturedReceiverFromOuterContext();
        if (captureReceiverType != null) {
            String fieldName = closure.getCapturedReceiverFieldName(typeMapper.getBindingContext(), languageVersionSettings);
            args.add(FieldInfo.createForHiddenField(ownerType, typeMapper.mapType(captureReceiverType), captureReceiverType, fieldName));
        }

        for (EnclosedValueDescriptor enclosedValueDescriptor : closure.getCaptureVariables().values()) {
            DeclarationDescriptor descriptor = enclosedValueDescriptor.getDescriptor();
            if ((descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) ||
                ExpressionTypingUtils.isLocalFunction(descriptor)) {
                args.add(
                        FieldInfo.createForHiddenField(
                                ownerType,
                                enclosedValueDescriptor.getType(),
                                enclosedValueDescriptor.getKotlinType(),
                                enclosedValueDescriptor.getFieldName()
                        )
                );
            }
            else if (descriptor instanceof FunctionDescriptor) {
                assert captureReceiverType != null;
            }
        }
        return args;
    }

    private static Type[] fieldListToTypeArray(List<FieldInfo> args) {
        Type[] argTypes = new Type[args.size()];
        for (int i = 0; i != argTypes.length; ++i) {
            argTypes[i] = args.get(i).getFieldType();
        }
        return argTypes;
    }

    @NotNull
    public static FunctionDescriptor getErasedInvokeFunction(@NotNull FunctionDescriptor function) {
        ClassDescriptor functionClass = DescriptorUtilsKt.getBuiltIns(function).getFunction(
                function.getValueParameters().size() + (function.getExtensionReceiverParameter() != null ? 1 : 0)
        );
        MemberScope scope = functionClass.getDefaultType().getMemberScope();
        return scope.getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND).iterator().next();
    }

    public boolean isCallableReference() {
        return functionReferenceTarget != null;
    }
}
