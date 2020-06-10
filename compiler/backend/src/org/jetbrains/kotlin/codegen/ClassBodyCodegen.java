/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.PsiElement;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.backend.common.bridges.ImplKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptor;
import org.jetbrains.kotlin.psi.synthetics.SyntheticClassOrObjectDescriptorKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter;
import org.jetbrains.kotlin.resolve.scopes.MemberScope;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.*;

import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.enumEntryNeedSubclass;
import static org.jetbrains.kotlin.resolve.DescriptorToSourceUtils.descriptorToDeclaration;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind.CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL;

public abstract class ClassBodyCodegen extends MemberCodegen<KtPureClassOrObject> {
    @NotNull
    public final KtPureClassOrObject myClass;

    @NotNull
    public final OwnerKind kind;

    @NotNull
    public final ClassDescriptor descriptor;

    protected ClassBodyCodegen(
            @NotNull KtPureClassOrObject myClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(state, parentCodegen, context, myClass, v);
        this.myClass = myClass;
        this.kind = context.getContextKind();
        this.descriptor = SyntheticClassOrObjectDescriptorKt.findClassDescriptor(myClass, bindingContext);
    }

    @Override
    protected void generateBody() {
        List<KtObjectDeclaration> companions = new ArrayList<>();
        if (kind != OwnerKind.DEFAULT_IMPLS && kind != OwnerKind.ERASED_INLINE_CLASS) {
            //generate nested classes first and only then generate class body. It necessary to access to nested CodegenContexts
            for (KtDeclaration declaration : myClass.getDeclarations()) {
                if (shouldProcessFirst(declaration)) {
                    //Generate companions after class body generation (need to record all synthetic accessors)
                    if (declaration instanceof KtObjectDeclaration && ((KtObjectDeclaration) declaration).isCompanion()) {
                        companions.add((KtObjectDeclaration) declaration);
                        CodegenUtilKt.populateCompanionBackingFieldNamesToOuterContextIfNeeded((KtObjectDeclaration) declaration, context, state);
                    }
                    else {
                        generateDeclaration(declaration);
                    }
                }
            }
        }

        for (KtDeclaration declaration : myClass.getDeclarations()) {
            if (!shouldProcessFirst(declaration)) {
                generateDeclaration(declaration);
            }
        }

        boolean generateNonClassMembers = shouldGenerateNonClassMembers();

        if (generateNonClassMembers) {
            generatePrimaryConstructorProperties();
            generateConstructors();
            generateDefaultImplsIfNeeded();
            generateErasedInlineClassIfNeeded();
            generateUnboxMethodForInlineClass();
        }

        // Generate _declared_ companions
        for (KtObjectDeclaration companion : companions) {
            genClassOrObject(companion);
        }

        // Generate synthetic nested classes
        Collection<DeclarationDescriptor> classifiers = descriptor
                .getUnsubstitutedMemberScope()
                .getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS, MemberScope.Companion.getALL_NAME_FILTER());
        for (DeclarationDescriptor memberDescriptor : classifiers) {
            if (memberDescriptor instanceof SyntheticClassOrObjectDescriptor) {
                genSyntheticClassOrObject((SyntheticClassOrObjectDescriptor) memberDescriptor);
            }
        }

        if (generateNonClassMembers) {
            generateBridges();
        }
    }

    private void generateBridges() {
        if (DescriptorUtils.isInterface(descriptor)) {
            return;
        }
        for (DeclarationDescriptor memberDescriptor : DescriptorUtils.getAllDescriptors(descriptor.getDefaultType().getMemberScope())) {
            if (memberDescriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor member = (CallableMemberDescriptor) memberDescriptor;
                if (!member.getKind().isReal() && ImplKt.findInterfaceImplementation(member) == null) {
                    if (member instanceof FunctionDescriptor) {
                        functionCodegen.generateBridges((FunctionDescriptor) member);
                    }
                    else if (member instanceof PropertyDescriptor) {
                        PropertyGetterDescriptor getter = ((PropertyDescriptor) member).getGetter();
                        if (getter != null) {
                            functionCodegen.generateBridges(getter);
                        }
                        PropertySetterDescriptor setter = ((PropertyDescriptor) member).getSetter();
                        if (setter != null) {
                            functionCodegen.generateBridges(setter);
                        }
                    }
                }
            }
        }
    }

    private boolean shouldGenerateNonClassMembers() {
        return !(myClass instanceof KtClassOrObject) ||
               state.getGenerateDeclaredClassFilter().shouldGenerateClassMembers((KtClassOrObject) myClass);
    }

    protected void generateConstructors() {}

    protected void generateDefaultImplsIfNeeded() {}

    protected void generateErasedInlineClassIfNeeded() {}

    protected void generateUnboxMethodForInlineClass() {}

    private static boolean shouldProcessFirst(KtDeclaration declaration) {
        return !(declaration instanceof KtProperty || declaration instanceof KtNamedFunction);
    }

    protected void generateDeclaration(KtDeclaration declaration) {
        if (declaration instanceof KtProperty || declaration instanceof KtNamedFunction || declaration instanceof KtTypeAlias) {
            if (shouldGenerateNonClassMembers()) {
                genSimpleMember(declaration);
            }
        }
        else if (declaration instanceof KtClassOrObject) {
            if (declaration instanceof KtEnumEntry && !enumEntryNeedSubclass(bindingContext, (KtEnumEntry) declaration)) {
                return;
            }

            genClassOrObject((KtClassOrObject) declaration);
        }
    }

    private void generatePrimaryConstructorProperties() {
        ClassConstructorDescriptor constructor = CollectionsKt.firstOrNull(descriptor.getConstructors());
        if (constructor == null) return;

        boolean isAnnotation = descriptor.getKind() == ClassKind.ANNOTATION_CLASS;
        FunctionDescriptor expectedAnnotationConstructor =
                isAnnotation && constructor.isActual()
                ? CodegenUtil.findExpectedFunctionForActual(constructor)
                : null;

        for (KtParameter p : getPrimaryConstructorParameters()) {
            if (p.hasValOrVar()) {
                PropertyDescriptor propertyDescriptor = bindingContext.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, p);
                if (propertyDescriptor != null) {
                    if (isAnnotation) {
                        propertyCodegen.generateConstructorPropertyAsMethodForAnnotationClass(
                                p, propertyDescriptor, expectedAnnotationConstructor
                        );
                    }
                    else {
                        propertyCodegen.generatePrimaryConstructorProperty(propertyDescriptor);
                    }
                }
            }
        }
    }

    @NotNull
    public List<KtParameter> getPrimaryConstructorParameters() {
        if (myClass instanceof KtClass) {
            return myClass.getPrimaryConstructorParameters();
        }
        return Collections.emptyList();
    }

    @Nullable
    @Override
    protected ClassDescriptor classForInnerClassRecord() {
        return InnerClassConsumer.Companion.classForInnerClassRecord(descriptor, false);
    }

    protected void generateDelegatesToDefaultImpl() {
        if (isJvmInterface(descriptor)) return;

        boolean isErasedInlineClass = InlineClassesUtilsKt.isInlineClass(descriptor) && kind == OwnerKind.ERASED_INLINE_CLASS;
        JvmKotlinType receiverType = new JvmKotlinType(typeMapper.mapType(descriptor), descriptor.getDefaultType());

        for (Map.Entry<FunctionDescriptor, FunctionDescriptor> entry : CodegenUtil.getNonPrivateTraitMethods(descriptor).entrySet()) {
            generateDelegationToDefaultImpl(entry.getKey(), entry.getValue(), receiverType, functionCodegen, state, isErasedInlineClass);
        }
    }

    public static void generateDelegationToDefaultImpl(
            @NotNull FunctionDescriptor interfaceFun,
            @NotNull FunctionDescriptor inheritedFun,
            @NotNull JvmKotlinType receiverType,
            @NotNull FunctionCodegen functionCodegen,
            @NotNull GenerationState state,
            boolean isErasedInlineClass
    ) {
        // Skip Java 8 default methods
        if (CodegenUtilKt.isDefinitelyNotDefaultImplsMethod(interfaceFun)) {
            return;
        }

        CallableMemberDescriptor actualImplementation =
                interfaceFun.getKind().isReal() ? interfaceFun : ImplKt.findImplementationFromInterface(interfaceFun);
        assert actualImplementation != null : "Can't find actual implementation for " + interfaceFun;
        if (JvmAnnotationUtilKt.isCallableMemberCompiledToJvmDefault(actualImplementation, state.getJvmDefaultMode())) {
            return;
        }

        KotlinTypeMapper typeMapper = state.getTypeMapper();
        functionCodegen.generateMethod(
                new JvmDeclarationOrigin(
                        CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL, descriptorToDeclaration(interfaceFun), interfaceFun, null
                ),
                inheritedFun,
                new FunctionGenerationStrategy.CodegenBased(state) {
                    @Override
                    public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
                        DeclarationDescriptor containingDeclaration = interfaceFun.getContainingDeclaration();
                        if (!DescriptorUtils.isInterface(containingDeclaration)) return;

                        DeclarationDescriptor declarationInheritedFun = inheritedFun.getContainingDeclaration();
                        PsiElement classForInheritedFun = descriptorToDeclaration(declarationInheritedFun);
                        if (classForInheritedFun instanceof KtDeclaration) {
                            codegen.markLineNumber(classForInheritedFun, false);
                        }

                        ClassDescriptor containingTrait = (ClassDescriptor) containingDeclaration;
                        Type traitImplType = typeMapper.mapDefaultImpls(containingTrait);

                        FunctionDescriptor originalInterfaceFun = interfaceFun.getOriginal();
                        Method traitMethod = typeMapper.mapAsmMethod(originalInterfaceFun, OwnerKind.DEFAULT_IMPLS);

                        putArgumentsOnStack(codegen, signature, traitMethod);
                        InstructionAdapter iv = codegen.v;

                        if (KotlinBuiltIns.isCloneable(containingTrait) && traitMethod.getName().equals("clone")) {
                            // A special hack for Cloneable: there's no kotlin/Cloneable$DefaultImpls class at runtime,
                            // and its 'clone' method is actually located in java/lang/Object
                            iv.invokespecial("java/lang/Object", "clone", "()Ljava/lang/Object;", false);
                        }
                        else {
                            iv.invokestatic(traitImplType.getInternalName(), traitMethod.getName(), traitMethod.getDescriptor(), false);
                        }

                        Type returnType = signature.getReturnType();
                        StackValue.onStack(traitMethod.getReturnType(), originalInterfaceFun.getReturnType()).put(returnType, iv);
                        iv.areturn(returnType);
                    }

                    private void putArgumentsOnStack(
                            @NotNull ExpressionCodegen codegen,
                            @NotNull JvmMethodSignature signature,
                            @NotNull Method defaultImplsMethod
                    ) {
                        InstructionAdapter iv = codegen.v;
                        Type[] myArgTypes = signature.getAsmMethod().getArgumentTypes();
                        Type[] toArgTypes = defaultImplsMethod.getArgumentTypes();

                        int myArgI = 0;
                        int argVar = 0;

                        KotlinType interfaceKotlinType = ((ClassDescriptor) inheritedFun.getContainingDeclaration()).getDefaultType();
                        StackValue.local(argVar, receiverType.getType(), receiverType.getKotlinType())
                                .put(OBJECT_TYPE, interfaceKotlinType, iv);
                        if (isErasedInlineClass) myArgI++;
                        argVar += receiverType.getType().getSize();

                        int toArgI = 1;

                        List<ParameterDescriptor> myParameters = getParameters(inheritedFun);
                        List<ParameterDescriptor> toParameters = getParameters(interfaceFun);
                        assert myParameters.size() == toParameters.size() :
                                "Inconsistent value parameters between delegating fun " + inheritedFun +
                                "and interface fun " + interfaceFun;

                        Iterator<ParameterDescriptor> myParametersIterator = myParameters.iterator();
                        Iterator<ParameterDescriptor> toParametersIterator = toParameters.iterator();
                        for (; myArgI < myArgTypes.length; myArgI++, toArgI++) {
                            Type myArgType = myArgTypes[myArgI];
                            Type toArgType = toArgTypes[toArgI];

                            KotlinType myArgKotlinType = myParametersIterator.hasNext() ? myParametersIterator.next().getType() : null;
                            KotlinType toArgKotlinType = toParametersIterator.hasNext() ? toParametersIterator.next().getType() : null;

                            StackValue.local(argVar, myArgType, myArgKotlinType)
                                    .put(toArgType, toArgKotlinType, iv);
                            argVar += myArgType.getSize();
                        }

                        assert toArgI == toArgTypes.length :
                                "Invalid trait implementation signature: " + signature +
                                " vs " + defaultImplsMethod + " for " + interfaceFun;
                    }

                    private List<ParameterDescriptor> getParameters(FunctionDescriptor functionDescriptor) {
                        List<ParameterDescriptor> valueParameterDescriptors =
                                new ArrayList<>(functionDescriptor.getValueParameters().size() + 1);

                        ReceiverParameterDescriptor extensionReceiverParameter = functionDescriptor.getExtensionReceiverParameter();
                        if (extensionReceiverParameter != null) {
                            valueParameterDescriptors.add(extensionReceiverParameter);
                        }

                        valueParameterDescriptors.addAll(functionDescriptor.getValueParameters());

                        return valueParameterDescriptors;
                    }
                }
        );
    }
}
