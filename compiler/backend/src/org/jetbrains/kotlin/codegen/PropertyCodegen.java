/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.annotation.AnnotatedSimple;
import org.jetbrains.kotlin.codegen.annotation.AnnotatedWithFakeAnnotations;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationSplitter;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtilKt;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils;
import org.jetbrains.kotlin.resolve.annotations.AnnotationUtilKt;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKt;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
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
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.*;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.FIELD_FOR_PROPERTY;
import static org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings.SYNTHETIC_METHOD_FOR_PROPERTY;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isCompanionObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isInterface;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.K_PROPERTY_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.annotations.AnnotationUtilKt.hasJvmFieldAnnotation;
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
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.DEFAULT_IMPLS
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        genBackingFieldAndAnnotations(declaration, descriptor, false);

        if (isAccessorNeeded(declaration, descriptor, getter)) {
            generateGetter(declaration, descriptor, getter);
        }
        if (isAccessorNeeded(declaration, descriptor, setter)) {
            generateSetter(declaration, descriptor, setter);
        }
    }

    private void genBackingFieldAndAnnotations(
            @Nullable KtNamedDeclaration declaration, @NotNull PropertyDescriptor descriptor, boolean isParameter
    ) {
        boolean hasBackingField = hasBackingField(descriptor);
        boolean hasDelegate = declaration instanceof KtProperty && ((KtProperty) declaration).hasDelegate();

        AnnotationSplitter annotationSplitter =
                AnnotationSplitter.create(LockBasedStorageManager.NO_LOCKS,
                                          descriptor.getAnnotations(),
                                          AnnotationSplitter.getTargetSet(isParameter, descriptor.isVar(), hasBackingField, hasDelegate));

        Annotations propertyAnnotations = annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY);

        // Fields and '$annotations' methods for non-private const properties are generated in the multi-file facade
        boolean isBackingFieldOwner = descriptor.isConst() && !Visibilities.isPrivate(descriptor.getVisibility())
                                      ? !(context instanceof MultifileClassPartContext)
                                      : CodegenContextUtil.isImplClassOwner(context);

        if (isBackingFieldOwner) {
            Annotations fieldAnnotations = annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.FIELD);
            Annotations delegateAnnotations = annotationSplitter.getAnnotationsForTarget(AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD);
            assert declaration != null : "Declaration is null: " + descriptor + " (context=" + context + ")";
            generateBackingField(declaration, descriptor, fieldAnnotations, delegateAnnotations);
            generateSyntheticMethodIfNeeded(descriptor, propertyAnnotations);
        }

        if (!propertyAnnotations.getAllAnnotations().isEmpty() && kind != OwnerKind.DEFAULT_IMPLS &&
            CodegenContextUtil.isImplClassOwner(context)) {
            v.getSerializationBindings().put(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor, getSyntheticMethodSignature(descriptor));
        }
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
            @Nullable KtPropertyAccessor accessor
    ) {
        if (isConstOrHasJvmFieldAnnotation(descriptor)) return false;

        boolean isDefaultAccessor = accessor == null || !accessor.hasBody();

        // Don't generate accessors for interface properties with default accessors in DefaultImpls
        if (kind == OwnerKind.DEFAULT_IMPLS && isDefaultAccessor) return false;

        if (declaration == null) return true;

        // Delegated or extension properties can only be referenced via accessors
        if (declaration.hasDelegate() || declaration.getReceiverTypeReference() != null) return true;

        // Companion object properties always should have accessors, because their backing fields are moved/copied to the outer class
        if (isCompanionObject(descriptor.getContainingDeclaration())) return true;

        // Non-const properties from multifile classes have accessors regardless of visibility
        if (!descriptor.isConst() && JvmFileClassUtilKt.isInsideJvmMultifileClassFile(declaration)) return true;

        // Private class properties have accessors only in cases when those accessors are non-trivial
        if (Visibilities.isPrivate(descriptor.getVisibility())) {
            return !isDefaultAccessor;
        }

        return true;
    }

    private static boolean areAccessorsNeededForPrimaryConstructorProperty(
            @NotNull PropertyDescriptor descriptor
    ) {
        if (hasJvmFieldAnnotation(descriptor)) return false;

        return !Visibilities.isPrivate(descriptor.getVisibility());
    }

    public void generatePrimaryConstructorProperty(@NotNull KtParameter p, @NotNull PropertyDescriptor descriptor) {
        genBackingFieldAndAnnotations(p, descriptor, true);

        if (areAccessorsNeededForPrimaryConstructorProperty(descriptor)) {
            generateGetter(p, descriptor, null);
            generateSetter(p, descriptor, null);
        }
    }

    public void generateConstructorPropertyAsMethodForAnnotationClass(KtParameter p, PropertyDescriptor descriptor) {
        JvmMethodGenericSignature signature = typeMapper.mapAnnotationParameterSignature(descriptor);
        String name = p.getName();
        if (name == null) return;
        MethodVisitor mv = v.newMethod(
                JvmDeclarationOriginKt.OtherOrigin(p, descriptor), ACC_PUBLIC | ACC_ABSTRACT, name,
                signature.getAsmMethod().getDescriptor(),
                signature.getGenericsSignature(),
                null
        );

        KtExpression defaultValue = p.getDefaultValue();
        if (defaultValue != null) {
            ConstantValue<?> constant = ExpressionCodegen.getCompileTimeConstant(defaultValue, bindingContext, true);
            assert !state.getClassBuilderMode().generateBodies || constant != null
                    : "Default value for annotation parameter should be compile time value: " + defaultValue.getText();
            if (constant != null) {
                AnnotationCodegen annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(mv, memberCodegen, typeMapper);
                annotationCodegen.generateAnnotationDefaultValue(constant, descriptor.getType());
            }
        }

        mv.visitEnd();
    }

    private boolean hasBackingField(@NotNull PropertyDescriptor descriptor) {
        return !isJvmInterface(descriptor.getContainingDeclaration()) &&
               kind != OwnerKind.DEFAULT_IMPLS &&
               !Boolean.FALSE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor));
    }

    private boolean generateBackingField(
            @NotNull KtNamedDeclaration p,
            @NotNull PropertyDescriptor descriptor,
            @NotNull Annotations backingFieldAnnotations,
            @NotNull Annotations delegateAnnotations
    ) {
        if (isJvmInterface(descriptor.getContainingDeclaration()) || kind == OwnerKind.DEFAULT_IMPLS) {
            return false;
        }

        if (p instanceof KtProperty && ((KtProperty) p).hasDelegate()) {
            generatePropertyDelegateAccess((KtProperty) p, descriptor, delegateAnnotations);
        }
        else if (Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor))) {
            generateBackingFieldAccess(p, descriptor, backingFieldAnnotations);
        }
        else {
            return false;
        }
        return true;
    }

    // Annotations on properties are stored in bytecode on an empty synthetic method. This way they're still
    // accessible via reflection, and 'deprecated' and 'private' flags prevent this method from being called accidentally
    private void generateSyntheticMethodIfNeeded(@NotNull PropertyDescriptor descriptor, @NotNull Annotations annotations) {
        if (annotations.getAllAnnotations().isEmpty()) return;

        DeclarationDescriptor contextDescriptor = context.getContextDescriptor();
        if (!isInterface(contextDescriptor) ||
            (isJvm8Interface(contextDescriptor, state) ? kind != OwnerKind.DEFAULT_IMPLS : kind == OwnerKind.DEFAULT_IMPLS)) {
            int flags = ACC_DEPRECATED | ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC;
            Method syntheticMethod = getSyntheticMethodSignature(descriptor);
            MethodVisitor mv = v.newMethod(JvmDeclarationOriginKt.OtherOrigin(descriptor), flags, syntheticMethod.getName(),
                                           syntheticMethod.getDescriptor(), null, null);
            AnnotationCodegen.forMethod(mv, memberCodegen, typeMapper)
                    .genAnnotations(new AnnotatedSimple(annotations), Type.VOID_TYPE, AnnotationUseSiteTarget.PROPERTY);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitEnd();
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
            KtNamedDeclaration element,
            PropertyDescriptor propertyDescriptor,
            boolean isDelegate,
            KotlinType kotlinType,
            Object defaultValue,
            Annotations annotations
    ) {
        int modifiers = getDeprecatedAccessFlag(propertyDescriptor);

        for (AnnotationCodegen.JvmFlagAnnotation flagAnnotation : AnnotationCodegen.FIELD_FLAGS) {
            if (flagAnnotation.hasAnnotation(propertyDescriptor.getOriginal())) {
                modifiers |= flagAnnotation.getJvmFlag();
            }
        }

        if (kind == OwnerKind.PACKAGE) {
            modifiers |= ACC_STATIC;
        }

        if (!propertyDescriptor.isLateInit() && (!propertyDescriptor.isVar() || isDelegate)) {
            modifiers |= ACC_FINAL;
        }

        if (AnnotationUtilKt.hasJvmSyntheticAnnotation(propertyDescriptor)) {
            modifiers |= ACC_SYNTHETIC;
        }

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

        FieldVisitor fv = builder.newField(
                JvmDeclarationOriginKt.OtherOrigin(element, propertyDescriptor), modifiers, name, type.getDescriptor(),
                isDelegate ? null : typeMapper.mapFieldSignature(kotlinType, propertyDescriptor), defaultValue
        );

        Annotated fieldAnnotated = new AnnotatedWithFakeAnnotations(propertyDescriptor, annotations);
        AnnotationCodegen.forField(fv, memberCodegen, typeMapper).genAnnotations(
                fieldAnnotated, type, isDelegate ? AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD : AnnotationUseSiteTarget.FIELD);
    }

    private void generatePropertyDelegateAccess(
            @NotNull KtProperty p,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations
    ) {
        KtExpression delegateExpression = p.getDelegateExpression();
        KotlinType delegateType = delegateExpression != null ? bindingContext.getType(p.getDelegateExpression()) : null;
        if (delegateType == null) {
            // If delegate expression is unresolved reference
            delegateType = ErrorUtils.createErrorType("Delegate type");
        }

        generateBackingField(p, propertyDescriptor, true, delegateType, null, annotations);
    }

    private void generateBackingFieldAccess(
            @NotNull KtNamedDeclaration p,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull Annotations annotations
    ) {
        Object value = null;

        if (shouldWriteFieldInitializer(propertyDescriptor)) {
            ConstantValue<?> initializer = propertyDescriptor.getCompileTimeInitializer();
            if (initializer != null) {
                value = initializer.getValue();
            }
        }

        generateBackingField(p, propertyDescriptor, false, propertyDescriptor.getType(), value, annotations);
    }

    private boolean shouldWriteFieldInitializer(@NotNull PropertyDescriptor descriptor) {
        //final field of primitive or String type
        if (!descriptor.isVar()) {
            Type type = typeMapper.mapType(descriptor);
            return AsmUtil.isPrimitive(type) || "java.lang.String".equals(type.getClassName());
        }
        return false;
    }

    private void generateGetter(@Nullable KtNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor getter) {
        generateAccessor(p, getter, descriptor.getGetter() != null
                                    ? descriptor.getGetter()
                                    : DescriptorFactory.createDefaultGetter(descriptor, Annotations.Companion.getEMPTY()));
    }

    private void generateSetter(@Nullable KtNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable KtPropertyAccessor setter) {
        if (!descriptor.isVar()) return;

        generateAccessor(p, setter, descriptor.getSetter() != null
                                    ? descriptor.getSetter()
                                    : DescriptorFactory.createDefaultSetter(descriptor, Annotations.Companion.getEMPTY()));
    }

    private void generateAccessor(
            @Nullable KtNamedDeclaration p,
            @Nullable KtPropertyAccessor accessor,
            @NotNull PropertyAccessorDescriptor accessorDescriptor
    ) {
        if (context instanceof MultifileClassFacadeContext && Visibilities.isPrivate(accessorDescriptor.getVisibility())) {
            return;
        }

        FunctionGenerationStrategy strategy;
        if (accessor == null || !accessor.hasBody()) {
            if (p instanceof KtProperty && ((KtProperty) p).hasDelegate()) {
                strategy = new DelegatedPropertyAccessorStrategy(state, accessorDescriptor, indexOfDelegatedProperty((KtProperty) p));
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

    public static int indexOfDelegatedProperty(@NotNull KtProperty property) {
        PsiElement parent = property.getParent();
        KtDeclarationContainer container;
        if (parent instanceof KtClassBody) {
            container = ((KtClassOrObject) parent.getParent());
        }
        else if (parent instanceof KtFile) {
            container = (KtFile) parent;
        }
        else if (KtPsiUtil.isScriptDeclaration(property)) {
            container = KtPsiUtil.getScript(property);
            assert  container != null : "Script declaration for property '" + property.getText() + "' should be not null!";
        }
        else {
            throw new UnsupportedOperationException("Unknown delegated property container: " + parent);
        }

        int index = 0;
        for (KtDeclaration declaration : container.getDeclarations()) {
            if (declaration instanceof KtProperty && ((KtProperty) declaration).hasDelegate()) {
                if (declaration == property) {
                    return index;
                }
                index++;
            }
        }

        throw new IllegalStateException("Delegated property not found in its parent: " + PsiUtilsKt.getElementTextWithContext(property));
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
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull KotlinTypeMapper typeMapper,
            @NotNull ResolvedCall<FunctionDescriptor> resolvedCall,
            final int indexInPropertyMetadataArray,
            int propertyMetadataArgumentIndex
    ) {
        CodegenContext<? extends ClassOrPackageFragmentDescriptor> ownerContext = codegen.getContext().getClassOrPackageParentContext();
        final Type owner;
        if (ownerContext instanceof ClassContext) {
            owner = typeMapper.mapClass(((ClassContext) ownerContext).getContextDescriptor());
        }
        else if (ownerContext instanceof PackageContext) {
            owner = ((PackageContext) ownerContext).getPackagePartType();
        }
        else if (ownerContext instanceof MultifileClassContextBase) {
            owner = ((MultifileClassContextBase) ownerContext).getFilePartType();
        }
        else {
            throw new UnsupportedOperationException("Unknown context: " + ownerContext);
        }

        codegen.tempVariables.put(
                resolvedCall.getCall().getValueArguments().get(propertyMetadataArgumentIndex).asElement(),
                new StackValue(K_PROPERTY_TYPE) {
                    @Override
                    public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
                        Field array = StackValue.field(
                                Type.getType("[" + K_PROPERTY_TYPE), owner, JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME, true, StackValue.none()
                        );
                        StackValue.arrayElement(
                                K_PROPERTY_TYPE, array, StackValue.constant(indexInPropertyMetadataArray, Type.INT_TYPE)
                        ).put(type, v);
                    }
                }
        );

        StackValue delegatedProperty = codegen.intermediateValueForProperty(propertyDescriptor, true, null, StackValue.LOCAL_0);
        return codegen.invokeFunction(resolvedCall, delegatedProperty);
    }

    private static class DelegatedPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased {
        private final int index;
        private final PropertyAccessorDescriptor propertyAccessorDescriptor;

        public DelegatedPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor, int index) {
            super(state);
            this.index = index;
            propertyAccessorDescriptor = descriptor;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;

            BindingContext bindingContext = state.getBindingContext();
            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, propertyAccessorDescriptor);
            assert resolvedCall != null : "Resolve call should be recorded for delegate call " + signature.toString();

            StackValue lastValue = invokeDelegatedPropertyConventionMethod(propertyAccessorDescriptor.getCorrespondingProperty(),
                                                                           codegen, state.getTypeMapper(), resolvedCall, index, 1);
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
