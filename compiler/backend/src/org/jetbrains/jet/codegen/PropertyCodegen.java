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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.getDeprecatedAccessFlag;
import static org.jetbrains.jet.codegen.AsmUtil.getVisibilityForSpecialPropertyBackingField;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class PropertyCodegen extends GenerationStateAware {
    @NotNull
    private final FunctionCodegen functionCodegen;

    @NotNull
    private final ClassBuilder v;

    @NotNull
    private final FieldOwnerContext context;

    @Nullable
    private MemberCodegen classBodyCodegen;

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
        PropertyDescriptor propertyDescriptor = DescriptorUtils.getPropertyDescriptor(p, bindingContext);
        assert kind instanceof OwnerKind.StaticDelegateKind || kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.TRAIT_IMPL
                : "Generating property with a wrong kind (" + kind + "): " + propertyDescriptor;

        if (kind != OwnerKind.TRAIT_IMPL && !(kind instanceof OwnerKind.StaticDelegateKind)) {
            generateBackingField(p, propertyDescriptor);
        }
        generateGetter(p, propertyDescriptor, p.getGetter());
        generateSetter(p, propertyDescriptor, p.getSetter());

        context.recordSyntheticAccessorIfNeeded(propertyDescriptor, typeMapper);
    }

    public void generatePrimaryConstructorProperty(JetParameter p, PropertyDescriptor descriptor) {
        generateBackingField(p, descriptor);
        generateGetter(p, descriptor, null);
        if (descriptor.isVar()) {
            generateSetter(p, descriptor, null);
        }
    }

    private void generateBackingField(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor) {
        //noinspection ConstantConditions
        boolean hasBackingField = bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
        boolean isDelegated = p instanceof JetProperty && ((JetProperty) p).getDelegateExpression() != null;
        if (hasBackingField || isDelegated) {
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if (isInterface(containingDeclaration)) {
                return;
            }

            FieldVisitor fieldVisitor = hasBackingField
                           ? generateBackingFieldAccess(p, propertyDescriptor)
                           : generatePropertyDelegateAccess((JetProperty) p, propertyDescriptor);

            AnnotationCodegen.forField(fieldVisitor, typeMapper).genAnnotations(propertyDescriptor);
        }

    }

    private FieldVisitor generateBackingField(JetNamedDeclaration element, PropertyDescriptor propertyDescriptor, boolean isDelegate, JetType jetType, Object defaultValue) {
        int modifiers = getDeprecatedAccessFlag(propertyDescriptor);

        if (KotlinBuiltIns.getInstance().isVolatile(propertyDescriptor)) {
            modifiers |= ACC_VOLATILE;
        }

        if (kind == OwnerKind.NAMESPACE) {
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
        } else {
            if (kind != OwnerKind.NAMESPACE || isDelegate) {
                modifiers |= ACC_PRIVATE;
            }
        }

        if (AsmUtil.isPropertyWithBackingFieldCopyInOuterClass(propertyDescriptor)) {
            ImplementationBodyCodegen parentBodyCodegen = getParentBodyCodegen(classBodyCodegen);
            parentBodyCodegen.addClassObjectPropertyToCopy(propertyDescriptor, defaultValue);
        }

        String name = backingFieldContext.getFieldName(propertyDescriptor, isDelegate);

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
            JetExpression initializer = p instanceof JetProperty ? ((JetProperty) p).getInitializer() : null;
            if (initializer != null) {
                CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, initializer);
                value = compileTimeValue != null ? compileTimeValue.getValue() : null;
            }
        }

        return generateBackingField(p, propertyDescriptor, false, propertyDescriptor.getType(), value);
    }

    private void generateGetter(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor, JetPropertyAccessor getter) {
        boolean defaultGetter = getter == null || getter.getBodyExpression() == null;

        //TODO: Now it's not enough information to properly resolve property from bytecode without generated getter and setter
        //if (!defaultGetter || isExternallyAccessible(propertyDescriptor)) {
        JvmPropertyAccessorSignature signature = typeMapper.mapGetterSignature(propertyDescriptor, kind);
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        getterDescriptor = getterDescriptor != null ? getterDescriptor : DescriptorResolver.createDefaultGetter(propertyDescriptor);

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
                                           true,
                                           getterDescriptor,
                                           strategy);
        }
        //}
    }

    private void generateSetter(JetNamedDeclaration p, PropertyDescriptor propertyDescriptor, JetPropertyAccessor setter) {
        boolean defaultSetter = setter == null || setter.getBodyExpression() == null;

        //TODO: Now it's not enough information to properly resolve property from bytecode without generated getter and setter
        if (/*!defaultSetter || isExternallyAccessible(propertyDescriptor) &&*/ propertyDescriptor.isVar()) {
            JvmPropertyAccessorSignature signature = typeMapper.mapSetterSignature(propertyDescriptor, kind);
            PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
            setterDescriptor = setterDescriptor != null ? setterDescriptor : DescriptorResolver.createDefaultSetter(propertyDescriptor);

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
                                               true,
                                               setterDescriptor,
                                               strategy);
            }
        }
    }


    private static class DefaultPropertyAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {

        public DefaultPropertyAccessorStrategy(
                @NotNull GenerationState state,
                @NotNull PropertyAccessorDescriptor callableDescriptor
        ) {
            super(state, callableDescriptor);
        }

        @Override
        public void doGenerateBody(
                ExpressionCodegen codegen, JvmMethodSignature signature
        ) {
            generateDefaultAccessor(callableDescriptor, codegen.v, codegen);
        }
    }

    private static void generateDefaultAccessor(
            @NotNull PropertyAccessorDescriptor accessorDescriptor,
            @NotNull InstructionAdapter iv,
            @NotNull ExpressionCodegen codegen
    ) {
        JetTypeMapper typeMapper = codegen.typeMapper;
        CodegenContext context = codegen.context;
        OwnerKind kind = context.getContextKind();

        PropertyDescriptor propertyDescriptor = accessorDescriptor.getCorrespondingProperty();
        Type type = typeMapper.mapType(propertyDescriptor);

        int paramCode = 0;
        if (kind != OwnerKind.NAMESPACE) {
            iv.load(0, OBJECT_TYPE);
            paramCode = 1;
        }

        StackValue property = codegen.intermediateValueForProperty(accessorDescriptor.getCorrespondingProperty(), true, null);

        if (accessorDescriptor instanceof PropertyGetterDescriptor) {
            property.put(type, iv);
            iv.areturn(type);
        }
        else if (accessorDescriptor instanceof  PropertySetterDescriptor) {
            ReceiverParameterDescriptor receiverParameter = propertyDescriptor.getReceiverParameter();
            if (receiverParameter != null) {
                paramCode += typeMapper.mapType(receiverParameter.getType()).getSize();
            }
            iv.load(paramCode, type);

            property.store(type, iv);
            iv.visitInsn(RETURN);
        } else {
            assert false : "Unreachable state";
        }
    }

    private static class DefaultPropertyWithDelegateAccessorStrategy extends FunctionGenerationStrategy.CodegenBased<PropertyAccessorDescriptor> {
        public DefaultPropertyWithDelegateAccessorStrategy(@NotNull GenerationState state, @NotNull PropertyAccessorDescriptor descriptor) {
            super(state, descriptor);
        }

        @Override
        public void doGenerateBody(
                @NotNull ExpressionCodegen codegen, @NotNull JvmMethodSignature signature
        ) {
            JetTypeMapper typeMapper = codegen.typeMapper;
            OwnerKind kind = codegen.context.getContextKind();
            InstructionAdapter iv = codegen.v;
            BindingContext bindingContext = state.getBindingContext();

            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, callableDescriptor);

            Call call = bindingContext.get(BindingContext.DELEGATED_PROPERTY_CALL, callableDescriptor);
            assert call != null : "Call should be recorded for delegate call " + signature.toString();

            PropertyDescriptor property = callableDescriptor.getCorrespondingProperty();
            Type asmType = typeMapper.mapType(property);

            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, OBJECT_TYPE);
            }

            StackValue delegatedProperty = codegen.intermediateValueForProperty(property, true, null);
            StackValue lastValue = codegen.invokeFunction(call, delegatedProperty, resolvedCall);

            if (lastValue.type != Type.VOID_TYPE) {
                lastValue.put(asmType, iv);
                iv.areturn(asmType);
            }
            else {
                iv.areturn(Type.VOID_TYPE);
            }
        }
    }

    public static void generateJetPropertyAnnotation(
            MethodVisitor mv, @NotNull JvmPropertyAccessorSignature propertyAccessorSignature,
            @NotNull PropertyDescriptor propertyDescriptor, @NotNull Visibility visibility
    ) {
        JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
        Modality modality = propertyDescriptor.getModality();
        int flags = getFlagsForVisibility(visibility) | JvmStdlibNames.FLAG_PROPERTY_BIT;
        if (isInterface(propertyDescriptor.getContainingDeclaration()) && modality != Modality.ABSTRACT) {
            flags |= modality == Modality.FINAL
                      ? JvmStdlibNames.FLAG_FORCE_FINAL_BIT
                      : JvmStdlibNames.FLAG_FORCE_OPEN_BIT;
        }
        aw.writeFlags(flags | DescriptorKindUtils.kindToFlags(propertyDescriptor.getKind()));
        aw.writeTypeParameters(propertyAccessorSignature.getKotlinTypeParameter());
        aw.writePropertyType(propertyAccessorSignature.getPropertyTypeKotlinSignature());
        aw.visitEnd();
    }

    public static String getterName(Name propertyName) {
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
    }

    public static String setterName(Name propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.asString());
    }

    public void genDelegate(PropertyDescriptor delegate, PropertyDescriptor overridden, StackValue field) {
        ClassDescriptor toClass = (ClassDescriptor) overridden.getContainingDeclaration();

        functionCodegen.genDelegate(delegate.getGetter(), toClass, field,
                                    typeMapper.mapGetterSignature(delegate, OwnerKind.IMPLEMENTATION),
                                    typeMapper.mapGetterSignature(overridden.getOriginal(), OwnerKind.IMPLEMENTATION));

        if (delegate.isVar()) {
            functionCodegen.genDelegate(delegate.getSetter(), toClass, field,
                                        typeMapper.mapSetterSignature(delegate, OwnerKind.IMPLEMENTATION),
                                        typeMapper.mapSetterSignature(overridden.getOriginal(), OwnerKind.IMPLEMENTATION));
        }
    }
}
