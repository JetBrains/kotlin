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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.Lists;
import com.intellij.util.ArrayUtil;
import kotlin.Pair;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure;
import org.jetbrains.kotlin.codegen.context.ClosureContext;
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor;
import org.jetbrains.kotlin.codegen.coroutines.CoroutineCodegenUtilKt;
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil;
import org.jetbrains.kotlin.codegen.serialization.JvmSerializerExtension;
import org.jetbrains.kotlin.codegen.signature.BothSignatureWriter;
import org.jetbrains.kotlin.codegen.signature.JvmSignatureWriter;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.serialization.DescriptorSerializer;
import org.jetbrains.kotlin.serialization.ProtoBuf;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.util.OperatorNameConventions;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
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
    private final FunctionDescriptor functionReferenceTarget;
    private final FunctionGenerationStrategy strategy;
    protected final CalculatedClosure closure;
    protected final Type asmType;
    protected final int visibilityFlag;
    private final boolean shouldHaveBoundReferenceReceiver;

    private Method constructor;
    protected Type superClassAsmType;

    public ClosureCodegen(
            @NotNull GenerationState state,
            @NotNull KtElement element,
            @Nullable SamType samType,
            @NotNull ClosureContext context,
            @Nullable FunctionDescriptor functionReferenceTarget,
            @NotNull FunctionGenerationStrategy strategy,
            @NotNull MemberCodegen<?> parentCodegen,
            @NotNull ClassBuilder classBuilder
    ) {
        super(state, parentCodegen, context, element, classBuilder);

        this.funDescriptor = context.getFunctionDescriptor();
        this.classDescriptor = context.getContextDescriptor();
        this.samType = samType;
        this.functionReferenceTarget = functionReferenceTarget;
        this.strategy = strategy;

        if (samType == null) {
            this.superInterfaceTypes = new ArrayList<KotlinType>();

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

        this.asmType = typeMapper.mapClass(classDescriptor);

        visibilityFlag = AsmUtil.getVisibilityAccessFlagForClass(classDescriptor);
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
            superInterfaceAsmTypes[i] = typeMapper.mapSupertype(superInterfaceType, sw).getInternalName();
            sw.writeInterfaceEnd();
        }

        v.defineClass(element,
                      state.getClassFileVersion(),
                      ACC_FINAL | ACC_SUPER | visibilityFlag,
                      asmType.getInternalName(),
                      sw.makeJavaGenericSignature(),
                      superClassAsmType.getInternalName(),
                      superInterfaceAsmTypes
        );

        InlineCodegenUtil.initDefaultSourceMappingIfNeeded(context, this, state);

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

        this.constructor = generateConstructor();

        if (isConst(closure)) {
            generateConstInstance(asmType, asmType);
        }

        genClosureFields(closure, v, typeMapper);
    }

    protected void generateClosureBody() {
        functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), funDescriptor, strategy);

        if (functionReferenceTarget != null) {
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
            erasedInterfaceFunction = samType.getAbstractMethod().getOriginal();
        }

        generateBridge(
                typeMapper.mapAsmMethod(erasedInterfaceFunction),
                typeMapper.mapAsmMethod(funDescriptor)
        );

        //TODO: rewrite cause ugly hack
        if (samType != null) {
            SimpleFunctionDescriptorImpl descriptorForBridges = SimpleFunctionDescriptorImpl
                    .create(funDescriptor.getContainingDeclaration(), funDescriptor.getAnnotations(),
                            erasedInterfaceFunction.getName(),
                            CallableMemberDescriptor.Kind.DECLARATION, funDescriptor.getSource());

            descriptorForBridges
                    .initialize(null, erasedInterfaceFunction.getDispatchReceiverParameter(), erasedInterfaceFunction.getTypeParameters(),
                                erasedInterfaceFunction.getValueParameters(), erasedInterfaceFunction.getReturnType(),
                                Modality.OPEN, erasedInterfaceFunction.getVisibility());

            DescriptorUtilsKt.setSingleOverridden(descriptorForBridges, erasedInterfaceFunction);
            functionCodegen.generateBridges(descriptorForBridges);
        }
    }

    @Override
    protected void generateKotlinMetadataAnnotation() {
        FunctionDescriptor frontendFunDescriptor = CodegenUtilKt.unwrapFrontendVersion(funDescriptor);
        FunctionDescriptor freeLambdaDescriptor = createFreeLambdaDescriptor(frontendFunDescriptor);
        Method method = v.getSerializationBindings().get(METHOD_FOR_FUNCTION, frontendFunDescriptor);
        assert method != null : "No method for " + frontendFunDescriptor;
        v.getSerializationBindings().put(METHOD_FOR_FUNCTION, freeLambdaDescriptor, method);

        final DescriptorSerializer serializer =
                DescriptorSerializer.createForLambda(new JvmSerializerExtension(v.getSerializationBindings(), state));

        final ProtoBuf.Function functionProto = serializer.functionProto(freeLambdaDescriptor).build();

        WriteAnnotationUtilKt.writeKotlinMetadata(v, state, KotlinClassHeader.Kind.SYNTHETIC_CLASS, 0, new Function1<AnnotationVisitor, Unit>() {
            @Override
            public Unit invoke(AnnotationVisitor av) {
                writeAnnotationData(av, serializer, functionProto);
                return Unit.INSTANCE;
            }
        });
    }

    /**
     * Given a function descriptor, creates another function descriptor with type parameters copied from outer context(s).
     * This is needed because once we're serializing this to a proto, there's no place to store information about external type parameters.
     */
    @NotNull
    private static FunctionDescriptor createFreeLambdaDescriptor(@NotNull FunctionDescriptor descriptor) {
        FunctionDescriptor.CopyBuilder<? extends FunctionDescriptor> builder = descriptor.newCopyBuilder();
        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(0);
        builder.setTypeParameters(typeParameters);

        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        while (container != null) {
            if (container instanceof ClassDescriptor) {
                typeParameters.addAll(((ClassDescriptor) container).getDeclaredTypeParameters());
            }
            else if (container instanceof CallableDescriptor && !(container instanceof ConstructorDescriptor)) {
                typeParameters.addAll(((CallableDescriptor) container).getTypeParameters());
            }
            container = container.getContainingDeclaration();
        }

        return typeParameters.isEmpty() ? descriptor : builder.build();
    }

    @Override
    protected void done() {
        writeOuterClassAndEnclosingMethod();
        super.done();
    }

    @NotNull
    public StackValue putInstanceOnStack(@NotNull final ExpressionCodegen codegen, @Nullable final StackValue functionReferenceReceiver) {
        return StackValue.operation(
                functionReferenceTarget != null ? K_FUNCTION : asmType,
                new Function1<InstructionAdapter, Unit>() {
                    @Override
                    public Unit invoke(InstructionAdapter v) {
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
                }
        );
    }

    protected void generateBridge(@NotNull Method bridge, @NotNull Method delegate) {
        if (bridge.equals(delegate)) return;

        MethodVisitor mv =
                v.newMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), ACC_PUBLIC | ACC_BRIDGE | ACC_SYNTHETIC,
                            bridge.getName(), bridge.getDescriptor(), null, ArrayUtil.EMPTY_STRING_ARRAY);

        if (!state.getClassBuilderMode().generateBodies) return;

        mv.visitCode();

        InstructionAdapter iv = new InstructionAdapter(mv);
        MemberCodegen.markLineNumberForDescriptor(DescriptorUtils.getParentOfType(funDescriptor, ClassDescriptor.class), iv);

        iv.load(0, asmType);

        Type[] myParameterTypes = bridge.getArgumentTypes();

        List<ParameterDescriptor> calleeParameters = CollectionsKt.plus(
                org.jetbrains.kotlin.utils.CollectionsKt.<ParameterDescriptor>singletonOrEmptyList(funDescriptor.getExtensionReceiverParameter()),
                funDescriptor.getValueParameters()
        );

        int slot = 1;
        for (int i = 0; i < calleeParameters.size(); i++) {
            Type type = myParameterTypes[i];
            StackValue.local(slot, type).put(typeMapper.mapType(calleeParameters.get(i)), iv);
            slot += type.getSize();
        }

        iv.invokevirtual(asmType.getInternalName(), delegate.getName(), delegate.getDescriptor(), false);
        StackValue.onStack(delegate.getReturnType()).put(bridge.getReturnType(), iv);

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
                PropertyReferenceCodegen.generateCallableReferenceSignature(iv, descriptor, state);
                iv.areturn(JAVA_STRING_TYPE);
                FunctionCodegen.endVisit(iv, "function reference getSignature", element);
            }
        }
    }

    public static void generateCallableReferenceDeclarationContainer(
            @NotNull InstructionAdapter iv,
            @NotNull CallableDescriptor descriptor,
            @NotNull GenerationState state
    ) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container instanceof ClassDescriptor) {
            // TODO: getDefaultType() here is wrong and won't work for arrays
            putJavaLangClassInstance(iv, state.getTypeMapper().mapType(((ClassDescriptor) container).getDefaultType()));
            wrapJavaClassIntoKClass(iv);
        }
        else if (container instanceof PackageFragmentDescriptor) {
            iv.aconst(state.getTypeMapper().mapOwner(descriptor));
            iv.aconst(state.getModuleName());
            iv.invokestatic(REFLECTION, "getOrCreateKotlinPackage",
                            Type.getMethodDescriptor(K_DECLARATION_CONTAINER_TYPE, getType(Class.class), getType(String.class)), false);
        }
        else {
            iv.aconst(null);
        }
    }

    @NotNull
    protected Method generateConstructor() {
        List<FieldInfo> args = calculateConstructorParameters(typeMapper, closure, asmType);

        Type[] argTypes = fieldListToTypeArray(args);

        Method constructor = new Method("<init>", Type.VOID_TYPE, argTypes);
        MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(element, funDescriptor), visibilityFlag, "<init>", constructor.getDescriptor(), null,
                                       ArrayUtil.EMPTY_STRING_ARRAY);
        if (state.getClassBuilderMode().generateBodies) {
            mv.visitCode();
            InstructionAdapter iv = new InstructionAdapter(mv);

            Pair<Integer, Type> receiverIndexAndType =
                    CallableReferenceUtilKt.generateClosureFieldsInitializationFromParameters(iv, closure, args);
            if (shouldHaveBoundReferenceReceiver && receiverIndexAndType == null) {
                throw new AssertionError("No bound reference receiver in constructor parameters: " + args);
            }
            int boundReferenceReceiverParameterIndex = shouldHaveBoundReferenceReceiver ? receiverIndexAndType.getFirst() : -1;
            Type boundReferenceReceiverType = shouldHaveBoundReferenceReceiver ? receiverIndexAndType.getSecond() : null;

            iv.load(0, superClassAsmType);

            String superClassConstructorDescriptor;
            if (superClassAsmType.equals(LAMBDA) || superClassAsmType.equals(FUNCTION_REFERENCE) ||
                superClassAsmType.equals(CoroutineCodegenUtilKt.COROUTINE_IMPL_ASM_TYPE)) {
                int arity = calculateArity();
                iv.iconst(arity);
                if (shouldHaveBoundReferenceReceiver) {
                    CallableReferenceUtilKt.loadBoundReferenceReceiverParameter(iv, boundReferenceReceiverParameterIndex, boundReferenceReceiverType);
                    superClassConstructorDescriptor = "(ILjava/lang/Object;)V";
                }
                else {
                    superClassConstructorDescriptor = "(I)V";
                }
            }
            else {
                assert !shouldHaveBoundReferenceReceiver : "Unexpected bound reference with supertype " + superClassAsmType;
                superClassConstructorDescriptor = "()V";
            }
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", superClassConstructorDescriptor, false);

            iv.visitInsn(RETURN);

            FunctionCodegen.endVisit(iv, "constructor", element);
        }
        return constructor;
    }

    protected int calculateArity() {
        int arity = funDescriptor.getValueParameters().size();
        if (funDescriptor.getExtensionReceiverParameter() != null) arity++;
        if (funDescriptor.getDispatchReceiverParameter() != null) arity++;
        return arity;
    }

    @NotNull
    public static List<FieldInfo> calculateConstructorParameters(
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull CalculatedClosure closure,
            @NotNull Type ownerType
    ) {
        List<FieldInfo> args = Lists.newArrayList();
        ClassDescriptor captureThis = closure.getCaptureThis();
        if (captureThis != null) {
            Type type = typeMapper.mapType(captureThis);
            args.add(FieldInfo.createForHiddenField(ownerType, type, CAPTURED_THIS_FIELD));
        }
        KotlinType captureReceiverType = closure.getCaptureReceiverType();
        if (captureReceiverType != null) {
            args.add(FieldInfo.createForHiddenField(ownerType, typeMapper.mapType(captureReceiverType), CAPTURED_RECEIVER_FIELD));
        }

        for (EnclosedValueDescriptor enclosedValueDescriptor : closure.getCaptureVariables().values()) {
            DeclarationDescriptor descriptor = enclosedValueDescriptor.getDescriptor();
            if ((descriptor instanceof VariableDescriptor && !(descriptor instanceof PropertyDescriptor)) ||
                ExpressionTypingUtils.isLocalFunction(descriptor)) {
                args.add(
                        FieldInfo.createForHiddenField(
                                ownerType, enclosedValueDescriptor.getType(), enclosedValueDescriptor.getFieldName()
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
}
