/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.context.PackageFacadeContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.getParentBodyCodegen;
import static org.jetbrains.jet.codegen.CodegenUtil.isInterface;
import static org.jetbrains.jet.codegen.JvmSerializationBindings.*;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isTrait;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class PropertyCodegen extends GenerationStateAware {
    @NotNull
    private final FunctionCodegen functionCodegen;

    @NotNull
    private final ClassBuilder v;

    @NotNull
    private final FieldOwnerContext context;

    @Nullable
    private final MemberCodegen classBodyCodegen;

    @NotNull
    private final OwnerKind kind;

    public PropertyCodegen(
            @NotNull FieldOwnerContext context,
            @NotNull ClassBuilder v,
            @NotNull FunctionCodegen functionCodegen,
            @Nullable MemberCodegen classBodyCodegen
    ) {
        super(functionCodegen.getState());
        this.v = v;
        this.functionCodegen = functionCodegen;
        this.context = context;
        this.classBodyCodegen = classBodyCodegen;
        this.kind = context.getContextKind();
    }

    public void gen(JetProperty p) {
        VariableDescriptor variableDescriptor = bindingContext.get(BindingContext.VARIABLE, p);
        assert variableDescriptor instanceof PropertyDescriptor : "Property should have a property descriptor: " + variableDescriptor;

        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variableDescriptor;
        assert kind == OwnerKind.PACKAGE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.TRAIT_IMPL
                : "Generating property with a wrong kind (" + kind + "): " + propertyDescriptor;


        if (context instanceof PackageFacadeContext) {
            Type ownerType = ((PackageFacadeContext) context).getDelegateToClassType();
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, propertyDescriptor, shortNameByAsmType(ownerType));
        }
        else if (!generateBackingField(p, propertyDescriptor)) {
            generateSyntheticMethodIfNeeded(propertyDescriptor);
        }

        generateGetter(p, propertyDescriptor, p.getGetter());
        generateSetter(p, propertyDescriptor, p.getSetter());

        context.recordSyntheticAccessorIfNeeded(propertyDescriptor, bindingContext);
    }

    public void generatePrimaryConstructorProperty(JetParameter p, PropertyDescriptor descriptor) {
        generateBackingField(p, descriptor);
        generateGetter(p, descriptor, null);
        if (descriptor.isVar()) {
            generateSetter(p, descriptor, null);
        }
    }

    public void generateConstructorPropertyAsMethodForAnnotationClass(JetParameter p, PropertyDescriptor descriptor) {
        Type type = state.getTypeMapper().mapType(descriptor);
        String name = p.getName();
        assert name != null : "Annotation parameter has no name: " + p.getText();
        MethodVisitor visitor = v.newMethod(p, ACC_PUBLIC | ACC_ABSTRACT, name, "()" + type.getDescriptor(), null, null);
        JetExpression defaultValue = p.getDefaultValue();
        if (defaultValue != null) {
            CompileTimeConstant<?> constant = ExpressionCodegen.getCompileTimeConstant(defaultValue, state.getBindingContext());
            assert constant != null : "Default value for annotation parameter should be compile time value: " + defaultValue.getText();
            AnnotationCodegen annotationCodegen = AnnotationCodegen.forAnnotationDefaultValue(visitor, typeMapper);
            annotationCodegen.generateAnnotationDefaultValue(constant, descriptor.getType());
        }
    }

    private boolean generateBackingField(@NotNull JetNamedDeclaration p, @NotNull PropertyDescriptor descriptor) {
        if (isInterface(descriptor.getContainingDeclaration()) || kind == OwnerKind.TRAIT_IMPL) {
            return false;
        }

        FieldVisitor fv;
        if (Boolean.TRUE.equals(bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, descriptor))) {
            fv = generateBackingFieldAccess(p, descriptor);
        }
        else if (p instanceof JetProperty && ((JetProperty) p).getDelegateExpression() != null) {
            fv = generatePropertyDelegateAccess((JetProperty) p, descriptor);
        }
        else {
            return false;
        }

        AnnotationCodegen.forField(fv, typeMapper).genAnnotations(descriptor);
        return true;
    }

    // Annotations on properties without backing fields are stored in bytecode on an empty synthetic method. This way they're still
    // accessible via reflection, and 'deprecated' and 'private' flags prevent this method from being called accidentally
    private void generateSyntheticMethodIfNeeded(@NotNull PropertyDescriptor descriptor) {
        if (descriptor.getAnnotations().isEmpty()) return;

        ReceiverParameterDescriptor receiver = descriptor.getReceiverParameter();
        Type receiverAsmType = receiver == null ? null : typeMapper.mapType(receiver.getType());
        Method method = JvmAbi.getSyntheticMethodSignatureForAnnotatedProperty(descriptor.getName(), receiverAsmType);

        if (!isTrait(context.getContextDescriptor()) || kind == OwnerKind.TRAIT_IMPL) {
            MethodVisitor mv = v.newMethod(null,
                                           ACC_DEPRECATED | ACC_FINAL | ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                                           method.getName(),
                                           method.getDescriptor(),
                                           null,
                                           null);
            AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(descriptor);
            mv.visitCode();
            mv.visitInsn(Opcodes.RETURN);
            mv.visitEnd();
        }
        else {
            Type tImplType = typeMapper.mapTraitImpl((ClassDescriptor) context.getContextDescriptor());
            v.getSerializationBindings().put(IMPL_CLASS_NAME_FOR_CALLABLE, descriptor, shortNameByAsmType(tImplType));
        }

        if (kind != OwnerKind.TRAIT_IMPL) {
            v.getSerializationBindings().put(SYNTHETIC_METHOD_FOR_PROPERTY, descriptor, method);
        }
    }

    private FieldVisitor generateBackingField(JetNamedDeclaration element, PropertyDescriptor propertyDescriptor, boolean isDelegate, JetType jetType, Object defaultValue) {
        int modifiers = getDeprecatedAccessFlag(propertyDescriptor);

        if (propertyDescriptor.getOriginal().getAnnotations().findAnnotation(AnnotationCodegen.VOLATILE_FQ_NAME) != null) {
            modifiers |= ACC_VOLATILE;
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
        if (AsmUtil.isPropertyWithBackingFieldInOuterClass(propertyDescriptor)) {
            modifiers |= ACC_STATIC | getVisibilityForSpecialPropertyBackingField(propertyDescriptor, isDelegate);
            ImplementationBodyCodegen codegen = getParentBodyCodegen(classBodyCodegen);
            builder = codegen.v;
            backingFieldContext = codegen.context;
            v.getSerializationBindings().put(STATIC_FIELD_IN_OUTER_CLASS, propertyDescriptor);
        } else {
            if (kind != OwnerKind.PACKAGE || isDelegate) {
                modifiers |= ACC_PRIVATE;
            }
        }

        if (AsmUtil.isPropertyWithBackingFieldCopyInOuterClass(propertyDescriptor)) {
            ImplementationBodyCodegen parentBodyCodegen = getParentBodyCodegen(classBodyCodegen);
            parentBodyCodegen.addClassObjectPropertyToCopy(propertyDescriptor, defaultValue);
        }

        String name = backingFieldContext.getFieldName(propertyDescriptor, isDelegate);

        v.getSerializationBindings().put(FIELD_FOR_PROPERTY, propertyDescriptor, Pair.create(type, name));

        return builder.newField(element, modifiers, name, type.getDescriptor(),
                                typeMapper.mapFieldSignature(jetType), defaultValue);
    }

    private FieldVisitor generatePropertyDelegateAccess(JetProperty p, PropertyDescriptor propertyDescriptor) {
        JetType delegateType = bindingContext.get(BindingContext.EXPRESSION_TYPE, p.getDelegateExpression());
        if (delegateType == null) {
            // If delegate expression is unresolved reference
            delegateType = ErrorUtils.createErrorType("Delegate type");
        }

        return generateBackingField(p, propertyDescriptor, true, delegateType, null);
    }

    private FieldVisitor generateBackingFieldAccess(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor) {
        Object value = null;

        if (ImplementationBodyCodegen.shouldWriteFieldInitializer(propertyDescriptor, typeMapper)) {
            CompileTimeConstant<?> initializer = propertyDescriptor.getCompileTimeInitializer();
            if (initializer != null) {
                value = initializer.getValue();
            }
        }

        return generateBackingField(p, propertyDescriptor, false, propertyDescriptor.getType(), value);
    }

    private void generateGetter(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor, JetPropertyAccessor getter) {
        boolean defaultGetter = getter == null || getter.getBodyExpression() == null;

        //TODO: Now it's not enough information to properly resolve property from bytecode without generated getter and setter
        //if (!defaultGetter || isExternallyAccessible(propertyDescriptor)) {
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        getterDescriptor = getterDescriptor != null ? getterDescriptor : DescriptorFactory.createDefaultGetter(propertyDescriptor);
        JvmMethodSignature signature = typeMapper.mapSignature(getterDescriptor, kind);

        if (kind != OwnerKind.TRAIT_IMPL || !defaultGetter) {
            FunctionGenerationStrategy strategy;
            if (defaultGetter) {
                if (p instanceof JetProperty && ((JetProperty) p).getDelegateExpression() != null) {
                    strategy = new DefaultPropertyWithDelegateAccessorStrategy(state, getterDescriptor);
                }
                else {
                    strategy = new DefaultPropertyAccessorStrategy(state, getterDescriptor);
                }
            }
            else {
                strategy = new FunctionGenerationStrategy.FunctionDefault(state, getterDescriptor, getter);
            }
            functionCodegen.generateMethod(getter != null ? getter : p,
                                           signature,
                                           getterDescriptor,
                                           strategy);
        }
        //}
    }

    private void generateSetter(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor, JetPropertyAccessor setter) {
        boolean defaultSetter = setter == null || setter.getBodyExpression() == null;

        //TODO: Now it's not enough information to properly resolve property from bytecode without generated getter and setter
        if (/*!defaultSetter || isExternallyAccessible(propertyDescriptor) &&*/ propertyDescriptor.isVar()) {
            PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
            setterDescriptor = setterDescriptor != null ? setterDescriptor : DescriptorFactory.createDefaultSetter(propertyDescriptor);
            JvmMethodSignature signature = typeMapper.mapSignature(setterDescriptor, kind);

            if (kind != OwnerKind.TRAIT_IMPL || !defaultSetter) {
                FunctionGenerationStrategy strategy;
                if (defaultSetter) {
                    if (p instanceof JetProperty && ((JetProperty) p).getDelegateExpression() != null) {
                        strategy = new DefaultPropertyWithDelegateAccessorStrategy(state, setterDescriptor);
                    }
                    else {
                        strategy = new DefaultPropertyAccessorStrategy(state, setterDescriptor);
                    }
                }
                else {
                    strategy = new FunctionGenerationStrategy.FunctionDefault(state, setterDescriptor, setter);
                }
                functionCodegen.generateMethod(setter != null ? setter : p,
                                               signature,
                                               setterDescriptor,
                                               strategy);
            }
        }
    }


    private static class DefaultPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
        public DefaultPropertyAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state, descriptor);
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;
            PropertyDescriptor propertyDescriptor = callableDescriptor.getCorrespondingProperty();

            int paramCode = 0;
            if (codegen.context.getContextKind() != OwnerKind.PACKAGE) {
                v.load(0, OBJECT_TYPE);
                paramCode = 1;
            }

            StackValue property = codegen.intermediateValueForProperty(propertyDescriptor, true, null);

            if (callableDescriptor instanceof PropertyGetterDescriptor) {
                Type type = signature.getReturnType();
                property.put(type, v);
                v.areturn(type);
            }
            else if (callableDescriptor instanceof PropertySetterDescriptor) {
                ReceiverParameterDescriptor receiverParameter = propertyDescriptor.getReceiverParameter();
                if (receiverParameter != null) {
                    paramCode += codegen.typeMapper.mapType(receiverParameter.getType()).getSize();
                }
                Type type = codegen.typeMapper.mapType(propertyDescriptor);
                v.load(paramCode, type);
                property.store(type, v);
                v.visitInsn(RETURN);
            } else {
                throw new IllegalStateException("Unknown property accessor: " + callableDescriptor);
            }
        }
    }

    private static class DefaultPropertyWithDelegateAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
        public DefaultPropertyWithDelegateAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state, descriptor);
        }

        @Override
        public void doGenerateBody(@NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature) {
            InstructionAdapter v = codegen.v;

            BindingContext bindingContext = state.getBindingContext();
            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, callableDescriptor);

            Call call = bindingContext.get(BindingContext.DELEGATED_PROPERTY_CALL, callableDescriptor);
            assert call != null : "Call should be recorded for delegate call " + signature.toString();

            if (codegen.context.getContextKind() != OwnerKind.PACKAGE) {
                v.load(0, OBJECT_TYPE);
            }

            PropertyDescriptor propertyDescriptor = callableDescriptor.getCorrespondingProperty();

            StackValue delegatedProperty = codegen.intermediateValueForProperty(propertyDescriptor, true, null);
            StackValue lastValue = codegen.invokeFunction(call, delegatedProperty, resolvedCall);

            Type asmType = signature.getReturnType();
            lastValue.put(asmType, v);
            v.areturn(asmType);
        }
    }

    public static String getterName(Name propertyName) {
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
    }

    public static String setterName(Name propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
    }

    public void genDelegate(@NotNull PropertyDescriptor delegate, @NotNull PropertyDescriptor overridden, @NotNull StackValue field) {
        ClassDescriptor toClass = (ClassDescriptor) overridden.getContainingDeclaration();

        PropertyGetterDescriptor getter = delegate.getGetter();
        if (getter != null) {
            //noinspection ConstantConditions
            functionCodegen.genDelegate(getter, toClass, field,
                                        typeMapper.mapSignature(getter), typeMapper.mapSignature(overridden.getGetter().getOriginal()));
        }

        PropertySetterDescriptor setter = delegate.getSetter();
        if (setter != null) {
            //noinspection ConstantConditions
            functionCodegen.genDelegate(setter, toClass, field,
                                        typeMapper.mapSignature(setter), typeMapper.mapSignature(overridden.getSetter().getOriginal()));
        }
    }
}
