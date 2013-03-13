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
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.state.GenerationStateAware;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.java.kt.DescriptorKindUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmUtil.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;
import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isExternallyAccessible;
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
        generateGetter(p, propertyDescriptor);
        generateSetter(p, propertyDescriptor);
    }

    public void generatePrimaryConstructorProperty(JetParameter p, PropertyDescriptor descriptor) {
        generateBackingField(p, descriptor);
        int accessFlags = getVisibilityAccessFlag(descriptor) | getModalityAccessFlag(descriptor) | getDeprecatedAccessFlag(descriptor);
        generateDefaultGetter(descriptor, accessFlags, p);
        if (descriptor.isVar()) {
            generateDefaultSetter(descriptor, accessFlags, p);
        }
    }

    private void generateBackingField(PsiElement p, PropertyDescriptor propertyDescriptor) {
        //noinspection ConstantConditions
        if (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if (isInterface(containingDeclaration)) {
                return;
            }

            Object value = null;
            JetExpression initializer = p instanceof JetProperty ? ((JetProperty) p).getInitializer() : null;
            if (initializer != null) {
                if (initializer instanceof JetConstantExpression) {
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
            FieldVisitor fieldVisitor = v.newField(p, modifiers, propertyDescriptor.getName().getName(), type.getDescriptor(), null, value);
            AnnotationCodegen.forField(fieldVisitor, typeMapper).genAnnotations(propertyDescriptor);
        }
    }

    private void generateGetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        JetPropertyAccessor getter = p.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getter.getBodyExpression() != null) {
            JvmPropertyAccessorSignature signature = typeMapper.mapGetterSignature(propertyDescriptor, kind);
            functionCodegen.generateMethod(getter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(),
                                           getterDescriptor);
        }
        else if (isExternallyAccessible(propertyDescriptor)) {
            int flags = getVisibilityAccessFlag(propertyDescriptor);
            flags |= getModalityAccessFlag(propertyDescriptor);
            flags |= getterDescriptor == null ? getDeprecatedAccessFlag(propertyDescriptor): getDeprecatedAccessFlag(getterDescriptor);
            generateDefaultGetter(propertyDescriptor, flags, p);
        }
    }

    private void generateSetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        JetPropertyAccessor setter = p.getSetter();
        if (setter != null && setter.getBodyExpression() != null) {
            JvmPropertyAccessorSignature signature = typeMapper.mapSetterSignature(propertyDescriptor, kind);
            functionCodegen.generateMethod(setter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(),
                                           propertyDescriptor.getSetter());
        }
        else if (isExternallyAccessible(propertyDescriptor) && propertyDescriptor.isVar()) {
            PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
            int flags = getModalityAccessFlag(propertyDescriptor);
            if (setterDescriptor == null) {
                flags |= getVisibilityAccessFlag(propertyDescriptor);
                flags |= getDeprecatedAccessFlag(propertyDescriptor);
            }
            else {
                flags |= getVisibilityAccessFlag(setterDescriptor);
                flags |= getDeprecatedAccessFlag(setterDescriptor);
            }
            generateDefaultSetter(propertyDescriptor, flags, p);
        }
    }

    private void generateDefaultGetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        checkMustGenerateCode(propertyDescriptor);

        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE || kind instanceof OwnerKind.StaticDelegateKind) {
            flags |= ACC_STATIC;
        }

        PsiElement psiElement = descriptorToDeclaration(bindingContext, propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if (isTrait) {
            flags |= ACC_ABSTRACT;
        }

        JvmPropertyAccessorSignature signature = typeMapper.mapGetterSignature(propertyDescriptor, kind);
        JvmMethodSignature jvmMethodSignature = signature.getJvmMethodSignature();
        String descriptor = jvmMethodSignature.getAsmMethod().getDescriptor();
        String getterName = getterName(propertyDescriptor.getName());
        MethodVisitor mv = v.newMethod(origin, flags, getterName, descriptor, jvmMethodSignature.getGenericsSignature(), null);
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(),
                                      jvmMethodSignature.getKotlinTypeParameter(), propertyDescriptor,
                                      getter == null
                                      ? propertyDescriptor.getVisibility()
                                      : getter.getVisibility());

        if (getter != null) {
            //noinspection ConstantConditions
            assert !getter.hasBody();
            AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(getter);
        }

        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES && !isTrait) {
            if (propertyDescriptor.getModality() != Modality.ABSTRACT) {
                mv.visitCode();
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    genStubThrow(mv);
                }
                else if (kind instanceof OwnerKind.StaticDelegateKind) {
                    FunctionCodegen.generateStaticDelegateMethodBody(mv, jvmMethodSignature.getAsmMethod(), (OwnerKind.StaticDelegateKind) kind);
                }
                else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    if (kind != OwnerKind.NAMESPACE) {
                        iv.load(0, OBJECT_TYPE);
                    }
                    Type type = typeMapper.mapType(propertyDescriptor);

                    iv.visitFieldInsn(
                            kind == OwnerKind.NAMESPACE ? GETSTATIC : GETFIELD,
                            typeMapper.getOwner(propertyDescriptor, kind, isCallInsideSameModuleAsDeclared(propertyDescriptor, context)).getInternalName(),
                            propertyDescriptor.getName().getName(),
                            type.getDescriptor());
                    iv.areturn(type);
                }
            }
        }
        FunctionCodegen.endVisit(mv, "getter", origin);

        FunctionCodegen.generateBridgeIfNeeded(context, state, v, jvmMethodSignature.getAsmMethod(), getter);
    }

    public static void generateJetPropertyAnnotation(
            MethodVisitor mv, @NotNull String kotlinType, @NotNull String typeParameters,
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
        aw.writeTypeParameters(typeParameters);
        aw.writePropertyType(kotlinType);
        aw.visitEnd();
    }

    private void generateDefaultSetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        checkMustGenerateCode(propertyDescriptor);

        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE || kind instanceof OwnerKind.StaticDelegateKind) {
            flags |= ACC_STATIC;
        }

        PsiElement psiElement = descriptorToDeclaration(bindingContext, propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if (isTrait) {
            flags |= ACC_ABSTRACT;
        }

        JvmPropertyAccessorSignature signature = typeMapper.mapSetterSignature(propertyDescriptor, kind);
        assert true;
        JvmMethodSignature jvmMethodSignature = signature.getJvmMethodSignature();
        String descriptor = jvmMethodSignature.getAsmMethod().getDescriptor();
        MethodVisitor mv = v.newMethod(origin, flags, setterName(propertyDescriptor.getName()), descriptor, jvmMethodSignature.getGenericsSignature(), null);
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        assert setter != null;
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(),
                                      jvmMethodSignature.getKotlinTypeParameter(), propertyDescriptor,
                                      setter.getVisibility());

        assert !setter.hasBody();
        AnnotationCodegen.forMethod(mv, typeMapper).genAnnotations(setter);

        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES && (!isTrait)) {
            if (propertyDescriptor.getModality() != Modality.ABSTRACT) {
                mv.visitCode();
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    genStubThrow(mv);
                }
                else if (kind instanceof OwnerKind.StaticDelegateKind) {
                    FunctionCodegen.generateStaticDelegateMethodBody(mv, jvmMethodSignature.getAsmMethod(), (OwnerKind.StaticDelegateKind) kind);
                }
                else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    Type type = typeMapper.mapType(propertyDescriptor);
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
                }
            }
            FunctionCodegen.endVisit(mv, "setter", origin);

            FunctionCodegen.generateBridgeIfNeeded(context, state, v, jvmMethodSignature.getAsmMethod(), setter);
        }
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
                                    typeMapper.mapGetterSignature(delegate, OwnerKind.IMPLEMENTATION).getJvmMethodSignature(),
                                    typeMapper.mapGetterSignature(overridden.getOriginal(), OwnerKind.IMPLEMENTATION).getJvmMethodSignature());

        if (delegate.isVar()) {
            functionCodegen.genDelegate(delegate.getSetter(), toClass, field,
                                        typeMapper.mapSetterSignature(delegate, OwnerKind.IMPLEMENTATION).getJvmMethodSignature(),
                                        typeMapper.mapSetterSignature(overridden.getOriginal(), OwnerKind.IMPLEMENTATION).getJvmMethodSignature());
        }
    }
}
