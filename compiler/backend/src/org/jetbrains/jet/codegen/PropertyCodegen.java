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
import org.jetbrains.asm4.FieldVisitor;
import org.jetbrains.asm4.MethodVisitor;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.InstructionAdapter;
import org.jetbrains.jet.codegen.context.CodegenContext;
import org.jetbrains.jet.codegen.signature.JvmPropertyAccessorSignature;
import org.jetbrains.jet.codegen.signature.kotlin.JetMethodAnnotationWriter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;

import java.util.BitSet;

import static org.jetbrains.asm4.Opcodes.*;
import static org.jetbrains.jet.codegen.AsmTypeConstants.*;
import static org.jetbrains.jet.codegen.CodegenUtil.*;
import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;

/**
 * @author max
 * @author alex.tkachman
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
        if (kind == OwnerKind.NAMESPACE || kind == OwnerKind.IMPLEMENTATION || kind == OwnerKind.TRAIT_IMPL) {
            if (kind != OwnerKind.TRAIT_IMPL) {
                generateBackingField(p, propertyDescriptor);
            }
            generateGetter(p, propertyDescriptor);
            generateSetter(p, propertyDescriptor);
        }
        else if (kind instanceof OwnerKind.DelegateKind) {
            generateDefaultGetter(propertyDescriptor, ACC_PUBLIC, p);
            if (propertyDescriptor.isVar()) {
                generateDefaultSetter(propertyDescriptor, ACC_PUBLIC, p);
            }
        }
    }

    public void generateBackingField(PsiElement p, PropertyDescriptor propertyDescriptor) {
        //noinspection ConstantConditions
        if (state.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
            DeclarationDescriptor containingDeclaration = propertyDescriptor.getContainingDeclaration();
            if (isInterface(containingDeclaration)) {
                return;
            }

            Object value = null;
            final JetExpression initializer = p instanceof JetProperty ? ((JetProperty) p).getInitializer() : null;
            if (initializer != null) {
                if (initializer instanceof JetConstantExpression) {
                    CompileTimeConstant<?> compileTimeValue = state.getBindingContext().get(BindingContext.COMPILE_TIME_VALUE, initializer);
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
            if (JetStandardLibrary.getInstance().isVolatile(propertyDescriptor)) {
                modifiers |= ACC_VOLATILE;
            }
            Type type = state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType(), MapTypeMode.VALUE);
            FieldVisitor fieldVisitor = v.newField(p, modifiers, propertyDescriptor.getName().getName(), type.getDescriptor(), null, value);
            AnnotationCodegen.forField(fieldVisitor, state.getInjector().getJetTypeMapper()).genAnnotations(propertyDescriptor);
        }
    }

    private void generateGetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor getter = p.getGetter();
        if (getter != null) {
            if (getter.getBodyExpression() != null) {
                JvmPropertyAccessorSignature signature =
                        state.getInjector().getJetTypeMapper().mapGetterSignature(propertyDescriptor, kind);
                functionCodegen.generateMethod(getter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(),
                                               propertyDescriptor.getGetter());
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
        return p.getVisibility() != Visibilities.PRIVATE || DescriptorUtils.isClassObject(p.getContainingDeclaration())
               || p.getContainingDeclaration() instanceof NamespaceDescriptor;
    }

    private void generateSetter(JetProperty p, PropertyDescriptor propertyDescriptor) {
        final JetPropertyAccessor setter = p.getSetter();
        if (setter != null) {
            if (setter.getBodyExpression() != null) {
                final PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
                assert setterDescriptor != null;
                JvmPropertyAccessorSignature signature =
                        state.getInjector().getJetTypeMapper().mapSetterSignature(propertyDescriptor, kind);
                functionCodegen.generateMethod(setter, signature.getJvmMethodSignature(), true, signature.getPropertyTypeKotlinSignature(),
                                               setterDescriptor);
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
        assert propertyDescriptor != null;
        int flags = getAccessModifiers(propertyDescriptor, 0) |
                    (propertyDescriptor.getModality() == Modality.ABSTRACT
                     ? ACC_ABSTRACT
                     : (propertyDescriptor.getModality() == Modality.FINAL ? ACC_FINAL : 0));
        generateDefaultGetter(propertyDescriptor, flags, p);
    }

    public void generateDefaultGetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        if (propertyDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            throw new IllegalStateException("must not generate code for fake overrides");
        }

        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE) {
            flags |= ACC_STATIC;
        }

        PsiElement psiElement = descriptorToDeclaration(state.getBindingContext(), propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if (isTrait && !(kind instanceof OwnerKind.DelegateKind)) {
            flags |= ACC_ABSTRACT;
        }

        if (propertyDescriptor.getModality() == Modality.FINAL) {
            flags |= ACC_FINAL;
        }

        JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapGetterSignature(propertyDescriptor, kind);
        final String descriptor = signature.getJvmMethodSignature().getAsmMethod().getDescriptor();
        String getterName = getterName(propertyDescriptor.getName());
        MethodVisitor mv = v.newMethod(origin, flags, getterName, descriptor, null, null);
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(),
                                      signature.getJvmMethodSignature().getKotlinTypeParameter(), propertyDescriptor,
                                      getter == null
                                      ? propertyDescriptor.getVisibility()
                                      : getter.getVisibility());

        if (getter != null) {
            //noinspection ConstantConditions
            assert !getter.hasBody();
            AnnotationCodegen.forMethod(mv, state.getInjector().getJetTypeMapper()).genAnnotations(getter);
        }

        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES && (!isTrait || kind instanceof OwnerKind.DelegateKind)) {
            if (propertyDescriptor.getModality() != Modality.ABSTRACT) {
                mv.visitCode();
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    StubCodegen.generateStubThrow(mv);
                }
                else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    if (kind != OwnerKind.NAMESPACE) {
                        iv.load(0, OBJECT_TYPE);
                    }
                    final Type type = state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType(), MapTypeMode.VALUE);

                    if ((kind instanceof OwnerKind.DelegateKind) != (propertyDescriptor.getKind() == FunctionDescriptor.Kind.DELEGATION)) {
                        throw new IllegalStateException("mismatching kind in " + propertyDescriptor);
                    }

                    if (kind instanceof OwnerKind.DelegateKind) {
                        OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                        dk.getDelegate().put(OBJECT_TYPE, iv);
                        iv.invokeinterface(dk.getOwnerClass(), getterName, descriptor);
                    }
                    else {
                        iv.visitFieldInsn(
                                kind == OwnerKind.NAMESPACE ? GETSTATIC : GETFIELD,
                                state.getInjector().getJetTypeMapper().getOwner(propertyDescriptor, kind).getInternalName(),
                                propertyDescriptor.getName().getName(),
                                type.getDescriptor());
                    }
                    iv.areturn(type);
                }
            }
        }
        FunctionCodegen.endVisit(mv, "getter", origin);
    }

    public static void generateJetPropertyAnnotation(
            MethodVisitor mv, @NotNull String kotlinType, @NotNull String typeParameters,
            @NotNull PropertyDescriptor propertyDescriptor, @NotNull Visibility visibility
    ) {
        JetMethodAnnotationWriter aw = JetMethodAnnotationWriter.visitAnnotation(mv);
        Modality modality = propertyDescriptor.getModality();
        BitSet flags = getFlagsForVisibility(visibility);
        flags.set(JvmStdlibNames.FLAG_PROPERTY_BIT);
        if (isInterface(propertyDescriptor.getContainingDeclaration()) && modality != Modality.ABSTRACT) {
            flags.set(modality == Modality.FINAL
                      ? JvmStdlibNames.FLAG_FORCE_FINAL_BIT
                      : JvmStdlibNames.FLAG_FORCE_OPEN_BIT);
        }
        aw.writeFlags(flags);
        aw.writeTypeParameters(typeParameters);
        aw.writePropertyType(kotlinType);
        aw.visitEnd();
    }

    private void generateDefaultSetter(JetProperty p) {
        final PropertyDescriptor propertyDescriptor = (PropertyDescriptor) state.getBindingContext().get(BindingContext.VARIABLE, p);
        assert propertyDescriptor != null;

        int modifiers = getAccessModifiers(propertyDescriptor, 0);
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        int flags = setter == null ? modifiers : getAccessModifiers(setter, modifiers);
        flags |= (propertyDescriptor.getModality() == Modality.ABSTRACT ? ACC_ABSTRACT : 0);
        generateDefaultSetter(propertyDescriptor, flags, p);
    }

    public void generateDefaultSetter(PropertyDescriptor propertyDescriptor, int flags, PsiElement origin) {
        if (propertyDescriptor.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) {
            throw new IllegalStateException("must not generate code for fake overrides");
        }

        if (kind == OwnerKind.TRAIT_IMPL) {
            return;
        }

        if (kind == OwnerKind.NAMESPACE) {
            flags |= ACC_STATIC;
        }

        PsiElement psiElement = descriptorToDeclaration(state.getBindingContext(), propertyDescriptor.getContainingDeclaration());
        boolean isTrait = psiElement instanceof JetClass && ((JetClass) psiElement).isTrait();
        if (isTrait && !(kind instanceof OwnerKind.DelegateKind)) {
            flags |= ACC_ABSTRACT;
        }

        if (propertyDescriptor.getModality() == Modality.FINAL) {
            flags |= ACC_FINAL;
        }

        JvmPropertyAccessorSignature signature = state.getInjector().getJetTypeMapper().mapSetterSignature(propertyDescriptor, kind);
        assert true;
        final String descriptor = signature.getJvmMethodSignature().getAsmMethod().getDescriptor();
        MethodVisitor mv = v.newMethod(origin, flags, setterName(propertyDescriptor.getName()), descriptor, null, null);
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        assert setter != null;
        generateJetPropertyAnnotation(mv, signature.getPropertyTypeKotlinSignature(),
                                      signature.getJvmMethodSignature().getKotlinTypeParameter(), propertyDescriptor,
                                      setter.getVisibility());

        assert !setter.hasBody();
        AnnotationCodegen.forMethod(mv, state.getInjector().getJetTypeMapper()).genAnnotations(setter);

        if (state.getClassBuilderMode() != ClassBuilderMode.SIGNATURES && (!isTrait || kind instanceof OwnerKind.DelegateKind)) {
            if (propertyDescriptor.getModality() != Modality.ABSTRACT) {
                mv.visitCode();
                if (state.getClassBuilderMode() == ClassBuilderMode.STUBS) {
                    StubCodegen.generateStubThrow(mv);
                }
                else {
                    InstructionAdapter iv = new InstructionAdapter(mv);
                    final Type type = state.getInjector().getJetTypeMapper().mapType(propertyDescriptor.getType(), MapTypeMode.VALUE);
                    int paramCode = 0;
                    if (kind != OwnerKind.NAMESPACE) {
                        iv.load(0, OBJECT_TYPE);
                        paramCode = 1;
                    }

                    if ((kind instanceof OwnerKind.DelegateKind) != (propertyDescriptor.getKind() == FunctionDescriptor.Kind.DELEGATION)) {
                        throw new IllegalStateException("mismatching kind in " + propertyDescriptor);
                    }

                    if (kind instanceof OwnerKind.DelegateKind) {
                        OwnerKind.DelegateKind dk = (OwnerKind.DelegateKind) kind;
                        iv.load(0, OBJECT_TYPE);
                        dk.getDelegate().put(OBJECT_TYPE, iv);

                        iv.load(paramCode, type);
                        iv.invokeinterface(dk.getOwnerClass(), setterName(propertyDescriptor.getName()), descriptor);
                    }
                    else {
                        iv.load(paramCode, type);
                        iv.visitFieldInsn(kind == OwnerKind.NAMESPACE ? PUTSTATIC : PUTFIELD,
                                          state.getInjector().getJetTypeMapper().getOwner(propertyDescriptor, kind).getInternalName(),
                                          propertyDescriptor.getName().getName(),
                                          type.getDescriptor());
                    }

                    iv.visitInsn(RETURN);
                }
            }
            FunctionCodegen.endVisit(mv, "setter", origin);
        }
    }

    public static String getterName(Name propertyName) {
        return JvmAbi.GETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.getName());
    }

    public static String setterName(Name propertyName) {
        return JvmAbi.SETTER_PREFIX + StringUtil.capitalizeWithJavaBeanConvention(propertyName.getName());
    }

    public void genDelegate(PropertyDescriptor declaration, PropertyDescriptor overriddenDescriptor, StackValue field) {
        JvmPropertyAccessorSignature jvmPropertyAccessorSignature =
                state.getInjector().getJetTypeMapper().mapGetterSignature(declaration, OwnerKind.IMPLEMENTATION);
        functionCodegen.genDelegate(declaration, overriddenDescriptor, field, jvmPropertyAccessorSignature.getJvmMethodSignature());

        if (declaration.isVar()) {
            jvmPropertyAccessorSignature = state.getInjector().getJetTypeMapper().mapSetterSignature(declaration, OwnerKind.IMPLEMENTATION);
            functionCodegen.genDelegate(declaration, overriddenDescriptor, field, jvmPropertyAccessorSignature.getJvmMethodSignature());
        }
    }
}
