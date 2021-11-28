/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.binding.MutableClosure;
import org.jetbrains.kotlin.codegen.context.ConstructorContext;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.context.MethodContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.codegen.MemberCodegen.markLineNumberForDescriptor;
import static org.jetbrains.kotlin.resolve.BindingContextUtils.getDelegationConstructorCall;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.*;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.org.objectweb.asm.Opcodes.RETURN;

public class ConstructorCodegen {
    private final ClassDescriptor descriptor;
    private final FieldOwnerContext<?> context;
    private final FunctionCodegen functionCodegen;
    private final MemberCodegen<?> memberCodegen;
    private final ClassBodyCodegen classBodyCodegen;
    private final GenerationState state;
    private final OwnerKind kind;
    private final ClassBuilder v;
    private final Type classAsmType;
    private final KtPureClassOrObject myClass;
    private final BindingContext bindingContext;
    private final KotlinTypeMapper typeMapper;

    public ConstructorCodegen(
            @NotNull ClassDescriptor descriptor,
            @NotNull FieldOwnerContext<?> context,
            @NotNull FunctionCodegen codegen,
            @NotNull MemberCodegen<?> memberCodegen,
            @NotNull ClassBodyCodegen classBodyCodegen,
            @NotNull GenerationState state,
            @NotNull OwnerKind kind,
            @NotNull ClassBuilder v,
            @NotNull Type type,
            @NotNull KtPureClassOrObject myClass,
            @NotNull BindingContext bindingContext
    ) {
        this.descriptor = descriptor;
        this.context = context;
        functionCodegen = codegen;
        this.memberCodegen = memberCodegen;
        this.classBodyCodegen = classBodyCodegen;
        this.state = state;
        this.kind = kind;
        this.v = v;
        this.classAsmType = type;
        this.myClass = myClass;
        this.bindingContext = bindingContext;
        this.typeMapper = state.getTypeMapper();
    }

    public void generatePrimaryConstructor(DelegationFieldsInfo delegationFieldsInfo, Type superClassAsmType) {
        if (isJvmInterface(descriptor)) return;

        ClassConstructorDescriptor constructorDescriptor = descriptor.getUnsubstitutedPrimaryConstructor();
        if (constructorDescriptor == null) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor, typeMapper);

        KtPrimaryConstructor primaryConstructor = myClass.getPrimaryConstructor();
        JvmDeclarationOrigin origin = JvmDeclarationOriginKt
                .OtherOrigin(primaryConstructor != null ? primaryConstructor : myClass.getPsiOrParent(), constructorDescriptor);
        functionCodegen.generateMethod(origin, constructorDescriptor, constructorContext,
                                       new FunctionGenerationStrategy.CodegenBased(state) {
                                           @Override
                                           public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                                               generatePrimaryConstructorImpl(
                                                       constructorDescriptor,
                                                       codegen,
                                                       delegationFieldsInfo,
                                                       primaryConstructor,
                                                       superClassAsmType
                                               );
                                           }
                                       }
        );

        OwnerKind ownerKindForDefault = context.getContextKind() == OwnerKind.ERASED_INLINE_CLASS
                                        ? OwnerKind.ERASED_INLINE_CLASS
                                        : OwnerKind.IMPLEMENTATION;
        functionCodegen.generateDefaultIfNeeded(constructorContext, constructorDescriptor, ownerKindForDefault, DefaultParameterValueLoader.DEFAULT, null);

        registerAccessorForHiddenConstructorIfNeeded(constructorDescriptor);

        new DefaultParameterValueSubstitutor(state).generatePrimaryConstructorOverloadsIfNeeded(constructorDescriptor, v, memberCodegen, kind, myClass);
    }

    private void registerAccessorForHiddenConstructorIfNeeded(ClassConstructorDescriptor descriptor) {
        if (DescriptorAsmUtil.isHiddenConstructor(descriptor)) {
            context.getAccessor(descriptor, AccessorKind.NORMAL, null, null);
        }
    }

    public void generateSecondaryConstructor(
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull Type superClassAsmType
    ) {
        if (!canHaveDeclaredConstructors(descriptor)) return;

        KtSecondaryConstructor constructor = (KtSecondaryConstructor) descriptorToDeclaration(constructorDescriptor);
        // Synthetic constructors don't have corresponding declarations
        if (constructor == null) return;

        ConstructorContext constructorContext = context.intoConstructor(constructorDescriptor, typeMapper);

        functionCodegen.generateMethod(
                JvmDeclarationOriginKt.OtherOrigin(constructorDescriptor),
                constructorDescriptor, constructorContext,
                new FunctionGenerationStrategy.CodegenBased(state) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        generateSecondaryConstructorImpl(constructorDescriptor, codegen, superClassAsmType);
                    }
                }
        );

        OwnerKind ownerKindForDefault = context.getContextKind() == OwnerKind.ERASED_INLINE_CLASS
                                        ? OwnerKind.ERASED_INLINE_CLASS
                                        : OwnerKind.IMPLEMENTATION;
        functionCodegen.generateDefaultIfNeeded(constructorContext, constructorDescriptor, ownerKindForDefault,
                                                DefaultParameterValueLoader.DEFAULT, null);

        new DefaultParameterValueSubstitutor(state).generateOverloadsIfNeeded(
                constructor, constructorDescriptor, constructorDescriptor, kind, v, memberCodegen
        );

        registerAccessorForHiddenConstructorIfNeeded(constructorDescriptor);
    }

    private void generateDelegatorToConstructorCall(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @Nullable ResolvedCall<ConstructorDescriptor> delegationConstructorCall,
            @NotNull Type superClassAsmType
    ) {
        MethodContext codegenContext = codegen.context;
        assert codegenContext instanceof ConstructorContext :
                "Constructor context expected: " + codegenContext;
        assert !((ConstructorContext) codegenContext).isThisInitialized() :
                "Delegating constructor call is already generated for " + ((ConstructorContext) codegenContext).getConstructorDescriptor();

        if (delegationConstructorCall == null) {
            genSimpleSuperCall(iv, superClassAsmType);
        }
        else {
            generateDelegationConstructorCall(iv, codegen, constructorDescriptor, delegationConstructorCall);
        }

        ((ConstructorContext) codegenContext).setThisInitialized(true);
    }

    private void generatePrimaryConstructorImpl(
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull DelegationFieldsInfo fieldsInfo,
            @Nullable KtPrimaryConstructor primaryConstructor,
            @NotNull Type superClassAsmType
    ) {
        InstructionAdapter iv = codegen.v;

        markLineNumberForConstructor(constructorDescriptor, primaryConstructor, codegen);

        if (OwnerKind.ERASED_INLINE_CLASS == kind) {
            memberCodegen.generateInitializers(() -> codegen);
            Type t = typeMapper.mapType(constructorDescriptor.getContainingDeclaration());
            iv.load(0, t);
            iv.areturn(t);
            return;
        }

        generateClosureInitialization(iv);

        generateDelegatorToConstructorCall(
                iv, codegen, constructorDescriptor, getDelegationConstructorCall(bindingContext, constructorDescriptor), superClassAsmType
        );

        for (KtSuperTypeListEntry specifier : myClass.getSuperTypeListEntries()) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                genCallToDelegatorByExpressionSpecifier(iv, codegen, (KtDelegatedSuperTypeEntry) specifier, fieldsInfo);
            }
        }

        int curParam = 0;
        List<ValueParameterDescriptor> parameters = constructorDescriptor.getValueParameters();
        for (KtParameter parameter : classBodyCodegen.getPrimaryConstructorParameters()) {
            if (parameter.hasValOrVar()) {
                VariableDescriptor descriptor = parameters.get(curParam);
                Type type = typeMapper.mapType(descriptor);
                iv.load(0, classAsmType);
                iv.load(codegen.myFrameMap.getIndex(descriptor), type);
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
                assert propertyDescriptor != null : "Property descriptor is not found for primary constructor parameter: " + parameter;
                iv.putfield(classAsmType.getInternalName(), context.getFieldName(propertyDescriptor, false), type.getDescriptor());
            }
            curParam++;
        }

        //object initialization was moved to initializeObjects()
        if (!isObject(descriptor) && !InlineClassesUtilsKt.isInlineClass(descriptor)) {
            memberCodegen.generateInitializers(() -> codegen);
        }
        iv.visitInsn(RETURN);
    }

    private void generateSecondaryConstructorImpl(
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull Type superClassAsmType
    ) {
        InstructionAdapter iv = codegen.v;

        KtSecondaryConstructor constructor =
                (KtSecondaryConstructor) DescriptorToSourceUtils.descriptorToDeclaration(constructorDescriptor);

        markLineNumberForConstructor(constructorDescriptor, constructor, codegen);

        ResolvedCall<ConstructorDescriptor> constructorDelegationCall =
                getDelegationConstructorCall(bindingContext, constructorDescriptor);
        ConstructorDescriptor delegateConstructor = constructorDelegationCall == null ? null :
                                                    constructorDelegationCall.getResultingDescriptor();

        generateDelegatorToConstructorCall(iv, codegen, constructorDescriptor, constructorDelegationCall, superClassAsmType);
        if (!isSameClassConstructor(delegateConstructor)) {
            // Initialization happens only for constructors delegating to super
            generateClosureInitialization(iv);
            memberCodegen.generateInitializers(() -> codegen);
        }

        assert constructor != null;
        if (constructor.hasBody()) {
            codegen.gen(constructor.getBodyExpression(), Type.VOID_TYPE);
        }

        if (OwnerKind.ERASED_INLINE_CLASS == kind) {
            iv.areturn(typeMapper.mapType(constructorDescriptor.getContainingDeclaration()));
        }
        else {
            iv.visitInsn(RETURN);
        }
    }

    private void genCallToDelegatorByExpressionSpecifier(
            InstructionAdapter iv,
            ExpressionCodegen codegen,
            KtDelegatedSuperTypeEntry specifier,
            DelegationFieldsInfo fieldsInfo
    ) {
        KtExpression expression = specifier.getDelegateExpression();

        DelegationFieldsInfo.Field fieldInfo = fieldsInfo.getInfo(specifier);
        if (fieldInfo == null) return;

        if (fieldInfo.generateField) {
            iv.load(0, classAsmType);
            fieldInfo.getStackValue().store(codegen.gen(expression), iv);
        }
    }

    private boolean isSameClassConstructor(@Nullable ConstructorDescriptor delegatingConstructor) {
        return delegatingConstructor != null && delegatingConstructor.getContainingDeclaration() == descriptor;
    }

    private void generateClosureInitialization(@NotNull InstructionAdapter iv) {
        MutableClosure closure = context.closure;
        if (closure != null) {
            List<FieldInfo> argsFromClosure = ClosureCodegen.calculateConstructorParameters(
                    typeMapper, state.getLanguageVersionSettings(), closure, classAsmType);

            int k = 1;
            for (FieldInfo info : argsFromClosure) {
                k = DescriptorAsmUtil.genAssignInstanceFieldFromParam(info, k, iv);
            }
        }
    }

    private void generateDelegationConstructorCall(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @NotNull ResolvedCall<ConstructorDescriptor> delegationConstructorCall
    ) {
        if (OwnerKind.ERASED_INLINE_CLASS != kind) {
            iv.load(0, OBJECT_TYPE);
        }

        ConstructorDescriptor delegateConstructor = SamCodegenUtil.resolveSamAdapter(codegen.getConstructorDescriptor(delegationConstructorCall));

        KotlinTypeMapper typeMapper = state.getTypeMapper();

        CallableMethod delegateConstructorCallable = typeMapper.mapToCallableMethod(delegateConstructor, false, kind);
        CallableMethod callable = typeMapper.mapToCallableMethod(constructorDescriptor, false, kind);

        List<JvmMethodParameterSignature> delegatingParameters = delegateConstructorCallable.getValueParameters();
        List<JvmMethodParameterSignature> parameters = callable.getValueParameters();

        ArgumentGenerator argumentGenerator;
        if (isSameClassConstructor(delegateConstructor)) {
            // if it's the same class constructor we should just pass all synthetic parameters
            argumentGenerator =
                    generateThisCallImplicitArguments(iv, codegen, delegateConstructor, delegateConstructorCallable, delegatingParameters,
                                                      parameters);
        }
        else {
            argumentGenerator =
                    generateSuperCallImplicitArguments(iv, codegen, constructorDescriptor, delegateConstructor, delegationConstructorCall,
                                                       delegateConstructorCallable,
                                                       delegatingParameters,
                                                       parameters);
        }

        codegen.invokeMethodWithArguments(
                delegateConstructorCallable, delegationConstructorCall, StackValue.none(), codegen.defaultCallGenerator, argumentGenerator);
    }

    private void genSimpleSuperCall(InstructionAdapter iv, Type superClassAsmType) {
        iv.load(0, superClassAsmType);
        if (descriptor.getKind() == ClassKind.ENUM_CLASS || descriptor.getKind() == ClassKind.ENUM_ENTRY) {
            iv.load(1, JAVA_STRING_TYPE);
            iv.load(2, Type.INT_TYPE);
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "(Ljava/lang/String;I)V", false);
        }
        else {
            iv.invokespecial(superClassAsmType.getInternalName(), "<init>", "()V", false);
        }
    }

    @NotNull
    private static ArgumentGenerator generateThisCallImplicitArguments(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ConstructorDescriptor delegatingConstructor,
            @NotNull CallableMethod delegatingCallable,
            @NotNull List<JvmMethodParameterSignature> delegatingParameters,
            @NotNull List<JvmMethodParameterSignature> parameters
    ) {
        int offset = 1;
        int index = 0;
        for (; index < delegatingParameters.size(); index++) {
            JvmMethodParameterKind delegatingKind = delegatingParameters.get(index).getKind();
            if (delegatingKind == JvmMethodParameterKind.VALUE) {
                assert index == parameters.size() || parameters.get(index).getKind() == JvmMethodParameterKind.VALUE:
                        "Delegating constructor has not enough implicit parameters: " + delegatingConstructor;
                break;
            }
            if (index >= parameters.size() || parameters.get(index).getKind() != delegatingKind) {
                throw new AssertionError(
                        "Constructors of the same class should have the same set of implicit arguments: " + delegatingConstructor);
            }
            JvmMethodParameterSignature parameter = parameters.get(index);

            iv.load(offset, parameter.getAsmType());
            offset += parameter.getAsmType().getSize();
        }

        assert index == parameters.size() || parameters.get(index).getKind() == JvmMethodParameterKind.VALUE :
                "Delegating constructor has not enough parameters: " + delegatingConstructor;

        return new CallBasedArgumentGenerator(codegen, codegen.defaultCallGenerator, delegatingConstructor.getValueParameters(),
                                              delegatingCallable.getValueParameterTypes());
    }

    @NotNull
    private ArgumentGenerator generateSuperCallImplicitArguments(
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull ConstructorDescriptor superConstructor,
            @NotNull ResolvedCall<ConstructorDescriptor> superConstructorCall,
            @NotNull CallableMethod superCallable,
            @NotNull List<JvmMethodParameterSignature> superParameters,
            @NotNull List<JvmMethodParameterSignature> parameters
    ) {
        int offset = 1;
        int superIndex = 0;

        // Here we match all the super constructor parameters except those with kind VALUE to the derived constructor parameters, push
        // them all onto the stack and update "offset" variable so that in the end it points to the slot of the first VALUE argument
        for (JvmMethodParameterSignature parameter : parameters) {
            if (superIndex >= superParameters.size()) break;

            JvmMethodParameterKind superKind = superParameters.get(superIndex).getKind();
            JvmMethodParameterKind kind = parameter.getKind();
            Type type = parameter.getAsmType();

            if (superKind == JvmMethodParameterKind.VALUE && kind == JvmMethodParameterKind.SUPER_CALL_PARAM) {
                // Stop when we reach the actual value parameters present in the code; they will be generated via ResolvedCall below
                break;
            }

            if (superKind == JvmMethodParameterKind.OUTER) {
                assert kind == JvmMethodParameterKind.OUTER || kind == JvmMethodParameterKind.SUPER_CALL_PARAM :
                        String.format("Non-outer parameter incorrectly mapped to outer for %s: %s vs %s",
                                      constructorDescriptor, parameters, superParameters);
                // Super constructor requires OUTER parameter, but our OUTER instance may be different from what is expected by the super
                // constructor. We need to traverse our outer classes from the bottom up, to find the needed class. See innerExtendsOuter.kt
                ClassDescriptor outerForSuper = (ClassDescriptor) superConstructor.getContainingDeclaration().getContainingDeclaration();
                codegen.generateThisOrOuter(outerForSuper, true, true).put(codegen.v);
                superIndex++;
            }
            else if (kind == JvmMethodParameterKind.SUPER_CALL_PARAM || kind == JvmMethodParameterKind.ENUM_NAME_OR_ORDINAL) {
                iv.load(offset, type);
                superIndex++;
            }

            offset += type.getSize();
        }

        if (isAnonymousObject(descriptor)) {
            List<JvmMethodParameterSignature> superValues = superParameters.subList(superIndex, superParameters.size());
            return new ObjectSuperCallArgumentGenerator(
                    superValues, iv,
                    superConstructor.getValueParameters(), codegen.typeMapper,
                    offset, superConstructorCall
            );
        }
        else {
            return new CallBasedArgumentGenerator(codegen, codegen.defaultCallGenerator, superConstructor.getValueParameters(),
                                                  superCallable.getValueParameterTypes());
        }
    }

    private static void markLineNumberForConstructor(
            @NotNull ClassConstructorDescriptor descriptor,
            @Nullable KtConstructor constructor,
            @NotNull ExpressionCodegen codegen
    ) {
        if (constructor == null) {
            markLineNumberForDescriptor(descriptor.getContainingDeclaration(), codegen.v);
        }
        else if (constructor.hasBody() && !(constructor instanceof KtSecondaryConstructor && !((KtSecondaryConstructor) constructor).hasImplicitDelegationCall())) {
            KtBlockExpression bodyExpression = constructor.getBodyExpression();
            List<KtExpression> statements = bodyExpression != null ? bodyExpression.getStatements() : Collections.emptyList();
            if (!statements.isEmpty()) {
                codegen.markStartLineNumber(statements.iterator().next());
            }
            else {
                codegen.markStartLineNumber(bodyExpression != null ? bodyExpression : constructor);
            }
        }
        else {
            codegen.markStartLineNumber(constructor);
        }
    }
}
