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
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.context.MethodContext;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.getDeprecatedAccessFlag;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;

public class PropertyCodegen extends GenerationStateAware {
    private final FunctionCodegen functionCodegen;
    private final ClassBuilder v;
    private final CodegenContext context;
    private final OwnerKind kind;

    public PropertyCodegen(CodegenContext context, ClassBuilder v, FunctionCodegen functionCodegen) {
        super(functionCodegen.getState());
        this.v = v;
        this.functionCodegen = functionCodegen;
        this.context = context;
        this.kind = context.getContextKind();
    }

    public void gen(JetProperty p) {
        VariableDescriptor descriptor = bindingContext.get(BindingContext.VARIABLE, p);
        if (!(descriptor instanceof PropertyDescriptor)) {
            throw new UnsupportedOperationException("expect a property to have a property descriptor");
        }
        PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        assert kind instanceof OwnerKind.StaticDelegateKind || kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.TRAIT_IMPL
                : "Generating property with a wrong kind (" + kind + "): " + descriptor;

        if (kind != OwnerKind.TRAIT_IMPL && !(kind instanceof OwnerKind.StaticDelegateKind)) {
            generateBackingField(p, propertyDescriptor);
        }
        generateGetter(p, propertyDescriptor, p.getGetter());
        generateSetter(p, propertyDescriptor, p.getSetter());
    }

    public void generatePrimaryConstructorProperty(JetParameter p, PropertyDescriptor descriptor) {
        generateBackingField(p, descriptor);
        generateGetter(p, descriptor, null);
        if (descriptor.isVar()) {
            generateSetter(p, descriptor, null);
        }
    }

    private void generateBackingField(PsiElement p, PropertyDescriptor propertyDescriptor) {
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

    private FieldVisitor generatePropertyDelegateAccess(JetProperty p, PropertyDescriptor propertyDescriptor) {
        int modifiers = ACC_PRIVATE | ACC_FINAL | getDeprecatedAccessFlag(propertyDescriptor);
        if (kind == OwnerKind.NAMESPACE) {
            modifiers |= ACC_STATIC;
        }
        if (KotlinBuiltIns.getInstance().isVolatile(propertyDescriptor)) {
            modifiers |= ACC_VOLATILE;
        }
        JetType delegateType = bindingContext.get(BindingContext.EXPRESSION_TYPE, p.getDelegateExpression());
        assert delegateType != null : "Type of delegate should be recorded: " + p.getText();
        Type type = typeMapper.mapType(delegateType);
        return v.newField(p, modifiers, JvmAbi.getPropertyDelegateName(propertyDescriptor.getName()), type.getDescriptor(),
                          null, null);
    }

    private FieldVisitor generateBackingFieldAccess(PsiElement p, PropertyDescriptor propertyDescriptor) {
        Object value = null;
        JetExpression initializer = p instanceof JetProperty ? ((JetProperty) p).getInitializer() : null;
        if (initializer != null) {
            if (initializer instanceof JetConstantExpression && !propertyDescriptor.getType().isNullable()) {
                CompileTimeConstant<?> compileTimeValue = bindingContext.get(BindingContext.COMPILE_TIME_VALUE, initializer);
                value = compileTimeValue != null ? compileTimeValue.getValue() : null;
            }
        }
        int modifiers;
        if (kind == OwnerKind.NAMESPACE) {
            modifiers = ACC_STATIC;
        }
        else {
            modifiers = ACC_PRIVATE;
        }
        if (!propertyDescriptor.isVar()) {
            modifiers |= ACC_FINAL;
        }
        modifiers |= getDeprecatedAccessFlag(propertyDescriptor);
        if (KotlinBuiltIns.getInstance().isVolatile(propertyDescriptor)) {
            modifiers |= ACC_VOLATILE;
        }
        Type type = typeMapper.mapType(propertyDescriptor);
        return v.newField(p, modifiers, propertyDescriptor.getName().getName(), type.getDescriptor(), null, value);
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
                    strategy = new DefaultPropertyWithDelegateAccessorStrategy(getterDescriptor);
                }
                else {
                    strategy = new DefaultPropertyAccessorStrategy(getterDescriptor);
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
                        strategy = new DefaultPropertyWithDelegateAccessorStrategy(setterDescriptor);
                    }
                    else {
                        strategy = new DefaultPropertyAccessorStrategy(setterDescriptor);
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


    private class DefaultPropertyAccessorStrategy extends FunctionGenerationStrategy {
        private final PropertyAccessorDescriptor descriptor;

        public DefaultPropertyAccessorStrategy(@NotNull PropertyAccessorDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context
        ) {
            generateDefaultAccessor(descriptor, new InstructionAdapter(mv), typeMapper, context);
        }
    }

    private static void generateDefaultAccessor(
            @NotNull PropertyAccessorDescriptor accessorDescriptor,
            @NotNull InstructionAdapter iv,
            @NotNull JetTypeMapper typeMapper,
            @NotNull CodegenContext context
    ) {
        OwnerKind kind = context.getContextKind();

        PropertyDescriptor propertyDescriptor = accessorDescriptor.getCorrespondingProperty();
        Type type = typeMapper.mapType(propertyDescriptor);
        if (accessorDescriptor instanceof PropertyGetterDescriptor) {
            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, OBJECT_TYPE);
            }
            iv.visitFieldInsn(
                    kind == OwnerKind.NAMESPACE ? GETSTATIC : GETFIELD,
                    typeMapper.getOwner(propertyDescriptor, kind, isCallInsideSameModuleAsDeclared(propertyDescriptor, context)).getInternalName(),
                    propertyDescriptor.getName().getName(),
                    type.getDescriptor());
            iv.areturn(type);
        }
        else if (accessorDescriptor instanceof  PropertySetterDescriptor) {
            int paramCode = 0;
            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, OBJECT_TYPE);
                paramCode = 1;
            }
            ReceiverParameterDescriptor receiverParameter = propertyDescriptor.getReceiverParameter();
            if (receiverParameter != null) {
                paramCode += typeMapper.mapType(receiverParameter.getType()).getSize();
            }
            iv.load(paramCode, type);
            iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? PUTSTATIC : PUTFIELD,
                              typeMapper.getOwner(propertyDescriptor, kind, isCallInsideSameModuleAsDeclared(propertyDescriptor, context)).getInternalName(),
                              propertyDescriptor.getName().getName(),
                              type.getDescriptor());

            iv.visitInsn(RETURN);
        } else {
            assert false : "Unreachable state";
        }
    }

    private class DefaultPropertyWithDelegateAccessorStrategy extends FunctionGenerationStrategy {
        private final PropertyAccessorDescriptor descriptor;

        public DefaultPropertyWithDelegateAccessorStrategy(@NotNull PropertyAccessorDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public void generateBody(
                @NotNull MethodVisitor mv,
                @NotNull JvmMethodSignature signature,
                @NotNull MethodContext context
        ) {
            InstructionAdapter iv = new InstructionAdapter(mv);
            ExpressionCodegen codegen = new ExpressionCodegen(
                    mv, getFrameMap(typeMapper, context), signature.getAsmMethod().getReturnType(), context, state);

            ResolvedCall<FunctionDescriptor> resolvedCall =
                    bindingContext.get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, descriptor);

            Call call = bindingContext.get(BindingContext.DELEGATED_PROPERTY_CALL, descriptor);
            assert call != null : "Call should be recorded for delegate call " + signature.toString();

            PropertyDescriptor property = descriptor.getCorrespondingProperty();
            Type asmType = typeMapper.mapType(property);

            if (kind != OwnerKind.NAMESPACE) {
                iv.load(0, OBJECT_TYPE);
            }

            StackValue.Property delegatedProperty = codegen.intermediateValueForProperty(property, true, null);
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
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.getName());
    }

    public static String setterName(Name propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.getName());
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
