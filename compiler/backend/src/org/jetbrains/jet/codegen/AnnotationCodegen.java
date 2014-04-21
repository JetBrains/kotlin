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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.org.objectweb.asm.*;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetAnnotationEntry;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetModifierList;
import org.jetbrains.jet.lang.psi.JetModifierListOwner;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContextUtils.descriptorToDeclaration;

public abstract class AnnotationCodegen {
    public static final FqName VOLATILE_FQ_NAME = new FqName("kotlin.volatile");

    private static final AnnotationVisitor NO_ANNOTATION_VISITOR = new AnnotationVisitor(Opcodes.ASM5) {};

    private final JetTypeMapper typeMapper;
    private final BindingContext bindingContext;

    private AnnotationCodegen(JetTypeMapper mapper) {
        typeMapper = mapper;
        bindingContext = typeMapper.getBindingContext();
    }

    public void genAnnotations(Annotated annotated) {
        if (annotated == null) {
            return;
        }

        if (!(annotated instanceof DeclarationDescriptor)) {
            return;
        }

        PsiElement psiElement = descriptorToDeclaration(bindingContext, (DeclarationDescriptor) annotated);

        JetModifierList modifierList = null;
        if (annotated instanceof ConstructorDescriptor && psiElement instanceof JetClass) {
            modifierList = ((JetClass) psiElement).getPrimaryConstructorModifierList();
        }
        else if (psiElement instanceof JetModifierListOwner) {
            modifierList = ((JetModifierListOwner) psiElement).getModifierList();
        }

        if (modifierList == null) {
            generateAdditionalAnnotations(annotated, Collections.<String>emptySet());
            return;
        }

        Set<String> annotationDescriptorsAlreadyPresent = new HashSet<String>();

        List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
        for (JetAnnotationEntry annotationEntry : annotationEntries) {
            ResolvedCall<?> resolvedCall = bindingContext.get(BindingContext.RESOLVED_CALL, annotationEntry.getCalleeExpression());
            if (resolvedCall == null) continue; // Skipping annotations if they are not resolved. Needed for JetLightClass generation

            AnnotationDescriptor annotationDescriptor = bindingContext.get(BindingContext.ANNOTATION, annotationEntry);
            if (annotationDescriptor == null) continue; // Skipping annotations if they are not resolved. Needed for JetLightClass generation
            if (isVolatile(annotationDescriptor)) continue;

            String descriptor = genAnnotation(annotationDescriptor);
            if (descriptor != null) {
                annotationDescriptorsAlreadyPresent.add(descriptor);
            }
        }

        generateAdditionalAnnotations(annotated, annotationDescriptorsAlreadyPresent);
    }

    private void generateAdditionalAnnotations(@NotNull Annotated annotated, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        if (annotated instanceof CallableDescriptor) {
            CallableDescriptor descriptor = (CallableDescriptor) annotated;

            // No need to annotate privates, synthetic accessors and their parameters
            if (isInvisibleFromTheOutside(descriptor)) return;
            if (descriptor instanceof ValueParameterDescriptor && isInvisibleFromTheOutside(descriptor.getContainingDeclaration())) return;

            generateNullabilityAnnotation(descriptor.getReturnType(), annotationDescriptorsAlreadyPresent);
        }
    }

    private static boolean isInvisibleFromTheOutside(@Nullable DeclarationDescriptor descriptor) {
        if (descriptor instanceof CallableMemberDescriptor && JetTypeMapper.isAccessor((CallableMemberDescriptor) descriptor)) return false;
        if (descriptor instanceof MemberDescriptor) {
            return AsmUtil.getVisibilityAccessFlag((MemberDescriptor) descriptor) == Opcodes.ACC_PRIVATE;
        }
        return false;
    }

    private void generateNullabilityAnnotation(@Nullable JetType type, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        if (type == null) return;

        if (isBareTypeParameterWithNullableUpperBound(type)) {
            // This is to account for the case of, say
            //   class Function<R> { fun invoke(): R }
            // it would be a shame to put @Nullable on the return type of the function, and force all callers to check for null,
            // so we put no annotations
            return;
        }

        boolean isNullableType = JvmCodegenUtil.isNullableType(type);
        if (!isNullableType && KotlinBuiltIns.getInstance().isPrimitiveType(type)) return;

        Class<?> annotationClass = isNullableType ? Nullable.class : NotNull.class;

        String descriptor = Type.getType(annotationClass).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.contains(descriptor)) {
            visitAnnotation(descriptor, false).visitEnd();
        }
    }

    private static boolean isBareTypeParameterWithNullableUpperBound(@NotNull JetType type) {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        return !type.isNullable() && classifier instanceof TypeParameterDescriptor && TypeUtils.hasNullableSuperType(type);
    }

    private static boolean isVolatile(@NotNull AnnotationDescriptor annotationDescriptor) {
        ClassifierDescriptor classDescriptor = annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        return classDescriptor != null && DescriptorUtils.getFqName(classDescriptor).equals(VOLATILE_FQ_NAME.toUnsafe());
    }

    public void generateAnnotationDefaultValue(@NotNull CompileTimeConstant value, @NotNull JetType expectedType) {
        AnnotationVisitor visitor = visitAnnotation(null, false);  // Parameters are unimportant
        genCompileTimeValue(null, value, expectedType, visitor);
        visitor.visitEnd();
    }

    @Nullable
    private String genAnnotation(@NotNull AnnotationDescriptor annotationDescriptor) {
        ClassifierDescriptor classifierDescriptor = annotationDescriptor.getType().getConstructor().getDeclarationDescriptor();
        assert classifierDescriptor != null : "Annotation descriptor has no class: " + annotationDescriptor;
        RetentionPolicy rp = getRetentionPolicy(classifierDescriptor);
        if (rp == RetentionPolicy.SOURCE) {
            return null;
        }

        String descriptor = typeMapper.mapType(annotationDescriptor.getType()).getDescriptor();
        AnnotationVisitor annotationVisitor = visitAnnotation(descriptor, rp == RetentionPolicy.RUNTIME);

        genAnnotationArguments(annotationDescriptor, annotationVisitor);
        annotationVisitor.visitEnd();

        return descriptor;
    }

    private void genAnnotationArguments(AnnotationDescriptor annotationDescriptor, AnnotationVisitor annotationVisitor) {
        for (Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>> entry : annotationDescriptor.getAllValueArguments().entrySet()) {
            ValueParameterDescriptor descriptor = entry.getKey();
            String name = descriptor.getName().asString();
            genCompileTimeValue(name, entry.getValue(), descriptor.getType(), annotationVisitor);
        }
    }

    private void genCompileTimeValue(
            @Nullable final String name,
            @NotNull CompileTimeConstant<?> value,
            @NotNull final JetType expectedType,
            @NotNull final AnnotationVisitor annotationVisitor
    ) {
        AnnotationArgumentVisitor argumentVisitor = new AnnotationArgumentVisitor<Void, Void>() {
            @Override
            public Void visitLongValue(@NotNull LongValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitIntValue(IntValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitShortValue(ShortValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitByteValue(ByteValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitDoubleValue(DoubleValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitFloatValue(FloatValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitBooleanValue(BooleanValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitCharValue(CharValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitStringValue(StringValue value, Void data) {
                return visitSimpleValue(value);
            }

            @Override
            public Void visitEnumValue(EnumValue value, Void data) {
                String propertyName = value.getValue().getName().asString();
                annotationVisitor.visitEnum(name, typeMapper.mapType(value.getType(KotlinBuiltIns.getInstance())).getDescriptor(), propertyName);
                return null;
            }

            @Override
            public Void visitArrayValue(ArrayValue value, Void data) {
                AnnotationVisitor visitor = annotationVisitor.visitArray(name);
                for (CompileTimeConstant<?> argument : value.getValue()) {
                    genCompileTimeValue(null, argument, value.getType(KotlinBuiltIns.getInstance()), visitor);
                }
                visitor.visitEnd();
                return null;
            }

            @Override
            public Void visitAnnotationValue(AnnotationValue value, Void data) {
                String internalAnnName = typeMapper.mapType(value.getValue().getType()).getDescriptor();
                AnnotationVisitor visitor = annotationVisitor.visitAnnotation(name, internalAnnName);
                genAnnotationArguments(value.getValue(), visitor);
                visitor.visitEnd();
                return null;
            }

            @Override
            public Void visitJavaClassValue(JavaClassValue value, Void data) {
                annotationVisitor.visit(name, typeMapper.mapType(value.getValue()));
                return null;
            }

            @Override
            public Void visitNumberTypeValue(IntegerValueTypeConstant value, Void data) {
                Object numberType = value.getValue(expectedType);
                annotationVisitor.visit(name, numberType);
                return null;
            }

            private Void visitSimpleValue(CompileTimeConstant value) {
                annotationVisitor.visit(name, value.getValue());
                return null;
            }

            @Override
            public Void visitErrorValue(ErrorValue value, Void data) {
                return visitUnsupportedValue(value);
            }

            @Override
            public Void visitNullValue(NullValue value, Void data) {
                return visitUnsupportedValue(value);
            }

            private Void visitUnsupportedValue(CompileTimeConstant value) {
                throw new IllegalStateException("Don't know how to compile annotation value " + value);
            }
        };

        value.accept(argumentVisitor, null);
    }

    @NotNull
    private RetentionPolicy getRetentionPolicy(@NotNull Annotated descriptor) {
        AnnotationDescriptor retentionAnnotation = descriptor.getAnnotations().findAnnotation(new FqName(Retention.class.getName()));
        if (retentionAnnotation == null) return RetentionPolicy.CLASS;

        Collection<CompileTimeConstant<?>> valueArguments = retentionAnnotation.getAllValueArguments().values();
        assert valueArguments.size() == 1 : "Retention should have an argument: " + retentionAnnotation + " on " + descriptor;
        CompileTimeConstant<?> compileTimeConstant = valueArguments.iterator().next();
        assert compileTimeConstant instanceof EnumValue : "Retention argument should be enum value: " + compileTimeConstant;
        ClassDescriptor enumEntry = ((EnumValue) compileTimeConstant).getValue();
        JetType classObjectType = enumEntry.getClassObjectType();
        assert classObjectType != null : "Enum entry should have a class object: " + enumEntry;
        assert "java/lang/annotation/RetentionPolicy".equals(typeMapper.mapType(classObjectType).getInternalName()) :
                "Retention argument should be of type RetentionPolicy: " + enumEntry;
        return RetentionPolicy.valueOf(enumEntry.getName().asString());
    }

    @NotNull
    abstract AnnotationVisitor visitAnnotation(String descr, boolean visible);

    public static AnnotationCodegen forClass(final ClassVisitor cv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(cv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forMethod(final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forField(final FieldVisitor fv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(fv.visitAnnotation(descr, visible));
            }
        };
    }

    public static AnnotationCodegen forParameter(final int parameter, final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitParameterAnnotation(parameter, descr, visible));
            }
        };
    }

    public static AnnotationCodegen forAnnotationDefaultValue(final MethodVisitor mv, JetTypeMapper mapper) {
        return new AnnotationCodegen(mapper) {
            @NotNull
            @Override
            AnnotationVisitor visitAnnotation(String descr, boolean visible) {
                return safe(mv.visitAnnotationDefault());
            }
        };
    }

    @NotNull
    private static AnnotationVisitor safe(@Nullable AnnotationVisitor av) {
        return av == null ? NO_ANNOTATION_VISITOR : av;
    }
}
