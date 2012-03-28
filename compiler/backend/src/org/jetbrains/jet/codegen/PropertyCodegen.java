/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author max
 */
public class PropertyCodegen {
    private final GenerationState state;
    private final FunctionCodegen functionCodegen;
    private final ClassBuilder v;
    private final OwnerKind kind;

    public PropertyCodegen(CodegenContext context, ClassBuilder v, FunctionCodegen functionCodegen, GenerationState state) {
        this.v = v;
        this.functionCodegen = functionCodegen;
        this.state = state;
        this.kind = context.getContextKind();
    }

    public void gen(JetProperty p) {
        final VariableDescriptor descriptor = state.getBindingContext().get(BindingContext.VARIABLE, p);
        if (!(descriptor instanceof PropertyDescriptor)) {
            throw new UnsupportedOperationException("expect a property to have a property descriptor");
        }
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
        if (kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION || kind ==OwnerKind.TRAIT_IMPL ) {
            if(kind != OwnerKind.TRAIT_IMPL)
                generateBackingField(p, propertyDescriptor);
            generateGetter(p, propertyDescriptor);
            generateSetter(p, propertyDescriptor);
        }
        else if (kind instanceof OwnerKind.DelegateKind) {
            generateDefaultGetter(propertyDescriptor, Opcodes.ACC_PUBLIC, p);
            if (propertyDescriptor.isVar()) {
                generateDefaultSetter(propertyDescriptor, Opcodes.ACC_PUBLIC, p);
            }
        }
    }

    private void generateBackingField(JetProperty p, PropertyDescriptor propertyDescriptor) {
        if (state.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if(CodegenUtil.isInterface(containingDeclaration))
                return;

            Object value = null;
            final JetExpression initializer = p.getInitializer();
            if (initializer != null) {
                if (initializer instanceof JetConstantExpression) {
                    CompileTimeConstant<?> compileTimeValue = state.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, initializer);
                    value = compileTimeValue != null ? compileTimeValue.getValue() : null;
                }
            }
            int modifiers;
            if (kind == OwnerKind.NAMESPACE) {
                int access = JetTypeMapper.getAccessModifiers(propertyDescriptor, 0);
                modifiers = access | Opcodes.ACC_STATIC;
            }
            else {
                modifiers = JetTypeMapper.getAccessModifiers(propertyDescriptor, 0);
            }
            if (!propertyDescriptor.isVar()) {
                modifiers |= Opcodes.ACC_FINAL;
            }
            if(state.getInjector().getJetStandardLibrary().isVolatile(propertyDescriptor)) {
                modifiers |= Opcodes.ACC_VOLATILE;
            }
            FieldVisitor fieldVisitor = v.newField(p, modifiers, p.getName(), state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType()).getDescriptor(), null, value);
            AnnotationCodegen.forField(fieldVisitor).genAnnotations(propertyDescriptor, state.getInjector().getJetTypeMapper());
        }
    }

    private void generateGetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor getter = p.getGetter();
        if (getter != null) {
            if (getter.getBodyExpression() != null) {
                JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapGetterSignature(propertyDescriptor, kind);
                functionCodegen.generateMethod(getter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(), propertyDescriptor.getGetter());
            }
            else if (isExternallyAccessible(propertyDescriptor)) {
                generateDefaultGetter(p);
            }
        }
        else if (isExternallyAccessible(propertyDescriptor)) {
            generateDefaultGetter(p);
        }
    }

    private static boolean isExternallyAccessible(PropertyDescriptor p) {
        return p.getVisibility() != Visibility.PRIVATE || CodegenUtil.isClassObject(p.getContainingDeclaration());
    }

    private void generateSetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor setter = p.getSetter();
        if (setter != null) {
            if (setter.getBodyExpression() != null) {
                final PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
                assert setterDescriptor != null;
                JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapSetterSignature(propertyDescriptor, kind);
                functionCodegen.generateMethod(setter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(), setterDescriptor);
            }
            else if (isExternallyAccessible(propertyDescriptor)) {
                generateDefaultSetter(p);
            }
        }
        else if (isExternallyAccessible(propertyDescriptor) && propertyDescriptor.isVar()) {
            generateDefaultSetter(p);
        }
    }

    private void generateDefaultGetter(JetProperty p) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, p);
        int flags = JetTypeMapper.getAccessModifiers(propertyDescriptor, 0) | (propertyDescriptor.getModality() == Modality.ABSTRACT ? Opcodes.ACC_ABSTRACT : 0);
        generateDefaultGetter(propertyDescriptor, flags, p);
    }

    public void generateDefaultGetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }

        PsiElement psiElement = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if(isTrait && !(kind instanceof OwnerKind.DelegateKind))
            flags |= Opcodes.ACC_ABSTRACT;

        if (propertyDescriptor.getModality() == Modality.FINAL) {
            flags |= Opcodes.ACC_FINAL;
        }

        JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapGetterSignature(propertyDescriptor, kind);
        final String descriptor = signature.getJvmMethodSignature().getAsmMethod().getDescriptor();
        String getterName = getterName(propertyDescriptor.getName());
        MethodVisitor mv = v.newMethod(origin, flags, getterName, descriptor, null, null);
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(), signature.getJvmMethodSignature().getKotlinTypeParameter());

        if(propertyDescriptor.getGetter() != null) {
            assert !propertyDescriptor.getGetter().hasBody();
            AnnotationCodegen.forMethod(mv).genAnnotations(propertyDescriptor.getGetter(), state.getInjector().getJetTypeMapper());
        }

        if (v.generateCode() != ClassBuilder.Mode.SIGNATURES && (!isTrait || kind instanceof OwnerKind.DelegateKind)) {
            if(propertyDescriptor.getModality() != Modality.ABSTRACT)  {
                mv.visitCode();
                if (v.generateCode() == ClassBuilder.Mode.STUBS) {
                    StubCodegen.generateStubThrow(mv);
                } else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    if (kind != OwnerKind.NAMESPACE) {
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                    }
                    final Type type = state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType());
                    if (kind instanceof OwnerKind.DelegateKind) {
                        OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                        dk.getDelegate().put(JetTypeMapper.TYPE_OBJECT, iv);
                        iv.invokeinterface(dk.getOwnerClass(), getterName, descriptor);
                    }
                    else {
                        iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
                                state.getInjector().getJetTypeMapper().getOwner(propertyDescriptor, kind), propertyDescriptor.getName(),
                                type.getDescriptor());
                    }
                    iv.areturn(type);
                }
            }
        }
        FunctionCodegen.endVisit(mv, "getter", origin);
    }

    public static void generateJetPropertyAnnotation(MethodVisitor mv, @NotNull String kotlinType, @NotNull String typeParameters) {
        JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
        aw.writeKind(JvmStdlibNames.JET_METHOD_KIND_PROPERTY);
        aw.writeTypeParameters(typeParameters);
        aw.writePropertyType(kotlinType);
        aw.visitEnd();
    }

    private void generateDefaultSetter(JetProperty p) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, p);
        assert propertyDescriptor != null;

        int modifiers = JetTypeMapper.getAccessModifiers(propertyDescriptor, 0);
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        int flags = setter == null ? modifiers : JetTypeMapper.getAccessModifiers(setter, modifiers);
        generateDefaultSetter(propertyDescriptor, flags, p);
    }

    public void generateDefaultSetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE) {
            flags |= Opcodes.ACC_STATIC;
        }

        PsiElement psiElement = state.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if(isTrait && !(kind instanceof OwnerKind.DelegateKind))
            flags |= Opcodes.ACC_ABSTRACT;

        if (propertyDescriptor.getModality() == Modality.FINAL) {
            flags |= Opcodes.ACC_FINAL;
        }

        JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapSetterSignature(propertyDescriptor, kind);
        final String descriptor = signature.getJvmMethodSignature().getAsmMethod().getDescriptor();
        MethodVisitor mv = v.newMethod(origin, flags, setterName(propertyDescriptor.getName()), descriptor, null, null);
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(), signature.getJvmMethodSignature().getKotlinTypeParameter());

        if(propertyDescriptor.getSetter() != null) {
            assert !propertyDescriptor.getSetter().hasBody();
            AnnotationCodegen.forMethod(mv).genAnnotations(propertyDescriptor.getSetter(), state.getInjector().getJetTypeMapper());
        }

        if (v.generateCode() != ClassBuilder.Mode.SIGNATURES && (!isTrait || kind instanceof OwnerKind.DelegateKind)) {
            if(propertyDescriptor.getModality() != Modality.ABSTRACT)  {
                mv.visitCode();
                if (v.generateCode() == ClassBuilder.Mode.STUBS) {
                    StubCodegen.generateStubThrow(mv);
                } else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    final Type type = state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType());
                    int paramCode = 0;
                    if (kind != OwnerKind.NAMESPACE) {
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        paramCode = 1;
                    }

                    if (kind instanceof OwnerKind.DelegateKind) {
                        OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                        iv.load(0, JetTypeMapper.TYPE_OBJECT);
                        dk.getDelegate().put(JetTypeMapper.TYPE_OBJECT, iv);

                        iv.load(paramCode, type);
                        iv.invokeinterface(dk.getOwnerClass(), setterName(propertyDescriptor.getName()), descriptor);
                    }
                    else {
                        iv.load(paramCode, type);
                        iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD,
                                state.getInjector().getJetTypeMapper().getOwner(propertyDescriptor, kind), propertyDescriptor.getName(),
                                type.getDescriptor());
                    }

                    iv.visitInsn(Opcodes.RETURN);
                }
            }
            FunctionCodegen.endVisit(mv, "setter", origin);
        }
    }

    public static String getterName(String propertyName) {
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }

    public static String setterName(String propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName);
    }

    public void genDelegate(PropertyDescriptor declaration, PropertyDescriptor overriddenDescriptor, StackValue field) {
        JvmPropertyAccessorSignature jvmPropertyAccessorSignature = state.getInjector().getJetTypeMapper().mapGetterSignature(declaration, OwnerKind.IMPLEMENTATION);
        functionCodegen.genDelegate(declaration, overriddenDescriptor, field, jvmPropertyAccessorSignature.getJvmMethodSignature());

        if(declaration.isVar()) {
            jvmPropertyAccessorSignature = state.getInjector().getJetTypeMapper().mapSetterSignature(declaration, OwnerKind.IMPLEMENTATION);
            functionCodegen.genDelegate(declaration, overriddenDescriptor, field, jvmPropertyAccessorSignature.getJvmMethodSignature());
        }
    }
}
