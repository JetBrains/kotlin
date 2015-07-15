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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.load.java.JvmAnnotationNames;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.resolve.constants.*;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.kotlin.types.Flexibility;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.TypesPackage;
import org.jetbrains.org.objectweb.asm.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.*;

import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage.getClassObjectType;

public abstract class AnnotationCodegen {

    public static final class JvmFlagAnnotation {
        private final FqName fqName;
        private final int jvmFlag;

        public JvmFlagAnnotation(@NotNull String fqName, int jvmFlag) {
            this.fqName = new FqName(fqName);
            this.jvmFlag = jvmFlag;
        }

        public boolean hasAnnotation(@NotNull Annotated annotated) {
            return annotated.getAnnotations().findAnnotation(fqName) != null;
        }

        public int getJvmFlag() {
            return jvmFlag;
        }
    }

    public static final List<JvmFlagAnnotation> FIELD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation("kotlin.jvm.volatile", Opcodes.ACC_VOLATILE),
            new JvmFlagAnnotation("kotlin.jvm.transient", Opcodes.ACC_TRANSIENT)
    );

    public static final List<JvmFlagAnnotation> METHOD_FLAGS = Arrays.asList(
            new JvmFlagAnnotation("kotlin.jvm.strictfp", Opcodes.ACC_STRICT),
            new JvmFlagAnnotation("kotlin.jvm.synchronized", Opcodes.ACC_SYNCHRONIZED),
            new JvmFlagAnnotation("kotlin.jvm.native", Opcodes.ACC_NATIVE)
    );

    private static final AnnotationVisitor NO_ANNOTATION_VISITOR = new AnnotationVisitor(Opcodes.ASM5) {};

    private final JetTypeMapper typeMapper;

    private AnnotationCodegen(JetTypeMapper mapper) {
        typeMapper = mapper;
    }

    /**
     * @param returnType can be null if not applicable (e.g. {@code annotated} is a class)
     */
    public void genAnnotations(@Nullable Annotated annotated, @Nullable Type returnType) {
        if (annotated == null) {
            return;
        }

        if (!(annotated instanceof DeclarationDescriptor)) {
            return;
        }

        Set<String> annotationDescriptorsAlreadyPresent = new HashSet<String>();

        for (AnnotationDescriptor annotation : annotated.getAnnotations()) {
            String descriptor = genAnnotation(annotation);
            if (descriptor != null) {
                annotationDescriptorsAlreadyPresent.add(descriptor);
            }
        }

        generateAdditionalAnnotations(annotated, returnType, annotationDescriptorsAlreadyPresent);
    }

    private void generateAdditionalAnnotations(
            @NotNull Annotated annotated,
            @Nullable Type returnType,
            @NotNull Set<String> annotationDescriptorsAlreadyPresent
    ) {
        if (annotated instanceof CallableDescriptor) {
            CallableDescriptor descriptor = (CallableDescriptor) annotated;

            // No need to annotate privates, synthetic accessors and their parameters
            if (isInvisibleFromTheOutside(descriptor)) return;
            if (descriptor instanceof ValueParameterDescriptor && isInvisibleFromTheOutside(descriptor.getContainingDeclaration())) return;

            if (returnType != null && !AsmUtil.isPrimitive(returnType)) {
                generateNullabilityAnnotation(descriptor.getReturnType(), annotationDescriptorsAlreadyPresent);
            }
        }
        if (annotated instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) annotated;
            if (classDescriptor.getKind() == ClassKind.ANNOTATION_CLASS) {
                generateRetentionAnnotation(classDescriptor, annotationDescriptorsAlreadyPresent);
            }
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

        if (TypesPackage.isFlexible(type)) {
            // A flexible type whose lower bound in not-null and upper bound is nullable, should not be annotated
            Flexibility flexibility = TypesPackage.flexibility(type);

            if (!TypeUtils.isNullableType(flexibility.getLowerBound()) && TypeUtils.isNullableType(flexibility.getUpperBound())) {
                AnnotationDescriptor notNull = type.getAnnotations().findAnnotation(JvmAnnotationNames.JETBRAINS_NOT_NULL_ANNOTATION);
                if (notNull != null) {
                    generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, NotNull.class);
                }
                return;
            }
        }

        boolean isNullableType = TypeUtils.isNullableType(type);

        Class<?> annotationClass = isNullableType ? Nullable.class : NotNull.class;

        generateAnnotationIfNotPresent(annotationDescriptorsAlreadyPresent, annotationClass);
    }

    private void generateRetentionAnnotation(@NotNull ClassDescriptor classDescriptor, @NotNull Set<String> annotationDescriptorsAlreadyPresent) {
        RetentionPolicy policy = getRetentionPolicy(classDescriptor);
        String descriptor = Type.getType(Retention.class).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.add(descriptor)) return;
        AnnotationVisitor visitor = visitAnnotation(descriptor, true);
        visitor.visitEnum("value", Type.getType(RetentionPolicy.class).getDescriptor(), policy.name());
        visitor.visitEnd();
    }

    private void generateAnnotationIfNotPresent(Set<String> annotationDescriptorsAlreadyPresent, Class<?> annotationClass) {
        String descriptor = Type.getType(annotationClass).getDescriptor();
        if (!annotationDescriptorsAlreadyPresent.contains(descriptor)) {
            visitAnnotation(descriptor, false).visitEnd();
        }
    }

    private static boolean isBareTypeParameterWithNullableUpperBound(@NotNull JetType type) {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        return !type.isMarkedNullable() && classifier instanceof TypeParameterDescriptor && TypeUtils.hasNullableSuperType(type);
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
        if (rp == RetentionPolicy.SOURCE && typeMapper.getClassBuilderMode() != ClassBuilderMode.LIGHT_CLASSES) {
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
            public Void visitKClassValue(KClassValue value, Void data) {
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

    private enum KotlinRetention {
        SOURCE(RetentionPolicy.SOURCE),
        BINARY(RetentionPolicy.CLASS),
        RUNTIME(RetentionPolicy.RUNTIME);

        final RetentionPolicy mapped;

        KotlinRetention(RetentionPolicy mapped) {
            this.mapped = mapped;
        }
    }

    @NotNull
    private RetentionPolicy getRetentionPolicy(@NotNull Annotated descriptor) {
        AnnotationDescriptor kotlinAnnotation = descriptor.getAnnotations().findAnnotation(KotlinBuiltIns.FQ_NAMES.annotation);
        if (kotlinAnnotation != null) {
            for (Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>> argument: kotlinAnnotation.getAllValueArguments().entrySet()) {
                if ("retention".equals(argument.getKey().getName().asString()) && argument.getValue() instanceof EnumValue) {
                    ClassDescriptor enumEntry = ((EnumValue) argument.getValue()).getValue();
                    JetType classObjectType = getClassObjectType(enumEntry);
                    if (classObjectType != null) {
                        if ("kotlin/annotation/AnnotationRetention".equals(typeMapper.mapType(classObjectType).getInternalName())) {
                            String entryName = enumEntry.getName().asString();
                            for (KotlinRetention retention: KotlinRetention.values()) {
                                if (retention.name().equals(entryName)) return retention.mapped;
                            }
                        }
                    }
                }
            }
        }
        AnnotationDescriptor retentionAnnotation = descriptor.getAnnotations().findAnnotation(new FqName(Retention.class.getName()));
        if (retentionAnnotation != null) {
            Collection<CompileTimeConstant<?>> valueArguments = retentionAnnotation.getAllValueArguments().values();
            if (!valueArguments.isEmpty()) {
                CompileTimeConstant<?> compileTimeConstant = valueArguments.iterator().next();
                if (compileTimeConstant instanceof EnumValue) {
                    ClassDescriptor enumEntry = ((EnumValue) compileTimeConstant).getValue();
                    JetType classObjectType = getClassObjectType(enumEntry);
                    if (classObjectType != null) {
                        if ("java/lang/annotation/RetentionPolicy".equals(typeMapper.mapType(classObjectType).getInternalName())) {
                            return RetentionPolicy.valueOf(enumEntry.getName().asString());
                        }
                    }
                }
            }
        }

        return RetentionPolicy.RUNTIME;
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
