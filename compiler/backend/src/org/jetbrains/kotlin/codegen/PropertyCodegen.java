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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.*;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.load.java.JvmAbi;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorFactory;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.org.objectweb.asm.FieldVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.List;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;
import static org.jetbrains.kotlin.codegen.JvmCodegenUtil.isInterface;
import static org.jetbrains.kotlin.codegen.JvmSerializationBindings.*;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isDefaultObject;
import static org.jetbrains.kotlin.resolve.DescriptorUtils.isTrait;
import static org.jetbrains.kotlin.resolve.jvm.AsmTypes.PROPERTY_METADATA_TYPE;
import static org.jetbrains.kotlin.resolve.jvm.diagnostics.DiagnosticsPackage.OtherOrigin;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PropertyCodegen {
    private final GenerationState state;
    private final ClassBuilder v;
    private final FunctionCodegen functionCodegen;
    private final JetTypeMapper typeMapper;
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

    public void gen(@NotNull JetProperty property) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, property);
        assert variableDescriptor instanceof PropertyDescriptor : "Property " + property.getText() + " should have a property descriptor: " + variableDescriptor;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
        gen(property, propertyDescriptor, property.getGetter(), property.getSetter());
    }

    public void generateInPackageFacade(@NotNull DeserializedPropertyDescriptor deserializedProperty) {
        assert context instanceof PackageFacadeContext : "should be called only for generating package facade: " + context;
        gen(null, deserializedProperty, null, null);
    }

    private void gen(
            @Nullable JetProperty declaration,
            @NotNull PropertyDescriptor descriptor,
            @Nullable JetPropertyAccessor getter,
            @Nullable JetPropertyAccessor setter
    ) {
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.TRAIT_IMPL
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        if (context instanceof PackageFacadeContext) {
            Type ownerType = ((PackageFacadeContext) context).getDelegateToClassType();
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, descriptor, shortNameByAsmType(ownerType));
        }
        else {
            assert declaration != null : "Declaration is null for different context: " + context;
            if (!generateBackingField(declaration, descriptor)) {
                generateSyntheticMethodIfNeeded(descriptor);
            }
        }

        if (isAccessorNeeded(declaration, descriptor, getter)) {
            generateGetter(declaration, descriptor, getter);
        }
        if (isAccessorNeeded(declaration, descriptor, setter)) {
            generateSetter(declaration, descriptor, setter);
        }

        context.recordSyntheticAccessorIfNeeded(descriptor, bindingContext);
    }

    /**
     * Determines if it's necessary to generate an accessor to the property, i.e. if this property can be referenced via getter/setter
     * for any reason
     *
     * @see JvmCodegenUtil#couldUseDirectAccessToProperty
     */
    private boolean isAccessorNeeded(
            @Nullable JetProperty declaration,
            @NotNull PropertyDescriptor descriptor,
            @Nullable JetPropertyAccessor accessor
    ) {
        boolean isDefaultAccessor = accessor == null || !accessor.hasBody();

        // Don't generate accessors for trait properties with default accessors in TRAIT_IMPL
        if (kind == OwnerKind.TRAIT_IMPL && isDefaultAccessor) return false;

        if (declaration == null) return true;

        // Delegated or extension properties can only be referenced via accessors
        if (declaration.hasDelegate() || declaration.getReceiverTypeReference() != null) return true;

        // Default object properties always should have accessors, because their backing fields are moved/copied to the outer class
        if (isDefaultObject(descriptor.getContainingDeclaration())) return true;

        // Private class properties have accessors only in cases when those accessors are non-trivial
        if (kind == OwnerKind.IMPLEMENTATION && Visibilities.isPrivate(descriptor.getVisibility())) {
            return !isDefaultAccessor;
        }

        return true;
    }

    public void generatePrimaryConstructorProperty(JetParameter p, PropertyDescriptor descriptor) {
        generateBackingField(p, descriptor);
        if (!Visibilities.isPrivate(descriptor.getVisibility())) {
            generateGetter(p, descriptor, null);
            if (descriptor.isVar()) {
                generateSetter(p, descriptor, null);
            }
        }
    }

    public void generateConstructorPropertyAsMethodForAnnotationClass(JetParameter p, PropertyDescriptor descriptor) {
        Type type = typeMapper.mapType(descriptor);
        String name = p.getName();
        assert name != null : "Annotation parameter has no name: " + p.getText();
        MethodVisitor mv = v.newMethod(OtherOrigin(p, descriptor), ACC_PUBLIC | ACC_ABSTRACT, name, "()" + type.getDescriptor(), null, null);

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            JetExpression defaultValue = p.getDefaultValue();
            if (defaultValue != null) {
                CompileTimeConstant<?> constant = ExpressionCodegen.getCompileTimeConstant(defaultValue, bindingContext);
                assert constant != null : "Default value for annotation parameter should be compile time value: " + defaultValue.getText();
                AnnotationCodegen annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(mv, typeMapper);
                annotationCodegen.generateAnnotationDefaultValue(constant, descriptor.getType());
            }
        }

        mv.visitEnd();
    }

    private boolean generateBackingField(@NotNull JetNamedDeclaration p, @NotNull PropertyDescriptor descriptor) {
        if (isInterface(descriptor.getContainingDeclaration()) || kind == OwnerKind.TRAIT_IMPL) {
            return false;
        }

        if (Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor))) {
            generateBackingFieldAccess(p, descriptor);
        }
        else if (p instanceof JetProperty && ((JetProperty) p).hasDelegate()) {
            generatePropertyDelegateAccess((JetProperty) p, descriptor);
        }
        else {
            return false;
        }
        return true;
    }

    // Annotations on properties without backing fields are stored in bytecode on an empty synthetic method. This way they're still
    // accessible via reflection, and 'deprecated' and 'private' flags prevent this method from being called accidentally
    private void generateSyntheticMethodIfNeeded(@NotNull PropertyDescriptor descriptor) {
        if (descriptor.getAnnotations().isEmpty()) return;

        ReceiverParameterDescriptor receiver = descriptor.getExtensionReceiverParameter();
        String name = JvmAbi.getSyntheticMethodNameForAnnotatedProperty(descriptor.getName());
        String desc = receiver == null ? "()V" : "(" + typeMapper.mapType(receiver.getType()) + ")V";

        if (!isTrait(context.getContextDescriptor()) || kind == OwnerKind.TRAIT_IMPL) {
            int flags = ACC_DEPRECATED | ACC_FINAL | ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC;
            MethodVisitor mv = v.newMethod(OtherOrigin(descriptor), flags, name, desc, null, null);
            AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(descriptor, Type.VOID_TYPE);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitEnd();
        }
        else {
            Type tImplType = typeMapper.mapTraitImpl((ClassDescriptor) context.getContextDescriptor());
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, descriptor, shortNameByAsmType(tImplType));
        }

        if (kind != OwnerKind.TRAIT_IMPL) {
            v.getSerializationBindings().put(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor, new Method(name, desc));
        }
    }

    private void generateBackingField(JetNamedDeclaration element, PropertyDescriptor propertyDescriptor, boolean isDelegate, JetType jetType, Object defaultValue) {
        int modifiers = getDeprecatedAccessFlag(propertyDescriptor);

        for (AnnotationCodegen.JvmFlagAnnotation flagAnnotation : AnnotationCodegen.FIELD_FLAGS) {
            if (flagAnnotation.hasAnnotation(propertyDescriptor.getOriginal())) {
                modifiers |= flagAnnotation.getJvmFlag();
            }
        }

        if (kind == OwnerKind.PACKAGE) {
            modifiers |= ACC_STATIC;
        }

        if (!propertyDescriptor.isVar() || isDelegate) {
            modifiers |= ACC_FINAL;
        }

        Type type = typeMapper.mapType(jetType);

        ClassBuilder builder = v;

        FieldOwnerContext backingFieldContext = context;
        if (AsmUtil.isInstancePropertyWithStaticBackingField(propertyDescriptor) ) {
            modifiers |= ACC_STATIC | getVisibilityForSpecialPropertyBackingField(propertyDescriptor, isDelegate);
            if (AsmUtil.isPropertyWithBackingFieldInOuterClass(propertyDescriptor)) {
                ImplementationBodyCodegen codegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
                builder = codegen.v;
                backingFieldContext = codegen.context;
                v.getSerializationBindings().put(STATIC_FIELD_IN_OUTER_CLASS, propertyDescriptor);
            }
        }
        else if (kind != OwnerKind.PACKAGE || isDelegate) {
            modifiers |= ACC_PRIVATE;
        }

        if (AsmUtil.isPropertyWithBackingFieldCopyInOuterClass(propertyDescriptor)) {
            ImplementationBodyCodegen parentBodyCodegen = (ImplementationBodyCodegen) memberCodegen.getParentCodegen();
            parentBodyCodegen.addDefaultObjectPropertyToCopy(propertyDescriptor, defaultValue);
        }

        String name = backingFieldContext.getFieldName(propertyDescriptor, isDelegate);

        v.getSerializationBindings().put(FIELD_FOR_PROPERTY, propertyDescriptor, Pair.create(type, name));

        FieldVisitor fv = builder.newField(OtherOrigin(element, propertyDescriptor), modifiers, name, type.getDescriptor(),
                                                typeMapper.mapFieldSignature(jetType), defaultValue);
        AnnotationCodegen.forField(fv, typeMapper).genAnnotations(propertyDescriptor, type);
    }

    private void generatePropertyDelegateAccess(JetProperty p, PropertyDescriptor propertyDescriptor) {
        JetType delegateType = bindingContext.get(BindingContext.EXPRESSION_TYPE, p.getDelegateExpression());
        if (delegateType == null) {
            // If delegate expression is unresolved reference
            delegateType = ErrorUtils.createErrorType("Delegate type");
        }

        generateBackingField(p, propertyDescriptor, true, delegateType, null);
    }

    private void generateBackingFieldAccess(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor) {
        Object value = null;

        if (shouldWriteFieldInitializer(propertyDescriptor)) {
            CompileTimeConstant<?> initializer = propertyDescriptor.getCompileTimeInitializer();
            if (initializer != null) {
                value = initializer.getValue();
            }
        }

        generateBackingField(p, propertyDescriptor, false, propertyDescriptor.getType(), value);
    }

    private boolean shouldWriteFieldInitializer(@NotNull PropertyDescriptor descriptor) {
        //final field of primitive or String type
        if (!descriptor.isVar()) {
            Type type = typeMapper.mapType(descriptor);
            return AsmUtil.isPrimitive(type) || "java.lang.String".equals(type.getClassName());
        }
        return false;
    }

    private void generateGetter(@Nullable JetNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable JetPropertyAccessor getter) {
        generateAccessor(p, getter, descriptor.getGetter() != null
                                    ? descriptor.getGetter()
                                    : DescriptorFactory.createDefaultGetter(descriptor));
    }

    private void generateSetter(@Nullable JetNamedDeclaration p, @NotNull PropertyDescriptor descriptor, @Nullable JetPropertyAccessor setter) {
        if (!descriptor.isVar()) return;

        generateAccessor(p, setter, descriptor.getSetter() != null
                                    ? descriptor.getSetter()
                                    : DescriptorFactory.createDefaultSetter(descriptor));
    }

    private void generateAccessor(
            @Nullable JetNamedDeclaration p,
            @Nullable JetPropertyAccessor accessor,
            @NotNull PropertyAccessorDescriptor accessorDescriptor
    ) {
        FunctionGenerationStrategy strategy;
        if (accessor == null || !accessor.hasBody()) {
            if (p instanceof JetProperty && ((JetProperty) p).hasDelegate()) {
                strategy = new DelegatedPropertyAccessorStrategy(state, accessorDescriptor, indexOfDelegatedProperty((JetProperty) p));
            }
            else {
                strategy = new DefaultPropertyAccessorStrategy(state, accessorDescriptor);
            }
        }
        else {
            strategy = new FunctionGenerationStrategy.FunctionDefault(state, accessorDescriptor, accessor);
        }

        functionCodegen.generateMethod(OtherOrigin(accessor != null ? accessor : p, accessorDescriptor), accessorDescriptor, strategy);
    }

    public static int indexOfDelegatedProperty(@NotNull JetProperty property) {
        PsiElement parent = property.getParent();
        JetDeclarationContainer container;
        if (parent instanceof JetClassBody) {
            container = ((JetClassOrObject) parent.getParent());
        }
        else if (parent instanceof JetFile) {
            container = (JetFile) parent;
        }
        else {
            throw new UnsupportedOperationException("Unknown delegated property container: " + parent);
        }

        int index = 0;
        for (JetDeclaration declaration : container.getDeclarations()) {
            if (declaration instanceof JetProperty && ((JetProperty) declaration).hasDelegate()) {
                if (declaration == property) {
                    return index;
                }
                index++;
            }
        }

        throw new IllegalStateException("Delegated property not found in its parent: " + JetPsiUtil.getElementTextWithContext(property));
    }


    private static class DefaultPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
        public DefaultPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state, descriptor);
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;
            PropertyDescriptor propertyDescriptor = callableDescriptor.getCorrespondingProperty();
            StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, true, null, StackValue.LOCAL_0);

            if (callableDescriptor instanceof PropertyGetterDescriptor) {
                Type type = signature.getReturnType();
                property.put(type, v);
                v.areturn(type);
            }
            else if (callableDescriptor instanceof PropertySetterDescriptor) {
                List<ValueParameterDescriptor> valueParameters = callableDescriptor.getValueParameters();
                assert valueParameters.size() == 1 : "Property setter should have only one value parameter but has " + callableDescriptor;
                int parameterIndex = codegen.lookupLocalIndex(valueParameters.get(0));
                assert parameterIndex >= 0 : "Local index for setter parameter should be positive or zero: " + callableDescriptor;
                Type type = codegen.typeMapper.mapType(propertyDescriptor);
                property.store(StackValue.local(parameterIndex, type), codegen.v);
                v.visitInsn(RETURN);
            }
            else {
                throw new IllegalStateException("Unknown property accessor: " + callableDescriptor);
            }
        }
    }

    public static StackValue invokeDelegatedPropertyConventionMethod(
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull ExpressionCodegen codegen,
            @NotNull JetTypeMapper typeMapper,
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
        else {
            throw new UnsupportedOperationException("Unknown context: " + ownerContext);
        }

        codegen.tempVariables.put(
                resolvedCall.getCall().getValueArguments().get(propertyMetadataArgumentIndex).asElement(),
                new StackValue(PROPERTY_METADATA_TYPE) {
                    @Override
                    public void putSelector(@NotNull Type type, @NotNull InstructionAdapter v) {
                        Field array = StackValue
                                .field(Type.getType("[" + PROPERTY_METADATA_TYPE), owner, JvmAbi.PROPERTY_METADATA_ARRAY_NAME, true,
                                       StackValue.none());
                        StackValue.arrayElement(PROPERTY_METADATA_TYPE, array, StackValue.constant(indexInPropertyMetadataArray, Type.INT_TYPE)).put(type, v);
                    }
                }
        );

        StackValue delegatedProperty = codegen.intermediateValueForProperty(propertyDescriptor, true, null, StackValue.LOCAL_0);
        return codegen.invokeFunction(resolvedCall, delegatedProperty);
    }

    private static class DelegatedPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
        private final int index;

        public DelegatedPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor, int index) {
            super(state, descriptor);
            this.index = index;
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;

            BindingContext bindingContext = state.getBindingContext();
            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, callableDescriptor);
            assert resolvedCall != null : "Resolve call should be recorded for delegate call " + signature.toString();

            StackValue lastValue = invokeDelegatedPropertyConventionMethod(callableDescriptor.getCorrespondingProperty(),
                                                                           codegen, state.getTypeMapper(), resolvedCall, index, 1);
            Type asmType = signature.getReturnType();
            lastValue.put(asmType, v);
            v.areturn(asmType);
        }
    }

    @NotNull
    public static String getterName(Name propertyName) {
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
    }

    @NotNull
    public static String setterName(Name propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
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
