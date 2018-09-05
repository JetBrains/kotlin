/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.CodegenContextUtil;
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext;
import org.jetbrains.kotlin.codegen.context.MultifileClassFacadeContext;
import org.jetbrains.kotlin.codegen.context.MultifileClassPartContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtilKt;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.util.UnderscoreUtilKt;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.getDeprecatedAccessFlag;
import static org.jetbrains.kotlin.codegen.AsmUtil.getVisibilityForBackingField;
import static org.jetbrains.kotlin.codegen.FunctionCodegen.processInterfaceMethod;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConstOrHasJvmFieldAnnotation;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.DELEGATED_PROPERTIES;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.DELEGATED_PROPERTY_METADATA_OWNER;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.*;
import static org.jetbrains.kotlin.diagnostics.Errors.EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isInterface;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.K_PROPERTY_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmFieldAnnotation;
import static org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt.hasJvmSyntheticAnnotation;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PropertyCodegen {
    private final GenerationState state;
    private final ClassBuilder v;
    private final FunctionCodegen functionCodegen;
    private final KotlinTypeMapper typeMapper;
    private final BindingContext bindingContext;
    private final FieldOwnerContext context;
    private final MemberCodegen<?> memberCodegen;
    private final OwnerKind kind;

    public PropertyCodegen(
            @NotNull FieldOwnerContext context,
            @NotNull ClassBuilder v,
            @NotNull FunctionCodegen functionCodegen,
            @NotNull MemberCodegen<?> memberCodegen
    ) {
        this.state = functionCodegen.state;
        this.v = v;
        this.functionCodegen = functionCodegen;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = state.getBindingContext();
        this.context = context;
        this.memberCodegen = memberCodegen;
        this.kind = context.getContextKind();
    }

    public void gen(@NotNull KtProperty property) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        if (!(variableDescriptor instanceof PropertyDescriptor)) {
            throw ExceptionLogger.logDescriptorNotFound(
                    "Property " + property.getName() + " should have a property descriptor: " + variableDescriptor, property
            );
        }

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
        gen(property, propertyDescriptor, property.getGetter(), property.getSetter());
    }

    public void genDestructuringDeclaration(@NotNull KtDestructuringDeclarationEntry entry) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, entry);
        if (!(variableDescriptor instanceof PropertyDescriptor)) {
            throw ExceptionLogger.logDescriptorNotFound(
                    "Destructuring declaration entry" + entry.getName() + " should have a property descriptor: " + variableDescriptor, entry
            );
        }

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
        genDestructuringDeclaration(entry, propertyDescriptor);
    }

    public void generateInPackageFacade(@NotNull DeserializedPropertyDescriptor deserializedProperty) {
        assert context instanceof MultifileClassFacadeContext : "should be called only for generating facade: " + context;
        gen(null, deserializedProperty, null, null);
    }

    private void gen(
            @Nullable KtProperty declaration,
            @NotNull PropertyDescriptor descriptor,
            @Nullable KtPropertyAccessor getter,
            @Nullable KtPropertyAccessor setter
    ) {
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION ||
               kind == OwnerKind.DEFAULT_IMPLS || kind == OwnerKind.ERASED_INLINE_CLASS
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        genBackingFieldAndAnnotations(declaration, descriptor);

        boolean isDefaultGetterAndSetter = isDefaultAccessor(getter) && isDefaultAccessor(setter);

        if (isAccessorNeeded(declaration, descriptor, getter, isDefaultGetterAndSetter)) {
            generateGetter(declaration, descriptor, getter);
        }
        if (isAccessorNeeded(declaration, descriptor, setter, isDefaultGetterAndSetter)) {
            generateSetter(declaration, descriptor, setter);
        }
    }

    private static boolean isDefaultAccessor(@Nullable KtPropertyAccessor accessor) {
        return accessor == null || !accessor.hasBody();
    }

    private void genDestructuringDeclaration(
            @NotNull KtDestructuringDeclarationEntry entry,
            @NotNull PropertyDescriptor descriptor
    ) {
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.DEFAULT_IMPLS
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        if (UnderscoreUtilKt.isSingleUnderscore(entry)) return;

        genBackingFieldAndAnnotations(entry, descriptor);

        generateGetter(entry, descriptor, null);
        generateSetter(entry, descriptor, null);
    }

    private void genBackingFieldAndAnnotations(@Nullable KtNamedDeclaration declaration, @NotNull PropertyDescriptor descriptor) {
        // Fields and '$annotations' methods for non-private const properties are generated in the multi-file facade
        boolean isBackingFieldOwner = descriptor.isConst() && !Visibilities.isPrivate(descriptor.getVisibility())
                                      ? !(context instanceof MultifileClassPartContext)
                                      : CodegenContextUtil.isImplementationOwner(context, descriptor);

        assert declaration != null : "Declaration is null: " + descriptor + " (context=" + context + ")";
        generateBackingField(declaration, descriptor, isBackingFieldOwner);
        generateSyntheticMethodIfNeeded(descriptor, isBackingFieldOwner);
    }

    /**
     * Determines if it's necessary to generate an accessor to the property, i.e. if this property can be referenced via getter/setter
     * for any reason
     *
     * @see JvmCodegenUtil#couldUseDirectAccessToProperty
     */
    private boolean isAccessorNeeded(
            @Nullable KtProperty declaration,
            @NotNull PropertyDescriptor descriptor,
            @Nullable KtPropertyAccessor accessor,
            boolean isDefaultGetterAndSetter
    ) {
        if (isConstOrHasJvmFieldAnnotation(descriptor)) return false;

        boolean isDefaultAccessor = isDefaultAccessor(accessor);

        // Don't generate accessors for interface properties with default accessors in DefaultImpls
        if (kind == OwnerKind.DEFAULT_IMPLS && isDefaultAccessor) return false;

        if (declaration == null) return true;

        // Delegated or extension properties can only be referenced via accessors
        if (declaration.hasDelegate() || declaration.getReceiverTypeReference() != null) return true;

        // Companion object properties should have accessors for non-private properties because these properties can be referenced
        // via getter/setter. But these accessors getter/setter are not required for private properties that have a default getter
        // and setter, in this case, the property can always be accessed through the accessor 'access<property name>$cp' and avoid some
        // useless indirection by using others accessors.
        if (isCompanionObject(descriptor.getContainingDeclaration())) {
            if (Visibilities.isPrivate(descriptor.getVisibility()) && isDefaultGetterAndSetter) {
                return false;
            }
            return true;
        }

        // Non-const properties from multifile classes have accessors regardless of visibility
        if (isNonConstTopLevelPropertyInMultifileClass(declaration, descriptor)) return true;

        // Private class properties have accessors only in cases when those accessors are non-trivial
        if (Visibilities.isPrivate(descriptor.getVisibility())) {
            return !isDefaultAccessor;
        }

        // Non-private properties with private setter should not be generated for trivial properties
        // as the class will use direct field access instead
        if (accessor != null && accessor.isSetter() && Visibilities.isPrivate(descriptor.getSetter().getVisibility())) {
            return !isDefaultAccessor;
        }

        return true;
    }

    private static boolean isNonConstTopLevelPropertyInMultifileClass(
            @NotNull KtProperty declaration,
            @NotNull PropertyDescriptor descriptor
    ) {
        return !descriptor.isConst() &&
               descriptor.getContainingDeclaration() instanceof PackageFragmentDescriptor &&
               JvmFileClassUtilKt.isInsideJvmMultifileClassFile(declaration);
    }

    private static boolean areAccessorsNeededForPrimaryConstructorProperty(
            @NotNull PropertyDescriptor descriptor,
            @NotNull OwnerKind kind
    ) {
        if (hasJvmFieldAnnotation(descriptor)) return false;
        if (kind == OwnerKind.ERASED_INLINE_CLASS) return false;

        Visibility visibility = descriptor.getVisibility();
        if (InlineClassesUtilsKt.isInlineClass(descriptor.getContainingDeclaration())) {
            return visibility.isPublicAPI();
        }
        else {
            return !Visibilities.isPrivate(visibility);
        }
    }

    public void generatePrimaryConstructorProperty(@NotNull KtParameter parameter, @NotNull PropertyDescriptor descriptor) {
        genBackingFieldAndAnnotations(parameter, descriptor);

        if (areAccessorsNeededForPrimaryConstructorProperty(descriptor, context.getContextKind())) {
            generateGetter(parameter, descriptor, null);
            generateSetter(parameter, descriptor, null);
        }
    }

    public void generateConstructorPropertyAsMethodForAnnotationClass(
            @NotNull KtParameter parameter,
            @NotNull PropertyDescriptor descriptor,
            @Nullable FunctionDescriptor expectedAnnotationConstructor
    ) {
        JvmMethodGenericSignature signature = typeMapper.mapAnnotationParameterSignature(descriptor);
        Method asmMethod = signature.getAsmMethod();
        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.OtherOrigin(parameter, descriptor),
                ACC_PUBLIC | ACC_ABSTRACT,
                asmMethod.getName(),
                asmMethod.getDescriptor(),
                signature.getGenericsSignature(),
                null
        );

        PropertyGetterDescriptor getter = descriptor.getGetter();
        assert getter != null : "Annotation property should have a getter: " + descriptor;
        v.getSerializationBindings().put(METHOD_FOR_FUNCTION, getter, asmMethod);
        AnnotationCodegen.forMethod(mv, memberCodegen, state).genAnnotations(getter, asmMethod.getReturnType());

        KtExpression defaultValue = loadAnnotationArgumentDefaultValue(parameter, descriptor, expectedAnnotationConstructor);
        if (defaultValue != null) {
            ConstantValue<?> constant = ExpressionCodegen.getCompileTimeConstant(
                    defaultValue, bindingContext, true, state.getShouldInlineConstVals());
            assert !state.getClassBuilderMode().generateBodies || constant != null
                    : "Default value for annotation parameter should be compile time value: " + defaultValue.getText();
            if (constant != null) {
                AnnotationCodegen annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(mv, memberCodegen, state);
                annotationCodegen.generateAnnotationDefaultValue(constant, descriptor.getType());
            }
        }

        mv.visitEnd();
    }

    private KtExpression loadAnnotationArgumentDefaultValue(
            @NotNull KtParameter ktParameter,
            @NotNull PropertyDescriptor descriptor,
            @Nullable FunctionDescriptor expectedAnnotationConstructor
    ) {
        KtExpression value = ktParameter.getDefaultValue();
        if (value != null) return value;

        if (expectedAnnotationConstructor != null) {
            ValueParameterDescriptor expectedParameter = CollectionsKt.single(
                    expectedAnnotationConstructor.getValueParameters(), parameter -> parameter.getName().equals(descriptor.getName())
            );
            PsiElement element = DescriptorToSourceUtils.descriptorToDeclaration(expectedParameter);
            if (!(element instanceof KtParameter)) {
                state.getDiagnostics().report(EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND.on(ktParameter));
                return null;
            }
            return ((KtParameter) element).getDefaultValue();
        }

        return null;
    }

    private void generateBackingField(
            @NotNull KtNamedDeclaration p,
            @NotNull PropertyDescriptor descriptor,
            boolean isBackingFieldOwner
    ) {
        if (isJvmInterface(descriptor.getContainingDeclaration()) || kind == OwnerKind.DEFAULT_IMPLS ||
            kind == OwnerKind.ERASED_INLINE_CLASS) {
            return;
        }

        boolean isDelegate = p instanceof KtProperty && ((KtProperty) p).hasDelegate();

        Object defaultValue;
        if (isDelegate) {
            defaultValue = null;
        }
        else if (Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor))) {
            if (shouldWriteFieldInitializer(descriptor)) {
                ConstantValue<?> initializer = descriptor.getCompileTimeInitializer();
                defaultValue = initializer == null ? null : initializer.getValue();
            }
            else {
                defaultValue = null;
            }
        }
        else {
            return;
        }

        generateBackingField(p, descriptor, isDelegate, defaultValue, isBackingFieldOwner);
    }

    // Annotations on properties are stored in bytecode on an empty synthetic method. This way they're still
    // accessible via reflection, and 'deprecated' and 'synthetic' flags prevent this method from being called accidentally
    private void generateSyntheticMethodIfNeeded(@NotNull PropertyDescriptor descriptor, boolean isBackingFieldOwner) {
        Annotations annotations = descriptor.getAnnotations();
        if (annotations.isEmpty()) return;

        Method signature = getSyntheticMethodSignature(descriptor);
        if (kind != OwnerKind.DEFAULT_IMPLS && CodegenContextUtil.isImplementationOwner(context, descriptor)) {
            v.getSerializationBindings().put(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor, signature);
        }

        if (isBackingFieldOwner) {
            if (!isInterface(context.getContextDescriptor()) ||
                processInterfaceMethod(descriptor, kind, false, true, state.getJvmDefaultMode())) {
                memberCodegen.generateSyntheticAnnotationsMethod(descriptor, signature, annotations);
            }
        }
    }

    @NotNull
    private Method getSyntheticMethodSignature(@NotNull PropertyDescriptor descriptor) {
        ReceiverParameterDescriptor receiver = descriptor.getExtensionReceiverParameter();
        String name = JvmAbi.getSyntheticMethodNameForAnnotatedProperty(descriptor.getName());
        String desc = receiver == null ? "()V" : "(" + typeMapper.mapType(receiver.getType()) + ")V";
        return new Method(name, desc);
    }

    private void generateBackingField(
            @NotNull KtNamedDeclaration element,
            @NotNull PropertyDescriptor propertyDescriptor,
            boolean isDelegate,
            @Nullable Object defaultValue,
            boolean isBackingFieldOwner
    ) {
        FieldDescriptor annotatedField = isDelegate ? propertyDescriptor.getDelegateField() : propertyDescriptor.getBackingField();

        int modifiers = getDeprecatedAccessFlag(propertyDescriptor);

        for (AnnotationCodegen.JvmFlagAnnotation flagAnnotation : AnnotationCodegen.FIELD_FLAGS) {
            modifiers |= flagAnnotation.getJvmFlag(annotatedField);
        }

        if (kind == OwnerKind.PACKAGE) {
            modifiers |= ACC_STATIC;
        }

        if (!propertyDescriptor.isLateInit() && (!propertyDescriptor.isVar() || isDelegate)) {
            modifiers |= ACC_FINAL;
        }

        if (hasJvmSyntheticAnnotation(propertyDescriptor)) {
            modifiers |= ACC_SYNTHETIC;
        }

        KotlinType kotlinType =
                isDelegate ? getDelegateTypeForProperty((KtProperty) element, propertyDescriptor) : propertyDescriptor.getType();
        Type type = typeMapper.mapType(kotlinType);

        ClassBuilder builder = v;

        FieldOwnerContext backingFieldContext = context;
        if (AsmUtil.isInstancePropertyWithStaticBackingField(propertyDescriptor) ) {
            modifiers |= ACC_STATIC;

            if (JvmAbi.isPropertyWithBackingFieldInOuterClass(propertyDescriptor)) {
                ImplementationBodyCodegen codegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
                builder = codegen.v;
                backingFieldContext = codegen.context;
            }
        }
        modifiers |= getVisibilityForBackingField(propertyDescriptor, isDelegate);

        if (AsmUtil.isPropertyWithBackingFieldCopyInOuterClass(propertyDescriptor)) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addCompanionObjectPropertyToCopy(propertyDescriptor, defaultValue);
        }

        String name = backingFieldContext.getFieldName(propertyDescriptor, isDelegate);

        v.getSerializationBindings().put(FIELD_FOR_PROPERTY, propertyDescriptor, Pair.create(type, name));

        if (isBackingFieldOwner) {
            FieldVisitor fv = builder.newField(
                    JvmDeclarationOriginKt.OtherOrigin(element, propertyDescriptor), modifiers, name, type.getDescriptor(),
                    isDelegate ? null : typeMapper.mapFieldSignature(kotlinType, propertyDescriptor), defaultValue
            );

            if (annotatedField != null) {
                AnnotationCodegen.forField(fv, memberCodegen, state).genAnnotations(annotatedField, type);
            }
        }
    }

    @NotNull
    private KotlinType getDelegateTypeForProperty(@NotNull KtProperty p, @NotNull PropertyDescriptor propertyDescriptor) {
        KotlinType delegateType = null;

        ResolvedCall<FunctionDescriptor> provideDelegateResolvedCall =
                bindingContext.get(BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, propertyDescriptor);
        KtExpression delegateExpression = p.getDelegateExpression();

        if (provideDelegateResolvedCall != null) {
            delegateType = provideDelegateResolvedCall.getResultingDescriptor().getReturnType();
        }
        else if (delegateExpression != null) {
            delegateType = bindingContext.getType(delegateExpression);
        }

        if (delegateType == null) {
            // Delegation convention is unresolved
            delegateType = ErrorUtils.createErrorType("Delegate type");
        }
        return delegateType;
    }

    private boolean shouldWriteFieldInitializer(@NotNull PropertyDescriptor descriptor) {
        if (!descriptor.isConst() &&
            state.getLanguageVersionSettings().supportsFeature(LanguageFeature.NoConstantValueAttributeForNonConstVals)) {
            return false;
        }

        //final field of primitive or String type
        if (!descriptor.isVar()) {
            Type type = typeMapper.mapType(descriptor);
            return AsmUtil.isPrimitive(type) || "java.lang.String".equals(type.getClassName());
        }
        return false;
    }

    private void generateGetter(
            @Nullable KtNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor getter
    ) {
        generateAccessor(p, getter, descriptor.getGetter() != null
                                    ? descriptor.getGetter()
                                    : DescriptorFactory.createDefaultGetter(descriptor, Annotations.Companion.getEMPTY()));
    }

    private void generateSetter(
            @Nullable KtNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor setter
    ) {
        if (!descriptor.isVar()) return;

        generateAccessor(p, setter, descriptor.getSetter() != null
                                    ? descriptor.getSetter()
                                    : DescriptorFactory.createDefaultSetter(
                                            descriptor, Annotations.Companion.getEMPTY(), Annotations.Companion.getEMPTY()
                                    ));
    }

    private void generateAccessor(
            @Nullable KtNamedDeclaration p,
            @Nullable KtPropertyAccessor accessor,
            @NotNull PropertyAccessorDescriptor accessorDescriptor
    ) {
        if (context instanceof MultifileClassFacadeContext &&
            (Visibilities.isPrivate(accessorDescriptor.getVisibility()) ||
             AsmUtil.getVisibilityAccessFlag(accessorDescriptor) == Opcodes.ACC_PRIVATE)) {
            return;
        }

        FunctionGenerationStrategy strategy;
        if (accessor == null || !accessor.hasBody()) {
            if (p instanceof KtProperty && ((KtProperty) p).hasDelegate()) {
                strategy = new DelegatedPropertyAccessorStrategy(state, accessorDescriptor);
            }
            else {
                strategy = new DefaultPropertyAccessorStrategy(state, accessorDescriptor);
            }
        }
        else {
            strategy = new FunctionGenerationStrategy.FunctionDefault(state, accessor);
        }

        functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(accessor != null ? accessor : p, accessorDescriptor), accessorDescriptor, strategy);
    }

    private static class DefaultPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased {
        private final PropertyAccessorDescriptor propertyAccessorDescriptor;

        public DefaultPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state);
            propertyAccessorDescriptor = descriptor;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;
            PropertyDescriptor propertyDescriptor = propertyAccessorDescriptor.getCorrespondingProperty();
            StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, true, null, StackValue.LOCAL_0);

            PsiElement jetProperty = DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor);
            if (jetProperty instanceof KtProperty || jetProperty instanceof KtParameter) {
                codegen.markLineNumber((KtElement) jetProperty, false);
            }

            if (propertyAccessorDescriptor instanceof PropertyGetterDescriptor) {
                Type type = signature.getReturnType();
                property.put(type, v);
                v.areturn(type);
            }
            else if (propertyAccessorDescriptor instanceof PropertySetterDescriptor) {
                List<ValueParameterDescriptor> valueParameters = propertyAccessorDescriptor.getValueParameters();
                assert valueParameters.size() == 1 : "Property setter should have only one value parameter but has " + propertyAccessorDescriptor;
                int parameterIndex = codegen.lookupLocalIndex(valueParameters.get(0));
                assert parameterIndex >= 0 : "Local index for setter parameter should be positive or zero: " + propertyAccessorDescriptor;
                Type type = codegen.typeMapper.mapType(propertyDescriptor);
                property.store(StackValue.local(parameterIndex, type), codegen.v);
                v.visitInsn(RETURN);
            }
            else {
                throw new IllegalStateException("Unknown property accessor: " + propertyAccessorDescriptor);
            }
        }
    }

    public static StackValue invokeDelegatedPropertyConventionMethod(
            @NotNull ExpressionCodegen codegen,
            @NotNull ResolvedCall<FunctionDescriptor> resolvedCall,
            @NotNull StackValue receiver,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        codegen.tempVariables.put(
                resolvedCall.getCall().getValueArguments().get(1).asElement(),
                getDelegatedPropertyMetadata(propertyDescriptor, codegen.getBindingContext())
        );

        return codegen.invokeFunction(resolvedCall, receiver);
    }

    @NotNull
    public static StackValue getDelegatedPropertyMetadata(
            @NotNull VariableDescriptorWithAccessors descriptor,
            @NotNull BindingContext bindingContext
    ) {
        Type owner = bindingContext.get(DELEGATED_PROPERTY_METADATA_OWNER, descriptor);
        assert owner != null : "Delegated property owner not found: " + descriptor;

        List<VariableDescriptorWithAccessors> allDelegatedProperties = bindingContext.get(DELEGATED_PROPERTIES, owner);
        int index = allDelegatedProperties == null ? -1 : allDelegatedProperties.indexOf(descriptor);
        if (index < 0) {
            throw new AssertionError("Delegated property not found in " + owner + ": " + descriptor);
        }

        StackValue.Field array = StackValue.field(
                Type.getType("[" + K_PROPERTY_TYPE), owner, JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME, true, StackValue.none()
        );
        return StackValue.arrayElement(K_PROPERTY_TYPE, null, array, StackValue.constant(index));
    }

    private static class DelegatedPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased {
        private final PropertyAccessorDescriptor propertyAccessorDescriptor;

        public DelegatedPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state);
            this.propertyAccessorDescriptor = descriptor;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;

            BindingContext bindingContext = state.getBindingContext();
            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, propertyAccessorDescriptor);
            assert resolvedCall != null : "Resolve call should be recorded for delegate call " + signature.toString();

            PropertyDescriptor propertyDescriptor = propertyAccessorDescriptor.getCorrespondingProperty();
            StackValue.Property property = codegen.intermediateValueForProperty(propertyDescriptor, true, null, StackValue.LOCAL_0);
            StackValue.Property delegate = property.getDelegateOrNull();
            assert delegate != null : "No delegate for delegated property: " + propertyDescriptor;
            StackValue lastValue = invokeDelegatedPropertyConventionMethod(codegen, resolvedCall, delegate, propertyDescriptor);
            Type asmType = signature.getReturnType();
            lastValue.put(asmType, v);
            v.areturn(asmType);
        }
    }

    public void genDelegate(@NotNull PropertyDescriptor delegate, @NotNull PropertyDescriptor delegateTo, @NotNull StackValue field) {
        ClassDescriptor toClass = (ClassDescriptor) delegateTo.getContainingDeclaration();

        PropertyGetterDescriptor getter = delegate.getGetter();
        if (getter != null) {
            //noinspection ConstantConditions
            functionCodegen.genDelegate(getter, delegateTo.getGetter().getOriginal(), toClass, field);
        }

        PropertySetterDescriptor setter = delegate.getSetter();
        if (setter != null) {
            //noinspection ConstantConditions
            functionCodegen.genDelegate(setter, delegateTo.getSetter().getOriginal(), toClass, field);
        }
    }
}
