/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.psi.PsiElement;
import kotlin.Pair;
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
import org.jetbrains.kotlin.load.java.DescriptorsJvmAbiUtil;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.InlineClassesUtilsKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.util.UnderscoreUtilKt;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.jvm.annotations.JvmAnnotationUtilKt;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.types.error.ErrorTypeKind;
import org.jetbrains.kotlin.types.error.ErrorUtils;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.getDeprecatedAccessFlag;
import static org.jetbrains.kotlin.codegen.DescriptorAsmUtil.getVisibilityForBackingField;
import static org.jetbrains.kotlin.codegen.FunctionCodegen.processInterfaceMethod;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isConstOrHasJvmFieldAnnotation;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isJvmInterface;
import static org.jetbrains.kotlin.codegen.binding.CodegenBinding.*;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.*;
import static org.jetbrains.kotlin.diagnostics.Errors.EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND;
import static org.jetbrains.kotlin.fileClasses.JvmFileClassUtilKt.isTopLevelInJvmMultifileClass;
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

        if (!UnderscoreUtilKt.isSingleUnderscore(entry)) {
            genDestructuringDeclaration((PropertyDescriptor) variableDescriptor);
        }
    }

    public void generateInPackageFacade(@NotNull DeserializedPropertyDescriptor deserializedProperty) {
        assert context instanceof MultifileClassFacadeContext : "should be called only for generating facade: " + context;

        genBackingFieldAndAnnotations(deserializedProperty);

        if (!isConstOrHasJvmFieldAnnotation(deserializedProperty)) {
            generateGetter(deserializedProperty, null);
            generateSetter(deserializedProperty, null);
        }
    }

    private void gen(
            @NotNull KtProperty declaration,
            @NotNull PropertyDescriptor descriptor,
            @Nullable KtPropertyAccessor getter,
            @Nullable KtPropertyAccessor setter
    ) {
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION ||
               kind == OwnerKind.DEFAULT_IMPLS || kind == OwnerKind.ERASED_INLINE_CLASS
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        genBackingFieldAndAnnotations(descriptor);

        boolean isDefaultGetterAndSetter = isDefaultAccessor(getter) && isDefaultAccessor(setter);

        if (isAccessorNeeded(descriptor, getter, isDefaultGetterAndSetter)) {
            generateGetter(descriptor, getter);
        }
        if (isAccessorNeeded(descriptor, setter, isDefaultGetterAndSetter)) {
            generateSetter(descriptor, setter);
        }
    }

    private static boolean isDefaultAccessor(@Nullable KtPropertyAccessor accessor) {
        return accessor == null || !accessor.hasBody();
    }

    private void genDestructuringDeclaration(@NotNull PropertyDescriptor descriptor) {
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.DEFAULT_IMPLS
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        genBackingFieldAndAnnotations(descriptor);

        generateGetter(descriptor, null);
        generateSetter(descriptor, null);
    }

    private void genBackingFieldAndAnnotations(@NotNull PropertyDescriptor descriptor) {
        // Fields and '$annotations' methods for non-private const properties are generated in the multi-file facade
        boolean isBackingFieldOwner = descriptor.isConst() && !DescriptorVisibilities.isPrivate(descriptor.getVisibility())
                                      ? !(context instanceof MultifileClassPartContext)
                                      : CodegenContextUtil.isImplementationOwner(context, descriptor);

        generateBackingField(descriptor, isBackingFieldOwner);
        generateSyntheticMethodIfNeeded(descriptor, isBackingFieldOwner);
    }

    private boolean isAccessorNeeded(
            @NotNull PropertyDescriptor descriptor,
            @Nullable KtPropertyAccessor accessor,
            boolean isDefaultGetterAndSetter
    ) {
        return isAccessorNeeded(descriptor, accessor, isDefaultGetterAndSetter, kind);
    }

    public static boolean isReferenceablePropertyWithGetter(@NotNull PropertyDescriptor descriptor) {
        PsiElement psiElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        KtDeclaration ktDeclaration = psiElement instanceof KtDeclaration ? (KtDeclaration) psiElement : null;
        if (ktDeclaration instanceof KtProperty) {
            KtProperty ktProperty = (KtProperty) ktDeclaration;
            boolean isDefaultGetterAndSetter =
                    isDefaultAccessor(ktProperty.getGetter()) && isDefaultAccessor(ktProperty.getSetter());
            return isAccessorNeeded(descriptor, ktProperty.getGetter(), isDefaultGetterAndSetter, OwnerKind.IMPLEMENTATION);
        } else if (ktDeclaration instanceof KtParameter) {
            return isAccessorNeeded(descriptor, null, true, OwnerKind.IMPLEMENTATION);
        } else {
            return isAccessorNeeded(descriptor, null, false, OwnerKind.IMPLEMENTATION);
        }
    }

    /**
     * Determines if it's necessary to generate an accessor to the property, i.e. if this property can be referenced via getter/setter
     * for any reason
     *
     * @see JvmCodegenUtil#couldUseDirectAccessToProperty
     */
    private static boolean isAccessorNeeded(
            @NotNull PropertyDescriptor descriptor,
            @Nullable KtPropertyAccessor accessor,
            boolean isDefaultGetterAndSetter,
            OwnerKind kind
    ) {
        if (isConstOrHasJvmFieldAnnotation(descriptor)) return false;

        boolean isDefaultAccessor = isDefaultAccessor(accessor);

        // Don't generate accessors for interface properties with default accessors in DefaultImpls
        if (kind == OwnerKind.DEFAULT_IMPLS && isDefaultAccessor) return false;

        // Delegated or extension properties can only be referenced via accessors
        if (descriptor.isDelegated() || descriptor.getExtensionReceiverParameter() != null) return true;

        // Companion object properties should have accessors for non-private properties because these properties can be referenced
        // via getter/setter. But these accessors getter/setter are not required for private properties that have a default getter
        // and setter, in this case, the property can always be accessed through the accessor 'access<property name>$cp' and avoid some
        // useless indirection by using others accessors.
        if (isCompanionObject(descriptor.getContainingDeclaration())) {
            if (DescriptorVisibilities.isPrivate(descriptor.getVisibility()) && isDefaultGetterAndSetter) {
                return false;
            }
            return true;
        }

        // Non-const properties from multifile classes have accessors regardless of visibility
        if (isTopLevelInJvmMultifileClass(descriptor)) return true;

        // Private class properties have accessors only in cases when those accessors are non-trivial
        if (DescriptorVisibilities.isPrivate(descriptor.getVisibility())) {
            return !isDefaultAccessor;
        }

        // Non-private properties with private setter should not be generated for trivial properties
        // as the class will use direct field access instead
        //noinspection ConstantConditions
        if (accessor != null && accessor.isSetter() && DescriptorVisibilities.isPrivate(descriptor.getSetter().getVisibility())) {
            return !isDefaultAccessor;
        }

        // Non-public API (private and internal) primary vals of inline classes don't have getter
        if (InlineClassesUtilsKt.isUnderlyingPropertyOfInlineClass(descriptor) && !descriptor.getVisibility().isPublicAPI()) {
            return false;
        }

        return true;
    }

    private static boolean areAccessorsNeededForPrimaryConstructorProperty(
            @NotNull PropertyDescriptor descriptor,
            @NotNull OwnerKind kind
    ) {
        if (hasJvmFieldAnnotation(descriptor)) return false;
        if (kind == OwnerKind.ERASED_INLINE_CLASS) return false;

        DescriptorVisibility visibility = descriptor.getVisibility();
        if (InlineClassesUtilsKt.isInlineClass(descriptor.getContainingDeclaration())) {
            return visibility.isPublicAPI();
        }
        else {
            return !DescriptorVisibilities.isPrivate(visibility);
        }
    }

    public void generatePrimaryConstructorProperty(@NotNull PropertyDescriptor descriptor) {
        genBackingFieldAndAnnotations(descriptor);

        if (areAccessorsNeededForPrimaryConstructorProperty(descriptor, context.getContextKind())) {
            generateGetter(descriptor, null);
            generateSetter(descriptor, null);
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
        AnnotationCodegen.forMethod(mv, memberCodegen, state).genAnnotations(getter, asmMethod.getReturnType(), null);

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

    private void generateBackingField(@NotNull PropertyDescriptor descriptor, boolean isBackingFieldOwner) {
        if (isJvmInterface(descriptor.getContainingDeclaration()) || kind == OwnerKind.DEFAULT_IMPLS ||
            kind == OwnerKind.ERASED_INLINE_CLASS) {
            return;
        }

        Object defaultValue;
        if (descriptor.isDelegated()) {
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

        generateBackingField(descriptor, descriptor.isDelegated(), defaultValue, isBackingFieldOwner);
    }

    // Annotations on properties are stored in bytecode on an empty synthetic method. This way they're still
    // accessible via reflection, and 'deprecated' and 'synthetic' flags prevent this method from being called accidentally
    private void generateSyntheticMethodIfNeeded(@NotNull PropertyDescriptor descriptor, boolean isBackingFieldOwner) {
        Annotations annotations = descriptor.getAnnotations();
        if (annotations.isEmpty()) return;

        Method signature = typeMapper.mapSyntheticMethodForPropertyAnnotations(descriptor);
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

    private void generateBackingField(
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

        KotlinType kotlinType = isDelegate ? getDelegateTypeForProperty(propertyDescriptor, bindingContext) : propertyDescriptor.getType();
        Type type = typeMapper.mapType(kotlinType);

        ClassBuilder builder = v;

        FieldOwnerContext backingFieldContext = context;
        List<String> additionalVisibleAnnotations = Collections.emptyList();
        if (DescriptorAsmUtil.isInstancePropertyWithStaticBackingField(propertyDescriptor) ) {
            modifiers |= ACC_STATIC;

            if (DescriptorsJvmAbiUtil.isPropertyWithBackingFieldInOuterClass(propertyDescriptor)) {
                ImplementationBodyCodegen codegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
                builder = codegen.v;
                backingFieldContext = codegen.context;
                if (DescriptorVisibilities.isPrivate(((ClassDescriptor) propertyDescriptor.getContainingDeclaration()).getVisibility())) {
                    modifiers |= ACC_DEPRECATED;
                    additionalVisibleAnnotations = Collections.singletonList(CodegenUtilKt.JAVA_LANG_DEPRECATED);
                }
            }
        }
        modifiers |= getVisibilityForBackingField(propertyDescriptor, isDelegate);

        if (DescriptorAsmUtil.isPropertyWithBackingFieldCopyInOuterClass(propertyDescriptor)) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addCompanionObjectPropertyToCopy(propertyDescriptor, defaultValue);
        }

        String name = backingFieldContext.getFieldName(propertyDescriptor, isDelegate);

        v.getSerializationBindings().put(FIELD_FOR_PROPERTY, propertyDescriptor, new Pair<>(type, name));

        if (isBackingFieldOwner) {
            String signature = isDelegate ? null : typeMapper.mapFieldSignature(kotlinType, propertyDescriptor);
            FieldVisitor fv = builder.newField(
                    JvmDeclarationOriginKt.OtherOrigin(propertyDescriptor), modifiers, name, type.getDescriptor(),
                    signature, defaultValue
            );

            if (annotatedField != null) {
                // Don't emit nullability annotations for backing field if:
                // - backing field is synthetic;
                // - property is lateinit (since corresponding field is actually nullable).
                boolean skipNullabilityAnnotations =
                        (modifiers & ACC_SYNTHETIC) != 0 ||
                        propertyDescriptor.isLateInit();
                AnnotationCodegen.forField(fv, memberCodegen, state, skipNullabilityAnnotations)
                        .genAnnotations(annotatedField, type, propertyDescriptor.getType(), null, additionalVisibleAnnotations);
            }

            if (propertyDescriptor.getContainingDeclaration() instanceof ClassDescriptor && JvmAnnotationUtilKt.isJvmRecord((ClassDescriptor) propertyDescriptor.getContainingDeclaration())) {
                ClassBuilderRecordKt.addRecordComponent(builder, name, type.getDescriptor(), signature);
            }
        }
    }

    @NotNull
    public static KotlinType getDelegateTypeForProperty(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull BindingContext bindingContext
    ) {
        ResolvedCall<FunctionDescriptor> provideDelegateResolvedCall =
                bindingContext.get(BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, propertyDescriptor);

        KtProperty property = (KtProperty) DescriptorToSourceUtils.descriptorToDeclaration(propertyDescriptor);
        KtExpression delegateExpression = property != null ? property.getDelegateExpression() : null;

        KotlinType delegateType;
        if (provideDelegateResolvedCall != null) {
            delegateType = provideDelegateResolvedCall.getResultingDescriptor().getReturnType();
        }
        else if (delegateExpression != null) {
            delegateType = bindingContext.getType(delegateExpression);
        }
        else {
            delegateType = null;
        }

        if (delegateType == null) {
            // Delegation convention is unresolved
            delegateType = ErrorUtils.createErrorType(ErrorTypeKind.TYPE_FOR_DELEGATION, delegateExpression.getText());
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

    private void generateGetter(@NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor getter) {
        generateAccessor(
                getter,
                descriptor.getGetter() != null ? descriptor.getGetter() : DescriptorFactory.createDefaultGetter(
                        descriptor, Annotations.Companion.getEMPTY()
                )
        );
    }

    private void generateSetter(@NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor setter) {
        if (!descriptor.isVar()) return;

        generateAccessor(
                setter,
                descriptor.getSetter() != null ? descriptor.getSetter() : DescriptorFactory.createDefaultSetter(
                        descriptor, Annotations.Companion.getEMPTY(), Annotations.Companion.getEMPTY()
                )
        );
    }

    private void generateAccessor(@Nullable KtPropertyAccessor accessor, @NotNull PropertyAccessorDescriptor descriptor) {
        if (context instanceof MultifileClassFacadeContext &&
            (DescriptorVisibilities.isPrivate(descriptor.getVisibility()) ||
             DescriptorAsmUtil.getVisibilityAccessFlag(descriptor) == Opcodes.ACC_PRIVATE)) {
            return;
        }

        FunctionGenerationStrategy strategy;
        if (accessor == null || !accessor.hasBody()) {
            if (descriptor.getCorrespondingProperty().isDelegated()) {
                strategy = new DelegatedPropertyAccessorStrategy(state, descriptor);
            }
            else {
                strategy = new DefaultPropertyAccessorStrategy(state, descriptor);
            }
        }
        else {
            strategy = new FunctionGenerationStrategy.FunctionDefault(state, accessor);
        }

        functionCodegen.generateMethod(JvmDeclarationOriginKt.OtherOrigin(descriptor), descriptor, strategy);
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

    public static boolean isDelegatedPropertyWithOptimizedMetadata(
            @NotNull VariableDescriptorWithAccessors descriptor,
            @NotNull BindingContext bindingContext
    ) {
        return Boolean.TRUE == bindingContext.get(DELEGATED_PROPERTY_WITH_OPTIMIZED_METADATA, descriptor);
    }

    public static @NotNull StackValue getOptimizedDelegatedPropertyMetadataValue() {
        return StackValue.constant(null, K_PROPERTY_TYPE);
    }

    @NotNull
    public static StackValue getDelegatedPropertyMetadata(
            @NotNull VariableDescriptorWithAccessors descriptor,
            @NotNull BindingContext bindingContext
    ) {
        if (isDelegatedPropertyWithOptimizedMetadata(descriptor, bindingContext)) {
            return getOptimizedDelegatedPropertyMetadataValue();
        }

        Type owner = bindingContext.get(DELEGATED_PROPERTY_METADATA_OWNER, descriptor);
        assert owner != null : "Delegated property owner not found: " + descriptor;

        List<VariableDescriptorWithAccessors> allDelegatedProperties = bindingContext.get(DELEGATED_PROPERTIES_WITH_METADATA, owner);
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
            KotlinType kotlinReturnType = propertyAccessorDescriptor.getOriginal().getReturnType();
            lastValue.put(asmType, kotlinReturnType, v);
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
